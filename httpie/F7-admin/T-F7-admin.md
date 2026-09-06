# T-F7 — Admin (audit, FAQ, LGPD, saúde)

> **Transação índice:** [`T-F7-ADMIN.md`](../../transaçõesBackend/F7%20—%20Admin/T-F7-ADMIN.md)  
> **Diagramas:** [`F7 — Admin/`](../../foundationDocs/sequenceDiagrams/F7%20—%20Admin/)  
> **Login:** `POST /auth/login` com o body abaixo. 

```json
{
  "identificador": "admin@ufpr.br",
  "senha": "Admin@123456"
}
```

Irmãos: [002 roles](T-F7-002-iam-perfis.md) · [003 workflow](T-F7-003-workflow-engine.md) · [004 templates](T-F7-004-templates.md) · [outbox](../transversal/T-10.6-admin-outbox.md).

---

## Saúde (público / actuator)

```
GET {{baseUrl}}/actuator/health
```

Link: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

**Esperado:** `{ "status": "UP" }`. Com token admin, `show-details=when-authorized` pode incluir componentes.

```
GET {{baseUrl}}/actuator/info
GET {{baseUrl}}/actuator/metrics
```

Swagger: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## Audit log (`system.admin`)

```
GET {{baseUrl}}/admin/audit?page=0&size=20
GET {{baseUrl}}/admin/audit?acao=LOGIN_SUCCESS&de=2026-08-01&ate=2026-08-20
GET {{baseUrl}}/admin/audit?idAtor={{alunoId}}&alvoTipo=usuario
```

**Esperado 200** paginado. Depois de um login, deve existir `LOGIN_SUCCESS` ou `LOGIN_FAILED`. Sem admin → **403**.

---

## FAQ admin

Body create: 

```json
{
  "pergunta": "Como solicitar aproveitamento de disciplina?",
  "resposta": "Acesse Solicitações > Nova Solicitação > Aproveitamento de Disciplina e anexe o histórico.",
  "categoria": "SOLICITACOES",
  "ordem": 1
}
```

```
POST {{baseUrl}}/faq
X-XSRF-TOKEN: {{xsrfToken}}
```

**Esperado 201** com `id` → `{{faqId}}`, `_links.self`.

```
PATCH {{baseUrl}}/faq/{{faqId}}
```

Cole no Body:

```json
{
  "resposta": "Resposta atualizada via HTTPie.",
  "ordem": 2
}
```

```
DELETE {{baseUrl}}/faq/{{faqId}}
```

Soft-delete (`ativo=false`). Público: `GET /faq` — [T-F8-002](../F8-cross/T-F8-002-suporte-faq.md).

---

## LGPD (no próprio admin ou em qualquer user)

```
POST {{baseUrl}}/me/data-export
GET  {{baseUrl}}/me/data-export/{{dataExportJobId}}
```

Ver [T-F1-003](../F1-aluno/T-F1-003-perfil.md) Passo 7.

---

## Usuários

CRUD em `/usuarios` (não `/admin/usuarios`), igual [T-F5](../F5-secretaria/T-F5-secretaria.md). Atribuir roles: [T-F7-002](T-F7-002-iam-perfis.md).
