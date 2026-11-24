-- Add distinct public profile fields for workers and employers.
alter table public.profiles
  add column if not exists organization_name text,
  add column if not exists organization_type text,
  add column if not exists organization_details text,
  add column if not exists contact_person text,
  add column if not exists experience text,
  add column if not exists availability text,
  add column if not exists expected_wage text,
  add column if not exists portfolio text;

create or replace function public.update_role_profile(
  p_organization_name text,
  p_organization_type text,
  p_organization_details text,
  p_contact_person text,
  p_experience text,
  p_availability text,
  p_expected_wage text,
  p_portfolio text
)
returns void language plpgsql security definer set search_path to ''
as $function$
declare v_uid uuid := auth.uid(); v_role public.account_role;
begin
  if v_uid is null then raise exception 'authentication_required' using errcode = '28000'; end if;
  select role into v_role from public.profiles where id = v_uid;
  if v_role is null then raise exception 'profile_not_found'; end if;
  if char_length(coalesce(p_organization_name, '')) > 160
     or char_length(coalesce(p_organization_type, '')) > 100
     or char_length(coalesce(p_organization_details, '')) > 1500
     or char_length(coalesce(p_contact_person, '')) > 100
     or char_length(coalesce(p_experience, '')) > 1000
     or char_length(coalesce(p_availability, '')) > 100
     or char_length(coalesce(p_expected_wage, '')) > 100
     or char_length(coalesce(p_portfolio, '')) > 1500 then
    raise exception 'role_profile_field_too_long';
  end if;
  if v_role = 'employer' then
    update public.profiles
    set organization_name = nullif(btrim(coalesce(p_organization_name, '')), ''),
        organization_type = nullif(btrim(coalesce(p_organization_type, '')), ''),
        organization_details = nullif(btrim(coalesce(p_organization_details, '')), ''),
        contact_person = nullif(btrim(coalesce(p_contact_person, '')), ''),
        updated_at = now()
    where id = v_uid;
  elsif v_role = 'worker' then
    update public.profiles
    set experience = nullif(btrim(coalesce(p_experience, '')), ''),
        availability = nullif(btrim(coalesce(p_availability, '')), ''),
        expected_wage = nullif(btrim(coalesce(p_expected_wage, '')), ''),
        portfolio = nullif(btrim(coalesce(p_portfolio, '')), ''),
        updated_at = now()
    where id = v_uid;
  else raise exception 'unsupported_profile_role';
  end if;
end;
$function$;
revoke all on function public.update_role_profile(text,text,text,text,text,text,text,text) from public, anon;
grant execute on function public.update_role_profile(text,text,text,text,text,text,text,text) to authenticated;
comment on column public.profiles.organization_name is 'Employer-facing organization or business name.';
comment on column public.profiles.experience is 'Worker-facing experience summary.';
