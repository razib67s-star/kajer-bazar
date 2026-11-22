-- Cover foreign keys used in joins, deletes, and admin review queues.
create index if not exists applications_worker_id_idx on public.applications(worker_id);
create index if not exists completion_requests_employer_id_idx on public.completion_requests(employer_id);
create index if not exists completion_requests_job_id_idx on public.completion_requests(job_id);
create index if not exists completion_requests_worker_id_idx on public.completion_requests(worker_id);
create index if not exists employer_requests_reviewed_by_idx on public.employer_requests(reviewed_by);
create index if not exists jobs_employer_id_idx on public.jobs(employer_id);

-- Keep workflow RPCs callable only by signed-in users. Function bodies still
-- enforce their own role and ownership checks.
revoke execute on function public.admin_review_employer_request(uuid, boolean) from public, anon;
revoke execute on function public.admin_review_job(uuid, boolean) from public, anon;
revoke execute on function public.apply_to_job(uuid, text) from public, anon;
revoke execute on function public.move_overdue_completions_to_admin_review() from public, anon;
revoke execute on function public.publish_job_for_review(uuid) from public, anon;
revoke execute on function public.request_job_completion(uuid) from public, anon;
revoke execute on function public.respond_job_completion(uuid, boolean) from public, anon;
revoke execute on function public.select_job_application(uuid) from public, anon;
revoke execute on function public.submit_employer_request(text, text, text, text, text) from public, anon;
revoke execute on function public.withdraw_job_application(uuid) from public, anon;

grant execute on function public.admin_review_employer_request(uuid, boolean) to authenticated;
grant execute on function public.admin_review_job(uuid, boolean) to authenticated;
grant execute on function public.apply_to_job(uuid, text) to authenticated;
grant execute on function public.move_overdue_completions_to_admin_review() to authenticated;
grant execute on function public.publish_job_for_review(uuid) to authenticated;
grant execute on function public.request_job_completion(uuid) to authenticated;
grant execute on function public.respond_job_completion(uuid, boolean) to authenticated;
grant execute on function public.select_job_application(uuid) to authenticated;
grant execute on function public.submit_employer_request(text, text, text, text, text) to authenticated;
grant execute on function public.withdraw_job_application(uuid) to authenticated;

-- Automatically move overdue completion requests into the admin queue.
create extension if not exists pg_cron with schema pg_catalog;
select cron.unschedule(jobid) from cron.job where jobname = 'kajer-bazar-overdue-completion-review';
select cron.schedule(
  'kajer-bazar-overdue-completion-review',
  '*/15 * * * *',
  $job$
    update public.completion_requests
       set review_status = 'admin_review'
     where review_status = 'awaiting_employer'
       and employer_response_due_at < now();

    update public.jobs j
       set status = 'admin_review'
     where j.status = 'in_progress'
       and exists (
         select 1 from public.completion_requests c
          where c.job_id = j.id
            and c.review_status = 'admin_review'
       );
  $job$
);
