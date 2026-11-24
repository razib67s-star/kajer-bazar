-- Community camera photos, worker showcase videos and public media feed.
create table if not exists public.media_posts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  media_type text not null check (media_type in ('image','video')),
  storage_path text not null,
  caption text not null default '' check (char_length(caption) <= 500),
  created_at timestamptz not null default now()
);
alter table public.media_posts enable row level security;

drop policy if exists "Published media is visible to everyone" on public.media_posts;
create policy "Published media is visible to everyone"
on public.media_posts for select to anon, authenticated using (true);
drop policy if exists "Users can publish their own media" on public.media_posts;
create policy "Users can publish their own media"
on public.media_posts for insert to authenticated
with check ((select auth.uid()) = user_id);
drop policy if exists "Users can delete their own media" on public.media_posts;
create policy "Users can delete their own media"
on public.media_posts for delete to authenticated
using ((select auth.uid()) = user_id);
grant select on public.media_posts to anon, authenticated;
grant insert, delete on public.media_posts to authenticated;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('community-media','community-media',true,52428800,
  array['image/jpeg','image/png','image/webp','video/mp4','video/quicktime','video/3gpp'])
on conflict (id) do update set public = true, file_size_limit = 52428800,
  allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "Community media is publicly readable" on storage.objects;
create policy "Community media is publicly readable"
on storage.objects for select to anon, authenticated
using (bucket_id = 'community-media');
drop policy if exists "Users upload media into their own folder" on storage.objects;
create policy "Users upload media into their own folder"
on storage.objects for insert to authenticated
with check (bucket_id = 'community-media' and (storage.foldername(name))[1] = (select auth.uid())::text);
drop policy if exists "Users delete their own community media" on storage.objects;
create policy "Users delete their own community media"
on storage.objects for delete to authenticated
using (bucket_id = 'community-media' and (storage.foldername(name))[1] = (select auth.uid())::text);
