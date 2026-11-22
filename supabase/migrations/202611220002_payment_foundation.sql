-- Kajer Bazar payment foundation (sandbox-ready; no live gateway credentials)
-- Money values are stored in paisa. 1 BDT = 100 paisa.
-- Client roles can read only their own payment records; all writes are server-only.

create table if not exists public.payment_orders (
  id uuid primary key default gen_random_uuid(),
  job_id uuid not null references public.jobs(id) on delete restrict,
  employer_id uuid not null references public.profiles(id) on delete restrict,
  worker_id uuid not null references public.profiles(id) on delete restrict,
  amount_paisa bigint not null check (amount_paisa > 0),
  commission_bps integer not null default 1000 check (commission_bps between 0 and 1000),
  commission_paisa bigint generated always as ((amount_paisa * commission_bps) / 10000) stored,
  worker_net_paisa bigint generated always as (amount_paisa - ((amount_paisa * commission_bps) / 10000)) stored,
  currency text not null default 'BDT' check (currency = 'BDT'),
  provider text not null default 'sandbox' check (provider in ('sandbox','sslcommerz')),
  environment text not null default 'sandbox' check (environment in ('sandbox','live')),
  status text not null default 'pending' check (status in ('pending','sandbox_paid','paid','failed','cancelled','refunded','disputed')),
  provider_transaction_id text unique,
  idempotency_key text not null unique,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check (employer_id <> worker_id),
  check (environment <> 'live' or provider <> 'sandbox'),
  check (status <> 'paid' or (provider = 'sslcommerz' and environment = 'live')),
  check (status <> 'sandbox_paid' or environment = 'sandbox')
);

create index if not exists payment_orders_employer_created_idx on public.payment_orders (employer_id, created_at desc);
create index if not exists payment_orders_worker_created_idx on public.payment_orders (worker_id, created_at desc);
create index if not exists payment_orders_job_idx on public.payment_orders (job_id);

create table if not exists public.payment_ledger (
  id uuid primary key default gen_random_uuid(),
  payment_order_id uuid not null references public.payment_orders(id) on delete restrict,
  entry_type text not null check (entry_type in ('employer_charge','platform_commission','worker_net','refund','adjustment')),
  amount_paisa bigint not null check (amount_paisa > 0),
  currency text not null default 'BDT' check (currency = 'BDT'),
  note text,
  created_at timestamptz not null default now(),
  unique (payment_order_id, entry_type)
);

create index if not exists payment_ledger_order_idx on public.payment_ledger (payment_order_id, created_at);

alter table public.payment_orders enable row level security;
alter table public.payment_ledger enable row level security;

drop policy if exists "payment parties can read their orders" on public.payment_orders;
create policy "payment parties can read their orders"
on public.payment_orders for select to authenticated
using ((select auth.uid()) = employer_id or (select auth.uid()) = worker_id);

drop policy if exists "payment parties can read their ledger" on public.payment_ledger;
create policy "payment parties can read their ledger"
on public.payment_ledger for select to authenticated
using (
  exists (
    select 1 from public.payment_orders p
    where p.id = payment_order_id
      and ((select auth.uid()) = p.employer_id or (select auth.uid()) = p.worker_id)
  )
);

revoke all on public.payment_orders from anon, authenticated;
revoke all on public.payment_ledger from anon, authenticated;
grant select on public.payment_orders to authenticated;
grant select on public.payment_ledger to authenticated;
