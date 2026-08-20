# T-F1-006 — Horas formativas

> **Transação:** [`T-F1-006`](../../transaçõesBackend/F1%20—%20Aluno/T-F1-006-FORMATIVAS.md)  
> **Diagrama:** [`US-F1-006`](../../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-006-FORMATIVAS.md)  
> **IDs:** `{{formativaId}}`, `{{storageKey}}`  

Aluno: `formative.submit` / `formative.view_own`. Revisor: `formative.review` (CAAF).

---

## Passo 1 — URL do comprovante

```
POST {{baseUrl}}/formativas/comprovantes/presigned-url
Authorization: Bearer {{accessTokenAluno}}
X-XSRF-TOKEN: {{xsrfToken}}
```

Cole no Body:

```json
{
  "filename": "comprovante.pdf",
  "contentType": "application/pdf"
}
```

PUT o PDF na `uploadUrl`. Copie `storageKey` → `{{storageKey}}`.

---

## Passo 2 — Submeter

Cole no Body:

```json
{
  "titulo": "Palestra: Machine Learning Aplicado",
  "descricao": "Participação na palestra promovida pelo DINF em 2026-06-15",
  "categoria": "PALESTRA",
  "cargaHoraria": 4.0,
  "dataRealizacao": "2026-06-15",
  "storageKeyComprovante": "{{storageKey}}"
}
```

```
POST {{baseUrl}}/formativas
```

**Esperado 201:**

```json
{
  "id": "…",
  "estado": "PENDENTE"
}
```

Copie `id` → `{{formativaId}}`.

---

## Passo 3 — Minhas + KPI

```
GET {{baseUrl}}/formativas/minhas?page=0&size=20
GET {{baseUrl}}/formativas/resumo
```

Resumo esperado:

```json
{
  "horasAprovadas": 0.0,
  "horasRequeridas": 120.0,
  "percentual": 0.0
}
```

Depois da aprovação o KPI sobe (dashboard BFF também).

---

## Passo 4 — Revisar (token CAAF / professor com `formative.review`)

Fila:

```
GET {{baseUrl}}/formativas/pendentes
Authorization: Bearer {{accessTokenProfessor}}
```

Cole no Body:

```json
{
  "acao": "APROVAR",
  "parecer": "Comprovante válido — carga horária conferida."
}
```

```
PATCH {{baseUrl}}/formativas/{{formativaId}}/review
```

**Esperado 200** com `estado: APROVADA`. Emite certificado origem `FORMATIVA` ([T-10.4](../transversal/T-10.4-certificado.md)).

Cole no Body:

```json
{
  "acao": "REJEITAR",
  "parecer": "Comprovante ilegível. Reenvie em PDF nativo."
}
```

Lote CAAF: [T-F4-001](../F4-comissoes/T-F4-001-caaf.md).
