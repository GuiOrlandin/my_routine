-- My Routine App — initial schema (T03)
-- Tables: notes, reminders, google_tokens
-- RLS: notes/reminders per-user; google_tokens service-role only

-- notes
create table notes (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users(id) not null,
  title text not null,
  body text default '',
  tags text[] default '{}',
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

alter table notes enable row level security;

create policy "users own notes" on notes
  for all
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

-- reminders
create table reminders (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users(id) not null,
  title text not null,
  due_at timestamptz not null,
  status text default 'pending',
  source text default 'manual',
  external_id text,
  recurrence text,
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

alter table reminders enable row level security;

create policy "users own reminders" on reminders
  for all
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

-- google_tokens (server-side only — RLS enabled, no client policies)
create table google_tokens (
  user_id uuid primary key references auth.users(id),
  refresh_token text not null,
  scopes text[] not null,
  updated_at timestamptz default now()
);

alter table google_tokens enable row level security;

revoke all on google_tokens from anon, authenticated;
