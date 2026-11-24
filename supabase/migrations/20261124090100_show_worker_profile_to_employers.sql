-- Let employers see the work-relevant parts of each applicant's worker profile.
drop function if exists public.get_employer_applications(uuid);
create function public.get_employer_applications(p_job_id uuid)
returns table(
  application_id uuid, job_id uuid, worker_id uuid, message text,
  status public.application_status, applied_at timestamptz,
  worker_name text, worker_district text, worker_upazila text,
  worker_work_type text, worker_skills text, worker_experience text,
  worker_availability text, worker_expected_wage text, worker_avatar_url text
)
language plpgsql stable security definer set search_path to ''
as $function$
begin
  if auth.uid() is null then raise exception 'authentication_required' using errcode = '28000'; end if;
  if not exists (select 1 from public.jobs j where j.id = p_job_id and j.employer_id = auth.uid()) then
    raise exception 'job_owner_required' using errcode = '42501';
  end if;
  return query
    select a.id, a.job_id, a.worker_id, a.message, a.status, a.created_at,
      coalesce(p.full_name, ''), coalesce(p.district, ''), coalesce(p.upazila, ''),
      coalesce(p.work_type, ''), coalesce(p.skills, ''), coalesce(p.experience, ''),
      coalesce(p.availability, ''), coalesce(p.expected_wage, ''), coalesce(p.avatar_url, '')
    from public.applications a join public.profiles p on p.id = a.worker_id
    where a.job_id = p_job_id order by a.created_at asc;
end;
$function$;
revoke all on function public.get_employer_applications(uuid) from public, anon;
grant execute on function public.get_employer_applications(uuid) to authenticated;
