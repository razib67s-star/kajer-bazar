-- Facebook-style public pages for Kajer Bazar.
create table if not exists public.community_pages (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  name text not null check (char_length(trim(name)) between 2 and 80),
  slug text not null unique check (char_length(slug) between 2 and 100),
  category text not null default 'সাধারণ',
  description text not null default '' check (char_length(description) <= 1000),
  avatar_url text not null default '',
  cover_url text not null default '',
  created_at timestamptz not null default now()
);
create index if not exists community_pages_owner_idx on public.community_pages(owner_id);
alter table public.community_pages enable row level security;
drop policy if exists "Pages are public" on public.community_pages;
create policy "Pages are public" on public.community_pages for select to anon, authenticated using (true);
drop policy if exists "Users create pages for themselves" on public.community_pages;
create policy "Users create pages for themselves" on public.community_pages for insert to authenticated with check ((select auth.uid()) = owner_id);
drop policy if exists "Owners update own pages" on public.community_pages;
create policy "Owners update own pages" on public.community_pages for update to authenticated using ((select auth.uid()) = owner_id) with check ((select auth.uid()) = owner_id);
drop policy if exists "Owners delete own pages" on public.community_pages;
create policy "Owners delete own pages" on public.community_pages for delete to authenticated using ((select auth.uid()) = owner_id);
grant select on public.community_pages to anon, authenticated;
grant insert, update, delete on public.community_pages to authenticated;

create table if not exists public.community_page_followers (
  page_id uuid not null references public.community_pages(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key(page_id,user_id)
);
create index if not exists community_page_followers_user_idx on public.community_page_followers(user_id);
alter table public.community_page_followers enable row level security;
drop policy if exists "Page follower lists are public" on public.community_page_followers;
create policy "Page follower lists are public" on public.community_page_followers for select to anon, authenticated using (true);
drop policy if exists "Users follow pages as themselves" on public.community_page_followers;
create policy "Users follow pages as themselves" on public.community_page_followers for insert to authenticated with check ((select auth.uid()) = user_id);
drop policy if exists "Users unfollow pages as themselves" on public.community_page_followers;
create policy "Users unfollow pages as themselves" on public.community_page_followers for delete to authenticated using ((select auth.uid()) = user_id);
grant select on public.community_page_followers to anon, authenticated;
grant insert, delete on public.community_page_followers to authenticated;

create table if not exists public.community_page_posts (
  id uuid primary key default gen_random_uuid(),
  page_id uuid not null references public.community_pages(id) on delete cascade,
  author_id uuid not null references auth.users(id) on delete cascade,
  body text not null check (char_length(trim(body)) between 1 and 2000),
  created_at timestamptz not null default now()
);
create index if not exists community_page_posts_feed_idx on public.community_page_posts(created_at desc);
create index if not exists community_page_posts_page_idx on public.community_page_posts(page_id,created_at desc);
alter table public.community_page_posts enable row level security;
drop policy if exists "Page posts are public" on public.community_page_posts;
create policy "Page posts are public" on public.community_page_posts for select to anon, authenticated using (true);
drop policy if exists "Page owners publish as page" on public.community_page_posts;
create policy "Page owners publish as page" on public.community_page_posts for insert to authenticated
with check ((select auth.uid()) = author_id and exists (select 1 from public.community_pages p where p.id=page_id and p.owner_id=(select auth.uid())));
drop policy if exists "Page owners delete page posts" on public.community_page_posts;
create policy "Page owners delete page posts" on public.community_page_posts for delete to authenticated
using (exists (select 1 from public.community_pages p where p.id=page_id and p.owner_id=(select auth.uid())));
grant select on public.community_page_posts to anon, authenticated;
grant insert, delete on public.community_page_posts to authenticated;
