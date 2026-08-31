# Fatia 5 — Estágio, TCC, egresso

**Objetivo da demo:** paths **reais** (não os do diagrama antigo). Aluno cria estágio; prof cria TCC; egresso **não** abre solicitação.

**Pré-requisito:** Fatias 0–2. COE/CAAF na fatia 6. Colação/diploma na fatia 7.  
**Oráculos:** `T-F1-007-008-estagio-tcc.md` · `T-F2-001-dashboard-egresso.md`

Paths **errados** que 404 (já vistos no HTTPie): `/estagios/me` → use `/internships/mine`. `/alumni/me` pode **não** existir — use BFF.

---

## 1. Whitelist

```
features/vinculos/
  EstagiosPage.tsx       # /estagios
  EstagioDetailPage.tsx  # /estagios/:id
  TccsPage.tsx           # /tccs
  TccDetailPage.tsx      # /tccs/:id
```

Dashboard egresso **já** é a `DashboardPage` da fatia 2 (`GET /bff/dashboard/egresso`). Nesta fatia só garantir o redirect quando `alumni.view_own`.

---

## 2. Estágio (aluno)

```
POST /internships
{
  "empresa": "Empresa XYZ Ltda.",
  "cargo": "Dev Backend",
  "cargaHorariaSemanal": 20,
  "inicio": "2026-03-01",
  "observacoes": "Estágio obrigatório TADS"
}
```

**201** `{ id, estado: "EM_ANDAMENTO" }`.

```
GET /internships/mine
GET /internships/{id}
```

Documentos MinIO:

```
POST /internships/{id}/documents/upload-url
{ "tipo": "CONTRATO", "nomeOriginal": "contrato.pdf", "contentType": "application/pdf" }
POST /internships/{id}/documents     # confirm
GET  /internships/{id}/documents
```

Conclusão: `POST /internships/{id}/conclude` — authority `internship.review` (COE), **não** o aluno. Sem o rel no `_links` do aluno, o botão não existe.

Pool/supervisor: `GET /commissions/coe/pool` — fatia 6.

UI: form create + lista mine + detalhe JsonPanel + HateoasBar.

---

## 3. TCC

Orientador (`tcc.supervise`) **cria**:

```
POST /tccs
{ "titulo": "…", "idCurso": "<uuid TADS>" }
```

Aluno envia PDF via `_links` de upload no detalhe (presign + confirm — espelhar estágio, paths no Swagger `/tccs/{id}/...`).

Não inventar `/tcc/me` se o list for `GET /tccs?mine=` ou `_links` do dashboard. Confirmar no SpringDoc. HTTPie T-F1-007-008 é a ordem.

Banca / modalidade: config do curso (fatia 7 `GET /courses/tads/config`) influi regra de negócio no **back**. Front só mostra 422/409 se o back recusar.

---

## 4. Egresso

Login: `ana.egressa@ufpr.br` / `EgressoS3nh@Forte!` (usuário criado nos testes HTTPie, **não** no Flyway). Se 401, a equipe recria via secretaria/colação.

- `GET /bff/dashboard/egresso` 200, **sem** `novaSolicitacao`
- Aluno chamando essa rota → 403
- Egresso em `/bff/dashboard/aluno` → 403
- Certificados: mesmo `GET /certificates/mine` (IDOR por `idAluno`)
- Diploma: `_links.download` do BFF ou `GET /graduations/{id}/diploma-url` (secretaria)

Diploma: seguir `_links.download` do BFF **e** `GET /graduations/{id}/diploma-url` quando a colação (fatia 7 / T-F5-005) tiver gerado PDF. Sem isso T-F2-001 fica incompleto. Dashboard + JsonPanel + esses links — sem CRUD extra de egresso.

---

## 5. Aceite

- [ ] POST internship aluno 201 EM_ANDAMENTO; GET mine contém o id
- [ ] Aluno detalhe **sem** rel conclude; token COE/admin **com** (ou 403 se clicar URL crua)
- [ ] POST tccs como aluno sem `tcc.supervise` → 403
- [ ] POST tccs como prof 201
- [ ] Egresso dashboard sem nova solicitação; aluno nesse path 403

## 6. Não fazer

- Telas COE aqui.
- Wizard de 19 tipos (já é fatia 3).
- Path `/estagios` no **cliente HTTP** — só na rota React; o fetch vai para `/internships`.
