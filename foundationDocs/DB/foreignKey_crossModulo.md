---
## FKs cross-módulo — mapa completo

**As-built (2026-08-29):** FKs **live** = Flyway V001–V019. Fonte: `foundationDocs/analysis/as-built-backend.md` §5.  
`request_line_item` **não existe** no Flyway — a FK `id_disciplina` abaixo **não é live**.  
`request_attachment.uploaded_by` **não existe** no V004.

### Live (Flyway) — origem 2026-06 ainda válida

| Coluna origem | Módulo origem | → Tabela destino | Módulo destino |
|---|---|---|---|
| `usuario.id_curso` | M1 IAM | `curso.id` | M2 Acadêmico |
| `curso.id_coordenador` | M2 Acadêmico | `usuario.id` | M1 IAM (FK tardia — I7) |
| `calendario_academico.id_request_type` | M2 Acadêmico | `request_type.id` | M3 Solicitações (I8) |
| `request.id_solicitante` | M3 Solicitações | `usuario.id` | M1 IAM |
| `request.id_curso` | M3 Solicitações | `curso.id` | M2 Acadêmico |
| `request_event.id_ator` | M3 Solicitações | `usuario.id` | M1 IAM |
| `formative_activity.id_curso` | M4 Formativas | `curso.id` | M2 Acadêmico |
| `formative_entry.id_aluno` | M4 Formativas | `usuario.id` | M1 IAM |
| `formative_entry.reviewed_by` | M4 Formativas | `usuario.id` | M1 IAM |
| `internship.id_aluno` | M5 Estágio | `usuario.id` | M1 IAM |
| `internship.id_orientador` | M5 Estágio | `usuario.id` | M1 IAM |
| `internship.id_coe` | M5 Estágio | `usuario.id` | M1 IAM |
| `tcc.id_curso` | M6 TCC | `curso.id` | M2 Acadêmico |
| `tcc_member.id_aluno` | M6 TCC | `usuario.id` | M1 IAM |
| `tcc_examiner.id_professor` | M6 TCC | `usuario.id` | M1 IAM |
| `communication.id_curso_alvo` | M7 Comunicação | `curso.id` | M2 Acadêmico |
| `communication.id_autor` | M7 Comunicação | `usuario.id` | M1 IAM |
| `communication_delivery.id_destinatario` | M7 Comunicação | `usuario.id` | M1 IAM |
| `notification_preference.id_usuario` | M7 Comunicação | `usuario.id` | M1 IAM |
| `outbox_event.aggregate_id` | M7 Outbox | — | polimórfico (sem FK) |
| `event_attendance.id_curso` | M8 Presença | `curso.id` | M2 Acadêmico |
| `event_attendance.organizador` | M8 Presença | `usuario.id` | M1 IAM |
| `attendance_session.id_aluno` | M8 Presença | `usuario.id` | M1 IAM |
| `certificate.id_beneficiario` | M9 Certificados | `usuario.id` | M1 IAM |
| `certificate.id_evento` | M9 Certificados | `event_attendance.id` | M8 Presença |
| `certificate.id_formativa` | M9 Certificados | `formative_entry.id` | M4 Formativas |
| `audit_log.id_ator` | M9 Auditoria | `usuario.id` | M1 IAM (nullable) |

### Live (Flyway) — mesmo módulo (V019)

| Coluna origem | Módulo | → Tabela destino | Notas |
|---|---|---|---|
| `request.id_request_type_version` | M3 Solicitações | `request_type_version.id` | mesmo módulo; nullable |
| `request_type_version.id_request_type` | M3 Solicitações | `request_type.id` | UNIQUE (`id_request_type`, `version`) |

### Não live (trilha 2026-06 apenas)

| Coluna origem | Motivo |
|---|---|
| `request_line_item.id_disciplina` → `disciplina.id` | Tabela **não migrada**. Linhas em `request.dados` JSONB. |
| `request_attachment.uploaded_by` → `usuario.id` | Coluna **ausente** no V004. |

### Extras as-built (V012, V014, V015) — FKs para IAM/Acadêmico

| Coluna origem | → Destino |
|---|---|
| `service_record.id_secretario` / `id_aluno` | `usuario.id` (`id_secretario` nullable após V015) |
| `support_ticket.id_usuario` / `id_atendente` | `usuario.id` |
| `device_fcm_token.id_usuario` | `usuario.id` |
| `graduation_record.id_aluno` / `delivered_by` | `usuario.id` |
| `graduation_record.id_curso` | `curso.id` |
| `graduation_record.id_periodo` | `periodo_letivo.id` (V015) |
| `secretary_task.id_assignee` | `usuario.id` |
| `import_job.id_ator` / `export_job.id_ator` | `usuario.id` |
| `communication_template_revision.id_autor` | `usuario.id` |
| `historico_escolar.id_aluno` | `usuario.id` |
| `historico_escolar.id_disciplina` | `disciplina.id` |
| `certificate.id_activity` | `formative_activity.id` (V015) |

**Padrão dominante:** M1 (`usuario`) e M2 (`curso`) são os dois hubs. Overlay as-built não inventa tabelas além do Flyway.

Pronto para Etapa 3 (merge → `modelo-logico.dbml`) — M3 overlay já alinhado a V004+V019.
