-- Marketplace commission rule: employer pays the platform fee on top.
-- amount_paisa is the agreed worker wage. Worker net equals the full wage.
alter table public.payment_orders
  add column if not exists employer_total_paisa bigint
  generated always as (amount_paisa + ((amount_paisa * commission_bps) / 10000)) stored;

alter table public.payment_orders
  drop column if exists worker_net_paisa;

alter table public.payment_orders
  add column worker_net_paisa bigint
  generated always as (amount_paisa) stored;

comment on column public.payment_orders.amount_paisa is 'Agreed worker wage in paisa; excludes employer-paid platform commission.';
comment on column public.payment_orders.commission_paisa is 'Platform commission in paisa, paid by employer on top of agreed worker wage.';
comment on column public.payment_orders.employer_total_paisa is 'Total amount due from employer: agreed worker wage plus platform commission.';
comment on column public.payment_orders.worker_net_paisa is 'Full agreed worker wage; commission is not deducted from worker.';
