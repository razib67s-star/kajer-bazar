-- Kajer Bazar social graph, engagement, product marketplace, trust, notifications and ads.
-- This migration adds secure data foundations; payment settlement and FCM delivery remain server/provider tasks.

-- Social graph
create table if not exists public.user_follows (
  follower_id uuid not null references auth.users(id) on delete cascade,
  following_id uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (follower_id, following_id),
  constraint user_follows_no_self check (follower_id <> following_id)
);
create index if not exists user_follows_following_idx on public.user_follows(following_id, created_at desc);
alter table public.user_follows enable row level security;
drop policy if exists "Follow graph readable by signed-in users" on public.user_follows;
create policy "Follow graph readable by signed-in users" on public.user_follows
  for select to authenticated using (true);
drop policy if exists "Users manage their own follows" on public.user_follows;
create policy "Users manage their own follows" on public.user_follows
  for all to authenticated using (follower_id = (select auth.uid()))
  with check (follower_id = (select auth.uid()) and follower_id <> following_id);
grant select, insert, delete on public.user_follows to authenticated;

-- Post reactions, comments and shares
create table if not exists public.media_post_likes (
  post_id uuid not null references public.media_posts(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (post_id, user_id)
);
create table if not exists public.media_post_comments (
  id uuid primary key default gen_random_uuid(),
  post_id uuid not null references public.media_posts(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  body text not null check (char_length(trim(body)) between 1 and 2000),
  created_at timestamptz not null default now()
);
create index if not exists media_post_comments_post_idx on public.media_post_comments(post_id, created_at);
create table if not exists public.media_post_shares (
  id uuid primary key default gen_random_uuid(),
  post_id uuid not null references public.media_posts(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  caption text not null default '' check (char_length(caption) <= 500),
  created_at timestamptz not null default now(),
  unique (post_id, user_id)
);
alter table public.media_post_likes enable row level security;
alter table public.media_post_comments enable row level security;
alter table public.media_post_shares enable row level security;
drop policy if exists "Post likes are publicly readable" on public.media_post_likes;
create policy "Post likes are publicly readable" on public.media_post_likes for select to anon, authenticated using (true);
drop policy if exists "Users manage own post likes" on public.media_post_likes;
create policy "Users manage own post likes" on public.media_post_likes for all to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
drop policy if exists "Post comments are publicly readable" on public.media_post_comments;
create policy "Post comments are publicly readable" on public.media_post_comments for select to anon, authenticated using (true);
drop policy if exists "Users create own post comments" on public.media_post_comments;
create policy "Users create own post comments" on public.media_post_comments for insert to authenticated
  with check (user_id = (select auth.uid()));
drop policy if exists "Users delete own post comments" on public.media_post_comments;
create policy "Users delete own post comments" on public.media_post_comments for delete to authenticated
  using (user_id = (select auth.uid()));
drop policy if exists "Post shares are publicly readable" on public.media_post_shares;
create policy "Post shares are publicly readable" on public.media_post_shares for select to anon, authenticated using (true);
drop policy if exists "Users create own post shares" on public.media_post_shares;
create policy "Users create own post shares" on public.media_post_shares for insert to authenticated
  with check (user_id = (select auth.uid()));
drop policy if exists "Users delete own post shares" on public.media_post_shares;
create policy "Users delete own post shares" on public.media_post_shares for delete to authenticated
  using (user_id = (select auth.uid()));
grant select on public.media_post_likes, public.media_post_comments, public.media_post_shares to anon, authenticated;
grant insert, delete on public.media_post_likes, public.media_post_shares to authenticated;
grant insert, delete on public.media_post_comments to authenticated;

-- Product listings and buyer orders. Order/payment settlement must be performed by trusted server code.
create table if not exists public.marketplace_products (
  id uuid primary key default gen_random_uuid(),
  seller_id uuid not null references auth.users(id) on delete cascade,
  title text not null check (char_length(trim(title)) between 3 and 120),
  description text not null default '' check (char_length(description) <= 4000),
  category text not null,
  price_paisa bigint not null check (price_paisa > 0),
  currency text not null default 'BDT' check (currency = 'BDT'),
  district text not null default '',
  upazila text not null default '',
  image_paths text[] not null default '{}',
  stock_quantity integer not null default 1 check (stock_quantity >= 0),
  status text not null default 'pending_review' check (status in ('pending_review','published','paused','sold','rejected')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists marketplace_products_browse_idx on public.marketplace_products(status, category, district, created_at desc);
create table if not exists public.marketplace_orders (
  id uuid primary key default gen_random_uuid(),
  product_id uuid not null references public.marketplace_products(id) on delete restrict,
  buyer_id uuid not null references auth.users(id) on delete restrict,
  seller_id uuid not null references auth.users(id) on delete restrict,
  quantity integer not null check (quantity between 1 and 100),
  unit_price_paisa bigint not null check (unit_price_paisa > 0),
  total_paisa bigint generated always as (quantity * unit_price_paisa) stored,
  status text not null default 'requested' check (status in ('requested','accepted','rejected','paid','shipped','completed','cancelled','disputed')),
  delivery_address text not null check (char_length(trim(delivery_address)) between 5 and 1000),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint marketplace_order_parties_differ check (buyer_id <> seller_id)
);
create index if not exists marketplace_orders_buyer_idx on public.marketplace_orders(buyer_id, created_at desc);
create index if not exists marketplace_orders_seller_idx on public.marketplace_orders(seller_id, created_at desc);
alter table public.marketplace_products enable row level security;
alter table public.marketplace_orders enable row level security;
drop policy if exists "Published products are public" on public.marketplace_products;
create policy "Published products are public" on public.marketplace_products for select to anon, authenticated
  using (status = 'published' or seller_id = (select auth.uid()));
drop policy if exists "Sellers create own pending products" on public.marketplace_products;
create policy "Sellers create own pending products" on public.marketplace_products for insert to authenticated
  with check (seller_id = (select auth.uid()) and status = 'pending_review');
drop policy if exists "Sellers update own products" on public.marketplace_products;
create policy "Sellers update own products" on public.marketplace_products for update to authenticated
  using (seller_id = (select auth.uid())) with check (seller_id = (select auth.uid()) and status in ('pending_review','paused'));
drop policy if exists "Order parties can read orders" on public.marketplace_orders;
create policy "Order parties can read orders" on public.marketplace_orders for select to authenticated
  using (buyer_id = (select auth.uid()) or seller_id = (select auth.uid()));
drop policy if exists "Buyers create orders for published products" on public.marketplace_orders;
create policy "Buyers create orders for published products" on public.marketplace_orders for insert to authenticated
  with check (buyer_id = (select auth.uid()) and exists (
    select 1 from public.marketplace_products p
    where p.id = product_id and p.seller_id = seller_id and p.status = 'published'
      and p.stock_quantity >= quantity and p.price_paisa = unit_price_paisa
  ));
revoke update, delete on public.marketplace_orders from anon, authenticated;
grant select on public.marketplace_products to anon, authenticated;
grant insert, update on public.marketplace_products to authenticated;
grant select, insert on public.marketplace_orders to authenticated;

-- Verified account applications and job-based ratings/reviews
create table if not exists public.verification_requests (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  details text not null check (char_length(trim(details)) between 10 and 3000),
  evidence_paths text[] not null default '{}',
  status text not null default 'pending' check (status in ('pending','approved','rejected')),
  admin_note text not null default '',
  created_at timestamptz not null default now(),
  reviewed_at timestamptz
);
create index if not exists verification_requests_review_idx on public.verification_requests(status, created_at);
alter table public.verification_requests enable row level security;
drop policy if exists "Users read own verification requests" on public.verification_requests;
create policy "Users read own verification requests" on public.verification_requests for select to authenticated
  using (user_id = (select auth.uid()));
drop policy if exists "Users submit own verification requests" on public.verification_requests;
create policy "Users submit own verification requests" on public.verification_requests for insert to authenticated
  with check (user_id = (select auth.uid()) and status = 'pending');
grant select, insert on public.verification_requests to authenticated;

create table if not exists public.user_reviews (
  id uuid primary key default gen_random_uuid(),
  job_id uuid not null references public.jobs(id) on delete restrict,
  reviewer_id uuid not null references auth.users(id) on delete cascade,
  reviewee_id uuid not null references auth.users(id) on delete cascade,
  rating smallint not null check (rating between 1 and 5),
  review text not null default '' check (char_length(review) <= 2000),
  created_at timestamptz not null default now(),
  unique (job_id, reviewer_id),
  check (reviewer_id <> reviewee_id)
);
alter table public.user_reviews enable row level security;
drop policy if exists "Reviews are publicly readable" on public.user_reviews;
create policy "Reviews are publicly readable" on public.user_reviews for select to anon, authenticated using (true);
drop policy if exists "Completed job participants can review" on public.user_reviews;
create policy "Completed job participants can review" on public.user_reviews for insert to authenticated
  with check (reviewer_id = (select auth.uid()) and exists (
    select 1 from public.jobs j where j.id = job_id and j.status = 'completed'
      and (j.employer_id = reviewer_id or exists (
        select 1 from public.applications a where a.job_id = j.id and a.worker_id = reviewer_id and a.status = 'selected'
      ))
      and (j.employer_id = reviewee_id or exists (
        select 1 from public.applications a where a.job_id = j.id and a.worker_id = reviewee_id and a.status = 'selected'
      ))
  ));
grant select on public.user_reviews to anon, authenticated;
grant insert on public.user_reviews to authenticated;

-- Push-token registry and in-app notification inbox. Delivery requires a trusted FCM sender.
create table if not exists public.device_push_tokens (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  token text not null unique,
  platform text not null default 'android' check (platform in ('android','ios')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
alter table public.device_push_tokens enable row level security;
drop policy if exists "Users manage own push tokens" on public.device_push_tokens;
create policy "Users manage own push tokens" on public.device_push_tokens for all to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
grant select, insert, update, delete on public.device_push_tokens to authenticated;

create table if not exists public.community_notifications (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  actor_id uuid references auth.users(id) on delete set null,
  notification_type text not null,
  title text not null check (char_length(title) <= 160),
  body text not null default '' check (char_length(body) <= 1000),
  target_id uuid,
  read_at timestamptz,
  created_at timestamptz not null default now()
);
create index if not exists community_notifications_inbox_idx on public.community_notifications(user_id, created_at desc);
alter table public.community_notifications enable row level security;
drop policy if exists "Users read own notifications" on public.community_notifications;
create policy "Users read own notifications" on public.community_notifications for select to authenticated
  using (user_id = (select auth.uid()));
drop policy if exists "Users mark own notifications read" on public.community_notifications;
create policy "Users mark own notifications read" on public.community_notifications for update to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
grant select, update on public.community_notifications to authenticated;

-- Moderated ad campaigns. Client users cannot mark campaigns paid or approved.
create table if not exists public.ad_campaigns (
  id uuid primary key default gen_random_uuid(),
  advertiser_id uuid not null references auth.users(id) on delete cascade,
  title text not null check (char_length(trim(title)) between 3 and 120),
  destination_url text not null,
  creative_path text,
  budget_paisa bigint not null check (budget_paisa > 0),
  status text not null default 'pending_review' check (status in ('pending_review','approved','active','paused','rejected','completed')),
  starts_at timestamptz,
  ends_at timestamptz,
  created_at timestamptz not null default now(),
  check (ends_at is null or starts_at is null or ends_at > starts_at)
);
alter table public.ad_campaigns enable row level security;
drop policy if exists "Active ads are publicly readable" on public.ad_campaigns;
create policy "Active ads are publicly readable" on public.ad_campaigns for select to anon, authenticated
  using (status = 'active' or advertiser_id = (select auth.uid()));
drop policy if exists "Advertisers create pending ads" on public.ad_campaigns;
create policy "Advertisers create pending ads" on public.ad_campaigns for insert to authenticated
  with check (advertiser_id = (select auth.uid()) and status = 'pending_review');
drop policy if exists "Advertisers pause own ads" on public.ad_campaigns;
create policy "Advertisers pause own ads" on public.ad_campaigns for update to authenticated
  using (advertiser_id = (select auth.uid())) with check (advertiser_id = (select auth.uid()) and status in ('pending_review','paused'));
grant select, insert, update on public.ad_campaigns to authenticated;
grant select on public.ad_campaigns to anon;

-- Common updated_at trigger for editable marketplace records.
drop trigger if exists marketplace_products_updated_at on public.marketplace_products;
create trigger marketplace_products_updated_at before update on public.marketplace_products
  for each row execute function public.set_updated_at();
drop trigger if exists marketplace_orders_updated_at on public.marketplace_orders;
create trigger marketplace_orders_updated_at before update on public.marketplace_orders
  for each row execute function public.set_updated_at();
