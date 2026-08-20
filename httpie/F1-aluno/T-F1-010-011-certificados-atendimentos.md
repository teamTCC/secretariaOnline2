# T-F1-010 / 011 — Certificados e atendimentos

> **Transação:** [`T-F1-010-011`](../../transaçõesBackend/F1%20—%20Aluno/T-F1-010-011-CERTIFICADOS-ATENDIMENTOS.md)  
> **Diagramas:** [`US-F1-010`](../../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-010-CERTIFICADOS.md) · [`US-F1-011`](../../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-011-ATENDIMENTOS.md)  
> **IDs:** `{{certificateId}}`, `{{certificateHash}}`, `{{serviceRecordId}}`

---

## Certificados (F1.10)

Pré-requisito: evento encerrado com presença **ou** formativa aprovada.

```
GET {{baseUrl}}/certificates/mine
Authorization: Bearer {{accessTokenAluno}}
```

**Esperado 200:**

```json
[
  {
    "id": "…",
    "idEvento": "…",
    "hashSha256": "a1b2…",
    "chCreditadas": 4.0,
    "issuedAt": "2026-08-19T20:00:00Z",
    "_links": {
      "download": "/certificates/{id}/download-url",
      "verify": "/publico/verificar-certificado/{hash}"
    }
  }
]
```

Copie `id` → `{{certificateId}}`, `hashSha256` → `{{certificateHash}}`.

```
GET {{baseUrl}}/certificates/{{certificateId}}/download-url
```

**Esperado:** URL MinIO TTL 15 min. IDOR (certificado de outro aluno) → **403**.

Link público: `{{baseUrl}}/publico/verificar-certificado/{{certificateHash}}` — [T-F0-007](../F0-publico/T-F0-006-007-verificacoes-publicas.md).

---

## Atendimentos (F1.11)

### Aluno agenda

Cole no Body:

```json
{
  "assunto": "Revisão de matrícula",
  "descricao": "Quero conferir disciplinas do período 2026/2.",
  "tipo": "AGENDAMENTO"
}
```

```
POST {{baseUrl}}/me/service-records
```

**Esperado 201** com `estado: AGENDADO`. Outbox `atendimentos.created`.

### Secretaria registra (gera ciência pendente)

Cole no Body:

```json
{
  "idAluno": "{{alunoId}}",
  "assunto": "Revisão de matrícula",
  "tipo": "PRESENCIAL",
  "descricao": "Atendimento de balcão registrado via HTTPie."
}
```

```
POST {{baseUrl}}/service-records
Authorization: Bearer {{accessTokenSecretaria}}
```

**Esperado 201** `PENDENTE_CIENCIA`. Copie `id` → `{{serviceRecordId}}`.

### Aluno lista e dá ciência

```
GET {{baseUrl}}/me/service-records?status=PENDENTE_CIENCIA
GET {{baseUrl}}/service-records?aluno=me&status=PENDENTE_CIENCIA
```

`_links.acknowledge` só se `PENDENTE_CIENCIA`.

```
POST {{baseUrl}}/service-records/{{serviceRecordId}}/acknowledge
```

**Esperado 200:** estado `CIENTE`. Outro aluno → **403**. KPI `atendimentosPendentes` no dashboard cai.
