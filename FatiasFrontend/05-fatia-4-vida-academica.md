# Fatia 4 — Vida acadêmica (formativas, presença, certificados, atendimentos, hub)

**Objetivo da demo:** o back aplica janela de presença, FGAC de certificado (IDOR 403), ciência de atendimento e inbox. Visual: forms nativos + JSON.

**Pré-requisito:** Fatias 0–3 (login + CSRF). Eventos exigem **dois perfis** (prof cria, aluno check-in).  
**Oráculos:** `T-F1-004`, `T-F1-006`, `T-F1-009`, `T-F1-010-011`.

Não reimplementar o wizard de solicitações. Links do dashboard aluno (`formativas`, `eventos`) já apontam para cá.

---

## 1. Whitelist

```
features/academico/
  FormativasPage.tsx          # /formativas
  EventosAlunoPage.tsx        # /eventos
  EventoSessaoPage.tsx        # /eventos/:id/presenca
  CertificadosPage.tsx        # /certificados
  AtendimentosPage.tsx        # /atendimentos
  InboxPage.tsx               # /comunicados
  FaqPage.tsx                 # /faq  (GET /faq — não /support/faq)
```

Professor **cria/abre janela** na fatia 6. Nesta fatia o aluno só consome sessão. Se a equipe ainda não tem fatia 6, a `EventosAlunoPage` pode incluir um aviso: “abra a janela logado como prof (HTTPie T-F1-009)”.

---

## 2. Formativas (aluno)

Authorities: `formative.submit`, `formative.view_own`. Review: CAAF fatia 6.

Fluxo MinIO (as-built):

1. `POST /formativas/comprovantes/presigned-url` `{ filename, contentType }` → `{ uploadUrl, storageKey }`
2. `PUT uploadUrl` via `AttachmentUpload` (mesmo da fatia 3). CORS MinIO falhou: JSON do presign + input `storageKey` **além** do PUT — o POST formativa continua obrigatório.
3. `POST /formativas` 

```json
{
  "titulo": "Palestra: Machine Learning Aplicado",
  "descricao": "…",
  "categoria": "PALESTRA",
  "cargaHoraria": 4.0,
  "dataRealizacao": "2026-06-15",
  "storageKeyComprovante": "<storageKey>"
}
```

**201** `{ id, estado: "PENDENTE" }`.

Lista: `GET /formativas/minhas` (confirmar path no Swagger se alias `GET /formativas?mine=`). `_links` do dashboard: `/formativas/minhas`.

UI: form + lista + JsonPanel. Sem galeria PDF.

---

## 3. Presença v4.1 (aluno)

Modos: `QR_SINGLE` | `QR_DUAL` | `SECRET_SINGLE` | `SECRET_DUAL`.  
Estados evento: `AGENDADO` → `EM_ANDAMENTO` (abre janela) → `CONCLUIDO`.

**Bug já corrigido no back:** abrir janela **não** pode persistir `AGENDADO` por cima. Check-in em `AGENDADO` → 409. Se a demo 409, o evento não está `EM_ANDAMENTO`.

Paths:

| Uso | Path |
|-----|------|
| Lista aluno | `GET /events?audience=me` (precisa `metadata.idCurso` = curso do evento) |
| Sessão HATEOAS | `GET /events/{id}/attendance/session` |
| Entrada | `POST /events/{id}/attendance/entry` |
| Saída (DUAL) | `POST /events/{id}/attendance/exit` |

Body check-in:

```json
{
  "pin": "123456",
  "qrToken": null,
  "deviceUuid": "<uuid estável do browser>"
}
```

Gerar `deviceUuid` uma vez em `localStorage` (não é JWT; é anti-share `UNIQUE (id_evento, device_uuid)` quando a política exige).

`_links` da sessão: as-built prefere map string; HTTPie ainda documenta array HAL. `normalizeLinks` da fatia 0.

Sem janela ativa: rel `confirmar-entrada` **some**. Fora da janela: POST 403/400. Rate limit no confirm.

PIN/QR: o **host** (prof) vê `secret` / `qrToken` na resposta de `POST .../windows/entry`. Aluno **não** deve receber o PIN no GET sessão. A UI do aluno tem um `<input pin>`. Na demo, a equipe cola o PIN da tela do professor (fatia 6) ou do HTTPie.

Não implementar câmera QR: input texto `qrToken`. **Os quatro modos** (`SECRET_SINGLE`, `SECRET_DUAL`, `QR_SINGLE`, `QR_DUAL`) têm de ser testáveis: aluno confirma entry e, se DUAL, exit. O host escolhe o modo na fatia 6.

---

## 4. Certificados (nascem auditados)

Pré: evento `CONCLUIDO` com presença completa **ou** formativa aprovada.

`GET /certificates/mine` → `hashSha256`, `_links.download`, `_links.verify` (`/publico/verificar-certificado/{hash}`).

`GET /certificates/{id}/download-url` → URL MinIO TTL 15 min. **IDOR** (id de outro aluno) → 403. Botão “tentar id aleatório” na página de testes é útil.

Verificação pública: já na fatia 1. Ed25519: chave **efêmera no restart da JVM** → cert antigo `INVALID`. Evento emitido na **mesma** JVM → `ED25519_VALID`. Mostrar o JSON do verify na UI.

---

## 5. Atendimentos (`service_record`)

Aluno agenda:

```
POST /me/service-records
{ "assunto", "descricao", "tipo": "AGENDAMENTO" }
```

201 `AGENDADO`. Outbox `atendimentos.created`.

Lista: `GET /me/service-records?status=PENDENTE_CIENCIA`  
Alias: `GET /service-records?aluno=me&status=PENDENTE_CIENCIA`

`_links.acknowledge` só se `PENDENTE_CIENCIA` (registro feito pela secretaria `POST /service-records`).

POST acknowledge no href do rel (método conforme `_links`; se só string, `POST {href}`).

Secretaria registra balcão → fatia 7.

---

## 6. Hub de comunicação

`GET /communications/me?page=&size=` → `content[].deliveryId`, `readAt`.

Marcar lido: seguir `_links` do item (não inventar path). Publicar é professor/admin (`communication.publish_class` / `communication.publish`) — fatia 6/7.

FAQ: `GET /faq` (seed V013). **Não** `GET /support/faq`.

Tickets (T-F8-002 aluno): `POST /support/tickets` `{ descricao }` (**não** `mensagem`). `GET /support/tickets/mine`. Staff fila/respond/close na fatia 7.

Inbox: marcar lido pelo `_links` do item (T-F1-004). Sem o POST/PATCH de read, a transação não está coberta.

---

## 7. Aceite

- [ ] Formativa PENDENTE aparece na lista aluno
- [ ] Sem janela: sessão sem rel confirmar; com janela + PIN: 200 e `entryConfirmedAt`
- [ ] Check-in evento AGENDADO: 409 (regra)
- [ ] Certificado próprio download 200; id alheio 403
- [ ] Verify público abre pelo hash
- [ ] FAQ 200 com seed
- [ ] Inbox aluno 200; marcar lido altera `readAt`
- [ ] POST ticket + GET mine
- [ ] Os 4 `attendanceMode` confirmam (entry; DUAL também exit)

## 8. Não fazer

- Geofence, BLE, trust score.
- Scanner de câmera.
- Dashboard duplicando KPIs de horas (já vêm no BFF).
- Tela CAAF aqui (fatia 6).
