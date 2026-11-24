-- Allow the community news feed to publish text-only posts.
alter table public.media_posts
  alter column storage_path drop not null;

alter table public.media_posts
  drop constraint if exists media_posts_media_type_check;

alter table public.media_posts
  add constraint media_posts_media_type_check
  check (media_type in ('image', 'video', 'text'));
