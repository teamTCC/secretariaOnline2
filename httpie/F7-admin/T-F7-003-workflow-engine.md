# T-F7-003 — Editor RequestType / workflow

> **Transação:** [`T-F7-003`](../../transaçõesBackend/F7%20—%20Admin/T-F7-003-WORKFLOW-ENGINE.md)  
> **Diagrama:** [`US-F7-003`](../../foundationDocs/sequenceDiagrams/F7%20—%20Admin/US-F7-003-WORKFLOW-ENGINE.md)  
> **Authority:** `request_type.manage` (listagem também com `request.view_curso`)

O aluno só vê tipos **ativos** (`ativo=true`) em `GET /requests/types`. Publish chama `ManageRequestTypeUseCase.publish` → snapshot **`request_type_version`** (Flyway **V019**). GET detalhe da instância usa o `form_schema` dessa versão.

---

## Passo 1 — Catálogo admin

```
GET {{baseUrl}}/request-types
Authorization: Bearer {{accessTokenAdmin}}
```

Inclui rascunhos. Copie um `id` seed se for só editar.

---

## Passo 2 — Criar rascunho

```
POST {{baseUrl}}/request-types
X-XSRF-TOKEN: {{xsrfToken}}
```

Cole no Body:

```json
{
  "code": "ATESTADO_MATRICULA_TESTE",
  "descricao": "Atestado de matrícula (rascunho HTTPie)",
  "prazoDias": 5,
  "formSchema": {
    "type": "object",
    "properties": {
      "finalidade": {
        "type": "string",
        "title": "Finalidade",
        "enum": ["BOLSA", "CONVENIO", "OUTRO"]
      }
    },
    "required": ["finalidade"]
  },
  "workflowJson": {
    "initial": "ABERTA",
    "states": ["RASCUNHO", "ABERTA", "EM_TRIAGEM", "DEFERIDA", "ARQUIVADA"],
    "transitions": [
      {
        "from": "ABERTA",
        "to": "EM_TRIAGEM",
        "action": "ASSIGN",
        "requiresAuthority": ["request.deliberate"]
      },
      {
        "from": "EM_TRIAGEM",
        "to": "DEFERIDA",
        "action": "DEFER",
        "requiresAuthority": ["request.deliberate"]
      }
    ]
  }
}
```

**Esperado 201** `ativo: false`. Copie `id` → use como `{{requestTypeId}}` só depois do publish.

```
PATCH {{baseUrl}}/request-types/{{requestTypeId}}
```

Cole no Body:

```json
{
  "prazoDias": 7,
  "descricao": "Atestado de matrícula (editado HTTPie)"
}
```

Schema inválido → **422**.

---

## Passo 3 — Publicar

```
POST {{baseUrl}}/request-types/{{requestTypeId}}/publish
```

Exige `formSchema` e `workflowJson` não vazios → `ativo=true` + linha em `request_type_version`.

Confira no token **aluno**:

```
GET {{baseUrl}}/requests/types
```

O `code` `ATESTADO_MATRICULA_TESTE` deve aparecer. Abra uma solicitação: [T-F1-005](../F1-aluno/T-F1-005-solicitacoes.md).

---

## Passo 4 — Delete

```
DELETE {{baseUrl}}/request-types/{{requestTypeId}}
```

Se já houver `request` apontando para o tipo → **400**. Sem histórico → 204.
