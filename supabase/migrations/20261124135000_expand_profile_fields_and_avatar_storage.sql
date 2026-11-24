-- Expand profiles and add per-user profile-photo uploads.
alter table public.profiles
  add column if not exists contact_phone text,
  add column if not exists current_address text,
  add column if not exists permanent_address text,
  add column if not exists work_type text,
  add column if not exists skills text,
  add column if not exists avatar_url text;

create or replace function public.update_my_profile(
  p_full_name text, p_contact_phone text, p_district text, p_upazila text,
  p_current_address text, p_permanent_address text, p_work_type text, p_skills text, p_avatar_url text
)
returns void language plpgsql security definer set search_path to ''
as $function$
declare v_uid uuid := auth.uid();
begin
  if v_uid is null then raise exception 'authentication_required' using errcode = '28000'; end if;
  if char_length(btrim(coalesce(p_full_name, ''))) < 2 or char_length(btrim(coalesce(p_full_name, ''))) > 100 then
    raise exception 'invalid_full_name';
  end if;
  if char_length(coalesce(p_contact_phone, '')) > 20
     or char_length(coalesce(p_district, '')) > 100
     or char_length(coalesce(p_upazila, '')) > 100
     or char_length(coalesce(p_current_address, '')) > 300
     or char_length(coalesce(p_permanent_address, '')) > 300
     or char_length(coalesce(p_work_type, '')) > 100
     or char_length(coalesce(p_skills, '')) > 1000
     or char_length(coalesce(p_avatar_url, '')) > 500 then
    raise exception 'profile_field_too_long';
  end if;
  if nullif(btrim(coalesce(p_avatar_url, '')), '') is not null
     and position('/storage/v1/object/public/avatars/' || v_uid::text || '/' in p_avatar_url) = 0 then
    raise exception 'invalid_avatar_url';
  end if;
  update public.profiles
  set full_name = btrim(p_full_name),
      contact_phone = nullif(btrim(coalesce(p_contact_phone, '')), ''),
      district = nullif(btrim(coalesce(p_district, '')), ''),
      upazila = nullif(btrim(coalesce(p_upazila, '')), ''),
      current_address = nullif(btrim(coalesce(p_current_address, '')), ''),
      permanent_address = nullif(btrim(coalesce(p_permanent_address, '')), ''),
      work_type = nullif(btrim(coalesce(p_work_type, '')), ''),
      skills = nullif(btrim(coalesce(p_skills, '')), ''),
      avatar_url = nullif(btrim(coalesce(p_avatar_url, '')), ''),
      updated_at = now()
  where id = v_uid;
  if not found then raise exception 'profile_not_found'; end if;
end;
$function$;
revoke all on function public.update_my_profile(text,text,text,text,text,text,text,text,text) from public, anon;
grant execute on function public.update_my_profile(text,text,text,text,text,text,text,text,text) to authenticated;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('avatars', 'avatars', true, 3145728, array['image/jpeg','image/png','image/webp'])
on conflict (id) do update set public = excluded.public,
  file_size_limit = excluded.file_size_limit, allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "avatars_select_own" on storage.objects;
create policy "avatars_select_own" on storage.objects for select to authenticated
using (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid())::text);
drop policy if exists "avatars_insert_own" on storage.objects;
create policy "avatars_insert_own" on storage.objects for insert to authenticated
with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid())::text);
drop policy if exists "avatars_update_own" on storage.objects;
create policy "avatars_update_own" on storage.objects for update to authenticated
using (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid())::text)
with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid())::text);
drop policy if exists "avatars_delete_own" on storage.objects;
create policy "avatars_delete_own" on storage.objects for delete to authenticated
using (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid())::text);
