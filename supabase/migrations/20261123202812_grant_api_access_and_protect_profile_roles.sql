-- Restore the minimum PostgREST privileges required by the Android client.
-- Row access remains controlled by the existing RLS policies.
grant usage on schema public to anon, authenticated;

grant select on public.jobs to anon, authenticated;
grant insert, update on public.jobs to authenticated;

grant select on public.profiles to authenticated;
grant update (full_name, phone, district, upazila) on public.profiles to authenticated;

grant select, insert on public.applications to authenticated;
grant select, insert on public.employer_requests to authenticated;
grant select on public.completion_requests, public.payment_orders, public.payment_ledger to authenticated;

-- A profile owner must never be able to promote their own role.
-- Admin approval RPCs remain able to change worker -> employer.
create or replace function public.force_default_profile_role()
returns trigger
language plpgsql
security definer
set search_path to ''
as $function$
begin
  if tg_op = 'INSERT' then
    new.role := 'worker'::public.account_role;
  elsif new.role is distinct from old.role and not public.is_current_user_admin() then
    new.role := old.role;
  end if;
  return new;
end;
$function$;

drop trigger if exists profiles_force_default_role on public.profiles;
create trigger profiles_force_default_role
before insert or update of role on public.profiles
for each row execute function public.force_default_profile_role();
