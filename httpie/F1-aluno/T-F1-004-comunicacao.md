# T-F1-004 — Hub de comunicação

> **Transação:** [`T-F1-004`](../../transaçõesBackend/F1%20—%20Aluno/T-F1-004-COMUNICACAO.md)  
> **Diagrama:** [`US-F1-004`](../../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-004-COMUNICACAO.md)  
> **IDs:** `{{deliveryId}}`, `{{communicationId}}`, `{{cursoId}}`  

Aluno: `communication.read`. Publicar: professor `communication.publish_class` (com `cursoId`) ou admin `communication.publish`.

---

## Passo 1 — Inbox do aluno

```
GET {{baseUrl}}/communications/me?page=0&size=20
Authorization: Bearer {{accessTokenAluno}}
```

**Esperado 200:**

```json
{
  "content": [
    {
      "deliveryId": "…",
      "communicationId": "…",
      "titulo": "…",
      "tipo": "AVISO",
      "readAt": null,
      "deliveredAt": "2026-08-19T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 0
}
```

Se a lista estiver vazia, publique no Passo 3 e volte.

Copie `deliveryId` → `{{deliveryId}}`.

---

## Passo 2 — Badge e marcar lido

```
GET {{baseUrl}}/communications/me/unread-count
```

**Esperado:** `{ "count": 1 }` (número).

```
PATCH {{baseUrl}}/communications/deliveries/{{deliveryId}}/read
X-XSRF-TOKEN: {{xsrfToken}}
```

**Esperado 200.** `unread-count` cai. `readAt` preenchido no próximo GET.

---

## Passo 3 — Publicar (professor / admin)

Body turma: 

```json
{
  "titulo": "Aviso da turma TADS — HTTPie",
  "conteudo": "Prazo de formativas encerra sexta-feira às 18h.",
  "tipo": "AVISO",
  "cursoId": "{{cursoId}}"
}
```

  
Body institucional (admin): 

```json
{
  "titulo": "Prazo para solicitações",
  "conteudo": "Atenção: prazo encerra hoje às 18h.",
  "tipo": "URGENTE",
  "cursoId": null
}
```

```
POST {{baseUrl}}/communications
Authorization: Bearer {{accessTokenProfessor}}
X-XSRF-TOKEN: {{xsrfToken}}
```

**Esperado 201:** `{ "id": "…", "entregas": 1 }`. Copie `id` → `{{communicationId}}`.

Fan-out: se `cursoId` preenchido, só usuários com `metadata.idCurso` igual. Sem curso = todos os ativos (`communication.publish`).

```
GET {{baseUrl}}/communications/{{communicationId}}
```

Detalhe do comunicado. Sem authority de publish: **403**.
