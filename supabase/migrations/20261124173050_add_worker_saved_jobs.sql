-- Keep the worker saved-jobs schema in source control.
-- This migration is already applied to the Kajer Bazar Supabase project
-- as version 20261124173050; this file restores the matching repository history.

create table if not exists public.saved_jobs (
  user_id uuid not null references public.profiles(id) on delete cascade,
  job_id uuid not null references public.jobs(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, job_id)
);

alter table public.saved_jobs enable row level security;

create index if not exists saved_jobs_job_id_idx on public.saved_jobs(job_id);

drop policy if exists "workers read own saved jobs" on public.saved_jobs;
create policy "workers read own saved jobs" on public.saved_jobs
  for select to authenticated
  using (user_id = (select auth.uid()));

drop policy if exists "workers save own jobs" on public.saved_jobs;
create policy "workers save own jobs" on public.saved_jobs
  for insert to authenticated
  with check (
    user_id = (select auth.uid())
    and exists (
      select 1 from public.profiles p
      where p.id = (select auth.uid())
        and p.role = 'worker'
        and p.account_status = 'active'
    )
    and exists (
      select 1 from public.jobs j
      where j.id = job_id
        and j.status = 'published'
    )
  );

drop policy if exists "workers remove own saved jobs" on public.saved_jobs;
create policy "workers remove own saved jobs" on public.saved_jobs
  for delete to authenticated
  using (user_id = (select auth.uid()));

grant select, insert, delete on public.saved_jobs to authenticated;
