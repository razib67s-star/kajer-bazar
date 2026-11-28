create schema if not exists private;
revoke all on schema private from public, anon, authenticated;

create or replace function private.notify_direct_message_insert()
returns trigger language plpgsql security definer set search_path = pg_catalog, public
as $$
begin
  if new.sender_id <> new.receiver_id then
    insert into public.notifications (recipient_id, actor_id, kind, title, body, target_type, target_id)
    values (new.receiver_id, new.sender_id, 'message', 'নতুন মেসেজ',
      case when new.body like '[[KAJER_BAZAR_IMAGE]]%' then 'আপনাকে একটি ছবি পাঠিয়েছেন' else left(new.body, 180) end,
      'message', new.id);
  end if;
  return new;
end;
$$;

create or replace function private.notify_job_alert_matches()
returns trigger language plpgsql security definer set search_path = pg_catalog, public
as $$
begin
  if new.status::text <> 'published' then return new; end if;
  if tg_op = 'UPDATE' and old.status::text = 'published' then return new; end if;
  insert into public.notifications (recipient_id, actor_id, kind, title, body, target_type, target_id)
  select a.user_id, new.employer_id, 'job', 'নতুন কাজের খবর',
    left(new.title || ' — ' || new.district, 180), 'job', new.id
  from public.job_alerts a
  where a.active = true and a.user_id <> new.employer_id
    and (a.category is null or a.category = '' or lower(a.category) = lower(new.category))
    and (a.district is null or a.district = '' or lower(a.district) = lower(new.district))
    and (a.upazila is null or a.upazila = '' or lower(a.upazila) = lower(coalesce(new.upazila, '')))
    and (a.keyword is null or a.keyword = '' or position(lower(a.keyword) in lower(new.title || ' ' || new.description)) > 0);
  return new;
end;
$$;

create or replace function private.notify_media_like_insert()
returns trigger language plpgsql security definer set search_path = pg_catalog, public
as $$
declare post_owner uuid;
begin
  select user_id into post_owner from public.media_posts where id = new.post_id;
  if post_owner is not null and post_owner <> new.user_id then
    insert into public.notifications (recipient_id, actor_id, kind, title, body, target_type, target_id)
    values (post_owner, new.user_id, 'like', 'আপনার পোস্টে লাইক', 'কেউ আপনার পোস্টে লাইক দিয়েছেন।', 'post', new.post_id);
  end if;
  return new;
end;
$$;

create or replace function private.notify_media_comment_insert()
returns trigger language plpgsql security definer set search_path = pg_catalog, public
as $$
declare post_owner uuid;
begin
  select user_id into post_owner from public.media_posts where id = new.post_id;
  if post_owner is not null and post_owner <> new.user_id then
    insert into public.notifications (recipient_id, actor_id, kind, title, body, target_type, target_id)
    values (post_owner, new.user_id, 'comment', 'আপনার পোস্টে মন্তব্য', left(new.body, 180), 'post', new.post_id);
  end if;
  return new;
end;
$$;

revoke all on function private.notify_direct_message_insert() from public, anon, authenticated;
revoke all on function private.notify_job_alert_matches() from public, anon, authenticated;
revoke all on function private.notify_media_like_insert() from public, anon, authenticated;
revoke all on function private.notify_media_comment_insert() from public, anon, authenticated;

drop trigger if exists direct_messages_create_notification on public.direct_messages;
create trigger direct_messages_create_notification after insert on public.direct_messages
for each row execute function private.notify_direct_message_insert();
drop trigger if exists jobs_notify_matching_alerts on public.jobs;
create trigger jobs_notify_matching_alerts after insert or update of status on public.jobs
for each row execute function private.notify_job_alert_matches();
drop trigger if exists media_likes_create_notification on public.media_post_likes;
create trigger media_likes_create_notification after insert on public.media_post_likes
for each row execute function private.notify_media_like_insert();
drop trigger if exists media_comments_create_notification on public.media_post_comments;
create trigger media_comments_create_notification after insert on public.media_post_comments
for each row execute function private.notify_media_comment_insert();
