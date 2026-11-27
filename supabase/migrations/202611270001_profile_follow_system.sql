-- Social follow graph for Kajer Bazar public profiles.
create table if not exists public.profile_follows (
  follower_id uuid not null references auth.users(id) on delete cascade,
  followed_id uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (follower_id, followed_id),
  constraint profile_follows_not_self check (follower_id <> followed_id)
);
create index if not exists profile_follows_followed_idx on public.profile_follows(followed_id, created_at desc);
alter table public.profile_follows enable row level security;
drop policy if exists "Follow relationships visible to everyone" on public.profile_follows;
create policy "Follow relationships visible to everyone" on public.profile_follows
  for select to anon, authenticated using (true);
drop policy if exists "Users follow from their own account" on public.profile_follows;
create policy "Users follow from their own account" on public.profile_follows
  for insert to authenticated with check ((select auth.uid()) = follower_id and follower_id <> followed_id);
drop policy if exists "Users unfollow from their own account" on public.profile_follows;
create policy "Users unfollow from their own account" on public.profile_follows
  for delete to authenticated using ((select auth.uid()) = follower_id);
grant select on public.profile_follows to anon, authenticated;
grant insert, delete on public.profile_follows to authenticated;
