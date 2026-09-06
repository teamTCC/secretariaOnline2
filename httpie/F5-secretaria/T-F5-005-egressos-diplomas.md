# T-F5-005 — Egressos e diplomas

> **Transação:** [`T-F5-005`](../../transaçõesBackend/F5%20—%20Secretaria/T-F5-005-EGRESSOS-DIPLOMAS.md)  
> **Diagrama:** [`US-F5-005`](../../foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/US-F5-005-EGRESSOS-DIPLOMAS.md)  
> **IDs:** `{{alunoId}}`, `{{cursoId}}`, `{{periodoId}}`, `{{graduationId}}`  

Authorities: `diploma.register`, `alumni.list`. O POST **revalida 5 critérios**; se um aluno falhar, o lote inteiro é rejeitado.

| # | Critério | `bloqueios[].razao` |
|---|----------|---------------------|
| 1 | TCC aprovado | `TCC` |
| 2 | Histórico todas `CONCLUIDA` | `HISTORICO` |
| 3 | Horas ≥ `curso.horas_formativas_minimas` | `HORAS_FORMATIVAS` |
| 4 | Sem `metadata.pendenciaFinanceira` | `FINANCEIRO` |
| 5 | Sem solicitação aberta/em deliberação/ajuste | `SOLICITACOES` |

Prepare o aluno: TCC [T-F1-008](../F1-aluno/T-F1-007-008-estagio-tcc.md), histórico [T-F6](../F6-coordenacao/T-F6-coordenacao.md), formativas aprovadas [T-F1-006](../F1-aluno/T-F1-006-formativas.md).

`{{periodoId}}`:

```
GET {{baseUrl}}/academico/periodos/ativo
```

---

## Passo 1 — Elegíveis

```
GET {{baseUrl}}/students?eligibleForGraduation=true
Authorization: Bearer {{accessTokenSecretaria}}
```

**Esperado 200:** cada item com `eligible` e `bloqueios: [{ razao, detalhe }]`. Use um `eligible: true` → `{{alunoId}}`.

Lista geral de alunos (mesmo controller): `GET /students` sem query.

---

## Passo 2 — Colação em lote

Campo do DTO: **`idCurso`** (não `cursoId`).

```
POST {{baseUrl}}/graduations
X-XSRF-TOKEN: {{xsrfToken}}
```

Cole no Body:

```json
{
  "alunoIds": ["{{alunoId}}"],
  "idCurso": "{{cursoId}}",
  "dataColacao": "2026-07-15",
  "livro": "12",
  "folha": "34",
  "ata": "001/2026",
  "periodoId": "{{periodoId}}"
}
```

**Esperado 200/201** com registros criados, role `EGRESSO`, PDF `diplomas/{id}.pdf`, outbox `graduations.confirmed`.

Aluno bloqueado → 4xx com razoes. Copie o id do registro → `{{graduationId}}`.

---

## Passo 3 — Listar, PDF, entrega física

```
GET {{baseUrl}}/secretaria/egressos
GET {{baseUrl}}/secretaria/egressos?format=csv
GET {{baseUrl}}/graduations
GET {{baseUrl}}/graduations/{{graduationId}}/diploma-url
PATCH {{baseUrl}}/graduations/{{graduationId}}/confirm-delivery
```

Diploma-url: `{ downloadUrl, hashSha256 }`. Confirm-delivery preenche `deliveredAt`. `_links.confirm-delivery` some quando `DIPLOMA_ENTREGUE`.

Dashboard egresso: [T-F2-001](../F2-egresso/T-F2-001-dashboard-egresso.md).
