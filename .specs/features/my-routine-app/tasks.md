# My Routine App — Tasks

**Design**: `.specs/features/my-routine-app/design.md`
**Context**: `.specs/features/my-routine-app/context.md`
**Status**: Draft
**Milestone**: M1 — Fundação + MVP

**Decisões aplicadas (context.md):**
- Armazenamento híbrido: notas → Supabase direto; lembretes + Google → Spring Boot
- Layout: dashboard "Hoje" na Home
- Recorrência: diário ou semanal no MVP
- UI: `@primo-brutality/ui` + NativeWind

**Convenção Tests:** nunca usar `Tests: none`. Use: `unit` / `integration` / `manual (gate)` / `manual (dashboard)` conforme o gate real da task.

---

## Execution Plan

### Phase 1: Infraestrutura (paralelo)

```
T01 [P] ──┐
T02 [P] ──┼──→ T03
T04 [P] ──┤
T05 [P] ──┘
```

### Phase 2: Back-end Java — domínio e persistência (sequencial)

```
T03 → T06 → T07 → T08 → T09 → T10
T04 → T11 (auth middleware, após T03 para validar JWT)
```

### Phase 3: Back-end Java — API REST (paralelo após T09 + T10 + T11)

```
              ┌→ T12 [P] ─┐
T09,T10,T11 ──┼→ T13 [P] ─┼──→ T16
              ├→ T14 [P] ─┤
              └→ T15 [P] ─┘
T02,T11,T09 ──→ T17 → T18 → T19
T12–T19 ──────→ T20 (deploy)
```

### Phase 4: Mobile — fundação (sequencial)

```
T05 → T21 → T22 → T23
T01 → T21
T04 → T24 (api client)
```

### Phase 5: Mobile — telas (paralelo após T23)

```
T23 ──┬→ T25 [P] (tabs layout)
      ├→ T26 [P] (notes list)
      ├→ T27 [P] (note form)
      ├→ T28 [P] (reminders list)
      ├→ T29 [P] (reminder form)
      ├→ T30 [P] (agenda tab)
      └→ T31 [P] (profile)
```

### Phase 6: Mobile — integração final (sequencial)

```
T28,T29 → T32 (notificações locais)
T26,T28,T30 → T33 (empty states)
T25,T26,T28,T30,T32 → T34 (dashboard Hoje)
T24,T20 → T35 (loading/erro + cold start Render)
```

---

## Task Breakdown — M1 (MVP)

### T01: Configurar projeto Supabase [P] — ⚠️ Parcial (docs ✅, dashboard manual pendente)

**What**: Criar projeto Supabase, habilitar Google Auth provider e documentar variáveis de ambiente.
**Where**: `mobile/.env.example`, `backend/.env.example`, `.specs/codebase/INTEGRATIONS.md` (atualizar)
**Depends on**: None
**Reuses**: `.specs/codebase/INTEGRATIONS.md`
**Requirements**: AUTH-01

**Tools**:
- MCP: Supabase (dashboard manual)
- Skill: `prisma-database-setup-postgresql` (referência RLS)

**Done when**:
- [x] Projeto Supabase criado (URL + anon key + service role key documentados em `.env.example`)
- [ ] Google provider habilitado no Supabase Auth *(passos em INTEGRATIONS.md § Setup)*
- [ ] Redirect URLs configuradas para Expo (`myroutine://` ou equivalente) *(valores documentados em INTEGRATIONS.md)*

**Tests**: manual (gate)
**Gate**: manual — variáveis presentes em `.env.example`

**Verify**:
```bash
# Arquivos existem e contêm SUPABASE_URL, SUPABASE_ANON_KEY
grep -q SUPABASE_URL mobile/.env.example
```

---

### T02: Configurar Google Cloud (OAuth + Calendar API) [P] — ⚠️ Parcial (docs ✅, console manual pendente)

**What**: Criar projeto Google Cloud, OAuth consent screen e habilitar Calendar API; documentar client ID/secret.
**Where**: `backend/.env.example`, `mobile/.env.example`
**Depends on**: None
**Reuses**: `.specs/codebase/INTEGRATIONS.md`
**Requirements**: GCAL-01, AUTH-01

**Tools**:
- MCP: NONE (console manual)
- Skill: NONE

**Done when**:
- [ ] OAuth 2.0 Client ID criado (Web + Android/iOS conforme Expo) *(passos em INTEGRATIONS.md § Setup T02)*
- [ ] Calendar API habilitada *(passos em INTEGRATIONS.md § Setup T02)*
- [x] Escopos documentados: `openid email profile`, `calendar.readonly`
- [x] Credenciais em `.env.example` (sem valores reais commitados)

**Tests**: manual (gate)
**Gate**: manual

**Verify**: `.env.example` contém `GOOGLE_CLIENT_ID` e `GOOGLE_CLIENT_SECRET`

---

### T03: Migrations Supabase (tabelas + RLS) — ⚠️ Parcial (SQL ✅, aplicar no dashboard/CLI pendente)

**What**: Criar SQL migration com tabelas `notes`, `reminders`, `google_tokens` e políticas RLS.
**Where**: `supabase/migrations/001_initial_schema.sql`
**Depends on**: T01
**Reuses**: Schema em `design.md` (Data Models)
**Requirements**: NOTE-01, REM-01, GCAL-01

**Tools**:
- MCP: Supabase
- Skill: `prisma-cli-migrate-dev` (referência SQL)

**Done when**:
- [x] Migration SQL define `notes`, `reminders`, `google_tokens` conforme design
- [x] RLS no SQL: `auth.uid() = user_id` em `notes` e `reminders`
- [x] `google_tokens` no SQL: RLS sem policies + `revoke` para anon/authenticated
- [ ] Migration aplicada no projeto Supabase *(passos em INTEGRATIONS.md § T03)*

**Tests**: manual (gate)
**Gate**: manual — `supabase db push` ou SQL executado sem erro

**Verify**:
```bash
# Migration file existe
test -f supabase/migrations/001_initial_schema.sql
```

---

### T04: Scaffold Spring Boot (estrutura + health) [P] — ✅

**What**: Criar estrutura `backend/` com Spring Boot 3, pacotes POO/SOLID e endpoint `GET /health`.
**Where**: `backend/pom.xml`, `backend/src/main/java/com/myroutine/`, `backend/README.md`
**Depends on**: None
**Reuses**: Estrutura em `design.md`, `.specs/project/JAVA-ROADMAP.md`
**Requirements**: (infra — habilita REM-*, GCAL-*)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `mvn spring-boot:run` inicia sem erro
- [x] `GET /health` retorna `{"status": "ok"}`
- [x] `pom.xml` com spring-boot-starter-web, validation, test
- [x] `backend/README.md` com seção "Estrutura de pacotes" explicando POO + SOLID

**Tests**: manual (gate)
**Gate**: manual
```bash
cd backend && mvn spring-boot:run &
curl -s http://localhost:8080/health
```

---

### T05: Configurar NativeWind + @primo-brutality/ui [P] — ✅

**What**: Instalar e configurar NativeWind e `@primo-brutality/ui` no Expo; validar um componente Button na tela.
**Where**: `mobile/package.json`, `mobile/tailwind.config.js`, `mobile/babel.config.js`, `mobile/src/app/index.tsx`
**Depends on**: None
**Reuses**: `mobile/src/global.css`
**Requirements**: UI-01, UI-03

**Tools**:
- MCP: Context7 (NativeWind + Expo docs)
- Skill: `react-native-expert`

**Done when**:
- [x] `npm install @primo-brutality/ui nativewind` concluído
- [x] NativeWind configurado (babel + tailwind.config)
- [x] Tela placeholder renderiza `<Button>` da lib com estilo brutalista
- [x] `npm run lint` passa (ou `tsc --noEmit` sem erros)

**Tests**: manual (gate)
**Gate**: `cd mobile && npm run lint`
**Verify**: App abre e exibe botão brutalista

---

### T06: Classe `Reminder` (domain) — ✅

**What**: Implementar entidade `Reminder` com validação, status e recorrência (daily/weekly).
**Where**: `backend/src/main/java/com/myroutine/domain/Reminder.java`, enums `ReminderStatus.java`, `Recurrence.java`
**Depends on**: T04
**Reuses**: Exemplo em `design.md`
**Requirements**: REM-01, REM-03, REM-04

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Classe `Reminder` com construtor, `markDone()`, `snooze()`
- [x] Validação: título vazio → `IllegalArgumentException`; data no passado → `IllegalArgumentException`
- [x] `ReminderStatus` enum: PENDING, DONE, SNOOZED
- [x] `Recurrence` enum: NONE, DAILY, WEEKLY
- [x] Javadoc em português explicando encapsulamento
- [x] Seção no `backend/README.md`: "Conceito: Classe e Encapsulamento"

**Tests**: unit (ReminderTest / T19)
**Gate**: manual — instanciar no código ou teste rápido

**Verify**:
```bash
cd backend && mvn -q test -Dtest=ReminderTest 2>/dev/null || \
  mvn -q exec:java -Dexec.mainClass="com.myroutine.domain.ReminderSmoke" 2>/dev/null || \
  echo "Compilar: mvn compile"
```

---

### T07: Interface `ReminderRepository` — ✅

**What**: Criar contrato (interface Java) para persistência de lembretes.
**Where**: `backend/src/main/java/com/myroutine/repository/ReminderRepository.java`
**Depends on**: T06
**Reuses**: Padrão em `design.md`
**Requirements**: REM-01

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `ReminderRepository` interface com métodos: `save`, `findById`, `findByUserId`, `update`, `delete`
- [x] Tipos de retorno usam `Reminder` do domain
- [x] Seção no `backend/README.md`: "Conceito: Interface e Polimorfismo (DIP + ISP)"

**Tests**: manual (gate)
**Gate**: manual — `mvn compile` sem erro

---

### T08: `SupabaseReminderRepository` — ✅

**What**: Implementar `ReminderRepository` usando WebClient + PostgREST Supabase (service role).
**Where**: `backend/src/main/java/com/myroutine/repository/SupabaseReminderRepository.java`
**Depends on**: T03, T07
**Reuses**: `ReminderRepository`, schema `reminders`
**Requirements**: REM-01, REM-03

**Tools**:
- MCP: Supabase
- Skill: NONE

**Done when**:
- [x] `@Repository` implementa todos os métodos de `ReminderRepository`
- [x] Mapeia linhas DB ↔ objeto `Reminder`
- [x] `save` persiste `recurrence` (daily/weekly/none)
- [x] Erros de DB levantam exceção clara (`DataAccessException` ou custom)

**Tests**: manual (gate)
**Gate**: manual — CRUD via script de teste ou curl após T12

---

### T09: `ReminderService`

**What**: Camada de serviço que orquestra criação, listagem, conclusão e exclusão de lembretes.
**Where**: `backend/src/main/java/com/myroutine/service/ReminderService.java`
**Depends on**: T08
**Reuses**: `Reminder`, `ReminderRepository`
**Requirements**: REM-01, REM-03, REM-04

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `@Service` com `ReminderRepository` injetado via construtor (DI)
- [x] Métodos: `createReminder`, `listReminders`, `markDone`, `deleteReminder`
- [x] `createReminder` valida via classe `Reminder` (domínio)
- [x] Seção no `backend/README.md`: "Conceito: Injeção de Dependência e SRP"

**Tests**: manual (gate)
**Gate**: manual

---

### T10: DTOs (records) para lembretes — ✅

**What**: Records de entrada/saída para API de lembretes com Bean Validation.
**Where**: `backend/src/main/java/com/myroutine/api/dto/`
**Depends on**: T06
**Reuses**: Enums do domain
**Requirements**: REM-01, REM-04

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `CreateReminderRequest`: title (`@NotBlank`), dueAt (`@Future`), recurrence (optional)
- [x] `UpdateReminderRequest`: campos opcionais
- [x] `ReminderResponse`: todos os campos + id, source, createdAt
- [x] Validação Bean Validation para `dueAt` no futuro

**Tests**: manual (gate)
**Gate**: manual — `mvn compile` sem erro

---

### T11: Auth filter (JWT Supabase) — ✅

**What**: Filtro Spring Security que valida JWT do Supabase e extrai `user_id`.
**Where**: `backend/src/main/java/com/myroutine/api/security/JwtAuthFilter.java`, `SecurityConfig.java`
**Depends on**: T04, T01
**Reuses**: Supabase JWT secret (JJWT)
**Requirements**: AUTH-01, AUTH-03

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `JwtAuthFilter` valida Bearer token
- [x] Popula `UserPrincipal` com `id` e `email`
- [x] Token inválido/expirado → HTTP 401
- [x] Seção no `backend/README.md`: "Conceito: Segurança isolada (SRP)"

**Tests**: manual (gate)
**Gate**: manual — request sem token retorna 401

---

### T12: `POST /reminders` [P]

**What**: Endpoint para criar lembrete.
**Where**: `backend/src/main/java/com/myroutine/api/controller/ReminderController.java`
**Depends on**: T09, T10, T11
**Reuses**: `ReminderService`, DTOs
**Requirements**: REM-01, REM-04

**Done when**:
- [x] `POST /reminders` aceita `CreateReminderRequest`
- [x] Requer auth (`@AuthenticationPrincipal UserPrincipal`)
- [x] Retorna `ReminderResponse` 201
- [x] Data no passado → 422 com mensagem em português

**Tests**: manual (gate)
**Gate**: manual
```bash
curl -X POST http://localhost:8080/reminders -H "Authorization: Bearer <token>" -H "Content-Type: application/json" -d '{"title":"Teste","dueAt":"2026-12-31T10:00:00Z"}'
```

---

### T13: `GET /reminders` [P] — ✅

**What**: Endpoint para listar lembretes do usuário (filtros: status, source, date range).
**Where**: `backend/src/main/java/com/myroutine/api/controller/ReminderController.java`
**Depends on**: T09, T10, T11
**Reuses**: `ReminderService`
**Requirements**: REM-01, REM-03

**Done when**:
- [x] `GET /reminders` retorna lista de `ReminderResponse`
- [x] Query params: `status`, `source`, `from`, `to` (opcionais)
- [x] Requer auth

**Tests**: manual (gate)
**Gate**: manual — curl retorna JSON array

---

### T14: `PATCH /reminders/{id}` [P] — ✅

**What**: Endpoint para atualizar lembrete (marcar concluído, editar, snooze).
**Where**: `backend/src/main/java/com/myroutine/api/controller/ReminderController.java`
**Depends on**: T09, T10, T11
**Reuses**: `ReminderService`
**Requirements**: REM-03

**Done when**:
- [x] `PATCH /reminders/{id}` aceita `UpdateReminderRequest`
- [x] `status: done` chama `markDone()`
- [x] Lembrete de outro usuário → 404
- [x] Retorna `ReminderResponse`

**Tests**: manual (gate)
**Gate**: manual

---

### T15: `DELETE /reminders/{id}` [P] — ✅

**What**: Endpoint para excluir lembrete.
**Where**: `backend/src/main/java/com/myroutine/api/controller/ReminderController.java`
**Depends on**: T09, T11
**Reuses**: `ReminderService`
**Requirements**: REM-01

**Done when**:
- [x] `DELETE /reminders/{id}` retorna 204
- [x] Lembrete inexistente ou de outro usuário → 404

**Tests**: manual (gate)
**Gate**: manual

---

### T16: `GoogleCalendarService` — ✅

**What**: Classe de integração que lista eventos dos próximos 7 dias e faz upsert em `reminders`.
**Where**: `backend/src/main/java/com/myroutine/integration/GoogleCalendarService.java`
**Depends on**: T02, T08, T11
**Reuses**: Google API Client for Java, tabela `google_tokens`
**Requirements**: GCAL-01, GCAL-02

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `connect(userId, authCode)` troca code por refresh_token e salva criptografado
- [x] `syncEvents(userId)` importa eventos 7 dias com `source=google`
- [x] `external_id` evita duplicatas (upsert)
- [x] Token revogado → exceção `GoogleAuthError`
- [x] Seção no `backend/README.md`: "Conceito: Integração Externa como Classe"

**Tests**: manual (gate)
**Gate**: manual

---

### T17: `POST /google/connect` — ✅

**What**: Endpoint que inicia/completa OAuth do Calendar e armazena refresh token.
**Where**: `backend/src/main/java/com/myroutine/api/controller/GoogleController.java`
**Depends on**: T16
**Reuses**: `GoogleCalendarService`
**Requirements**: GCAL-01, GCAL-04

**Done when**:
- [x] `POST /google/connect` recebe `code` do OAuth flow
- [x] Requer auth Supabase JWT
- [x] Retorna status de conexão

**Tests**: manual (gate)
**Gate**: manual

---

### T18: `POST /google/sync` — ✅

**What**: Endpoint que dispara sincronização manual de eventos Google → lembretes.
**Where**: `backend/src/main/java/com/myroutine/api/controller/GoogleController.java`
**Depends on**: T16
**Reuses**: `GoogleCalendarService`
**Requirements**: GCAL-02, GCAL-04

**Done when**:
- [x] `POST /google/sync` importa eventos e retorna contagem
- [x] Sem token Google → 400 com mensagem "Conecte sua agenda"
- [x] Token revogado → 401 com mensagem para reconectar

**Tests**: unit — ApiExceptionHandlerGoogleTest (400/401 + type) + GoogleControllerSyncTest (sync count / 400 / 401)
**Gate**: `cd backend && mvn test` (Google*Sync* / ApiExceptionHandlerGoogle*) + curl manual opcional

---

### T19: Configurar JUnit 5 + testes de domínio — ✅

**What**: Setup JUnit 5 + Mockito e testes unitários para `Reminder` e `ReminderService` (mocks).
**Where**: `backend/src/test/java/com/myroutine/`
**Depends on**: T06, T09
**Reuses**: `TESTING.md`
**Requirements**: (qualidade — habilita gate automatizado)

**Done when**:
- [x] JUnit 5 + Mockito configurados no `pom.xml` (`spring-boot-starter-test`)
- [x] Testes: criar Reminder válido, rejeitar data passada, markDone
- [x] Testes: ReminderService com mock repository
- [x] `cd backend && mvn test` passa (mínimo 5 testes) — 19 testes no total

**Tests**: unit
**Gate**: `cd backend && mvn test`

---

### T20: Deploy Spring Boot no Render — ⚠️ Parcial (Blueprint/Dockerfile/README ✅, deploy dashboard pendente)

**What**: Deploy da API no Render free tier (JAR) com variáveis de ambiente e README de produção.
**Where**: `backend/render.yaml`, `backend/README.md`
**Depends on**: T12, T13, T14, T15, T17, T18, T19
**Reuses**: `.env.example`
**Requirements**: (infra produção)

**Tools**:
- MCP: NONE
- Skill: `render-deploy`

**Done when**:
- [ ] API acessível em URL pública `https://*.onrender.com` *(Blueprint Path: `backend/render.yaml` — conectar no Dashboard Render)*
- [ ] `GET /health` responde em produção *(após create do serviço)*
- [x] Variáveis de ambiente declaradas no `render.yaml` (`sync: false` — preencher no Dashboard)
- [x] README documenta cold start (~30s) e como rodar local (`mvn spring-boot:run`)
- [x] `Dockerfile` multi-stage empacota JAR; `application.yml` usa `PORT`

**Tests**: manual (gate)
**Gate**: manual — curl na URL de produção

---

### T21: Cliente Supabase no mobile

**What**: Configurar `@supabase/supabase-js` com persistência de sessão AsyncStorage.
**Where**: `mobile/src/lib/supabase.ts`
**Depends on**: T01, T05
**Reuses**: Expo secure storage patterns
**Requirements**: AUTH-01, AUTH-02

**Done when**:
- [ ] Cliente exportado com `createClient`
- [ ] Sessão persiste entre reinícios do app
- [ ] Tipos TypeScript para `Note` (tabela notes)

**Tests**: manual (gate)
**Gate**: `cd mobile && npm run lint`

---

### T22: Cliente API Spring Boot no mobile

**What**: HTTP client que injeta JWT Supabase nos requests ao back-end Java.
**Where**: `mobile/src/lib/api.ts`
**Depends on**: T04, T21
**Reuses**: `supabase.ts` para obter token
**Requirements**: REM-01, GCAL-02

**Done when**:
- [ ] Funções: `createReminder`, `listReminders`, `updateReminder`, `deleteReminder`
- [ ] Funções: `connectGoogle`, `syncGoogle`
- [ ] Header `Authorization: Bearer` automático
- [ ] Tratamento de erro 401 → redirect login

**Tests**: manual (gate)
**Gate**: `cd mobile && npm run lint`

---

### T23: Root layout + auth guard

**What**: Layout raiz que redireciona para login ou tabs conforme sessão.
**Where**: `mobile/src/app/_layout.tsx`, `mobile/src/app/(auth)/login.tsx` (stub)
**Depends on**: T21, T05
**Reuses**: Expo Router groups
**Requirements**: AUTH-02, AUTH-03, UI-02

**Done when**:
- [ ] Sem sessão → redirect `/(auth)/login`
- [ ] Com sessão → redirect `/(tabs)`
- [ ] Splash screen durante verificação de sessão
- [ ] Token refresh automático via Supabase

**Tests**: manual (gate)
**Gate**: manual — fluxo login/logout

---

### T24: Tela de login Google

**What**: Tela com botão "Entrar com Google" usando Supabase OAuth + expo-web-browser.
**Where**: `mobile/src/app/(auth)/login.tsx`
**Depends on**: T21, T23
**Reuses**: `@primo-brutality/ui` Button
**Requirements**: AUTH-01, AUTH-04, UI-01

**Done when**:
- [ ] Botão abre fluxo OAuth Google
- [ ] Sucesso → navega para `/(tabs)`
- [ ] Erro → Alert em português
- [ ] Visual brutalista (Button + Card da lib)

**Tests**: manual (gate)
**Gate**: manual — login completo no dispositivo/emulador

---

### T25: Tab navigation layout [P]

**What**: Layout de abas: Hoje, Notas, Lembretes, Agenda, Perfil.
**Where**: `mobile/src/app/(tabs)/_layout.tsx`
**Depends on**: T23
**Reuses**: Expo Router Tabs, ícones expo-symbols
**Requirements**: UI-02, UI-03

**Done when**:
- [ ] 5 abas navegáveis com ícones
- [ ] Estilo tab bar compatível com tema brutalista
- [ ] Arquivos placeholder: `index.tsx`, `notes.tsx`, `reminders.tsx`, `agenda.tsx`, `profile.tsx`

**Tests**: manual (gate)
**Gate**: `cd mobile && npm run lint`

---

### T26: Lista de notas (Supabase direto) [P]

**What**: Tela que lista notas do usuário via Supabase client.
**Where**: `mobile/src/app/(tabs)/notes.tsx`, `mobile/src/hooks/useNotes.ts`
**Depends on**: T21, T25
**Reuses**: `supabase.ts`, `@primo-brutality/ui` Card
**Requirements**: NOTE-01, NOTE-04, UI-01

**Done when**:
- [ ] Lista notas ordenadas por `updated_at` desc
- [ ] Pull-to-refresh funciona
- [ ] FAB ou botão "Nova nota" visível
- [ ] Empty state brutalista quando lista vazia

**Tests**: manual (gate)
**Gate**: manual

---

### T27: Formulário criar/editar nota [P]

**What**: Modal ou tela para criar e editar nota (título + corpo).
**Where**: `mobile/src/app/notes/[id].tsx`, `mobile/src/app/notes/new.tsx`
**Depends on**: T21, T26
**Reuses**: `@primo-brutality/ui` Input, TextArea
**Requirements**: NOTE-01, NOTE-02, NOTE-03

**Done when**:
- [ ] Criar nota salva no Supabase com `user_id`
- [ ] Editar atualiza `updated_at`
- [ ] Excluir com confirmação (Alert)
- [ ] Validação: título obrigatório

**Tests**: manual (gate)
**Gate**: manual — CRUD completo de notas

---

### T28: Lista de lembretes (via API) [P]

**What**: Tela que lista lembretes do back-end com filtro por status.
**Where**: `mobile/src/app/(tabs)/reminders.tsx`, `mobile/src/hooks/useReminders.ts`
**Depends on**: T22, T25
**Reuses**: `api.ts`, `@primo-brutality/ui`
**Requirements**: REM-01, REM-03, UI-01

**Done when**:
- [ ] Lista lembretes via `GET /reminders`
- [ ] Badge de status: pendente / concluído
- [ ] Swipe ou botão para marcar concluído (`PATCH`)
- [ ] Exibe recorrência (diário/semanal) se houver

**Tests**: manual (gate)
**Gate**: manual

---

### T29: Formulário criar lembrete com recorrência [P]

**What**: Formulário com título, data/hora e dropdown recorrência (nenhuma/diário/semanal).
**Where**: `mobile/src/app/reminders/new.tsx`
**Depends on**: T22, T28
**Reuses**: `@primo-brutality/ui` Input, Select/Button
**Requirements**: REM-01, REM-04, UI-01

**Done when**:
- [ ] DateTimePicker para `due_at`
- [ ] Dropdown: sem recorrência / diário / semanal
- [ ] Submit chama `POST /reminders`
- [ ] Data no passado → mensagem de erro em português

**Tests**: manual (gate)
**Gate**: manual

---

### T30: Aba Agenda (eventos Google) [P]

**What**: Tela que exibe eventos importados do Google com badge "Google".
**Where**: `mobile/src/app/(tabs)/agenda.tsx`
**Depends on**: T22, T25
**Reuses**: `api.ts`, `@primo-brutality/ui` Badge
**Requirements**: GCAL-03, UI-01

**Done when**:
- [ ] Lista lembretes com `source=google`
- [ ] Badge "Google" em cada item
- [ ] Botão "Sincronizar" chama `POST /google/sync`
- [ ] Estado vazio: "Conecte sua agenda no Perfil"

**Tests**: manual (gate)
**Gate**: manual

---

### T31: Tela Perfil + conectar Google Agenda [P]

**What**: Perfil do usuário, logout e fluxo para conectar Google Calendar.
**Where**: `mobile/src/app/(tabs)/profile.tsx`
**Depends on**: T21, T22, T25
**Reuses**: Supabase auth, Google OAuth
**Requirements**: GCAL-01, GCAL-04, AUTH-02

**Done when**:
- [ ] Exibe nome e e-mail do usuário
- [ ] Botão "Conectar Google Agenda" abre OAuth com escopo calendar
- [ ] Status: conectado / não conectado
- [ ] Botão logout limpa sessão
- [ ] Erro de token revogado → CTA reconectar

**Tests**: manual (gate)
**Gate**: manual

---

### T32: Notificações locais (expo-notifications)

**What**: Agendar notificação local ao criar lembrete; cancelar ao concluir/excluir.
**Where**: `mobile/src/lib/notifications.ts`, integração em `useReminders.ts`
**Depends on**: T28, T29
**Reuses**: `expo-notifications`
**Requirements**: REM-02

**Done when**:
- [ ] Permissão solicitada no primeiro lembrete
- [ ] `scheduleNotificationAsync` com `due_at` do lembrete
- [ ] Permissão negada → lembrete salvo sem notificação (sem crash)
- [ ] Concluir/excluir cancela notificação agendada

**Tests**: manual (gate)
**Gate**: manual — lembrete em 2 min dispara notificação

---

### T33: Empty states brutalistas [P]

**What**: Componente reutilizável de empty state para Notas, Lembretes e Agenda.
**Where**: `mobile/src/components/EmptyState.tsx`
**Depends on**: T26, T28, T30
**Reuses**: `@primo-brutality/ui`
**Requirements**: NOTE-04, UI-01

**Done when**:
- [ ] Props: `title`, `description`, `actionLabel`, `onAction`
- [ ] Visual brutalista (Card + Button)
- [ ] Usado nas 3 listas quando vazias

**Tests**: manual (gate)
**Gate**: `cd mobile && npm run lint`

---

### T34: Dashboard "Hoje" (Home)

**What**: Tela inicial com resumo: próximos lembretes, notas recentes, eventos Google do dia.
**Where**: `mobile/src/app/(tabs)/index.tsx`
**Depends on**: T25, T26, T28, T30, T32
**Reuses**: hooks `useNotes`, `useReminders`
**Requirements**: UI-01, UI-02 (decisão context: dashboard Hoje)

**Done when**:
- [ ] Seção "Próximos lembretes" (hoje, pendentes, máx 5)
- [ ] Seção "Notas recentes" (máx 3)
- [ ] Seção "Agenda de hoje" (eventos Google)
- [ ] Saudação com nome do usuário
- [ ] Links rápidos para criar nota/lembrete

**Tests**: manual (gate)
**Gate**: manual — dashboard reflete dados reais

---

### T35: Estados de loading e erro (cold start Render)

**What**: Loading spinners e mensagem amigável quando API está em cold start ou offline.
**Where**: `mobile/src/components/LoadingOverlay.tsx`, `mobile/src/lib/api.ts`
**Depends on**: T22, T24
**Reuses**: `@primo-brutality/ui` Spinner
**Requirements**: (edge case spec: Render cold start)

**Done when**:
- [ ] Request > 5s mostra "Aguarde, servidor iniciando..."
- [ ] Sem internet → mensagem offline em português
- [ ] Retry automático 1x em timeout

**Tests**: manual (gate)
**Gate**: manual

---

## Task Breakdown — M2 (futuro, resumo)

| ID | Task | Requirements | Depende de M1 |
|----|------|--------------|---------------|
| T36 | Alexa Skill + Account Linking | ALEXA-01–03 | T20 |
| T37 | Intent `GetTodayReminders` | ALEXA-01, ALEXA-02 | T36 |
| T38 | `GmailService` + filtro Nubank | AUTO-01 | T20 |
| T39 | Tela aprovação de lembrete sugerido | AUTO-02, AUTO-03 | T38 |
| T40 | Share intent Android | SHARE-01, SHARE-02 | T29 |

---

## Task Breakdown — M3 (futuro, resumo)

| ID | Task | Requirements |
|----|------|--------------|
| T41 | Sync bidirecional Google Calendar | (novo spec) |
| T42 | Offline-first com cache local | (novo spec) |
| T43 | Widget Android próximo lembrete | (novo spec) |

---

## Requirement Traceability (M1)

| Requirement | Task(s) |
|-------------|---------|
| AUTH-01 | T01, T02, T11, T21, T24 |
| AUTH-02 | T21, T23, T31 |
| AUTH-03 | T11, T23 |
| AUTH-04 | T24 |
| NOTE-01 | T03, T26, T27 |
| NOTE-02 | T27 |
| NOTE-03 | T27 |
| NOTE-04 | T26, T33 |
| REM-01 | T03, T06–T09, T12, T13, T22, T28, T29 |
| REM-02 | T32 |
| REM-03 | T06, T09, T14, T28 |
| REM-04 | T06, T10, T12, T29 |
| GCAL-01 | T02, T16, T17, T31 |
| GCAL-02 | T16, T18, T22, T30 |
| GCAL-03 | T30, T34 |
| GCAL-04 | T17, T18, T31 |
| UI-01 | T05, T24–T34 |
| UI-02 | T23, T25, T34 |
| UI-03 | T05, T25 |

**Coverage M1:** 19 requisitos P1 → 35 tasks (T01–T35)
**Coverage M2/M3:** 8 requisitos → T36–T43 (planejado)

---

## Parallel Execution Map

```
Phase 1:  T01 ║ T02 ║ T04 ║ T05
Phase 2:  T03
Phase 3:  T06 → T07 → T08 → T09 → T10
          T11 (parallel com T06–T09 após T04+T01)
Phase 4:  T12 ║ T13 ║ T14 ║ T15  (após T09,T10,T11)
          T17 → T18 (após T16)
          T16 (após T02,T08,T11)
          T19 (após T06,T09)
Phase 5:  T20 (após API + T19)
Phase 6:  T21 → T22 → T23 → T24
Phase 7:  T25 ║ T26 ║ T27 ║ T28 ║ T29 ║ T30 ║ T31  (após T24)
Phase 8:  T32 → T33 → T34 → T35
```

---

## Task Granularity Check

| Task | Scope | Status |
|------|-------|--------|
| T01: Supabase setup | 1 infra deliverable | ✅ Granular |
| T06: Reminder domain class | 1 class | ✅ Granular |
| T12: POST /reminders | 1 endpoint | ✅ Granular |
| T26: Notes list | 1 screen + 1 hook | ✅ Granular |
| T34: Dashboard Hoje | 1 screen (composição) | ✅ Granular |
| T16: GoogleCalendarService | 1 integration class | ✅ Granular |

**Granularity check**: ✅ Todas as tasks M1 são atômicas (1 endpoint, 1 classe, 1 tela ou 1 config).

---

## Diagram-Definition Cross-Check

| Task | Depends On (body) | Diagram Shows | Status |
|------|-------------------|---------------|--------|
| T01 | None | Phase 1 root | ✅ |
| T02 | None | Phase 1 root | ✅ |
| T03 | T01 | T01 → T03 | ✅ |
| T04 | None | Phase 1 root | ✅ |
| T05 | None | Phase 1 root | ✅ |
| T06 | T04 | T04 → T06 | ✅ |
| T07 | T06 | T06 → T07 | ✅ |
| T08 | T03, T07 | T03 → T08 | ✅ |
| T09 | T08 | T08 → T09 | ✅ |
| T10 | T06 | T06 → T10 | ✅ |
| T11 | T04, T01 | T04+T01 → T11 | ✅ |
| T12–T15 | T09, T10, T11 | Parallel após T11 | ✅ |
| T16 | T02, T08, T11 | T02,T11,T09 → T16 | ✅ |
| T17–T18 | T16 | T16 → T17 → T18 | ✅ |
| T19 | T06, T09 | Após domain+service | ✅ |
| T20 | T12–T19 | Final API phase | ✅ |
| T21 | T01, T05 | T05 → T21 | ✅ |
| T22 | T04, T21 | T21 → T22 | ✅ |
| T23 | T21, T05 | T21 → T23 | ✅ |
| T24 | T21, T23 | T23 → T24 | ✅ |
| T25–T31 | T23/T25 + deps | Phase 5 parallel | ✅ |
| T32 | T28, T29 | T28,T29 → T32 | ✅ |
| T33 | T26, T28, T30 | Phase 6 | ✅ |
| T34 | T25–T30, T32 | Phase 6 final | ✅ |
| T35 | T22, T24 | Phase 6 | ✅ |

**Cross-check**: ✅ Diagrama consistente com dependências.

---

## Test Co-location Validation

| Task | Code Layer | Matrix Requires | Task Says | Status |
|------|------------|-----------------|-----------|--------|
| T01–T05, T07–T17, T20–T35 | Config/UI/routes / gates manuais | manual (gate) | manual (gate) | ✅ OK |
| T06 | domain | unit (planejado T19) | unit (ReminderTest / T19) | ✅ OK |
| T18 | Google sync API | unit | unit — ApiExceptionHandlerGoogleTest + GoogleControllerSyncTest | ✅ OK |
| T19 | Java domain/services | unit | unit | ✅ OK |

**Nota:** `TESTING.md` define JUnit 5 como gate principal. T19 configura o gate e cobre `Reminder` / `ReminderService`. T06 aponta para `ReminderTest` (T19); T18 tem testes unitários próprios. Demais tasks usam `manual (gate)`.

---

## Próximo passo (Execute)

Ordem sugerida para começar implementação:

1. **T01 + T02 + T04 + T05** em paralelo (infra)
2. **T03** (migrations)
3. **T06 → T15** (back-end core)
4. **T21 → T24** (mobile auth)
5. **T25 → T31** em paralelo (telas)
6. **T32 → T35** (polish MVP)

**Commit sugerido por task:** `feat(scope): descrição curta` — ex.: `feat(backend): add Reminder domain class`

**Roadmap Java:** `.specs/project/JAVA-ROADMAP.md`
