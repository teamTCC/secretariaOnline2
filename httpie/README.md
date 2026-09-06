# HTTPie Desktop — Tutoriais de teste das transações

> Pasta irmã de `[transaçõesBackend/](../transaçõesBackend/README.md)`.  
> Cada tutorial descreve **como testar no HTTPie Desktop** o que as transações implementam no backend.  
> Contrato vivo: [`as-built-backend.md`](../foundationDocs/analysis/as-built-backend.md).

---

## Comece aqui

| Ordem | Arquivo                                                              | Para quê                                                       |
| ----- | -------------------------------------------------------------------- | -------------------------------------------------------------- |
| 1     | [00-setup-httpie-desktop.md](00-setup-httpie-desktop.md)             | Instalar, coleção, ambiente, CSRF, Bearer, cookies             |
| 2     | [01-ids-credenciais-e-ambiente.md](01-ids-credenciais-e-ambiente.md) | Usuários demo, placeholders `{{id}}`, SQL para descobrir UUIDs |
| 3     | [ambiente/local.json](ambiente/local.json)                           | Variáveis para colar no Environment do HTTPie                  |
| 4     | Tutoriais `F0` → `F8` + transversais                                 | Passo a passo + JSON esperado                                  |

Os **bodies JSON** estão **dentro de cada tutorial** — copie o bloco e cole no painel Body do HTTPie.

---

## Convenções deste pacote

- **Base URL local:** `http://localhost:8080`
- **Swagger (contrato vivo):** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **E-mail em dev:** `ops/docker-compose.yml` **não** sobe Mailhog/Mailpit nesta passagem as-built. Leia o JWT de reset/OTT no payload de `outbox_event` (`GET /admin/outbox`). Catcher SMTP em `:8025` é opcional e fora do compose operacional.
- **Placeholders:** valores `{{entre_colchetes}}` devem ser substituídos por IDs reais da sua base (veja o catálogo).
- **Caminhos reais dos controllers** (não só os dos diagramas). Quando o diagrama e o código divergem, o tutorial usa o **código**.

Cada tutorial aponta de volta para:

- a transação em `transaçõesBackend/`
- o diagrama em `foundationDocs/sequenceDiagrams/`

---

## Índice

### Setup

| ID  | Tutorial                                                             |
| --- | -------------------------------------------------------------------- |
| —   | [00 · Setup HTTPie Desktop](00-setup-httpie-desktop.md)              |
| —   | [01 · IDs, credenciais e ambiente](01-ids-credenciais-e-ambiente.md) |
| —   | [02 · Bootstrap de usuários demo](02-bootstrap-usuarios-demo.md)     |

### F0 — Público (sem JWT, ou JWT de 1 uso)

| ID           | Tutorial                                                                             | Transação                                                                                   | Diagrama                                                                                                                                                                                                |
| ------------ | ------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| T-F0-001     | [Login / refresh / logout / CSRF / **POST /auth/ott**](F0-publico/T-F0-001-login.md) | [T-F0-001](../transaçõesBackend/F0%20—%20Público/T-F0-001-LOGIN.md)                         | [US-F0-001](../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-001-LOGIN.md) (inclui F0.1-g OTT)                                                                                                 |
| T-F0-002     | [Recuperar senha](F0-publico/T-F0-002-recuperar-senha.md)                            | [T-F0-002](../transaçõesBackend/F0%20—%20Público/T-F0-002-RECUPERAR-SENHA.md)               | [US-F0-002](../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-002-RECUPERAR-SENHA.md)                                                                                                           |
| T-F0-003     | [Nova senha (token 1 uso)](F0-publico/T-F0-003-nova-senha.md)                        | [T-F0-003](../transaçõesBackend/F0%20—%20Público/T-F0-003-NOVA-SENHA.md)                    | [US-F0-003](../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-003-NOVA-SENHA.md)                                                                                                                |
| T-F0-004     | [Contato público](F0-publico/T-F0-004-contato.md)                                    | [T-F0-004](../transaçõesBackend/F0%20—%20Público/T-F0-004-CONTATO.md)                       | [US-F0-004](../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-004-CONTATO.md)                                                                                                                   |
| T-F0-005     | [Erros RFC 7807](F0-publico/T-F0-005-erros.md)                                       | [T-F0-005](../transaçõesBackend/F0%20—%20Público/T-F0-005-ERRO.md)                          | [US-F0-005](../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-005-ERRO.md)                                                                                                                      |
| T-F0-006/007 | [Protocolo + certificado públicos](F0-publico/T-F0-006-007-verificacoes-publicas.md) | [T-F0-006-007](../transaçõesBackend/F0%20—%20Público/T-F0-006-007-VERIFICACOES-PUBLICAS.md) | [US-F0-006](../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-006-VERIFICAR-PROTOCOLO.md) · [US-F0-007](../foundationDocs/sequenceDiagrams/F0%20—%20Público/US-F0-007-VERIFICAR-CERTIFICADO.md) |

### F1 — Aluno

| ID           | Tutorial                                                                          | Transação                                                                                     | Diagrama                                                                                                                                                                            |
| ------------ | --------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| T-F1-001     | [Dashboard BFF](F1-aluno/T-F1-001-dashboard.md)                                   | [T-F1-001](../transaçõesBackend/F1%20—%20Aluno/T-F1-001-DASHBOARD.md)                         | [US-F1-001](../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-001-DASHBOARD.md)                                                                                               |
| T-F1-002     | [Primeiro acesso](F1-aluno/T-F1-002-primeiro-acesso.md)                           | [T-F1-002](../transaçõesBackend/F1%20—%20Aluno/T-F1-002-PRIMEIRO-ACESSO.md)                   | [US-F1-002](../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-002-PRIMEIRO-ACESSO.md)                                                                                         |
| T-F1-003     | [Perfil /me](F1-aluno/T-F1-003-perfil.md)                                         | [T-F1-003](../transaçõesBackend/F1%20—%20Aluno/T-F1-003-PERFIL.md)                            | [US-F1-003](../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-003-PERFIL.md)                                                                                                  |
| T-F1-004     | [Comunicação (inbox)](F1-aluno/T-F1-004-comunicacao.md)                           | [T-F1-004](../transaçõesBackend/F1%20—%20Aluno/T-F1-004-COMUNICACAO.md)                       | [US-F1-004](../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-004-COMUNICACAO.md)                                                                                             |
| T-F1-005     | [Solicitações + workflow](F1-aluno/T-F1-005-solicitacoes.md)                      | [T-F1-005](../transaçõesBackend/F1%20—%20Aluno/T-F1-005-SOLICITACOES.md)                      | [US-F1-005](../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-005-SOLICITACOES.md)                                                                                            |
| T-F1-006     | [Horas formativas](F1-aluno/T-F1-006-formativas.md)                               | [T-F1-006](../transaçõesBackend/F1%20—%20Aluno/T-F1-006-FORMATIVAS.md)                        | [US-F1-006](../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-006-FORMATIVAS.md)                                                                                              |
| T-F1-007/008 | [Estágio e TCC](F1-aluno/T-F1-007-008-estagio-tcc.md)                             | [T-F1-007-008](../transaçõesBackend/F1%20—%20Aluno/T-F1-007-008-ESTAGIO-TCC.md)               | [US-F1-007](../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-007-ESTAGIO.md) · [US-F1-008](../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-008-TCC.md)               |
| T-F1-009     | [Presença em eventos](F1-aluno/T-F1-009-presenca.md)                              | [T-F1-009](../transaçõesBackend/F1%20—%20Aluno/T-F1-009-PRESENCA.md)                          | [US-F1-009](../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-009-PRESENCA.md)                                                                                                |
| T-F1-010/011 | [Certificados e atendimentos](F1-aluno/T-F1-010-011-certificados-atendimentos.md) | [T-F1-010-011](../transaçõesBackend/F1%20—%20Aluno/T-F1-010-011-CERTIFICADOS-ATENDIMENTOS.md) | [US-F1-010](../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-010-CERTIFICADOS.md) · [US-F1-011](../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-011-ATENDIMENTOS.md) |

### F2 — Egresso

| ID       | Tutorial                                                      | Transação                                                                       | Diagrama                                                                                        |
| -------- | ------------------------------------------------------------- | ------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| T-F2-001 | [Dashboard egresso](F2-egresso/T-F2-001-dashboard-egresso.md) | [T-F2-001](../transaçõesBackend/F2%20—%20Egresso/T-F2-001-DASHBOARD-EGRESSO.md) | [US-F2-001](../foundationDocs/sequenceDiagrams/F2%20—%20Egresso/US-F2-001-DASHBOARD-EGRESSO.md) |

### F3 — Professor

| ID   | Tutorial                                                                    | Transação                                                         | Diagrama                                                           |
| ---- | --------------------------------------------------------------------------- | ----------------------------------------------------------------- | ------------------------------------------------------------------ |
| T-F3 | [Dashboard, eventos, deliberar, formativas](F3-professor/T-F3-professor.md) | [T-F3](../transaçõesBackend/F3%20—%20Professor/T-F3-PROFESSOR.md) | [pasta F3](../foundationDocs/sequenceDiagrams/F3%20—%20Professor/) |

### F4 — Comissões

| ID       | Tutorial                              | Transação                                                                     | Diagrama                                                                                      |
| -------- | ------------------------------------- | ----------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| T-F4-001 | [CAAF](F4-comissoes/T-F4-001-caaf.md) | [T-F4-001](../transaçõesBackend/F4%20—%20Comissões/T-F4-001-COMISSAO-CAAF.md) | [US-F4-001](../foundationDocs/sequenceDiagrams/F4%20—%20Comissões/US-F4-001-COMISSAO-CAAF.md) |
| T-F4-002 | [COE](F4-comissoes/T-F4-002-coe.md)   | [T-F4-002](../transaçõesBackend/F4%20—%20Comissões/T-F4-002-COMISSAO-COE.md)  | [US-F4-002](../foundationDocs/sequenceDiagrams/F4%20—%20Comissões/US-F4-002-COMISSAO-COE.md)  |

### F5 — Secretaria

| ID       | Tutorial                                                                  | Transação                                                                          | Diagrama                                                                                           |
| -------- | ------------------------------------------------------------------------- | ---------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| T-F5     | [Dashboard, fila, alunos, atendimentos](F5-secretaria/T-F5-secretaria.md) | [T-F5](../transaçõesBackend/F5%20—%20Secretaria/T-F5-SECRETARIA.md)                | [pasta F5](../foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/)                                |
| T-F5-005 | [Egressos / diplomas](F5-secretaria/T-F5-005-egressos-diplomas.md)        | [T-F5-005](../transaçõesBackend/F5%20—%20Secretaria/T-F5-005-EGRESSOS-DIPLOMAS.md) | [US-F5-005](../foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/US-F5-005-EGRESSOS-DIPLOMAS.md) |
| T-F5-009 | [Importação CSV](F5-secretaria/T-F5-009-importacoes.md)                   | [T-F5-009](../transaçõesBackend/F5%20—%20Secretaria/T-F5-009-IMPORTACOES.md)       | [US-F5-009](../foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/US-F5-009-IMPORTACOES.md)       |
| T-F5-010 | [Exportações CSV](F5-secretaria/T-F5-010-exportacoes.md)                  | [T-F5-010](../transaçõesBackend/F5%20—%20Secretaria/T-F5-010-EXPORTACOES.md)       | [US-F5-010](../foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/US-F5-010-EXPORTACOES.md)       |
| T-F5-011 | [Estatísticas](F5-secretaria/T-F5-011-estatisticas.md)                    | [T-F5-011](../transaçõesBackend/F5%20—%20Secretaria/T-F5-011-ESTATISTICAS.md)      | [US-F5-011](../foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/US-F5-011-ESTATISTICAS.md)      |
| T-F5-012 | [Kanban / tarefas](F5-secretaria/T-F5-012-tarefas.md)                     | [T-F5-012](../transaçõesBackend/F5%20—%20Secretaria/T-F5-012-TAREFAS.md)           | [US-F5-012](../foundationDocs/sequenceDiagrams/F5%20—%20Secretaria/US-F5-012-TAREFAS.md)           |

### F6 — Coordenação

| ID   | Tutorial                                                        | Transação                                                                                                                                                         | Diagrama                                                                                                                                                                                          |
| ---- | --------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| T-F6 | [Config curso + relatórios](F6-coordenacao/T-F6-coordenacao.md) | [T-F6-001](../transaçõesBackend/F6%20—%20Coordenação/T-F6-001-CONFIGURAR-CURSO.md) · [T-F6-002](../transaçõesBackend/F6%20—%20Coordenação/T-F6-002-RELATORIOS.md) | [US-F6-001](../foundationDocs/sequenceDiagrams/F6%20—%20Coordenação/US-F6-001-CONFIGURAR-CURSO.md) · [US-F6-002](../foundationDocs/sequenceDiagrams/F6%20—%20Coordenação/US-F6-002-RELATORIOS.md) |

### F7 — Admin

| ID       | Tutorial                                                   | Transação                                                                         | Diagrama                                                                                           |
| -------- | ---------------------------------------------------------- | --------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| T-F7     | [Audit, FAQ, LGPD, saúde](F7-admin/T-F7-admin.md)          | [T-F7](../transaçõesBackend/F7%20—%20Admin/T-F7-ADMIN.md)                         | [pasta F7](../foundationDocs/sequenceDiagrams/F7%20—%20Admin/)                                     |
| T-F7-002 | [Perfis e authorities](F7-admin/T-F7-002-iam-perfis.md)    | [T-F7-002](../transaçõesBackend/F7%20—%20Admin/T-F7-002-IAM-PERFIS.md)            | [US-F7-002](../foundationDocs/sequenceDiagrams/F7%20—%20Admin/US-F7-002-IAM-PERFIS-AUTORIDADES.md) |
| T-F7-003 | [Editor RequestType](F7-admin/T-F7-003-workflow-engine.md) | [T-F7-003](../transaçõesBackend/F7%20—%20Admin/T-F7-003-WORKFLOW-ENGINE.md)       | [US-F7-003](../foundationDocs/sequenceDiagrams/F7%20—%20Admin/US-F7-003-WORKFLOW-ENGINE.md)        |
| T-F7-004 | [Templates de comunicação](F7-admin/T-F7-004-templates.md) | [T-F7-004](../transaçõesBackend/F7%20—%20Admin/T-F7-004-TEMPLATES-COMUNICACAO.md) | [US-F7-004](../foundationDocs/sequenceDiagrams/F7%20—%20Admin/US-F7-004-TEMPLATES-COMUNICACAO.md)  |

### F8 — Cross-cutting

| ID       | Tutorial                                          | Transação                                                                        | Diagrama                                                                                         |
| -------- | ------------------------------------------------- | -------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| T-F8-001 | [Busca global](F8-cross/T-F8-001-busca.md)        | [T-F8-001](../transaçõesBackend/F8%20—%20Cross-cutting/T-F8-001-BUSCA-GLOBAL.md) | [US-F8-001](../foundationDocs/sequenceDiagrams/F8%20—%20Cross-cutting/US-F8-001-BUSCA-GLOBAL.md) |
| T-F8-002 | [FAQ + tickets](F8-cross/T-F8-002-suporte-faq.md) | [T-F8-002](../transaçõesBackend/F8%20—%20Cross-cutting/T-F8-002-SUPORTE-FAQ.md)  | [US-F8-002](../foundationDocs/sequenceDiagrams/F8%20—%20Cross-cutting/US-F8-002-SUPORTE-FAQ.md)  |

### Transversais

| ID     | Tutorial                                                     | Transação                                                         | Diagrama                                                                                |
| ------ | ------------------------------------------------------------ | ----------------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| T-10.1 | [Outbox (observar no banco)](transversal/T-10.1-outbox.md)   | [T-10.1](../transaçõesBackend/transversal/T-10.1-OUTBOX.md)       | [10.1](../foundationDocs/sequenceDiagrams/transversal/10.1-outbox-notificacao.md)       |
| T-10.4 | [Certificado anti-fraude](transversal/T-10.4-certificado.md) | [T-10.4](../transaçõesBackend/transversal/T-10.4-CERTIFICADO.md)  | [10.4](../foundationDocs/sequenceDiagrams/transversal/10.4-certificado-emissao.md)      |
| T-10.6 | [Admin outbox](transversal/T-10.6-admin-outbox.md)           | [T-10.6](../transaçõesBackend/transversal/T-10.6-ADMIN-OUTBOX.md) | [US-F7-005](../foundationDocs/sequenceDiagrams/F7%20—%20Admin/US-F7-005-JOBS-OUTBOX.md) |
| T-10.7 | [Redis session + cache BFF](F0-publico/T-F0-001-login.md) + [dashboard](F1-aluno/T-F1-001-dashboard.md) | [T-10.7](../transaçõesBackend/transversal/T-10.7-REDIS-BFF.md) | — |

---

## Ordem sugerida de teste (caminhada)

```
1. GET  /actuator/health
2. GET  /auth/csrf                          → cookie XSRF-TOKEN
3. POST /auth/login                         → cookies access_token + refresh_token (sem JWT no JSON)
3b. POST /auth/ott {token}                  → mesmo 200 + cookies; 2ª chamada 401 (replay)
4. GET  /me                                 → confirma sessão (cookie ou Bearer fallback)
5. GET  /bff/dashboard/aluno                (session do aluno)
6. GET  /requests/types                     → copie idRequestType e idCurso
7. POST /requests                           → copie requestId
8. GET  /requests/{id}                      → leia `_links` (mapa rel → string URL, não HAL `{ href }`)
9. Continue pela transação que estiver implementando
```

Detalhes: [00-setup-httpie-desktop.md](00-setup-httpie-desktop.md).
