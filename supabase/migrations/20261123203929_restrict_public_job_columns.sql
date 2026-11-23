-- Published jobs are public, but private work addresses and employer identifiers
-- must not be exposed to anonymous clients through table-wide SELECT grants.
revoke select on table public.jobs from anon, authenticated;

grant select (id, title, description, category, district, upazila, agreed_wage_paisa, status, created_at)
  on public.jobs to anon;

grant select (id, title, description, category, district, upazila, agreed_wage_paisa, status, created_at, employer_id)
  on public.jobs to authenticated;
