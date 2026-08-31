# FatiasFrontend — harness de contrato (web de testes)

Pasta de **contexto para o Grok implementar o frontend em fatias isoladas**.

Isto **não** é o produto visual (DashboardA / shadcn / Figma). É uma SPA **enxuta** cujo único objetivo é provar para a equipe que o **backend já rodando** entrega:

- cookies HttpOnly + CSRF Double Submit + Redis `sid`
- RFC 7807
- FGAC (403 cruzado por authority)
- **workflow engine** (`form_schema`, `workflow_json`, `request_type_version`, transições)
- **HATEOAS** `_links: Record<string, string>` (nunca HAL `{ href }`)

Código React: **mínimo de tokens no visual**. **Máxima cobertura funcional** — os 19 tipos e todas as transações `httpie/` F0–F8 entram no browser (três telas genéricas de solicitação, não 19 páginas). Ver [`09-cobertura-transacoes.md`](09-cobertura-transacoes.md).

Uma fatia por sessão. Não implementar fatia N+1 na mesma sessão.

---

## Ordem (obrigatória)

| # | Arquivo | O que prova |
|---|---------|-------------|
| 0 | [`00-contexto-geral.md`](00-contexto-geral.md) | stack, pastas, regras anti-token, o que **não** usar nesta versão |
| 1 | [`01-fatia-0-fundacao-http.md`](01-fatia-0-fundacao-http.md) | `client.ts`, CSRF, refresh, Problem+JSON, `useActions` |
| 2 | [`02-fatia-1-publico-f0.md`](02-fatia-1-publico-f0.md) | login / forgot / contato / erro / protocolo público |
| 3 | [`03-fatia-2-shell-dashboard.md`](03-fatia-2-shell-dashboard.md) | `/me`, first-access, BFF aluno, logout, 403 cruzado |
| 4 | [`04-fatia-3-workflow-engine.md`](04-fatia-3-workflow-engine.md) | **núcleo**: 19 tipos no mesmo wizard, draft, anexos, transitions |
| 5 | [`05-fatia-4-vida-academica.md`](05-fatia-4-vida-academica.md) | formativas, eventos/PIN, certificados, atendimentos, inbox |
| 6 | [`06-fatia-5-estagio-tcc-egresso.md`](06-fatia-5-estagio-tcc-egresso.md) | internships, tccs, BFF egresso |
| 7 | [`07-fatia-6-professor-comissoes.md`](07-fatia-6-professor-comissoes.md) | dashboard prof, janela de presença, CAAF/COE |
| 8 | [`08-fatia-7-secretaria-gestao.md`](08-fatia-7-secretaria-gestao.md) | fila, on-behalf, reports, import/export, admin types, search |
| — | [`09-cobertura-transacoes.md`](09-cobertura-transacoes.md) | mapa T-F* → tela (caixa-preta; cobertura 100%) |

Cada arquivo de fatia tem: pré-requisito, whitelist de arquivos, contrato HTTP as-built, UI mínima, checklist de aceite, o que **não** fazer.

---

## Como o Grok deve trabalhar

1. Ler **sempre** `00-contexto-geral.md` + **só** o arquivo da fatia pedida + as linhas dessa fatia em `09-cobertura-transacoes.md`.
2. Não reler `plano-entregas-frontend.md` / Figma / shadcn para esta versão (são o produto futuro).
3. Contrato vivo: código Kotlin + `httpie/` + `logs/log testes httpie - req_resp.md`.
4. Em conflito HU antiga vs as-built: **as-built vence** (`foundationDocs/analysis/as-built-backend.md`).
5. Parar quando o checklist da fatia estiver verde. Não “já deixar pronto” a fatia seguinte.

---

## Backend local (já validado via HTTPie)

- API: `http://localhost:8080`
- SPA: `http://localhost:5173` (CORS `app.cors.allowed-origins`)
- Profile `dev`: `COOKIE_SECURE=false`, SameSite=Lax
- Seed Flyway: admin, cursos TADS/ES, 19 `request_type`, FAQ, roles
- Usuários demo: `httpie/02-bootstrap-usuarios-demo.md` + `httpie/ambiente/local.json`

Credenciais típicas (se o bootstrap já rodou):

| Perfil | Email | Senha |
|--------|-------|-------|
| Admin | `admin@ufpr.br` | `Admin@123456` |
| Aluno | `ana.aluno@ufpr.br` | `AlunoS3nh@Forte!` |
| Professor | `prof.ana@ufpr.br` | `ProfS3nh@Forte!` |
| Secretaria | `secretaria@ufpr.br` | `SecrS3nh@Forte!` |
| Coordenador | `coord.tads@ufpr.br` | `CoordS3nh@Forte!` |

---

## Destino do código

Implementar em `frontend-web/` (hoje só tem `docs/`). Não criar um segundo app. Mobile Expo **não** entra nesta versão de testes (mesmo contrato Bearer fica documentado no contexto geral para a equipe).
