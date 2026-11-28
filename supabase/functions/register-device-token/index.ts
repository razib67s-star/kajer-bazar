import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";
const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};
Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return Response.json({ error: "Method not allowed" }, { status: 405, headers: corsHeaders });
  const authHeader = req.headers.get("Authorization");
  if (!authHeader?.startsWith("Bearer ")) return Response.json({ error: "Authentication required" }, { status: 401, headers: corsHeaders });
  const url = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  if (!url || !anonKey) return Response.json({ error: "Server configuration missing" }, { status: 500, headers: corsHeaders });
  const supabase = createClient(url, anonKey, { global: { headers: { Authorization: authHeader } }, auth: { persistSession: false, autoRefreshToken: false } });
  const { data: userData, error: userError } = await supabase.auth.getUser();
  if (userError || !userData.user) return Response.json({ error: "Invalid session" }, { status: 401, headers: corsHeaders });
  let body: { token?: unknown; action?: unknown };
  try { body = await req.json(); } catch { return Response.json({ error: "Invalid JSON body" }, { status: 400, headers: corsHeaders }); }
  const token = typeof body.token === "string" ? body.token.trim() : "";
  const action = body.action === "unregister" ? "unregister" : "register";
  if (!token || token.length > 4096) return Response.json({ error: "Invalid device token" }, { status: 400, headers: corsHeaders });
  if (action === "unregister") {
    const { error } = await supabase.from("device_push_tokens").delete().eq("user_id", userData.user.id).eq("fcm_token", token);
    if (error) return Response.json({ error: "Could not remove device token" }, { status: 500, headers: corsHeaders });
    return Response.json({ ok: true, action }, { headers: corsHeaders });
  }
  const { error } = await supabase.from("device_push_tokens").upsert({ user_id: userData.user.id, fcm_token: token, platform: "android", updated_at: new Date().toISOString() }, { onConflict: "user_id,fcm_token" });
  if (error) return Response.json({ error: "Could not save device token" }, { status: 500, headers: corsHeaders });
  return Response.json({ ok: true, action }, { headers: corsHeaders });
});
