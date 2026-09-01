# Log Fatia 3 — Motor de solicitações (19 tipos, três telas)

**Quando:** 2026-08-31 (caixa-branca + HTTP/UI contra API real, com correção de bugs de front)  
**Contrato:** `FatiasFrontend/04-fatia-3-workflow-engine.md`  
**Oráculos:** `httpie/F1-aluno/T-F1-005-solicitacoes.md`  
**SPA:** `http://localhost:5173` (`frontend-web/`)  
**API:** `http://localhost:8080`

**Resultado (reteste 18:38):** aceite §9 **passa**. Front (5) + backend (unique de rascunho, `ABERTURA` no submit, seed de disciplinas). GitNexus: `SubmitDraftUseCase` / `findMaxNumeroAnual` risco **LOW**.

Delegating to: frontend-engineer, workflow-engine-specialist. GitNexus: SPA untracked — impacto UNKNOWN.

---

## 1) Caixa-preta HTTP (aluno `ana.aluno@ufpr.br`)

| Caso | Status |
|------|--------|
| `GET /requests/types` | **19** codes |
| `GET /academico/disciplinas?idCurso=TADS` | **200**, 5 disciplinas (V022) |
| `POST /requests` HISTORICO sem `vias` | **422** `propriedade obrigatória 'vias'` |
| `POST /requests` HISTORICO com `vias: 1` | **201** |
| `POST /requests` AUTORIZACAO_IMAGEM (`aceiteTermos: true`) | **201** `2026/0005` |
| `POST /requests` ADIANTAMENTO tabela `disciplinasDesejadas[]` | **201** `2026/0006` |
| Filtros `typeCode=HISTORICO_ESCOLAR` / `estado=ABERTA` | **200**, só aquele tipo / estado |
| Presign órfão + **PUT MinIO** (browser, CORS) | **200** + **200** |
| `POST /requests` SEGUNDA sem atestado | **422** anexo obrigatório |
| `POST /requests` SEGUNDA + PUT + attachments inline | **201** `2026/0007`, `GET attachments` tem `ATESTADO_MEDICO` |
| `POST /requests` COLACAO sem comprovante | **422** `COMPROVANTE_MOTIVO` |
| 1º `POST /requests/draft` | **201** `RASCUNHO` |
| 2º `POST /requests/draft` (mesmo `idCurso`/`ano`) | **201** (V021 índice parcial) |
| Submit do 2º draft AUTORIZACAO | **200** `2026/0009`; events `ABERTURA:RASCUNHO→ABERTA` |

Professor `prof.ana@ufpr.br`: `GET /requests` **200** (9 itens, `request.view_curso`); `POST /requests` **403**; detalhe com `assign`; attachments/protocol **200**. Lista mostra checkbox só onde `_links.bulk_deliberate`.

---

## 2) Bugs de front encontrados e corrigidos

### B1 — `schema.default` ignorado (`HISTORICO.vias`)

Campo número nascia vazio; open sem preencher → 422. Schema tem `"default": 1`.

**Fix:** `defaultsFromSchema` no wizard; `min`/`max` no `<input type=number>`.  
**Prova UI:** vias mostra **1**; POST só com finalidade CONCURSO → `2026/0008` `dados.vias = 1`.

### B2 — `datalist` id inválido e duplicado

`list="Disciplina *-dl"` (espaço + `*`); N linhas da tabela compartilhavam o mesmo id.

**Fix:** `useId()` por `EntitySelect`.  
**Prova:** três linhas → ids `:r1:`, `:r3:`, `:r5:` (únicos, sem espaço).

### B3 — detalhe não passava `idCurso` no entity-select

`GET /academico/disciplinas` **sem** query. Spec: passar `idCurso` no lookup.

**Fix:** `idCurso` de `GET /me.metadata.idCurso`.  
**Prova:** detalhe `2026/0006` → `GET /academico/disciplinas?idCurso=01a05940-ec36-75cc-ab53-d999ce0a7fa1`.

### B4 — invalidação da lista não batia na query key

`invalidateQueries(queryKeys.requests({}))` = `['requests','list',{}]`. A lista usa `{estado, typeCode, idCurso, page}`. TanStack prefix **não** casa objeto diferente.

**Fix:** `invalidateQueries({ queryKey: ['requests', 'list'] })`.

### B5 — `onReady` após PUT MinIO falho + confirm invisível no wizard

Ghost `storageKey` ia no `POST /requests` → 400. Sem `requestId`, não havia botão de fallback.

**Fix:** `onReady` só se PUT **ok**; botão **incluir no POST** quando há `storageKey` (CORS/HTTPie). Confirm no detalhe permanece.

---

## 3) Bugs de backend corrigidos (reteste)

| Bug | Fix | Prova |
|-----|-----|--------|
| 2º draft **500** unique `(numero_anual, ano, id_curso)` | V021 índice parcial só em protocolo oficial (`estado <> RASCUNHO`, `numero_anual > 0`) | dois `POST /draft` **201** |
| Submit sem `ABERTURA` | `SubmitDraftUseCase` grava `RequestEvent` | events `ABERTURA:RASCUNHO→ABERTA`; protocolo `2026/0009` |
| Entity-select vazio | V022 seed TADS (`ADS001`–`ADS005`) + ES | UI datalist 5 opções `codigo — nome`; UUID no `value` |

`GET /me.authorities[]` e aceite §9.9 (publish admin) continuam fora desta fatia.

---

## 4) Aceite §9 (após correção)

1. 19 codes no `<select>` — **PASS**
2. DECLARACAO open; aluno sem `defer`; sec ASSIGN→DEFER→DEFERIDA — **PASS**
3. Draft → PATCH → submit; 2º draft no mesmo curso — **PASS**
4. SEGUNDA sem atestado **422**; com PUT MinIO + inline **201** — **PASS**
5. TRANCAMENTO ≥1 linha → `dados.disciplinas[]` — **PASS**
6. Entity-select lista disciplinas e grava UUID — **PASS** (`ADS001`–`ADS005`)
7. Timeline `ABERTURA` no open **e** no submit de rascunho — **PASS**
8. Action `DEFERIR` → **422** — **PASS**
9. Snapshot schema após publish admin — **N/A** (fatia 7)
