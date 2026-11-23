-- Permit a signed-in user to update only their own basic profile fields.
-- Account role and phone authentication identity are not client-editable.
create or replace function public.update_my_profile(
  p_full_name text,
  p_district text,
  p_upazila text
)
returns void
language plpgsql
security definer
set search_path to ''
as $function$
begin
  if auth.uid() is null then
    raise exception 'authentication_required' using errcode = '28000';
  end if;
  if char_length(btrim(coalesce(p_full_name, ''))) < 2
     or char_length(btrim(coalesce(p_full_name, ''))) > 100 then
    raise exception 'invalid_full_name';
  end if;
  if char_length(coalesce(p_district, '')) > 100
     or char_length(coalesce(p_upazila, '')) > 100 then
    raise exception 'invalid_location';
  end if;

  update public.profiles
  set full_name = btrim(p_full_name),
      district = nullif(btrim(coalesce(p_district, '')), ''),
      upazila = nullif(btrim(coalesce(p_upazila, '')), '')
  where id = auth.uid();

  if not found then
    raise exception 'profile_not_found';
  end if;
end;
$function$;

revoke all on function public.update_my_profile(text, text, text) from public, anon;
grant execute on function public.update_my_profile(text, text, text) to authenticated;
