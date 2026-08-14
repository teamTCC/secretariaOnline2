O **backend das transações dos diagramas** (MVP + profundidade beyond-MVP) está implementado. O que resta é produto fora deste recorte: web, mobile, testes E2E e OpenAPI tipado.

## Já coberto no backend

Login, CSRF Double Submit, contato público, solicitações/workflow, formativas (comprovante MinIO + certificado na aprovação), presença, estágio, TCC, comissões, dashboards BFF, comunicação (TemplateEngine + in-app), atendimentos (ciência + agendamento aluno), diplomas/colação (5 critérios + PDF), import CSV alunos/professores, export assíncrono, kanban, relatórios filtrados (série, carga, colações/ano), admin (roles, RequestType, templates, outbox, audit, FAQ), busca `pg_trgm` + timeout 5s, suporte.

---

## Fora deste recorte (não backend)

| Item | Por quê |
|------|--------|
| App **Expo/mobile** | Não há pasta `mobile/` |
| **Frontend web** (React) | Não há `frontend-web/` neste repo |
| Testes de integração/E2E | Unitários IAM + workflow |
| OpenAPI tipado | Muita resposta `Map<String, Any?>` |
| Import Excel | CSV UTF-8 apenas |

---

**Resumo:** o backend cobre as transações dos diagramas com PDF, CSRF, config de curso, comprovante, relatórios analíticos e worker de export. Falta o walking skeleton web/mobile para a defesa como produto.
