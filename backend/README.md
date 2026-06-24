# My Routine — Back-end (FastAPI)

API REST em Python para lembretes, integração Google Calendar e Alexa.

## Desenvolvimento local

```bash
cd backend
python -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

Verificar saúde:

```bash
curl http://localhost:8000/health
# {"status":"ok"}
```

## Estrutura de pastas

O back-end segue **Programação Orientada a Objetos** com camadas separadas. Cada pasta ensina um conceito:

```
backend/app/
├── main.py              # Ponto de entrada FastAPI — monta a aplicação
├── api/routes/          # Controllers HTTP — recebem requisições, delegam aos services
├── domain/              # Entidades de domínio (Reminder, Note) — regras e validação
├── services/            # Lógica de negócio — orquestra domain + repositories
├── repositories/        # Acesso a dados — contratos abstratos e implementações Supabase
└── integrations/        # APIs externas (Google Calendar, Alexa) — fase posterior
```

| Pasta | Conceito POO | Responsabilidade |
|-------|--------------|------------------|
| `domain/` | Classe, encapsulamento, Enum | Objetos com estado válido e comportamentos (`mark_done()`, etc.) |
| `repositories/` | ABC, herança, polimorfismo | Contrato de persistência; implementações trocáveis (Supabase, mock) |
| `services/` | Composição, injeção de dependência | Orquestra domínio + repositórios; sem HTTP nem SQL direto |
| `api/routes/` | DTOs (Pydantic), DI do framework | Traduz HTTP ↔ serviços; valida entrada, formata saída |

**Fluxo de uma requisição:** `route` → `service` → `domain` + `repository` → resposta JSON.

As entidades e serviços serão implementados nas tasks T06–T19.
