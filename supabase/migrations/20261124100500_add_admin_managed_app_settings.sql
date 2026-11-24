create table if not exists public.app_settings (
  id integer primary key default 1 check (id = 1),
  announcement text not null default '',
  support_phone text not null default '',
  maintenance_mode boolean not null default false,
  updated_at timestamptz not null default now(),
  updated_by uuid references auth.users(id)
);
insert into public.app_settings (id) values (1) on conflict (id) do nothing;
alter table public.app_settings enable row level security;
revoke all on public.app_settings from anon, authenticated;
grant select on public.app_settings to anon, authenticated;
grant update on public.app_settings to authenticated;
drop policy if exists "Public can read app settings" on public.app_settings;
create policy "Public can read app settings" on public.app_settings for select to anon, authenticated using (true);
drop policy if exists "Only admins can update app settings" on public.app_settings;
create policy "Only admins can update app settings" on public.app_settings for update to authenticated
using (exists (select 1 from public.profiles p where p.id = (select auth.uid()) and p.role = 'admin'))
with check (exists (select 1 from public.profiles p where p.id = (select auth.uid()) and p.role = 'admin'));
