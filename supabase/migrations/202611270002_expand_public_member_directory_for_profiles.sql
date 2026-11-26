-- Keep the public member directory in sync with social profile cover and worker details.
alter table public.messaging_directory
  add column if not exists cover_url text,
  add column if not exists work_type text,
  add column if not exists skills text,
  add column if not exists experience text,
  add column if not exists availability text,
  add column if not exists expected_wage text;

create or replace function private.sync_messaging_directory()
returns trigger language plpgsql security definer set search_path to 'public', 'pg_temp'
as $function$
begin
  if TG_OP = 'DELETE' then
    delete from public.messaging_directory where id = OLD.id;
    return OLD;
  end if;
  if NEW.account_status = 'active' then
    insert into public.messaging_directory(
      id, full_name, role, district, upazila, avatar_url, cover_url,
      work_type, skills, experience, availability, expected_wage, updated_at
    )
    values (
      NEW.id, NEW.full_name, NEW.role::text, NEW.district, NEW.upazila, NEW.avatar_url, NEW.cover_url,
      NEW.work_type, NEW.skills, NEW.experience, NEW.availability, NEW.expected_wage, now()
    )
    on conflict (id) do update set
      full_name = excluded.full_name, role = excluded.role, district = excluded.district,
      upazila = excluded.upazila, avatar_url = excluded.avatar_url, cover_url = excluded.cover_url,
      work_type = excluded.work_type, skills = excluded.skills, experience = excluded.experience,
      availability = excluded.availability, expected_wage = excluded.expected_wage, updated_at = now();
  else
    delete from public.messaging_directory where id = NEW.id;
  end if;
  return NEW;
end;
$function$;

drop trigger if exists sync_messaging_directory_after_profile_change on public.profiles;
create trigger sync_messaging_directory_after_profile_change
after insert or delete or update of full_name, role, district, upazila, avatar_url, cover_url,
  work_type, skills, experience, availability, expected_wage, account_status
on public.profiles for each row execute function private.sync_messaging_directory();

update public.messaging_directory d
set cover_url = p.cover_url, work_type = p.work_type, skills = p.skills,
    experience = p.experience, availability = p.availability, expected_wage = p.expected_wage
from public.profiles p where p.id = d.id;
