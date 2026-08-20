# T-F8-001 — Busca global

> **Transação:** [`T-F8-001`](../../transaçõesBackend/F8%20—%20Cross-cutting/T-F8-001-BUSCA-GLOBAL.md)  
> **Diagrama:** [`US-F8-001`](../../foundationDocs/sequenceDiagrams/F8%20—%20Cross-cutting/US-F8-001-BUSCA-GLOBAL.md)

Qualquer autenticado. FGAC no use case: aluno **não** vê `USUARIO`; solicitações só as próprias. Timeout servidor 5 s → `{ timedOut: true }`.

Path real: `GET /search?q=&types=&page=&size=`  
`types`: `USUARIO,EVENTO,REQUEST,CURSO` (vírgula). Sem `types` = todos.

A resposta do código é lista **plana** `{ type, id, title, subtitle, href }` (não os arrays agrupados do diagrama).

---

## Passo 1 — Como aluno

```
GET {{baseUrl}}/search?q=ana&page=0&size=10
Authorization: Bearer {{accessTokenAluno}}
```

**Esperado 200:** eventos/cursos/solicitações próprias. Itens `type=USUARIO` **ausentes**.

`q` vazio ou curto demais pode devolver lista vazia (empty state).

---

## Passo 2 — Como secretaria / admin

```
GET {{baseUrl}}/search?q=ana&types=USUARIO
GET {{baseUrl}}/search?q=DECLARACAO&types=REQUEST
GET {{baseUrl}}/search?q=TADS&types=CURSO
```

**Esperado:** usuários com `href` para ficha; requests com id clicável `/requests/{id}`.

Copie um `id` da resposta para o environment (`{{alunoId}}`, `{{requestId}}`, `{{eventoId}}`).

---

## Passo 3 — Timeout (só se o banco estiver lento)

Não force timeout em dev saudável. Se `_degraded`/`timedOut` aparecer, o fan-out estourou 5 s — verifique Postgres/`pg_trgm`.
