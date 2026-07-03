# Camadas do Back-end — My Routine

Guia de estudo para fixar o papel de cada camada no Spring Boot do projeto **My Routine**.
Baseado no código atual em `backend/src/main/java/com/myroutine/` e no roadmap em `backend/README.md`.

---

## Visão geral

O back-end segue **arquitetura em camadas** (layered architecture): cada pacote tem uma responsabilidade clara e depende apenas das camadas **abaixo** dele — nunca do contrário.

```
┌─────────────────────────────────────────────────────────────────┐
│  Cliente (app mobile, curl, Postman)                            │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP (JSON)
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  api/controller     — porta de entrada HTTP                     │
│  api/dto            — contrato JSON (request/response)           │
│  api/security       — JWT, autenticação (planejado)             │
└────────────────────────────┬────────────────────────────────────┘
                             │ objetos Java (DTO → primitivos/enums)
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  service            — casos de uso / orquestração               │
└────────────────────────────┬────────────────────────────────────┘
                             │ domain.Reminder, enums
                             ▼
┌──────────────────────┐     ┌────────────────────────────────────┐
│  domain              │◄────│  repository (interface)            │
│  regras + estado     │     │  contrato de persistência          │
└──────────────────────┘     └────────────┬───────────────────────┘
                                          │ implementação
                                          ▼
                             ┌────────────────────────────────────┐
                             │  SupabaseReminderRepository        │
                             │  (+ records internos de mapeamento)│
                             └────────────┬───────────────────────┘
                                          │ HTTP PostgREST
                                          ▼
                             ┌────────────────────────────────────┐
                             │  Supabase / PostgreSQL             │
                             └────────────────────────────────────┘

        config — atravessa tudo: beans, properties, WebClient
```

**Regra de ouro:** dados descem como **comandos/consultas**; respostas sobem como **DTOs** (ou domínio convertido). O domínio **nunca** conhece HTTP, JSON ou Supabase.

---

## Mapa de pacotes

| Pacote | Status no projeto | Arquivos de referência |
|--------|-------------------|------------------------|
| `domain/` | ✅ Implementado | `Reminder.java`, `ReminderStatus.java`, `Recurrence.java` |
| `repository/` | ✅ Implementado | `ReminderRepository.java`, `SupabaseReminderRepository.java` |
| `service/` | ✅ Implementado | `ReminderService.java` |
| `config/` | ✅ Implementado | `SupabaseConfig.java`, `SupabaseProperties.java` |
| `api/controller/` | ⚠️ Parcial | `HealthController.java` (só `/health`; REST de lembretes vem depois) |
| `api/dto/` | 📋 Planejado | Records de request/response com Bean Validation |
| `api/map/` ou `mapper/` | 📋 Planejado | Conversão explícita DTO ↔ Domain ↔ Response |
| `api/security/` | 📋 Planejado | Filtro JWT Supabase |
| `integration/` | 📋 Planejado | Google Calendar, Alexa |

---

## 1. Domain (domínio)

**O que é:** o coração da aplicação. Objetos que representam conceitos do negócio com **regras e comportamentos**, independentes de framework, banco ou HTTP.

**Pacote:** `com.myroutine.domain`

### Responsabilidades

- Validar invariantes no construtor (`título não vazio`, `data não no passado` para criação).
- Expor comportamentos de negócio (`markDone()`, `snooze()`).
- Usar enums para valores fechados (`ReminderStatus`, `Recurrence`).
- Recusar estado inválido com `IllegalArgumentException`.

### O que NÃO faz

- Não sabe o que é `@RestController`, `WebClient` ou SQL.
- Não serializa JSON.
- Não lê variáveis de ambiente.

### Padrões no projeto

| Padrão | Exemplo |
|--------|---------|
| Encapsulamento | Campos `private`; alteração via métodos, não setters públicos soltos |
| Factory para reidratação | `Reminder.fromPersistence(...)` — reconstrói do banco **sem** revalidar “data futura” |
| Enum com valor de persistência | `ReminderStatus.PENDING` ↔ `"pending"` via `getValue()` / `fromValue()` |

### Trecho-chave

```java
// Criação nova — valida data futura
public Reminder(String title, Instant dueAt, String userId, Recurrence recurrence) { ... }

// Leitura do banco — pula validação de data passada
public static Reminder fromPersistence(...) {
    return new Reminder(..., true);
}

public void markDone() {
    this.status = ReminderStatus.DONE;
}
```

### Perguntas para se testar

1. Por que `fromPersistence` existe em vez de usar o construtor público?
2. Onde deve ficar a regra “só o dono pode marcar como feito”? **Resposta:** no `service` (autorização), não no `domain` — o domínio só sabe marcar como feito.
3. `Reminder` deveria ter anotações Jackson (`@JsonProperty`)? **Resposta:** não — isso acopla domínio à API.

---

## 2. Repository (repositório)

**O que é:** abstração de **persistência**. Define *o que* pode ser salvo/busca/atualizado/deletado, sem expor *como* (PostgREST, JDBC, memória).

**Pacote:** `com.myroutine.repository`

### Duas partes

1. **Interface** (`ReminderRepository`) — contrato estável que o `service` consome.
2. **Implementação** (`SupabaseReminderRepository`) — adapter que fala HTTP com Supabase.

### Responsabilidades

- CRUD e consultas (`save`, `findById`, `findByUserId`, `update`, `delete`).
- Traduzir linhas do banco ↔ objetos `domain` (mapeamento interno).
- Traduzir falhas de infraestrutura em exceções de persistência (`ReminderPersistenceException`).

### O que NÃO faz

- Não decide regras de negócio (“usuário X pode ver lembrete Y?” → `service`).
- Não expõe endpoints HTTP.
- Não valida título vazio (já validado no domínio antes do `save`).

### SOLID aplicado

| Princípio | Como aparece |
|-----------|--------------|
| **DIP** | `ReminderService` depende de `ReminderRepository`, não de `SupabaseReminderRepository` |
| **LSP** | Qualquer implementação da interface pode ser injetada (Supabase, in-memory em testes) |
| **ISP** | Interface enxuta — só métodos que o service realmente usa |

### Mapeamento hoje (dentro do repository)

O projeto ainda **não** tem pacote `map/` separado. O mapeamento vive como **records privados** dentro de `SupabaseReminderRepository`:

- `ReminderRow` — formato que o PostgREST devolve (snake_case: `user_id`, `due_at`).
- `ReminderWrite` — payload de insert.
- `toDomain(ReminderRow)` — chama `Reminder.fromPersistence(...)`.

Isso é válido no MVP; um pacote `map/` separado faz sentido quando DTOs de API e linhas de banco divergirem mais.

### Fluxo mental

```
service chama repository.save(reminder)
    → ReminderWrite.fromDomain(reminder)
    → POST /rest/v1/reminders
    → List<ReminderRow> na resposta
    → toDomain(row) → Reminder
```

---

## 3. Service (serviço)

**O que é:** camada de **casos de uso**. Orquestra domínio + repositório para cumprir uma intenção do usuário (“criar lembrete”, “marcar como feito”).

**Pacote:** `com.myroutine.service`

### Responsabilidades

- Coordenar passos de um fluxo (buscar → validar ownership → alterar domínio → persistir).
- Aplicar regras que cruzam entidades ou contexto (`userId` do JWT vs `reminder.getUserId()`).
- Lançar exceções de aplicação (`NoSuchElementException` quando não encontra ou não é dono).

### O que NÃO faz

- Não parseia query strings nem status HTTP.
- Não monta JSON de resposta.
- Não abre conexão HTTP com Supabase diretamente.

### Exemplo real: `markDone`

```java
public Reminder markDone(UUID id, String userId) {
    Reminder reminder = requireOwnedReminder(id, userId);  // autorização
    reminder.markDone();                                    // domínio
    return reminderRepository.update(id, reminder);       // persistência
}
```

### Onde colocar cada tipo de lógica

| Tipo de lógica | Camada |
|----------------|--------|
| “Título não pode ser vazio” | `domain` |
| “Data não pode ser no passado” (criação) | `domain` |
| “Só o dono pode deletar” | `service` |
| “Ordenar por `due_at` ascendente” | `repository` (query) |
| “Retornar 404 se não existir” | `controller` (+ `@ExceptionHandler`) |

### Injeção de dependência

```java
@Service
public class ReminderService {
    private final ReminderRepository reminderRepository;

    public ReminderService(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }
}
```

Spring injeta `SupabaseReminderRepository` automaticamente porque ele tem `@Repository` e implementa a interface.

---

## 4. Controller (controlador REST)

**O que é:** **borda HTTP**. Traduz requisições em chamadas de service e devolve respostas HTTP (status + corpo).

**Pacote:** `com.myroutine.api.controller`

### Responsabilidades

- Mapear rotas (`@GetMapping`, `@PostMapping`, …).
- Validar entrada superficial (formato JSON, campos obrigatórios via `@Valid` nos DTOs).
- Extrair contexto HTTP (headers, path variables, usuário autenticado).
- Converter resultado do service em DTO de response + status (`201 Created`, `404`, etc.).

### O que NÃO faz

- Não instancia `Reminder` com regras complexas sem passar pelo service.
- Não chama `WebClient` / repository diretamente.
- Não contém regra de negócio (“se status é DONE, não pode snooze”).

### Estado atual no projeto

Só existe `HealthController`:

```java
@RestController
public class HealthController {
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
```

É um controller **mínimo** — sem service, sem DTO — adequado para health check.

### Controller futuro (lembrete) — esqueleto esperado

```java
@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private final ReminderService reminderService;
    private final ReminderMapper reminderMapper;

    @PostMapping
    public ResponseEntity<ReminderResponse> create(
            @Valid @RequestBody CreateReminderRequest request,
            @AuthenticationPrincipal String userId) {

        Reminder created = reminderService.createReminder(
                request.title(),
                request.dueAt(),
                userId,
                request.recurrence());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reminderMapper.toResponse(created, /* id do banco */));
    }
}
```

---

## 5. DTO (Data Transfer Object)

**O que é:** objetos **só para transporte** — formato estável do contrato REST (JSON). Normalmente `record` imutáveis com Bean Validation.

**Pacote planejado:** `com.myroutine.api.dto`

### Por que existem (se já temos `domain`?)

| Aspecto | Domain | DTO |
|---------|--------|-----|
| Propósito | Regras de negócio | Contrato da API |
| Evolução | Muda com o negócio | Muda com versão da API (`v1`, `v2`) |
| Campos expostos | Todos os internos relevantes | Só o que o cliente precisa ver |
| Validação | Invariantes de negócio | Formato, tamanho, `@NotBlank`, `@Future` |
| Framework | Zero dependência web | `@Valid`, Jackson |

### Tipos comuns

```
request/
  CreateReminderRequest.java   → POST body
  UpdateReminderRequest.java   → PATCH body
  SnoozeReminderRequest.java   → POST /{id}/snooze

response/
  ReminderResponse.java        → um lembrete
  ReminderListResponse.java    → lista paginada (se necessário)
```

### Exemplo conceitual

```java
public record CreateReminderRequest(
        @NotBlank String title,
        @NotNull @Future Instant dueAt,
        Recurrence recurrence
) {}
```

```java
public record ReminderResponse(
        UUID id,
        String title,
        Instant dueAt,
        String status,
        String recurrence
) {}
```

### O que NÃO faz

- Não chama repository.
- Não contém `markDone()` — isso é domínio.
- Não deve ser usado dentro de `SupabaseReminderRepository` (use records de persistência).

---

## 6. Map / Mapper (mapeamento)

**O que é:** conversão **explícita** entre representações. Evita espalhar `new ReminderResponse(...)` pelo controller e mantém uma única fonte de verdade para cada transformação.

**Pacote planejado:** `com.myroutine.api.map` ou `com.myroutine.api.mapper`

### Três “mundos” de objetos

```
JSON (wire)  ←→  DTO (api)  ←→  Domain  ←→  Row/Write (repository)
     ↑              ↑              ↑              ↑
  Jackson      Mapper API     Service/Domain   Mapper persistência
```

### Responsabilidades do mapper de API

- `toResponse(Reminder domain, UUID id)` — domain → DTO de saída.
- `toDomain(CreateReminderRequest dto, String userId)` — opcional; muitas vezes o service recebe campos soltos e o domínio valida no construtor.

### Onde o projeto mapeia hoje

| Conversão | Onde está |
|-----------|-----------|
| Domain → insert JSON | `ReminderWrite.fromDomain()` em `SupabaseReminderRepository` |
| Row JSON → Domain | `toDomain(ReminderRow)` no mesmo arquivo |
| Domain → update JSON | `Map<String, Object>` no método `update()` |

Quando os controllers REST chegarem, um `ReminderMapper` na camada `api` centraliza DTO ↔ response. O mapeamento de banco pode continuar no repository ou extrair para `repository/mapper` se crescer.

### Anti-padrões

- Retornar `Reminder` (domain) direto no `@RestController` — acopla API ao domínio e vaza estrutura interna.
- Usar o mesmo DTO para request e response — campos de criação e leitura raramente são iguais.
- Mapper com lógica de negócio — se precisa validar regra, pertence ao `domain` ou `service`.

---

## 7. Config (configuração)

**O que é:** **infraestrutura Spring** — beans, properties, clientes HTTP, CORS, segurança. Não contém regra de negócio.

**Pacote:** `com.myroutine.config`

### Arquivos atuais

| Classe | Função |
|--------|--------|
| `SupabaseProperties` | Lê `supabase.url` e `supabase.service-role-key` do `application.yml` / env |
| `SupabaseConfig` | Cria o bean `WebClient` apontando para `.../rest/v1/` com headers de service role |

### Ligação com `application.yml`

```yaml
supabase:
  url: ${SUPABASE_URL:}
  service-role-key: ${SUPABASE_SERVICE_ROLE_KEY:}
```

`@ConfigurationProperties(prefix = "supabase")` em `SupabaseProperties` faz o binding automático (kebab-case no YAML → camelCase no Java).

### Por que service role no back-end?

O `WebClient` usa `SUPABASE_SERVICE_ROLE_KEY` para bypass de RLS no servidor. A **autorização por usuário** fica na aplicação (`userId` do JWT + filtro no service/repository), não confiando só no anon key do mobile.

### O que mais entrará em `config/`

- `SecurityConfig` — filtro JWT, rotas públicas (`/health`) vs protegidas.
- `CorsConfig` — origens do app Expo em dev/prod.
- Beans de integração (Google, Alexa).

### O que NÃO faz

- Não implementa `createReminder`.
- Não define entidades de domínio.

---

## Fluxos completos (estudo)

### A) Criar lembrete (quando REST existir)

```
1. POST /api/reminders + JSON
2. Controller: @Valid CreateReminderRequest, extrai userId do JWT
3. Service: new Reminder(...) → validação no construtor
4. Repository: ReminderWrite → POST Supabase → ReminderRow → toDomain
5. Mapper: Reminder → ReminderResponse
6. Controller: 201 Created + JSON
```

### B) Marcar como feito (já no service)

```
1. (futuro) PATCH /api/reminders/{id}/done
2. Controller: id + userId autenticado
3. Service.requireOwnedReminder → markDone() → repository.update
4. Mapper → ReminderResponse
5. 200 OK
```

### C) Health check (hoje)

```
GET /health → HealthController → Map.of("status","ok") → 200
```

Sem service — exceção aceitável para endpoints de infraestrutura triviais.

---

## Tabela rápida: “onde coloco isso?”

| Pergunta | Camada |
|----------|--------|
| Validar se título tem pelo menos 1 caractere? | `domain` (negócio) + DTO `@NotBlank` (formato API) |
| Verificar JWT e extrair `userId`? | `api/security` + `controller` |
| Garantir que usuário só vê seus lembretes? | `service` (+ query `findByUserId`) |
| Montar URL `?user_id=eq.xxx`? | `repository` |
| Configurar timeout do WebClient? | `config` |
| Retornar 404 vs 403? | `controller` + `@ExceptionHandler` |
| Enum `pending`/`done` no JSON? | `response` DTO ou mapper |
| Exceção quando Supabase retorna 500? | `repository` → `ReminderPersistenceException` |

---

## Dependências entre camadas (regra de import)

```
controller  →  service, dto, mapper
service     →  domain, repository (interface)
repository  →  domain
domain      →  (nada do projeto acima)
config      →  libraries Spring; injetado em repository/integration
dto/mapper  →  domain (mapper pode ler domain; DTO não deve)
```

**Teste rápido:** se `domain/Reminder.java` importar algo de `api` ou `repository`, algo está errado.

---

## Princípios SOLID — resumo por camada

| Camada | S | O | L | I | D |
|--------|---|---|---|---|---|
| Domain | Uma entidade, suas regras | — | — | — | — |
| Repository | Só persistência | Nova impl sem mudar interface | Impl substituível | Interface mínima | Service depende da interface |
| Service | Um caso de uso por método | — | — | — | Depende de `ReminderRepository` |
| Controller | Só HTTP | — | — | — | Depende de `ReminderService` |
| Config | Um bean por concern | — | — | — | — |

---

## Glossário

| Termo | Significado |
|-------|-------------|
| **POJO / entidade de domínio** | Classe Java simples com comportamento (`Reminder`) |
| **Adapter** | `SupabaseReminderRepository` adapta PostgREST à interface |
| **DTO** | Objeto de transferência; não tem comportamento de negócio |
| **Bean** | Objeto gerenciado pelo Spring (`@Service`, `@Repository`, `@Bean`) |
| **DI** | Injeção de dependência via construtor |
| **PostgREST** | API REST automática sobre tabelas Postgres (usada pelo Supabase) |
| **RLS** | Row Level Security — políticas no Postgres; bypass com service role no servidor |

---

## Checklist de revisão (antes de um PR)

- [ ] Controller fino — só HTTP e conversão DTO?
- [ ] Service sem imports de `spring-web`?
- [ ] Domain sem Jackson/JPA/Supabase?
- [ ] Repository não decide “é dono?” — só persiste e consulta?
- [ ] Config sem regra de negócio?
- [ ] DTOs separados para request e response?
- [ ] Testes de domínio sem Spring (`ReminderTest` é o modelo)?

---

## Referências no repositório

| Recurso | Caminho |
|---------|---------|
| README do back-end | `backend/README.md` |
| Entidade principal | `domain/Reminder.java` |
| Contrato de persistência | `repository/ReminderRepository.java` |
| Adapter Supabase | `repository/SupabaseReminderRepository.java` |
| Casos de uso | `service/ReminderService.java` |
| Config Supabase | `config/SupabaseConfig.java`, `config/SupabaseProperties.java` |
| Testes de domínio | `src/test/java/.../domain/ReminderTest.java` |

---

*Última atualização: alinhado ao código do MVP (domain + repository + service + config + health). DTO, mapper e controllers REST de lembretes entram nas próximas tasks da API.*
