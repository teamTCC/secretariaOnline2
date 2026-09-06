# T-F8-002 — Suporte e FAQ

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F8 — Cross-cutting/US-F8-002-SUPORTE-FAQ.md`](../../foundationDocs/sequenceDiagrams/F8 — Cross-cutting/US-F8-002-SUPORTE-FAQ.md)  
> **Status:** ✅ Implementado — SupportController (FAQ público + tickets) + migration V012

---

## O que os diagramas especificam

### F8.2-D01 — `GET /support/faq` (FAQ dinâmico por perfil)

```
GET /support/faq?perfil=ALUNO
Authorization: Bearer eyJhbGci...  (qualquer usuário logado)
```

> O parâmetro `perfil` é resolvido a partir do claim `role` do JWT pelo controller — o frontend **não precisa enviá-lo manualmente**, mas pode para forçar um perfil.

Busca itens ativos da tabela `faq_items` ordenados por relevância do perfil.

**JSON de saída (200):**

```json
[
  {
    "id": "uuid-001",
    "pergunta": "Como submeter uma atividade formativa?",
    "resposta": "Acesse o menu Formativas > Nova Atividade e preencha o formulário...",
    "ordem": 1
  },
  {
    "id": "uuid-002",
    "pergunta": "Como acompanhar o status da minha solicitação?",
    "resposta": "No dashboard, no card 'Solicitações Recentes' você vê o status atual...",
    "ordem": 2
  }
]
```

> Gerenciamento do FAQ (criar/editar/desativar itens) é responsabilidade do Admin (US-F7, escopo futuro).

---

### F8.2-D02 — `POST /support/tickets` (Enviar ticket)

```
POST /support/tickets
Authorization: Bearer eyJhbGci...  (qualquer usuário logado)
Content-Type: application/json

{
  "assunto": "Erro ao submeter atividade formativa",
  "mensagem": "Ao tentar enviar o comprovante do minicurso, recebo erro 500..."
}
```

Backend usa o **workflow engine** via `RequestType = SUPORTE_TECNICO` (DRY total — reutiliza a engine de solicitações). O ticket entra na fila de triagem da secretaria como qualquer outra solicitação.

**Transação atômica:**

```sql
BEGIN;
INSERT INTO requests (
  type='SUPORTE_TECNICO', estado='ABERTA',
  student_id=:userId, dados={"assunto": "...", "mensagem": "..."},
  numero=:gerarNumeroAnual   -- ex: SUP-2026-042
);
INSERT INTO outbox_event (type='support.ticket_created', payload={...});
COMMIT;
```

**JSON de saída (201):**

```json
{
  "id": "9a8b7c6d-...",
  "numero": "SUP-2026-042",
  "_links": {
    "self": "/solicitacoes/9a8b7c6d-..."
  }
}
```

Após o COMMIT, o `OutboxDispatcher` processa `support.ticket_created` e notifica a secretaria (→ [T-10.1-OUTBOX](../transversal/T-10.1-OUTBOX.md)).

**Validações backend (defense-in-depth):**

```json
HTTP/1.1 422 Unprocessable Entity
{
  "type": "validation_error",
  "status": 422,
  "detail": "assunto: deve ter no máximo 200 caracteres."
}
```

---

### F8.2-D03 — Rate Limit 429 (Bucket4j — 3 tickets/hora)

```
POST /support/tickets  (quarto ticket na mesma hora)
Authorization: Bearer eyJhbGci...
```

Bucket4j intercepta **antes** de qualquer lógica de negócio. Rate limit por `userId` (não por IP — evita bloqueio de NAT compartilhado).

```json
HTTP/1.1 429 Too Many Requests
Retry-After: 2520

{
  "type": "https://secretariaonline.ufpr.br/errors/rate_limit_exceeded",
  "title": "Limite de requisições excedido",
  "status": 429,
  "detail": "Limite de 3 tickets/hora atingido.",
  "retryAfterMinutes": 42
}
```

O frontend lê `retryAfterMinutes` para exibir o countdown ao usuário.

---

## DTOs esperados

```kotlin
data class CreateTicketRequest(
    @field:NotBlank @field:Size(max = 200)
    val assunto: String,

    @field:NotBlank @field:Size(max = 2000)
    val mensagem: String
)

data class TicketCreatedDto(
    val id: UUID,
    val numero: String,   // ex: "SUP-2026-042"
    val _links: Map<String, String>
)

data class FaqItemDto(
    val id: UUID,
    val pergunta: String,
    val resposta: String,
    val ordem: Int
)
```

---

## Migração necessária

```sql
-- faq_items (gerenciado pelo admin)
CREATE TABLE faq_items (
    id          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    pergunta    TEXT NOT NULL,
    resposta    TEXT NOT NULL,
    perfil_alvo VARCHAR(50),          -- 'ALUNO', 'PROFESSOR', 'ALL', etc.
    ordem       INT  NOT NULL DEFAULT 0,
    ativa       BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

> Tabela `requests` e workflow engine já existem — ticket de suporte é apenas um `RequestType` novo a inserir na tabela `request_type`.

---

## O que está implementado

[`iam/api/SupportController.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/SupportController.kt)

| Endpoint | Auth | Notas |
|----------|------|--------|
| `GET /faq` | público | lista por `categoria` opcional |
| `POST /support/tickets` | autenticado | ticket próprio |
| `PATCH /support/tickets/{id}/close` | autenticado | **só o autor** ou `user.manage_students` / `system.admin` |
| `PATCH /support/tickets/{id}/respond` | `user.manage_students` | secretaria |

Tickets **não** passam pelo motor de solicitações (tabela `support_ticket`, não `RequestType=SUPORTE_TECNICO`).

---

## O que precisa ser implementado ( lacunas vs diagrama )

- FAQ filtrado automaticamente pelo perfil do JWT (`?perfil=ALUNO` no diagrama)
- Ticket como `RequestType` + protocolo `SUP-YYYY-NNN` + outbox `support.ticket_created`
- Rate limit 3/h em `POST /support/tickets`

---

## Checklist de Verificação

- [x] `GET /faq` → lista pública (sem JWT)
- [x] `POST /support/tickets` → abre ticket do usuário autenticado
- [x] `PATCH /support/tickets/{id}/close` → dono ou secretaria (403 para terceiros)
- [x] `PATCH /support/tickets/{id}/respond` → secretaria
- [ ] Ticket via workflow engine / protocolo `SUP-YYYY-NNN`
- [ ] Rate limit em abertura de ticket

