-- Facebook-style profile cover photos and follower/following counters.
alter table public.profiles add column if not exists cover_url text;

create or replace function public.update_my_cover(p_cover_url text)
returns void language plpgsql security definer set search_path to ''
as $function$
declare v_uid uuid := auth.uid();
begin
  if v_uid is null then raise exception 'authentication_required' using errcode = '28000'; end if;
  if p_cover_url is null or char_length(p_cover_url) > 600
     or position('/storage/v1/object/public/avatars/' || v_uid::text || '/cover.' in p_cover_url) = 0 then
    raise exception 'invalid_cover_url';
  end if;
  update public.profiles set cover_url = p_cover_url, updated_at = now() where id = v_uid;
  if not found then raise exception 'profile_not_found'; end if;
end;
$function$;
revoke all on function public.update_my_cover(text) from public, anon;
grant execute on function public.update_my_cover(text) to authenticated;

create or replace function public.get_profile_follow_counts(p_user_id uuid)
returns table(follower_count bigint, following_count bigint)
language sql stable security definer set search_path to ''
as $function$
  select
    (select count(*) from public.user_follows where following_id = p_user_id),
    (select count(*) from public.user_follows where follower_id = p_user_id)
  where auth.uid() is not null;
$function$;
revoke all on function public.get_profile_follow_counts(uuid) from public, anon;
grant execute on function public.get_profile_follow_counts(uuid) to authenticated;
