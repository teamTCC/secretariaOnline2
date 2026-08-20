# T-F7-004 — Templates de comunicação

> **Transação:** [`T-F7-004`](../../transaçõesBackend/F7%20—%20Admin/T-F7-004-TEMPLATES-COMUNICACAO.md)  
> **Diagrama:** [`US-F7-004`](../../foundationDocs/sequenceDiagrams/F7%20—%20Admin/US-F7-004-TEMPLATES-COMUNICACAO.md)  
> **IDs:** `{{templateId}}`  
> **Authority:** `communication.manage_templates`

Placeholders suportados pelo `TemplateEngine`: `{{nome}}`, `{{tipo}}`, `{{estadoNovo}}`, `{{protocolo}}`, etc. Seed V015: `solicitacoes.transicionada`, `atendimentos.created`, `graduations.confirmed`, `imports.completed`, `exports.ready`, `contato.recebido`.

---

## Passo 1 — Listar

```
GET {{baseUrl}}/communication-templates
Authorization: Bearer {{accessTokenAdmin}}
```

**Esperado 200:** itens com `variaveis: ["nome","email",…]` (autocomplete). Copie um `id` seed ou crie no passo 2.

---

## Passo 2 — Criar (versão 1)

```
POST {{baseUrl}}/communication-templates
X-XSRF-TOKEN: {{xsrfToken}}
```

Cole no Body:

```json
{
  "codigo": "httpie.teste.deferido",
  "titulo": "Solicitação deferida (teste)",
  "assunto": "Sua solicitação {{protocolo}} foi deferida",
  "corpo": "Olá {{nome}}, o estado novo é {{estadoNovo}}.",
  "canal": "EMAIL"
}
```

**Esperado 201** → `{{templateId}}`.

---

## Passo 3 — Nova revisão (imutável)

Cole no Body:

```json
{
  "assunto": "Atualização: solicitação {{protocolo}} deferida",
  "corpo": "Olá {{nome}}, conferido via HTTPie. Estado: {{estadoNovo}}."
}
```

```
POST {{baseUrl}}/communication-templates/{{templateId}}/revisions
GET  {{baseUrl}}/communication-templates/{{templateId}}/versions
GET  {{baseUrl}}/communication-templates/{{templateId}}/versions/1
```

A revisão antiga permanece readonly. O dispatcher (T-10.1) lê a versão atual do catálogo.

Sem authority → **403**.
