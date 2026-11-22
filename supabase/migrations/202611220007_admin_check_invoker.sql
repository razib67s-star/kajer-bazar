-- This helper is used by RLS policies. It only reads the caller's own profile,
-- so SECURITY INVOKER is sufficient and avoids unnecessary elevated privileges.
alter function public.is_current_user_admin() security invoker;
