create or replace function public.apply_to_job(p_job_id uuid, p_message text default null)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare v_application uuid;
begin
  if auth.uid() is null then raise exception 'authentication_required' using errcode = '28000'; end if;
  if not exists(select 1 from public.profiles p where p.id = auth.uid() and p.role = 'worker') then
    raise exception 'worker_account_required' using errcode = '42501';
  end if;
  if p_message is not null and char_length(p_message) > 2000 then
    raise exception 'application_message_too_long';
  end if;
  insert into public.applications(job_id,worker_id,message)
  select j.id, auth.uid(), nullif(btrim(p_message),'')
  from public.jobs j where j.id = p_job_id and j.status = 'published'
  returning id into v_application;
  if v_application is null then raise exception 'job_not_available'; end if;
  return v_application;
exception
  when unique_violation then raise exception 'already_applied';
end;
$$;
revoke all on function public.apply_to_job(uuid,text) from public, anon;
grant execute on function public.apply_to_job(uuid,text) to authenticated;
