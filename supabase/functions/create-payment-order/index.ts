import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};
function reply(status: number, body: Record<string, unknown>) {
  return new Response(JSON.stringify(body), { status, headers: { ...corsHeaders, "Content-Type": "application/json" } });
}
Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return reply(405, { error: "method_not_allowed" });
  const authHeader = req.headers.get("Authorization") ?? "";
  const token = authHeader.startsWith("Bearer ") ? authHeader.slice(7) : "";
  if (!token) return reply(401, { error: "authentication_required" });
  let payload: { job_id?: string; application_id?: string; idempotency_key?: string };
  try { payload = await req.json(); } catch { return reply(400, { error: "invalid_json" }); }
  const { job_id, application_id, idempotency_key } = payload;
  const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
  if (!job_id || !uuidPattern.test(job_id) || !application_id || !uuidPattern.test(application_id)) return reply(400, { error: "valid_job_and_application_ids_required" });
  if (!idempotency_key || idempotency_key.length < 8 || idempotency_key.length > 128) return reply(400, { error: "idempotency_key_must_be_8_to_128_characters" });
  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !serviceRoleKey) return reply(503, { error: "server_configuration_missing" });
  const admin = createClient(supabaseUrl, serviceRoleKey, { auth: { persistSession: false, autoRefreshToken: false } });
  const { data: authData, error: authError } = await admin.auth.getUser(token);
  if (authError || !authData.user) return reply(401, { error: "invalid_session" });
  const employerId = authData.user.id;
  const { data: profile, error: profileError } = await admin.from("profiles").select("role").eq("id", employerId).maybeSingle();
  if (profileError) return reply(500, { error: "profile_lookup_failed" });
  if (!profile || profile.role !== "employer") return reply(403, { error: "employer_account_required" });
  const { data: existing, error: existingError } = await admin.from("payment_orders")
    .select("id, job_id, employer_id, worker_id, amount_paisa, commission_paisa, worker_net_paisa, currency, status, environment")
    .eq("idempotency_key", idempotency_key).maybeSingle();
  if (existingError) return reply(500, { error: "idempotency_lookup_failed" });
  if (existing) {
    if (existing.employer_id !== employerId || existing.job_id !== job_id) return reply(409, { error: "idempotency_key_already_used" });
    return reply(200, { payment: existing, reused: true, live_payment_enabled: false });
  }
  const { data: job, error: jobError } = await admin.from("jobs")
    .select("id, employer_id, agreed_wage_paisa, status").eq("id", job_id).maybeSingle();
  if (jobError) return reply(500, { error: "job_lookup_failed" });
  if (!job || job.employer_id !== employerId) return reply(404, { error: "job_not_found_for_employer" });
  if (!["in_progress", "completed"].includes(job.status)) return reply(409, { error: "job_must_be_in_progress_or_completed" });
  const { data: application, error: applicationError } = await admin.from("applications")
    .select("id, job_id, worker_id, status").eq("id", application_id).eq("job_id", job_id).maybeSingle();
  if (applicationError) return reply(500, { error: "application_lookup_failed" });
  if (!application || application.status !== "selected") return reply(409, { error: "selected_application_required" });
  if (application.worker_id === employerId) return reply(400, { error: "payer_and_worker_must_differ" });
  const { data: payment, error: insertError } = await admin.from("payment_orders").insert({
    job_id, employer_id: employerId, worker_id: application.worker_id,
    amount_paisa: job.agreed_wage_paisa, commission_bps: 1000,
    provider: "sandbox", environment: "sandbox", status: "pending", idempotency_key,
  }).select("id, job_id, employer_id, worker_id, amount_paisa, commission_paisa, worker_net_paisa, currency, status, environment").single();
  if (insertError) {
    if (insertError.code === "23505") return reply(409, { error: "duplicate_payment_request" });
    return reply(500, { error: "payment_order_creation_failed" });
  }
  return reply(201, { payment, reused: false, live_payment_enabled: false, message: "Pending sandbox order created. No money was collected." });
});