# T-F1-009 — Presença em eventos (v4.1)

> **Transação:** [`T-F1-009`](../../transaçõesBackend/F1%20—%20Aluno/T-F1-009-PRESENCA.md)  
> **Diagrama:** [`US-F1-009`](../../foundationDocs/sequenceDiagrams/F1%20—%20Aluno/US-F1-009-PRESENCA.md)  
> **IDs:** `{{eventoId}}`, `{{pinEntrada}}`, `{{qrToken}}`, `{{deviceUuid}}`, `{{cursoId}}`

Dois papéis no mesmo tutorial: **professor** cria/abre janela; **aluno** confirma. Use dois environments ou troque `{{accessToken}}`.

Modos: `SECRET_SINGLE` · `SECRET_DUAL` · `QR_SINGLE` · `QR_DUAL`.

---

## Passo 1 — Criar evento (professor `event.manage`)

Cole no Body:

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

Ajuste `inicioEm`/`fimEm` para **agora ± algumas horas** (senão a janela pode abrir mas o estado ainda `AGENDADO`).

```
POST {{baseUrl}}/events
Authorization: Bearer {{accessTokenProfessor}}
X-XSRF-TOKEN: {{xsrfToken}}
```

**Esperado 201:** `{ "id": "…", "_links": { "self": "/events/…" } }` → `{{eventoId}}`.

```
GET {{baseUrl}}/events/{{eventoId}}
```

Leia `estado` e `_links`. Host em `EM_ANDAMENTO` ganha `abrir-janela-entrada` e `encerrar-evento`.

Se o create deixar `AGENDADO`, o host precisa avançar o estado conforme a API (muitos fluxos passam a `EM_ANDAMENTO` ao abrir a primeira janela — confira a resposta do Passo 2).

---

## Passo 2 — Abrir janela de entrada (`event.host`)

Cole no Body:

```json
{
  "durationSeconds": 900
}
```

(900 s = 15 min)

```
POST {{baseUrl}}/events/{{eventoId}}/attendance/windows/entry
```

**Esperado 200** com a janela no JSONB. Copie `secret` (PIN 6 dígitos) → `{{pinEntrada}}`. Em modo QR copie `qrToken` → `{{qrToken}}`.

DUAL — saída:

```
POST {{baseUrl}}/events/{{eventoId}}/attendance/windows/exit
```

Cole no Body:

```json
{
  "durationSeconds": 900
}
```

---

## Passo 3 — Aluno vê a sessão (HATEOAS)

```
GET {{baseUrl}}/events?audience=me
Authorization: Bearer {{accessTokenAluno}}
```

O aluno precisa de `metadata.idCurso` = curso do evento ([bootstrap](../02-bootstrap-usuarios-demo.md)).

```
GET {{baseUrl}}/events/{{eventoId}}/attendance/session
```

**Esperado 200:**

```json
{
  "idEvento": "…",
  "estado": "EM_ANDAMENTO",
  "attendanceMode": "SECRET_SINGLE",
  "entryConfirmedAt": null,
  "exitConfirmedAt": null,
  "isComplete": false,
  "_links": [
    { "rel": "self", "href": "/events/…/attendance/session" },
    { "rel": "confirmar-entrada", "href": "/events/…/attendance/entry", "type": "POST" }
  ]
}
```

Sem janela ativa o link `confirmar-entrada` some. Fora da janela o POST dá **403/400**.

---

## Passo 4 — Confirmar entrada (PIN)

Cole no Body:

```json
{
  "pin": "{{pinEntrada}}",
  "qrToken": null,
  "deviceUuid": "{{deviceUuid}}"
}
```

```
POST {{baseUrl}}/events/{{eventoId}}/attendance/entry
Authorization: Bearer {{accessTokenAluno}}
X-XSRF-TOKEN: {{xsrfToken}}
```

**Esperado 200:** `{ "mensagem": "Entry confirmada com sucesso." }`

PIN errado → erro de negócio. Mesmo `{{deviceUuid}}` com outro aluno → conflito de device binding.

QR:

- Body 

```json
{
  "pin": null,
  "qrToken": "{{qrToken}}",
  "deviceUuid": "{{deviceUuid}}"
}
```

 no mesmo POST entry, **ou**
- `POST /events/{{eventoId}}/attendance/qr/validate` com 

```json
{
  "qrToken": "{{qrToken}}",
  "deviceUuid": "{{deviceUuid}}"
}
```

A fase (entrada/saída) é inferida da sessão.

Saída (DUAL): `POST /events/{{eventoId}}/attendance/exit`.

---

## Passo 5 — Encerrar e emitir certificados (host)

```
POST {{baseUrl}}/events/{{eventoId}}/close
Authorization: Bearer {{accessTokenProfessor}}
```

**Esperado 200:**

```json
{
  "mensagem": "Evento encerrado. 1 certificados emitidos.",
  "certificadosEmitidos": 1
}
```

Estado `CONCLUIDO` (imutável). Aluno: [T-F1-010](T-F1-010-011-certificados-atendimentos.md). Público: [T-F0-007](../F0-publico/T-F0-006-007-verificacoes-publicas.md).
