import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function reply(body: Record<string, unknown>, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return reply({ error: "শুধু POST অনুরোধ গ্রহণযোগ্য।" }, 405);

  const authHeader = req.headers.get("Authorization") ?? "";
  const token = authHeader.startsWith("Bearer ") ? authHeader.slice(7) : "";
  if (!token) return reply({ error: "লগইন সেশন পাওয়া যায়নি। আবার লগইন করুন।" }, 401);

  const url = Deno.env.get("SUPABASE_URL");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !serviceKey) return reply({ error: "সার্ভারের নিরাপদ সেটিংস অসম্পূর্ণ।" }, 500);

  const admin = createClient(url, serviceKey, {
    auth: { autoRefreshToken: false, persistSession: false },
  });

  try {
    const { data: authData, error: authError } = await admin.auth.getUser(token);
    const user = authData.user;
    if (authError || !user) return reply({ error: "সেশনটি বৈধ নয়। আবার লগইন করুন।" }, 401);

    const userId = user.id;
    const { data: profile, error: profileError } = await admin
      .from("profiles").select("role,avatar_url").eq("id", userId).maybeSingle();
    if (profileError) throw profileError;
    if (profile?.role === "admin") {
      return reply({ error: "প্রধান অ্যাডমিন অ্যাকাউন্ট অ্যাপ থেকে মুছতে পারবেন না।" }, 403);
    }

    const { count: paymentCount, error: paymentError } = await admin
      .from("payment_orders").select("id", { count: "exact", head: true })
      .or(`employer_id.eq.${userId},worker_id.eq.${userId}`);
    if (paymentError) throw paymentError;
    if ((paymentCount ?? 0) > 0) {
      return reply({
        error: "এই অ্যাকাউন্টের সঙ্গে পেমেন্ট রেকর্ড যুক্ত আছে। আর্থিক রেকর্ড সুরক্ষার জন্য সহায়তা নম্বরে যোগাযোগ করে অ্যাকাউন্ট বন্ধের অনুরোধ করুন।",
      }, 409);
    }

    const { data: ownedJobs, error: jobsReadError } = await admin
      .from("jobs").select("id").eq("employer_id", userId);
    if (jobsReadError) throw jobsReadError;
    const jobIds = (ownedJobs ?? []).map((job) => job.id as string);

    const { error: reviewedError } = await admin
      .from("employer_requests").update({ reviewed_by: null }).eq("reviewed_by", userId);
    if (reviewedError) throw reviewedError;

    const { error: settingsError } = await admin
      .from("app_settings").update({ updated_by: null }).eq("updated_by", userId);
    if (settingsError) throw settingsError;

    const { error: employerRequestsError } = await admin
      .from("employer_requests").delete().eq("user_id", userId);
    if (employerRequestsError) throw employerRequestsError;

    if (jobIds.length > 0) {
      const { error: completionByJobsError } = await admin
        .from("completion_requests").delete().in("job_id", jobIds);
      if (completionByJobsError) throw completionByJobsError;
      const { error: appsByJobsError } = await admin
        .from("applications").delete().in("job_id", jobIds);
      if (appsByJobsError) throw appsByJobsError;
      const { error: jobsDeleteError } = await admin
        .from("jobs").delete().eq("employer_id", userId);
      if (jobsDeleteError) throw jobsDeleteError;
    }

    const { error: completionByUserError } = await admin
      .from("completion_requests").delete()
      .or(`worker_id.eq.${userId},employer_id.eq.${userId}`);
    if (completionByUserError) throw completionByUserError;

    const { error: appsByUserError } = await admin
      .from("applications").delete().eq("worker_id", userId);
    if (appsByUserError) throw appsByUserError;

    const { data: avatarFiles, error: avatarListError } = await admin.storage
      .from("avatars").list(userId);
    if (avatarListError) throw avatarListError;
    const avatarPaths = (avatarFiles ?? []).map((file) => `${userId}/${file.name}`);
    if (avatarPaths.length > 0) {
      const { error: avatarDeleteError } = await admin.storage.from("avatars").remove(avatarPaths);
      if (avatarDeleteError) throw avatarDeleteError;
    }

    const { error: deleteError } = await admin.auth.admin.deleteUser(userId);
    if (deleteError) throw deleteError;

    return reply({ success: true, message: "অ্যাকাউন্ট ও সংশ্লিষ্ট ব্যক্তিগত তথ্য মুছে ফেলা হয়েছে।" });
  } catch (error) {
    console.error("delete-account failed", error);
    return reply({ error: "অ্যাকাউন্ট মুছে ফেলা যায়নি। পরে আবার চেষ্টা করুন বা সহায়তায় যোগাযোগ করুন।" }, 500);
  }
});
