-- 24-hour text stories for the social feed.
create table if not exists public.user_stories (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  body text not null check (char_length(body) between 1 and 500),
  created_at timestamptz not null default now(),
  expires_at timestamptz not null default (now() + interval '24 hours'),
  constraint user_stories_expiry_window check (expires_at > created_at and expires_at <= created_at + interval '24 hours 5 minutes')
);

create index if not exists user_stories_active_idx
  on public.user_stories (expires_at desc, created_at desc);

alter table public.user_stories enable row level security;

drop policy if exists "Active stories are publicly readable" on public.user_stories;
create policy "Active stories are publicly readable"
  on public.user_stories for select to anon, authenticated
  using (expires_at > now());

drop policy if exists "Users can publish own stories" on public.user_stories;
create policy "Users can publish own stories"
  on public.user_stories for insert to authenticated
  with check ((select auth.uid()) = user_id
    and expires_at > now()
    and expires_at <= now() + interval '24 hours 5 minutes');

drop policy if exists "Users can delete own stories" on public.user_stories;
create policy "Users can delete own stories"
  on public.user_stories for delete to authenticated
  using ((select auth.uid()) = user_id);

grant select on public.user_stories to anon, authenticated;
grant insert, delete on public.user_stories to authenticated;
