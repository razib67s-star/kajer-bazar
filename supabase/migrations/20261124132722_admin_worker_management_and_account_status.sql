alter table public.profiles
  add column if not exists account_status text not null default 'active'
  check (account_status in ('active','suspended'));

create index if not exists app_settings_updated_by_idx
  on public.app_settings(updated_by);

create or replace function public.admin_list_profiles()
returns table (
  id uuid, full_name text, phone text, contact_phone text,
  district text, upazila text, role text, account_status text,
  created_at timestamptz
)
language plpgsql
security definer
set search_path = ''
as $$
begin
  if auth.uid() is null or not public.is_current_user_admin() then
    raise exception 'শুধু অ্যাডমিন ব্যবহারকারী তালিকা দেখতে পারবেন';
  end if;
  return query
    select p.id, p.full_name, p.phone, p.contact_phone, p.district,
           p.upazila, p.role::text, p.account_status, p.created_at
    from public.profiles p
    order by p.created_at desc
    limit 500;
end;
$$;

create or replace function public.admin_update_profile(
  p_user_id uuid, p_full_name text, p_contact_phone text,
  p_district text, p_upazila text, p_account_status text
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  if auth.uid() is null or not public.is_current_user_admin() then
    raise exception 'শুধু অ্যাডমিন প্রোফাইল সম্পাদনা করতে পারবেন';
  end if;
  if p_user_id is null or p_full_name is null or length(trim(p_full_name)) not between 1 and 120 then
    raise exception 'নাম ১ থেকে ১২০ অক্ষরের মধ্যে দিন';
  end if;
  if p_account_status not in ('active','suspended') then
    raise exception 'অ্যাকাউন্টের অবস্থা সঠিক নয়';
  end if;
  if exists (select 1 from public.profiles p where p.id = p_user_id and p.role = 'admin') then
    raise exception 'অ্যাডমিন অ্যাকাউন্ট এই ফাংশন দিয়ে পরিবর্তন করা যাবে না';
  end if;
  update public.profiles
     set full_name = trim(p_full_name),
         contact_phone = nullif(trim(coalesce(p_contact_phone,'')), ''),
         district = nullif(trim(coalesce(p_district,'')), ''),
         upazila = nullif(trim(coalesce(p_upazila,'')), ''),
         account_status = p_account_status,
         updated_at = now()
   where id = p_user_id;
  if not found then raise exception 'ব্যবহারকারী পাওয়া যায়নি'; end if;
end;
$$;

revoke all on function public.admin_list_profiles() from public, anon;
revoke all on function public.admin_update_profile(uuid,text,text,text,text,text) from public, anon;
grant execute on function public.admin_list_profiles() to authenticated;
grant execute on function public.admin_update_profile(uuid,text,text,text,text,text) to authenticated;
