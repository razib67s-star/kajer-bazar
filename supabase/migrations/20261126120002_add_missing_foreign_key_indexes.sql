-- Add missing indexes for foreign-key lookups flagged by Supabase Performance Advisor.
-- Additive only: no rows or existing indexes are changed.
create index if not exists content_reports_reporter_id_idx
  on public.content_reports (reporter_id);
create index if not exists content_reports_reviewed_by_idx
  on public.content_reports (reviewed_by);
create index if not exists job_reviews_reviewer_id_idx
  on public.job_reviews (reviewer_id);
create index if not exists notifications_actor_id_idx
  on public.notifications (actor_id);
create index if not exists saved_workers_worker_id_idx
  on public.saved_workers (worker_id);
create index if not exists worker_verification_requests_reviewed_by_idx
  on public.worker_verification_requests (reviewed_by);
create index if not exists worker_verification_requests_worker_id_idx
  on public.worker_verification_requests (worker_id);
