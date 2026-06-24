-- rls_auto_enable() is an event-trigger helper (Supabase RLS auto-enable pattern).
-- It must not be callable via PostgREST /rest/v1/rpc by client roles.

REVOKE EXECUTE ON FUNCTION public.rls_auto_enable() FROM PUBLIC, anon, authenticated;

ALTER FUNCTION public.rls_auto_enable()
  SET search_path = pg_catalog, public, pg_temp;
