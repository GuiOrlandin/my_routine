-- Optimize RLS: evaluate auth.uid() once per query, not per row.

drop policy if exists "users own notes" on notes;
create policy "users own notes" on notes
  for all
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

drop policy if exists "users own reminders" on reminders;
create policy "users own reminders" on reminders
  for all
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);
