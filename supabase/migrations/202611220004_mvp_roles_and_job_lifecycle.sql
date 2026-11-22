-- Kajer Bazar MVP: secure role requests and job lifecycle RPCs.
-- New accounts remain workers until an administrator approves an employer request.
create or replace function public.handle_new_auth_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.profiles (id, full_name, phone)
  values (
    new.id,
    coalesce(new.raw_user_meta_data ->> 'full_name', ''),
    new.phone
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

create table if not exists public.employer_requests (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  business_name text not null check (char_length(business_name) between 2 and 120),
  contact_name text not null check (char_length(contact_name) between 2 and 120),
  district text not null,
  upazila text not null,
  details text check (details is null or char_length(details) <= 1000),
  status text not null default 'pending' check (status in ('pending','approved','rejected')),
  reviewed_by uuid references public.profiles(id),
  reviewed_at timestamptz,
  created_at timestamptz not null default now()
);
create unique index if not exists employer_requests_one_pending_per_user
  on public.employer_requests(user_id) where status = 'pending';
alter table public.employer_requests enable row level security;
create policy "users read own employer requests" on public.employer_requests
  for select to authenticated using (user_id = (select auth.uid()));
create policy "workers submit employer requests" on public.employer_requests
  for insert to authenticated with check (
    user_id = (select auth.uid())
    and exists (select 1 from public.profiles p where p.id = (select auth.uid()) and p.role = 'worker')
  );
create policy "admins read employer requests" on public.employer_requests
  for select to authenticated using (
    exists (select 1 from public.profiles p where p.id = (select auth.uid()) and p.role = 'admin')
  );

create or replace function public.is_current_user_admin()
returns boolean
language sql stable security definer
set search_path = ''
as $$
  select exists (select 1 from public.profiles p where p.id = (select auth.uid()) and p.role = 'admin');
$$;
revoke all on function public.is_current_user_admin() from public, anon;
grant execute on function public.is_current_user_admin() to authenticated;

create or replace function public.submit_employer_request(
  p_business_name text, p_contact_name text, p_district text, p_upazila text, p_details text default null
)
returns uuid
language plpgsql security definer
set search_path = ''
as $$
declare v_id uuid;
begin
  if auth.uid() is null then raise exception 'authentication_required' using errcode = '28000'; end if;
  if not exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'worker') then
    raise exception 'worker_account_required' using errcode = '42501';
  end if;
  insert into public.employer_requests(user_id,business_name,contact_name,district,upazila,details)
  values(auth.uid(), btrim(p_business_name), btrim(p_contact_name), btrim(p_district), btrim(p_upazila), nullif(btrim(p_details),''))
  returning id into v_id;
  return v_id;
end;
$$;

create or replace function public.publish_job_for_review(p_job_id uuid)
returns void
language plpgsql security definer
set search_path = ''
as $$
begin
  if auth.uid() is null then raise exception 'authentication_required' using errcode = '28000'; end if;
  update public.jobs
  set status = 'pending_review'
  where id = p_job_id and employer_id = auth.uid() and status = 'draft'
    and exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'employer');
  if not found then raise exception 'job_not_publishable' using errcode = '42501'; end if;
end;
$$;

create or replace function public.admin_review_job(p_job_id uuid, p_approve boolean)
returns void
language plpgsql security definer
set search_path = ''
as $$
begin
  if not public.is_current_user_admin() then raise exception 'admin_required' using errcode = '42501'; end if;
  update public.jobs set status = case when p_approve then 'published'::public.job_status else 'closed'::public.job_status end
  where id = p_job_id and status = 'pending_review';
  if not found then raise exception 'job_not_pending_review'; end if;
end;
$$;

create or replace function public.select_job_application(p_application_id uuid)
returns void
language plpgsql security definer
set search_path = ''
as $$
declare v_job_id uuid;
begin
  if auth.uid() is null then raise exception 'authentication_required' using errcode = '28000'; end if;
  select a.job_id into v_job_id
  from public.applications a join public.jobs j on j.id = a.job_id
  where a.id = p_application_id and j.employer_id = auth.uid() and j.status = 'published'
  for update of j;
  if v_job_id is null then raise exception 'application_not_selectable' using errcode = '42501'; end if;
  update public.applications set status = 'rejected'
    where job_id = v_job_id and status = 'submitted' and id <> p_application_id;
  update public.applications set status = 'selected'
    where id = p_application_id and status = 'submitted';
  if not found then raise exception 'application_not_submitted'; end if;
  update public.jobs set status = 'in_progress' where id = v_job_id;
end;
$$;

create or replace function public.withdraw_job_application(p_application_id uuid)
returns void
language plpgsql security definer
set search_path = ''
as $$
begin
  update public.applications set status = 'withdrawn'
  where id = p_application_id and worker_id = auth.uid() and status = 'submitted';
  if not found then raise exception 'application_not_withdrawable' using errcode = '42501'; end if;
end;
$$;

create or replace function public.request_job_completion(p_job_id uuid)
returns uuid
language plpgsql security definer
set search_path = ''
as $$
declare v_employer uuid; v_request uuid;
begin
  if auth.uid() is null then raise exception 'authentication_required' using errcode = '28000'; end if;
  select j.employer_id into v_employer
  from public.jobs j join public.applications a on a.job_id = j.id
  where j.id = p_job_id and j.status = 'in_progress'
    and a.worker_id = auth.uid() and a.status = 'selected'
  for update of j;
  if v_employer is null then raise exception 'selected_worker_required' using errcode = '42501'; end if;
  if exists(select 1 from public.completion_requests c where c.job_id = p_job_id and c.review_status in ('awaiting_employer','dispute_review','admin_review')) then
    raise exception 'completion_request_already_active';
  end if;
  insert into public.completion_requests(job_id,worker_id,employer_id)
  values(p_job_id,auth.uid(),v_employer) returning id into v_request;
  return v_request;
end;
$$;

create or replace function public.respond_job_completion(p_request_id uuid, p_confirm boolean)
returns void
language plpgsql security definer
set search_path = ''
as $$
declare v_job uuid;
begin
  if auth.uid() is null then raise exception 'authentication_required' using errcode = '28000'; end if;
  select c.job_id into v_job from public.completion_requests c
  where c.id = p_request_id and c.employer_id = auth.uid() and c.review_status = 'awaiting_employer'
  for update;
  if v_job is null then raise exception 'completion_request_not_available' using errcode = '42501'; end if;
  if p_confirm then
    update public.completion_requests set employer_confirmed_at = now(), review_status = 'completed' where id = p_request_id;
    update public.jobs set status = 'completed' where id = v_job and status = 'in_progress';
  else
    update public.completion_requests set disputed_at = now(), review_status = 'dispute_review' where id = p_request_id;
    update public.jobs set status = 'disputed' where id = v_job and status = 'in_progress';
  end if;
end;
$$;

create or replace function public.move_overdue_completions_to_admin_review()
returns integer
language plpgsql security definer
set search_path = ''
as $$
declare v_count integer;
begin
  if not public.is_current_user_admin() then raise exception 'admin_required' using errcode = '42501'; end if;
  update public.completion_requests
  set review_status = 'admin_review'
  where review_status = 'awaiting_employer' and employer_response_due_at < now();
  get diagnostics v_count = row_count;
  update public.jobs j set status = 'admin_review'
  where j.status = 'in_progress' and exists (
    select 1 from public.completion_requests c where c.job_id = j.id and c.review_status = 'admin_review'
  );
  return v_count;
end;
$$;

revoke all on function public.submit_employer_request(text,text,text,text,text) from public, anon;
revoke all on function public.publish_job_for_review(uuid) from public, anon;
revoke all on function public.admin_review_job(uuid,boolean) from public, anon;
revoke all on function public.select_job_application(uuid) from public, anon;
revoke all on function public.withdraw_job_application(uuid) from public, anon;
revoke all on function public.request_job_completion(uuid) from public, anon;
revoke all on function public.respond_job_completion(uuid,boolean) from public, anon;
revoke all on function public.move_overdue_completions_to_admin_review() from public, anon;
grant execute on function public.submit_employer_request(text,text,text,text,text) to authenticated;
grant execute on function public.publish_job_for_review(uuid) to authenticated;
grant execute on function public.admin_review_job(uuid,boolean) to authenticated;
grant execute on function public.select_job_application(uuid) to authenticated;
grant execute on function public.withdraw_job_application(uuid) to authenticated;
grant execute on function public.request_job_completion(uuid) to authenticated;
grant execute on function public.respond_job_completion(uuid,boolean) to authenticated;
grant execute on function public.move_overdue_completions_to_admin_review() to authenticated;
