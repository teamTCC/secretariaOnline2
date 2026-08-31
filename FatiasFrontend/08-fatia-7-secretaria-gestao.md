# Fatia 7 — Secretaria, coordenação, admin, busca

**Objetivo da demo:** fila + HATEOAS, on-behalf, FGAC da busca (aluno sem `type=USUARIO`), editor RequestType (publish → V019 snapshot), reports com SQL já corrigido (timestamptz).

**Pré-requisito:** Fatia 3 (motor).  
**Oráculos:** `T-F5-*.md` · `T-F6-coordenacao.md` · `T-F7-*.md` · `T-F8-001` · `T-F8-002`

Logins:

| Perfil | Email | Senha |
|--------|-------|-------|
| Secretaria | `secretaria@ufpr.br` | `SecrS3nh@Forte!` |
| Coordenador | `coord.tads@ufpr.br` | `CoordS3nh@Forte!` |
| Admin | `admin@ufpr.br` | `Admin@123456` |

---

## 1. Whitelist

```
features/staff/
  SecretariaDashboard.tsx     # já pode ser a DashboardPage da fatia 2
  UsuariosPage.tsx            # /usuarios
  OnBehalfPage.tsx            # /secretaria/nova-on-behalf
  TarefasPage.tsx             # /tarefas  GET /tasks
  ReportsPage.tsx             # /relatorios
  CourseConfigPage.tsx        # /cursos/:id/config
  RequestTypesAdminPage.tsx   # /admin/request-types
  RolesAdminPage.tsx          # /admin/roles
  SearchPage.tsx              # /busca
  ImportExportPage.tsx        # /import /export
  OutboxAdminPage.tsx         # /admin/outbox?status=PROCESSED
```

Reusar `SolicitacoesListPage` com query `estado=ABERTA` como **fila**. Não criar FilaPage clone.

---

## 2. Secretaria

Dashboard: `GET /bff/dashboard/secretaria` — `kpis.emTriagem`, `_links.solicitacoes`, `_links.usuarios`. Cache `secretaria:static` 60s.

### Fila e transições

Mesma fatia 3. Secretaria vê `assign` / `defer`.

`PATCH /requests/bulk-deliberate`

```json
{ "ids": ["…"], "action": "DEFER", "parecer": "…" }
```

**422** se a máquina não permite DEFER daquele estado (ex.: DECLARACAO_MATRICULA ainda ABERTA). **409** rollback all-or-nothing. Mostrar Problem. Authority `request.deliberate` ou `image_authorization.review`.

### On-behalf (prova importante)

Authority as-built: `request.internal_open` **ou** `request.open_on_behalf` (controller aceita as duas). Sem elas → 400 `IllegalArgumentException`.

```
GET /usuarios?email=ana.aluno@ufpr.br   → alunoId
POST /requests
{
  "idRequestType": "…",
  "idCurso": "…",
  "idSolicitanteOnBehalf": "<alunoId>",
  "dados": { "finalidade": "BOLSA", "observacoes": "Aberta pelo balcão" }
}
```

**201.** Detalhe: `idSolicitante` = aluno, **não** a secretaria.

### Usuários

```
GET /usuarios?page=&size=&email=&nome=
GET /usuarios/{id}
PATCH /usuarios/{id}  { "ativo": false }
```

Query `nome` já não quebra `lower(bytea)` (CAST corrigido). Sem `system.admin` / list authority → 403.

### Atendimento de balcão

`POST /service-records` `{ idAluno, assunto, tipo: "PRESENCIAL", descricao }` → `PENDENTE_CIENCIA`. Aluno dá ciência na fatia 4.

### Tarefas kanban

Path real: `GET /tasks` — **não** `/tarefas`. Tutorial T-F5-012.

### Import / export CSV

T-F5-009 / T-F5-010. Jobs: `POST /export/...` + poll. UI: botão + JsonPanel do job. Não parsear CSV no React.

### Diplomas / colação (T-F5-005 — obrigatório)

Paths `GraduationController` `/graduations` (list, colar, diploma-url). Lista + botões `_links` + JsonPanel. Sem esta tela o egresso não prova download de diploma.

---

## 3. Coordenação

`{id}` aceita **UUID ou sigla** (`tads`). Ownership: `curso.id_coordenador == currentUserId()` (admin bypass) senão 403.

```
GET  /courses/tads/config
PATCH /courses/tads/config
```

Campos: `horasFormativasMinimas`, `duracaoCalendario`, `bancaMembrosExternos`, `bancaModalidade`, `regimento`.

Relatório: `GET /reports/coordinator` — **não** um `RelatoriosController` antigo.

`GET /reports/secretary` — secretaria. Native SQL timestamps já CAST timestamptz (regressão 42P18 corrigida). Query `from`/`to` opcionais.

Ana **não** elegível a formatura se horas formativas < mínimo após PATCH config — `bloqueios[]` no endpoint de graduação (T-F5 / T-F6). Mostrar JSON; não mascarar bloqueio.

---

## 4. Admin

### Request types (V019)

```
GET    /request-types
POST   /request-types          # rascunho ativo=false
PATCH  /request-types/{id}
POST   /request-types/{id}/publish   # snapshot request_type_version
DELETE /request-types/{id}
```

Aluno `GET /requests/types` só `ativo=true`. Depois do publish, **novas** instâncias usam o schema novo; instâncias velhas mantêm snapshot.

UI de testes: `<textarea>` JSON para `formSchema` e `workflowJson` + botão publish. Sem editor visual de state machine.

### IAM

```
GET /admin/roles          # alias /admin/perfis
GET /admin/autoridades
POST /admin/roles
```

Authority `iam.manage_roles`.

### Outbox

`GET /admin/outbox` default **PENDING**. Para ver e-mails disparados: `?status=PROCESSED`. Sem o query param a lista parece “vazia” e a equipe acha que o dispatcher morreu.

### Templates / audit

`GET /communication-templates` (path real; não o do diagrama se 404). `GET /audit` se existir no Swagger.

---

## 5. Busca global (F8)

```
GET /search?q=ana&page=0&size=10
GET /search?q=ana&types=USUARIO
```

`types`: `USUARIO,EVENTO,REQUEST,CURSO`. Resposta **plana**: `{ type, id, title, subtitle, href }[]` (+ `timedOut?`). Timeout 5s.

**FGAC:** aluno **não** recebe `type=USUARIO`. Secretaria/admin sim. Prova: dois logins, mesma `q=ana`.

`SearchController` copia `SecurityContext` para `supplyAsync` (já corrigido). Se voltar usuário no resultado do aluno, é regressão.

UI: input `q` + `JsonPanel`. Clique `href` → navigate.

---

## 6. FAQ / tickets staff

FAQ admin: `POST/PATCH/DELETE /faq`.  
Fila: `GET /support/tickets`. Respond/close: `PATCH /support/tickets/{id}/respond` · `/close`. DTO `descricao`.

---

## 7. Disciplinas (lookup wizard)

`GET /academico/disciplinas?search=` **sem** `idCurso` usa `searchActiveAll`.  
Com `idCurso` usa `searchByCurso`. CAST UUID bytea já corrigido.  
Alias: `GET /academico/cursos/{cursoId}/disciplinas`.

O wizard da fatia 3 já faz entity-select; esta tela staff só precisa do mesmo lookup se filtrar alunos/disciplinas.

---

## 8. Aceite (roteiro equipe)

1. Sec dashboard 200; prof nesse path 403
2. On-behalf 201; detalhe `idSolicitante` = Ana
3. Bulk DEFER em ABERTA declaração → 422 visível; ASSIGN depois DEFER → 200
4. Busca aluno `q=ana`: sem USUARIO; busca sec: com USUARIO
5. Admin publish type → aluno vê em `/requests/types`
6. Outbox `status=PROCESSED` mostra eventos
7. Reports secretary/coordinator 200 (não 500 42P18)
8. Coord `GET /courses/tads/config` 200; aluno 403
9. `GET /tasks` 200 (não `/tarefas`)
10. T-F5-005 colação/diploma-url 200 quando houver registro
11. T-F7-004 templates listáveis; T-F7 audit/FAQ admin disparáveis
12. Import + export job criados e polled (JsonPanel)

## 9. Não fazer

- Reimplementar workflow na tela admin além de textarea JSON.
- Relatórios em Excel no browser.
- Copiar 19 forms.
- Esconder botões por `roles.includes('ADMIN')` — o admin **tem** as authorities; o teste 403 é com aluno.
