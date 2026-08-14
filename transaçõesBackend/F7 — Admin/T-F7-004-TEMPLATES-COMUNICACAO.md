# T-F7-004 — Templates de Comunicação

> **Diagrama:** [`foundationDocs/sequenceDiagrams/F7 — Admin/US-F7-004-TEMPLATES-COMUNICACAO.md`](../../foundationDocs/sequenceDiagrams/F7 — Admin/US-F7-004-TEMPLATES-COMUNICACAO.md)  
> **Status:** ✅ Catálogo versionado (revisões imutáveis)  
> **Capability:** `communication.manage_templates`

Os handlers de outbox (`RequestTransitionOutboxHandler`, `ComunicacaoOpsOutboxHandler`) consomem o catálogo via [`TemplateEngine`](../../backend/modules/comunicacao/src/main/kotlin/br/ufpr/sept/so2/modules/comunicacao/application/TemplateEngine.kt). Placeholders `{{nome}}`, `{{tipo}}`, `{{estadoNovo}}`, etc. Seed V015: `solicitacoes.transicionada`, `atendimentos.created`, `graduations.confirmed`, `imports.completed`, `exports.ready`, `contato.recebido`.

---

## Arquivo

[`comunicacao/api/CommunicationTemplateController.kt`](../../backend/modules/comunicacao/src/main/kotlin/br/ufpr/sept/so2/modules/comunicacao/api/CommunicationTemplateController.kt)

```
GET  /communication-templates
POST /communication-templates
     { "codigo": "aproveitamento.deferido", "titulo": "...", "assunto": "...", "corpo": "Olá {{nome}}", "canal": "EMAIL" }

POST /communication-templates/{id}/revisions   → incrementa versao (não apaga histórico)
GET  /communication-templates/{id}/versions
GET  /communication-templates/{id}/versions/{rev}
```

A listagem inclui `variaveis: ["nome","email","protocolo","eventoTitulo"]` para o autocomplete do editor (client-side).

---

## Checklist

- [x] Listar / criar (v1) / nova revisão
- [x] Histórico imutável
- [x] TemplateEngine do dispatcher lendo o catálogo (T-10.1)
