# T-F1-004 — Comunicações (Hub de Notificações)

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-004-COMUNICACAO.md`](../../foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-004-COMUNICACAO.md)  
> **Status:** ✅ Implementado — inbox in-app, marcar lido, publicar comunicado

---

## O que está no banco

A migration `V007__comunicacao_auditoria_schema.sql` cria as tabelas:
- `comunicacao` — mensagens/comunicados
- `communication_delivery` — entregas por canal

---

## Arquivos implementados

| Papel | Arquivo |
|-------|---------|
| Controller | [`comunicacao/api/CommunicationsController.kt`](../../backend/modules/comunicacao/src/main/kotlin/br/ufpr/sept/so2/modules/comunicacao/api/CommunicationsController.kt) |
| Repositórios | [`comunicacao/infrastructure/persistence/ComunicacaoJpaRepositories.kt`](../../backend/modules/comunicacao/src/main/kotlin/br/ufpr/sept/so2/modules/comunicacao/infrastructure/persistence/ComunicacaoJpaRepositories.kt) |
| Entidades | [`comunicacao/infrastructure/persistence/ComunicacaoEntities.kt`](../../backend/modules/comunicacao/src/main/kotlin/br/ufpr/sept/so2/modules/comunicacao/infrastructure/persistence/ComunicacaoEntities.kt) |

---

## Endpoints implementados

| Endpoint | Capability | Função |
|----------|-----------|--------|
| `GET /communications` | `communication.read` | Lista comunicados publicados |
| `GET /communications/me` | `communication.read` | Inbox do usuário autenticado |
| `GET /communications/me/unread-count` | `communication.read` | Badge de não lidos |
| `PATCH /communications/deliveries/{id}/read` | `communication.read` | Marcar como lido |
| `POST /communications` | `communication.publish` ou `communication.publish_class` | Publicar comunicado |
| `GET /communications/{id}` | `communication.read` | Detalhe do comunicado |

---

## JSON Responses

### GET /communications/me (inbox paginado)
```json
{
  "content": [
    {
      "deliveryId": "uuid",
      "communicationId": "uuid",
      "titulo": "Calendário de Defesas 2025",
      "tipo": "AVISO",
      "readAt": null,
      "deliveredAt": "2025-08-01T10:00:00Z"
    }
  ],
  "page": 0, "size": 20, "totalElements": 5
}
```

### POST /communications
```json
// Request — publish institucional (communication.publish): todos os ativos
{
  "titulo": "Prazo para solicitações",
  "conteudo": "Atenção: prazo encerra hoje às 18h.",
  "tipo": "URGENTE",
  "cursoId": null
}
// Request — publish de turma (communication.publish_class): cursoId obrigatório
{
  "titulo": "Aviso da turma",
  "conteudo": "…",
  "tipo": "AVISO",
  "cursoId": "uuid-do-curso"
}
// Response 201
{ "id": "uuid", "entregas": 42 }
```

Fan-out in-app: se `audiencia` tem `cursoId`, só usuários ativos com `metadata.idCurso` igual; senão todos os ativos.

---

## Checklist de Verificação

- [x] `GET /communications/me` paginado → inbox do aluno
- [x] `GET /communications/me/unread-count` → `{ "count": 3 }`
- [x] `PATCH /communications/deliveries/{id}/read` → marcar como lido
- [x] `POST /communications` com `tipo` e `cursoId` → fan-out só da audiência (ou todos se admin)
