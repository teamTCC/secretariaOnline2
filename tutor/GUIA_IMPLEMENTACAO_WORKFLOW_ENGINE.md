# Guia de implementação — Workflow Engine (frontend)

O conteúdo canônico vive em:

**[`frontend-web/docs/GUIA_IMPLEMENTACAO_WORKFLOW_ENGINE.md`](../frontend-web/docs/GUIA_IMPLEMENTACAO_WORKFLOW_ENGINE.md)**

Atualizado em 2026-08-29 contra `foundationDocs/analysis/as-built-backend.md`:

- `_links` sempre `Map<String,String>` (sem HAL `{ href }`)
- GET de lista/detalhe/types em `RequestQuery`
- Publish grava snapshot `request_type_version` (Flyway V019); detalhe usa `formSchema` da versão da instância
- Deep-link: `POST /auth/ott` `{ "token" }`
- Lookup de disciplinas: `GET /academico/disciplinas` (alias) **ou** `GET /academico/cursos/{cursoId}/disciplinas`

Não edite um segundo guia neste arquivo — altere só o canônico.
