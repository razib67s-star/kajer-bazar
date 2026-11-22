-- Admin-only review access and employer approval action.
create policy "admins can read all jobs" on public.jobs
for select to authenticated using (public.is_current_user_admin());

create policy "admins can read all applications" on public.applications
for select to authenticated using (public.is_current_user_admin());

create policy "admins can read all completion requests" on public.completion_requests
for select to authenticated using (public.is_current_user_admin());

create or replace function public.admin_review_employer_request(p_request_id uuid, p_approve boolean)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare v_user_id uuid;
begin
  if not public.is_current_user_admin() then raise exception 'admin_required' using errcode = '42501'; end if;
  select user_id into v_user_id from public.employer_requests
  where id = p_request_id and status = 'pending' for update;
  if v_user_id is null then raise exception 'employer_request_not_pending'; end if;
  update public.employer_requests
  set status = case when p_approve then 'approved' else 'rejected' end,
      reviewed_by = auth.uid(), reviewed_at = now()
  where id = p_request_id;
  if p_approve then
    update public.profiles set role = 'employer' where id = v_user_id and role = 'worker';
  end if;
end;
$$;
revoke all on function public.admin_review_employer_request(uuid,boolean) from public, anon;
grant execute on function public.admin_review_employer_request(uuid,boolean) to authenticated;
