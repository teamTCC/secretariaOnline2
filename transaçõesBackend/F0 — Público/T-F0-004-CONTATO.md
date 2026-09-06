# T-F0-004 — Página de Contato

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F0 — Público/US-F0-004-CONTATO.md`](../../foundationDocs/sequenceDiagrams/F0 — Público/US-F0-004-CONTATO.md)  
> **Status:** ✅ Backend público — `GET` dados da secretaria + `POST` mensagem (outbox + rate limit)

---

## Descrição

O diagrama original marcava a tela como estática. O backend agora expõe contato institucional e aceita mensagens.

## Arquivos

| Papel | Arquivo |
|-------|---------|
| Controller | [`iam/api/ContatoPublicoController.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/ContatoPublicoController.kt) |
| Config YAML | `app.contato.*` em [`application.yml`](../../backend/app/src/main/resources/application.yml) |
| Persistência | `contact_message` (V015) + `ContactMessageEntity` |
| Outbox | `contato.recebido` → [`ContatoOutboxHandler`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/infrastructure/outbox/ContatoOutboxHandler.kt) encaminha e-mail para `app.contato.email` |
| Rate limit | `POST /publico/contato` — 10/min por IP (mesmo bucket das consultas públicas) |

---

## GET — dados de contato

```
GET /publico/contato
```

Sem autenticação. Também emite o cookie CSRF (`XSRF-TOKEN`) para o SPA poder enviar o formulário.

```json
{
  "nome": "Secretaria SEPT — UFPR",
  "endereco": "Rua Dr. Alcides Vieira Arcoverde, 1225 — …",
  "telefone": "(41) 3360-4900",
  "email": "secretaria.sept@ufpr.br",
  "horario": "Segunda a sexta, 8h–17h",
  "_links": { "enviar": "/publico/contato" }
}
```

---

## POST — enviar mensagem

```
POST /publico/contato
Content-Type: application/json
X-XSRF-TOKEN: <cookie XSRF-TOKEN>
```

```json
{
  "nome": "Ana Silva",
  "email": "ana@ufpr.br",
  "assunto": "Horário de atendimento",
  "mensagem": "A secretaria atende no sábado?"
}
```

**202 Accepted** `{ id, status: "ACEITO", mensagem }`. Persistido em `contact_message` (status `NOVO`) e enfileirado no outbox.

CSRF: Double Submit Cookie — obter token via `GET /auth/csrf` ou `GET /publico/contato` e ecoar no header `X-XSRF-TOKEN`. Login/refresh continuam isentos.

---

## Checklist

- [x] `GET /publico/contato` → dados de `app.contato`
- [x] `POST /publico/contato` → 202 + outbox `contato.recebido`
- [x] Rate limit 429
- [x] CSRF Double Submit nas mutações autenticadas + neste POST público
