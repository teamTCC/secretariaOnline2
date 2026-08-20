# T-F3 — Professor (dashboard, eventos, deliberar, formativas, comunicado)

> **Transação:** [`T-F3-PROFESSOR.md`](../../transaçõesBackend/F3%20—%20Professor/T-F3-PROFESSOR.md)  
> **Diagramas:** [`foundationDocs/sequenceDiagrams/F3 — Professor/`](../../foundationDocs/sequenceDiagrams/F3%20—%20Professor/)  
> **Login:** `POST /auth/login` com o body abaixo. 

```json
{
  "identificador": "prof.ana@ufpr.br",
  "senha": "ProfS3nh@Forte!"
}
```

Os endpoints são os **mesmos** do aluno, com authorities diferentes. Este tutorial só monta a ordem no HTTPie.

---

## F3.1 — Dashboard

```
GET {{baseUrl}}/bff/dashboard/professor
Authorization: Bearer {{accessTokenProfessor}}
```

**Esperado 200:** `meusEventos`, `solicitacoesPendentes`, `_links.novoEvento` (`/events`), `_links.meuEventos`.

Sem `dashboard.view_self_professor` → **403**.

---

## F3.2 — Eventos

Siga [T-F1-009](../F1-aluno/T-F1-009-presenca.md) Passos 1, 2 e 5 com este token.

```
GET {{baseUrl}}/events?host=me
POST {{baseUrl}}/events
POST {{baseUrl}}/events/{{eventoId}}/attendance/windows/entry
POST {{baseUrl}}/events/{{eventoId}}/close
```

Bodies de evento e janela — cole o JSON correspondente ao passo:

SECRET_SINGLE: 

```json
{
  "titulo": "Palestra: Inteligência Artificial na Engenharia",
  "descricao": "Palestra do Prof. Dr. João com 4h de carga formativa. Criado via HTTPie.",
  "idCurso": "{{cursoId}}",
  "attendanceMode": "SECRET_SINGLE",
  "chCreditadas": 4.0,
  "inicioEm": "2026-08-20T14:00:00Z",
  "fimEm": "2026-08-20T18:00:00Z"
}
```

QR_DUAL:

```json
{
  "titulo": "Workshop React — QR dual",
  "descricao": "Entrada e saída por QR. Teste HTTPie.",
  "idCurso": "{{cursoId}}",
  "attendanceMode": "QR_DUAL",
  "chCreditadas": 8.0,
  "inicioEm": "2026-08-21T13:00:00Z",
  "fimEm": "2026-08-21T21:00:00Z"
}
```

Janela (entrada/saída):

```json
{
  "durationSeconds": 900
}
```

---

## F3.3 — Deliberar solicitações

```
GET {{baseUrl}}/requests?estado=EM_DELIBERACAO
GET {{baseUrl}}/requests/{{requestId}}
POST {{baseUrl}}/requests/{{requestId}}/transitions
```

Cole no Body:

```json
{
  "action": "DEFER",
  "parecer": "Deferido conforme documentação apresentada (teste HTTPie)."
}
```

Action seed: **`DEFER`**.

HATEOAS no detalhe: só chame o rel que aparecer. Detalhe do motor: [T-F1-005](../F1-aluno/T-F1-005-solicitacoes.md).

---

## F3.4 — Revisar formativas

```
GET {{baseUrl}}/formativas/pendentes
PATCH {{baseUrl}}/formativas/{{formativaId}}/review
```

Cole no Body:

```json
{
  "acao": "APROVAR",
  "parecer": "Comprovante válido — carga horária conferida."
}
```

Precisa `formative.review` (role CAAF / admin). Ver [T-F4-001](../F4-comissoes/T-F4-001-caaf.md).

---

## F3.5 / F3.6 — Estágio e TCC (orientação)

[T-F1-007-008](../F1-aluno/T-F1-007-008-estagio-tcc.md) — lados `internship.supervise` e `tcc.supervise`.

---

## F3.7 — Publicar comunicado

Cole no Body:

```json
{
  "titulo": "Aviso da turma TADS — HTTPie",
  "conteudo": "Prazo de formativas encerra sexta-feira às 18h.",
  "tipo": "AVISO",
  "cursoId": "{{cursoId}}"
}
```

`cursoId` é obrigatório para `communication.publish_class`.

```
POST {{baseUrl}}/communications
```

**Esperado 201** `{ id, entregas }`. Sem `cursoId` com só `publish_class` → **422**. Sem authority → **403**.

Inbox do aluno: [T-F1-004](../F1-aluno/T-F1-004-comunicacao.md).
