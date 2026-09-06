# T-F5-005 — Egressos e Colação de Grau

> **Diagrama:** [`foundationDocs/sequenceDiagrams/F5 — Secretaria/US-F5-005-EGRESSOS-DIPLOMAS.md`](../../foundationDocs/sequenceDiagrams/F5 — Secretaria/US-F5-005-EGRESSOS-DIPLOMAS.md)  
> **Status:** ✅ Colação em lote com **5 critérios**, PDF de diploma, livro/folha/ata  
> **Migration:** V014 (`graduation_record`) + V015 (livro/folha/ata/período/PDF)

---

## Arquivos

| Papel | Arquivo |
|-------|---------|
| Controller | [`GraduationController.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/api/GraduationController.kt) |
| Elegibilidade | [`GraduationEligibilityService.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/application/GraduationEligibilityService.kt) |
| PDF diploma | [`DiplomaPdfService.kt`](../../backend/modules/iam/src/main/kotlin/br/ufpr/sept/so2/modules/iam/application/DiplomaPdfService.kt) (OpenPDF → MinIO) |

---

## Alunos elegíveis (5 critérios)

```
GET /students?eligibleForGraduation=true
```

Cada item traz `eligible` e `bloqueios: [{ razao, detalhe }]`.

| # | Critério | Bloqueio (`razao`) |
|---|----------|-------------------|
| 1 | TCC `APROVADO` (ou `aprovado=true`) | `TCC` |
| 2 | Todas as disciplinas ativas do curso em `historico_escolar` = `CONCLUIDA` | `HISTORICO` |
| 3 | Horas formativas ≥ `curso.horas_formativas_minimas` | `HORAS_FORMATIVAS` |
| 4 | `metadata.pendenciaFinanceira != true` | `FINANCEIRO` |
| 5 | Nenhuma solicitação `ABERTA` / `EM_DELIBERACAO` / `EM_AJUSTE` | `SOLICITACOES` |

Também exige ativo + GRR e não-EGRESSO (`CADASTRO` / `EGRESSO`). Curso do aluno: `usuario.metadata.idCurso`.

`POST /graduations` **revalida** os 5 critérios; rejeita o lote se algum aluno falhar.

Body extra: `livro`, `folha`, `ata`, `periodoId`. Gera PDF em `diplomas/{id}.pdf`.

```
GET /graduations/{id}/diploma-url  → { downloadUrl, hashSha256 }
PATCH /graduations/{id}/confirm-delivery
```

---

## Checklist

- [x] 5 critérios na listagem e no POST
- [x] PDF de diploma + URL presignada
- [x] Livro / folha / ata / período
- [x] Role EGRESSO + outbox `graduations.confirmed`
