# T-F1-005 — Solicitações (motor de workflow)

> **Transação:** [`T-F1-005`](../../transaçõesBackend/F1%20—%20Aluno/T-F1-005-SOLICITACOES.md)  
> **Diagrama:** [`US-F1-005`](../../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-005-SOLICITACOES.md)  
> **IDs:** `{{requestTypeId}}`, `{{cursoId}}`, `{{requestId}}`, `{{disciplinaId}}`, `{{storageKey}}`, `{{sha256}}`  

Esta é a transação mais longa. Siga a ordem. Actions do workflow no **seed** são `ASSIGN`, `FORWARD_TO_DELIBERATOR`, `DEFER`, `DENY`, `REQUEST_ADJUSTMENT`, `RESUBMIT` — **não** `DEFERIR`. Confira sempre `_links` do detalhe.

---

## Passo 1 — Tipos (JSON Schema dinâmico)

```
GET {{baseUrl}}/requests/types
Authorization: Bearer {{accessTokenAluno}}
```

**Esperado 200:** array. Seed típico: `SEGUNDA_CHAMADA`, `TRANCAMENTO_DISCIPLINA`, `DECLARACAO_MATRICULA`.

Copie o `id` de `DECLARACAO_MATRICULA` → `{{requestTypeId}}` (o form é o mais simples: só `finalidade`).

`formSchema` é o contrato do wizard — o HTTPie ignora e manda `dados` na mão.

Se a lista estiver vazia: publique um tipo como admin ([T-F7-003](../F7-admin/T-F7-003-workflow-engine.md)).

---

## Passo 2 — Abrir solicitação (sem anexo)

Cole no Body:

```json
{
  "idRequestType": "{{requestTypeId}}",
  "idCurso": "{{cursoId}}",
  "dados": {
    "finalidade": "BOLSA",
    "observacoes": "Declaração para processo de bolsa permanência UFPR."
  }
}
```

```
POST {{baseUrl}}/requests
Authorization: Bearer {{accessTokenAluno}}
X-XSRF-TOKEN: {{xsrfToken}}
```

**Esperado 201:**

```json
{
  "id": "0193a0c0-…",
  "_links": { "self": "/requests/0193a0c0-…" }
}
```

Copie `id` → `{{requestId}}`. Estado inicial `ABERTA`. Outbox `solicitacoes.aberta`.

`GET /academico/cursos` se `{{cursoId}}` ainda for placeholder.

---

## Passo 3 — Listar as minhas

```
GET {{baseUrl}}/requests?page=0&size=20
GET {{baseUrl}}/requests?estado=ABERTA&page=0&size=20
```

Aluno com só `request.view_own`: o servidor **ignora** qualquer tentativa de ver outro `idSolicitante`. Secretaria (`request.view_curso`) vê o curso inteiro.

---

## Passo 4 — Detalhe + HATEOAS

```
GET {{baseUrl}}/requests/{{requestId}}
```

**Esperado 200:** `estado`, `dados`, `prazoEm`, `_links`.

Como aluno em `ABERTA` você em geral vê só `self` (e talvez anexos). Como professor/secretaria com `request.deliberate` aparecem rels das transições (`assign`, `defer`, `deny`, …).

Copie o `rel`/`href` que for testar.

---

## Passo 5 — Protocolo (aluno) e link público

```
GET {{baseUrl}}/requests/{{requestId}}/protocol
```

**Esperado:**

```json
{
  "protocolo": "2026/0001",
  "tipo": "DECLARACAO_MATRICULA",
  "estado": "ABERTA",
  "_links": { "public": "/publico/solicitacoes/2026/1" }
}
```

Preencha `{{requestAno}}` / `{{requestNumero}}`. Verificação anônima: [T-F0-006](../F0-publico/T-F0-006-007-verificacoes-publicas.md).

---

## Passo 6 — Histórico de eventos

```
GET {{baseUrl}}/requests/{{requestId}}/events
```

**Esperado:** lista cronológica (`ABERTURA` → transições). Vazio de transições logo após o create, com um evento de abertura.

---

## Passo 7 — Transição (troque para token da secretaria/professor)

Descubra a action no `workflow_json` do tipo ou nos `_links`.

Para `DECLARACAO_MATRICULA` o seed faz `ABERTA → EM_TRIAGEM` via `ASSIGN`, depois `DEFER`.

Body assign: 

```json
{
  "action": "ASSIGN",
  "parecer": null
}
```

```
POST {{baseUrl}}/requests/{{requestId}}/transitions
Authorization: Bearer {{accessTokenSecretaria}}
X-XSRF-TOKEN: {{xsrfToken}}
```

**Esperado 200:** `{ "mensagem": "Transição 'ASSIGN' aplicada com sucesso." }` (texto pode variar).

Depois: 

```json
{
  "action": "DEFER",
  "parecer": "Deferido conforme documentação apresentada (teste HTTPie)."
}
```

Action inexistente no estado atual → **400/422**. Sem `request.deliberate` → **403**.

Indeferir: 

```json
{
  "action": "DENY",
  "parecer": "Indeferido: documentação incompleta (teste HTTPie)."
}
```

---

## Passo 8 — Rascunho

Cole no Body:

```json
{
  "idRequestType": "{{requestTypeId}}",
  "idCurso": "{{cursoId}}",
  "dados": {
    "finalidade": "OUTRO",
    "observacoes": "Rascunho — ainda vou completar."
  }
}
```

```
POST {{baseUrl}}/requests/draft
```

**Esperado 201:** `estado` implícito `RASCUNHO`, `_links.submit`. **Não** gera protocolo nem outbox.

```
POST {{baseUrl}}/requests/{{requestId}}/submit
```

**Esperado 200:** `estado: ABERTA`, `protocolo: "2026/0002"`.

---

## Passo 9 — Anexos (MinIO)

1. Presign:

```
POST {{baseUrl}}/requests/attachments/presigned-url
```

Cole no Body:

```json
{
  "nomeOriginal": "historico_escolar.pdf",
  "contentType": "application/pdf",
  "categoria": "HISTORICO_ESCOLAR"
}
```

**Esperado:** `{ "uploadUrl", "storageKey" }` → `{{storageKey}}`.

2. `PUT` na `uploadUrl` com bytes do PDF (`Content-Type: application/pdf`).

3. Calcule SHA-256 do arquivo (PowerShell):

```powershell
Get-FileHash .\historico.pdf -Algorithm SHA256
```

Cole o hex em `{{sha256}}`.

4. Abra request com anexos: 

```json
{
  "idRequestType": "{{requestTypeId}}",
  "idCurso": "{{cursoId}}",
  "dados": {
    "finalidade": "CONVENIO",
    "observacoes": "Solicitação com histórico em anexo."
  },
  "attachments": [
    {
      "storageKey": "{{storageKey}}",
      "sha256": "{{sha256}}",
      "nomeOriginal": "historico_escolar.pdf",
      "contentType": "application/pdf",
      "categoria": "HISTORICO_ESCOLAR",
      "tamanhoBytes": 204800
    }
  ]
}
```

Listar / baixar / apagar:

```
GET    {{baseUrl}}/requests/{{requestId}}/attachments
GET    {{baseUrl}}/requests/{{requestId}}/attachments/{{attachmentId}}/download-url
DELETE {{baseUrl}}/requests/{{requestId}}/attachments/{{attachmentId}}
```

Delete só o solicitante, só em `ABERTA`/`RASCUNHO` → **204**.

---

## Passo 10 — Bulk (secretaria)

Ver [T-F5](../F5-secretaria/T-F5-secretaria.md). Cole no Body:

```json
{
  "ids": ["{{requestId}}"],
  "action": "DEFER",
  "parecer": "Autorização deferida em lote (HTTPie)."
}
```

```
PATCH {{baseUrl}}/requests/bulk-deliberate
```

Falha em um item → **409** e rollback de todos.
