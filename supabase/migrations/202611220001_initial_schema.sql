-- Initial secure schema for Kajer Bazar (Supabase/PostgreSQL)
-- Apply through Supabase SQL Editor or the Supabase CLI.
-- Never put the service_role key in the Android app.

create extension if not exists pgcrypto;

create type public.account_role as enum ('worker', 'employer', 'admin');
create type public.job_status as enum ('draft', 'pending_review', 'published', 'paused', 'closed', 'in_progress', 'completed', 'disputed', 'admin_review');
create type public.application_status as enum ('submitted', 'withdrawn', 'selected', 'rejected');

create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text not null default '',
  phone text,
  district text,
  upazila text,
  role public.account_role not null default 'worker',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- Client-created accounts always start as workers.
-- Employer/admin role changes must be performed by trusted server/admin tooling.
create or replace function public.force_default_profile_role()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  new.role := 'worker'::public.account_role;
  return new;
end;
$$;

create trigger profiles_force_default_role
before insert on public.profiles
for each row execute function public.force_default_profile_role();

-- Create a minimal profile automatically after Supabase Auth creates an account.
create or replace function public.handle_new_auth_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.profiles (id, full_name)
  values (new.id, coalesce(new.raw_user_meta_data ->> 'full_name', ''));
  return new;
end;
$$;

create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_auth_user();

create table public.jobs (
  id uuid primary key default gen_random_uuid(),
  employer_id uuid not null references public.profiles(id) on delete restrict,
  title text not null check (char_length(title) between 5 and 120),
  description text not null check (char_length(description) between 20 and 5000),
  category text not null,
  district text not null,
  upazila text,
  work_address text,
  agreed_wage_paisa bigint not null check (agreed_wage_paisa > 0),
  status public.job_status not null default 'draft',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table public.applications (
  id uuid primary key default gen_random_uuid(),
  job_id uuid not null references public.jobs(id) on delete cascade,
  worker_id uuid not null references public.profiles(id) on delete restrict,
  message text check (message is null or char_length(message) <= 2000),
  status public.application_status not null default 'submitted',
  created_at timestamptz not null default now(),
  unique (job_id, worker_id)
);

create table public.completion_requests (
  id uuid primary key default gen_random_uuid(),
  job_id uuid not null references public.jobs(id) on delete restrict,
  worker_id uuid not null references public.profiles(id) on delete restrict,
  employer_id uuid not null references public.profiles(id) on delete restrict,
  worker_confirmed_at timestamptz not null default now(),
  employer_response_due_at timestamptz not null default (now() + interval '3 days'),
  employer_confirmed_at timestamptz,
  disputed_at timestamptz,
  review_status text not null default 'awaiting_employer'
    check (review_status in ('awaiting_employer', 'completed', 'dispute_review', 'admin_review')),
  created_at timestamptz not null default now()
);

create index jobs_public_search_idx on public.jobs(status, district, category, created_at desc);
create index applications_job_idx on public.applications(job_id, created_at desc);
create index completion_due_idx on public.completion_requests(employer_response_due_at)
  where review_status = 'awaiting_employer';

create or replace function public.set_updated_at()
returns trigger language plpgsql
set search_path = ''
as $$
begin
  new.updated_at := now();
  return new;
end;
$$;

create trigger profiles_updated_at before update on public.profiles
for each row execute function public.set_updated_at();
create trigger jobs_updated_at before update on public.jobs
for each row execute function public.set_updated_at();

-- These functions are invoked by database triggers, not directly by API clients.
revoke execute on function public.force_default_profile_role() from public, anon, authenticated;
revoke execute on function public.handle_new_auth_user() from public, anon, authenticated;
revoke execute on function public.rls_auto_enable() from public, anon, authenticated;

alter table public.profiles enable row level security;
alter table public.jobs enable row level security;
alter table public.applications enable row level security;
alter table public.completion_requests enable row level security;

-- Profiles: users can read/update only their own basic profile.
create policy "profile owner can read" on public.profiles
for select to authenticated using (id = (select auth.uid()));
create policy "profile owner can update basic fields" on public.profiles
for update to authenticated using (id = (select auth.uid()))
with check (id = (select auth.uid()));

-- Prevent clients from changing account role.
revoke update on public.profiles from authenticated;
grant update (full_name, phone, district, upazila) on public.profiles to authenticated;

-- Anyone signed in may browse published jobs. Employers may manage their own jobs.
create policy "published jobs readable" on public.jobs
for select to authenticated
using (status = 'published' or employer_id = (select auth.uid()));
create policy "employers create own draft jobs" on public.jobs
for insert to authenticated
with check (
  employer_id = (select auth.uid())
  and status = 'draft'
  and exists (
    select 1 from public.profiles p
    where p.id = (select auth.uid()) and p.role = 'employer'
  )
);
create policy "employers update own draft jobs" on public.jobs
for update to authenticated
using (
  employer_id = (select auth.uid())
  and status in ('draft', 'paused')
  and exists (
    select 1 from public.profiles p
    where p.id = (select auth.uid()) and p.role = 'employer'
  )
)
with check (
  employer_id = (select auth.uid())
  and status in ('draft', 'paused')
);

-- Workers may submit an application only for a published job and only as themselves.
create policy "workers apply to published jobs" on public.applications
for insert to authenticated
with check (
  worker_id = (select auth.uid())
  and exists (
    select 1 from public.profiles p
    where p.id = (select auth.uid()) and p.role = 'worker'
  )
  and exists (
    select 1 from public.jobs j
    where j.id = job_id and j.status = 'published'
  )
);
create policy "worker and job owner can read applications" on public.applications
for select to authenticated
using (
  worker_id = (select auth.uid())
  or exists (
    select 1 from public.jobs j
    where j.id = job_id and j.employer_id = (select auth.uid())
  )
);

-- Completion/dispute records are deliberately not client-writable yet.
-- A trusted backend function must enforce both-party confirmation and the 3-day Admin Review rule.
create policy "participants can read completion requests" on public.completion_requests
for select to authenticated
using (worker_id = (select auth.uid()) or employer_id = (select auth.uid()));

-- No client INSERT/UPDATE policies for completion_requests or payment records.
-- Do not add payment settlement until server-side workflow and gateway reconciliation are tested.
