create table if not exists public.device_push_tokens (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  fcm_token text not null,
  platform text not null default 'android' check (platform in ('android')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (user_id, fcm_token)
);

create index if not exists device_push_tokens_user_id_idx
  on public.device_push_tokens(user_id);

alter table public.device_push_tokens enable row level security;

drop policy if exists "Users can view own push tokens" on public.device_push_tokens;
create policy "Users can view own push tokens"
  on public.device_push_tokens for select to authenticated
  using ((select auth.uid()) = user_id);

drop policy if exists "Users can register own push tokens" on public.device_push_tokens;
create policy "Users can register own push tokens"
  on public.device_push_tokens for insert to authenticated
  with check ((select auth.uid()) = user_id);

drop policy if exists "Users can update own push tokens" on public.device_push_tokens;
create policy "Users can update own push tokens"
  on public.device_push_tokens for update to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

drop policy if exists "Users can delete own push tokens" on public.device_push_tokens;
create policy "Users can delete own push tokens"
  on public.device_push_tokens for delete to authenticated
  using ((select auth.uid()) = user_id);

grant select, insert, update, delete on public.device_push_tokens to authenticated;
