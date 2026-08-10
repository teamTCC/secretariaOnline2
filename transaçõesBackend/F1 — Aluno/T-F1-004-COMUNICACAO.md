# T-F1-004 — Comunicações (Hub de Notificações)

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-004-COMUNICACAO.md`](../../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-004-COMUNICACAO.md)  
> **Status:** ⏳ Stub — entidades no banco, controller não implementado

---

## O que está no banco

A migration `V007__comunicacao_auditoria_schema.sql` cria as tabelas:
- `comunicacao` — mensagens/comunicados
- `communication_delivery` — entregas por canal

---

## O que precisa ser implementado

| Endpoint | Diagrama | Capability |
|----------|----------|------------|
| `GET /communications/me` | F1.6-D01 — hub do aluno | `communication.view_own` |
| `GET /communications/me/{id}` | F1.6-D02 — marcar como lida | `communication.view_own` |
| `POST /communications` | F3.8-D01 — professor publica | `communication.publish` |

---

## Nota

O `OutboxDispatcher` processa `certificate.issued`, `solicitacoes.deliberated`, etc. e deve inserir em `communication_delivery`. Esta integração está pendente — ver [T-10.1-OUTBOX](../transversal/T-10.1-OUTBOX.md).

---

## Checklist de Verificação

- [ ] `GET /communications/me` paginado — **não implementado**
- [ ] Marcar comunicação como lida — **não implementado**
- [ ] `POST /communications` com audiência e template — **não implementado**
