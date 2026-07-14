# My Routine — Back-end (Spring Boot)

API REST em Java para lembretes, integração Google Calendar e Alexa.

## Desenvolvimento local

**Pré-requisitos:** Java 21+, Maven 3.9+

```bash
cd backend
mvn spring-boot:run
```

Verificar saúde:

```bash
curl http://localhost:8080/health
# {"status":"ok"}
```

Build e testes:

```bash
mvn clean test
```

## Estrutura de pacotes

O back-end segue **Programação Orientada a Objetos** e **SOLID** com camadas separadas.
Roadmap completo: `.specs/project/JAVA-ROADMAP.md`

```
backend/src/main/java/com/myroutine/
├── MyRoutineApplication.java    # Ponto de entrada Spring Boot
├── api/
│   ├── controller/              # Controllers REST — recebem HTTP, delegam aos services
│   ├── dto/                     # Records de request/response (Bean Validation)
│   └── security/                # Filtro JWT Supabase
├── domain/                      # Entidades de domínio (Reminder) — regras e validação
├── service/                     # Lógica de negócio — orquestra domain + repositories
├── repository/                  # Interfaces + implementações Supabase
├── integration/                 # APIs externas (Google Calendar, Alexa)
└── config/                      # Beans Spring, CORS, properties
```

| Pacote | Conceito POO/SOLID | Responsabilidade |
|--------|-------------------|------------------|
| `domain/` | Classe, encapsulamento, Enum | Objetos com estado válido e comportamentos (`markDone()`, etc.) |
| `repository/` | Interface, DIP, LSP | Contrato de persistência; implementações trocáveis |
| `service/` | Composição, DI, SRP | Orquestra domínio + repositórios; sem HTTP nem SQL direto |
| `api/controller/` | SRP | Traduz HTTP ↔ serviços; valida entrada, formata saída |
| `integration/` | OCP | Adapters para APIs externas |

**Fluxo de uma requisição:** `controller` → `service` → `domain` + `repository` → resposta JSON.

As camadas restantes serão implementadas nas tasks T07–T19.

## Conceito: Classe e Encapsulamento

A entidade `Reminder` (`domain/Reminder.java`) ilustra os pilares de POO no domínio:

| Conceito | Onde aparece | O que faz |
|----------|--------------|-----------|
| **Classe** | `class Reminder` | Molde que define estrutura e comportamento |
| **Instância** | `new Reminder("Reunião", dueAt, userId)` | Objeto concreto criado a partir da classe |
| **Construtor** | `Reminder(...)` | Inicializa atributos e valida entrada |
| **Atributos privados** | `title`, `dueAt`, `status` | Estado interno — acesso via getters |
| **Métodos** | `markDone()`, `snooze()` | Comportamentos que alteram estado interno |
| **Enum** | `ReminderStatus`, `Recurrence` | Conjunto fechado de valores válidos |

**Encapsulamento em ação:** título vazio ou data no passado disparam `IllegalArgumentException` no construtor — a classe recusa estado inválido. Métodos como `markDone()` e `snooze()` alteram `status` e `dueAt` internamente, sem expor atribuição direta.

## Conceito: Interface e Polimorfismo (DIP + ISP)

A interface `ReminderRepository` (`repository/ReminderRepository.java`) define **o que** o domínio precisa persistir, sem dizer **como** (PostgREST, SQL, memória):

| Conceito | Onde aparece | O que faz |
|----------|--------------|-----------|
| **Interface** | `interface ReminderRepository` | Contrato com `save`, `findById`, `findByUserId`, `update`, `delete` |
| **DIP** | `ReminderService` (T09) dependerá da interface | Camada de negócio não conhece Supabase — só o contrato |
| **ISP** | Métodos mínimos para lembretes | Sem operações genéricas de banco que o service não usa |
| **Polimorfismo** | `SupabaseReminderRepository implements ReminderRepository` (T08) | Spring injeta a implementação; o service chama métodos do contrato |

**Por que interface e não classe concreta?** `ReminderService` orquestra regras de negócio; persistência é responsabilidade separada (SRP). A interface permite trocar a implementação (Supabase hoje, in-memory em testes) sem reescrever o service — princípio Aberto/Fechado na prática.

## Conceito: Adapter e LSP (implementação Supabase)

`SupabaseReminderRepository` (`repository/SupabaseReminderRepository.java`) é o **adapter** concreto que fala PostgREST:

| Conceito | Onde aparece | O que faz |
|----------|--------------|-----------|
| **Implementação** | `class SupabaseReminderRepository implements ReminderRepository` | Traduz chamadas do contrato em HTTP para `/rest/v1/reminders` |
| **Composição** | `WebClient supabaseClient` injetado | Não implementa HTTP na mão — reutiliza cliente Spring |
| **LSP** | Mesmos métodos da interface | `ReminderService` não sabe se os dados vêm do Supabase ou de um mock |
| **Mapeamento** | `ReminderRow` ↔ `Reminder.fromPersistence(...)` | Colunas snake_case do Postgres viram objeto de domínio |
| **Erros** | `ReminderPersistenceException` | Falhas HTTP/PostgREST viram exceção de persistência clara |

**Service role:** o `WebClient` usa `SUPABASE_SERVICE_ROLE_KEY` (servidor only) para bypass de RLS — lembretes são filtrados pela aplicação via `user_id` do JWT (T11).

## Conceito: Injeção de Dependência e SRP

`ReminderService` (`service/ReminderService.java`) é a camada que **orquestra** casos de uso sem conhecer HTTP nem PostgREST:

| Conceito | Onde aparece | O que faz |
|----------|--------------|-----------|
| **DI (Injeção de Dependência)** | `ReminderService(ReminderRepository repo)` | Spring fornece a implementação concreta no construtor — o service não faz `new SupabaseReminderRepository()` |
| **SRP** | Métodos `createReminder`, `listReminders`, `markDone`, `deleteReminder` | Cada método coordena um fluxo; validação fica no domínio, persistência no repositório |
| **Composição** | Usa `Reminder` + `ReminderRepository` | O service não herda de ninguém — compõe dependências para cumprir regras de negócio |
| **DIP** | Campo `private final ReminderRepository` | Depende da interface, não da implementação Supabase |

**Fluxo `createReminder`:** controller passa título/data/userId → `new Reminder(...)` valida no construtor → `repository.save()` persiste. **Fluxo `markDone`:** busca por id, confere `user_id`, chama `reminder.markDone()` no domínio → `repository.update()`.

## Conceito: Segurança isolada (SRP)

Autenticação JWT fica em `api/security/` — controllers e services **não** validam tokens:

| Componente | Onde | O que faz |
|------------|------|-----------|
| **JwtAuthFilter** | `api/security/JwtAuthFilter.java` | Lê `Authorization: Bearer`, valida assinatura HS256 com `SUPABASE_JWT_SECRET` |
| **UserPrincipal** | `api/security/UserPrincipal.java` | Carrega `id` (claim `sub`) e `email` do payload Supabase |
| **SecurityConfig** | `config/SecurityConfig.java` | Rotas públicas (`/health`) vs protegidas; sessão stateless; 401 sem auth |
| **SRP** | Filtro ≠ controller ≠ service | Cada camada tem uma responsabilidade; JWT não vaza para domínio |

**Fluxo:** request → `JwtAuthFilter` extrai JWT → `UserPrincipal` no `SecurityContext` → controller usa `@AuthenticationPrincipal UserPrincipal user` → service recebe `userId` já confiável. Token inválido ou expirado → HTTP **401** antes de chegar ao controller.

## Conceito: Integração Externa como Classe

APIs externas (Google Calendar, futuramente Alexa/Gmail) ficam em `integration/` — **adapters** com contrato próprio, sem misturar OAuth/HTTP com controllers de lembretes:

| Conceito | Onde aparece | O que faz |
|----------|--------------|-----------|
| **Port (ISP)** | `GoogleCalendarPort` | Contrato mínimo: `connect`, `syncEvents` — quem consome não vê a API Google |
| **Adapter (OCP)** | `GoogleCalendarService implements GoogleCalendarPort` | Troca code→refresh_token, lista eventos 7 dias, upsert com `source=google` |
| **Criptografia** | `TokenCipher` (AES-256-GCM) | Refresh token só vai para `google_tokens` já cifrado |
| **Persistência** | `GoogleTokenRepository` + `ReminderRepository.upsertGoogleEvent` | Tokens e lembretes via PostgREST (service role) |
| **Erro de auth** | `GoogleAuthError` | Token ausente/revogado — o endpoint (T17/T18) traduz em 400/401 |

**Fluxo `connect`:** authorization code → `GoogleAuthorizationCodeTokenRequest` → refresh_token → `TokenCipher.encrypt` → `google_tokens`. **Fluxo `syncEvents`:** descriptografa token → Calendar API `events.list` (agora → +7 dias) → upsert por `external_id` (sem duplicar no re-sync). Token revogado → `GoogleAuthError`.
