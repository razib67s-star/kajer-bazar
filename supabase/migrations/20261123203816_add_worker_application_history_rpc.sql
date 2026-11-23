-- Allow a worker to see only their own application history and current job status.
create or replace function public.get_worker_applications()
returns table (
  application_id uuid,
  job_id uuid,
  job_title text,
  district text,
  upazila text,
  agreed_wage_paisa bigint,
  application_status public.application_status,
  job_status public.job_status,
  applied_at timestamptz
)
language plpgsql
stable
security definer
set search_path to ''
as $function$
begin
  if auth.uid() is null then
    raise exception 'authentication_required' using errcode = '28000';
  end if;
  if not exists (
    select 1 from public.profiles p
    where p.id = auth.uid() and p.role = 'worker'
  ) then
    raise exception 'worker_account_required' using errcode = '42501';
  end if;

  return query
    select a.id, a.job_id, j.title, j.district, coalesce(j.upazila, ''),
           j.agreed_wage_paisa, a.status, j.status, a.created_at
    from public.applications a
    join public.jobs j on j.id = a.job_id
    where a.worker_id = auth.uid()
    order by a.created_at desc
    limit 100;
end;
$function$;

revoke all on function public.get_worker_applications() from public, anon;
grant execute on function public.get_worker_applications() to authenticated;
