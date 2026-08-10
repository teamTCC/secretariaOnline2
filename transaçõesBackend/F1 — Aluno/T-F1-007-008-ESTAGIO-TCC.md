# T-F1-007 / T-F1-008 — Estágio e TCC

> **Diagramas de referência:**  
> - [`foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-007-ESTAGIO.md`](../../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-007-ESTAGIO.md)  
> - [`foundationDocs/sequenceDiagrams/F1 — Aluno/US-F1-008-TCC.md`](../../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-008-TCC.md)  
> **Status:** ⏳ Stub — entidades e tabelas criadas pela migration V005

---

## O que está no banco

A migration `V005__formativas_estagio_tcc_schema.sql` cria:
- `estagio` / `estagio_relatorio`
- `tcc` / `tcc_banca`

## Relação com Solicitações

Fluxos de estágio e TCC que geram solicitações formais (registro de estágio obrigatório, solicitação de defesa) são roteados pelo **motor de workflow genérico** — ver [T-F1-005-SOLICITACOES](T-F1-005-SOLICITACOES.md).

## O que precisa ser implementado

| Módulo | Controller | Status |
|--------|-----------|--------|
| Estágio | `EstagioController` | ⏳ Não existe |
| TCC | `TccController` | ⏳ Não existe |

---

## Checklist de Verificação

- [ ] `GET /estagios/meu` → dados do estágio ativo do aluno
- [ ] `POST /estagios/{id}/relatorios` → submeter relatório semestral
- [ ] `GET /tcc/meu` → status do TCC do aluno
- [ ] `POST /tcc/{id}/bancas` → solicitar banca (via workflow)
