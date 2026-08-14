# T-F6-001 — Configurar Parâmetros do Curso

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F6 — Coordenação/US-F6-001-CONFIGURAR-CURSO.md`](../../foundationDocs/sequenceDiagrams/F6 — Coordenação/US-F6-001-CONFIGURAR-CURSO.md)  
> **Status:** ✅ `GET/PATCH /courses/{id}/config` + CRUD de cursos/disciplinas/períodos + histórico escolar  
> **Capability:** `course.config` (coordenador dono) ou `system.admin`

---

## Configuração do curso

`{id}` aceita **UUID** ou **sigla** (`TADS`).

```
GET /courses/tads/config
Authorization: Bearer …  (course.config)
```

Ownership: `curso.id_coordenador == currentUserId()` (admin bypass). Senão **403**.

```json
{
  "courseId": "uuid",
  "sigla": "TADS",
  "horasFormativasMinimas": 120,
  "duracaoCalendario": "15_SEMANAS",
  "bancaMembrosExternos": 1,
  "bancaModalidade": "PRESENCIAL",
  "regimento": null,
  "_links": { "self": "/courses/{id}/config", "update": "/courses/{id}/config" }
}
```

```
PATCH /courses/{id}/config
{ "horasFormativasMinimas": 150 }
```

Validação: horas `[0,1000]`, duração `15_SEMANAS|18_SEMANAS`, banca `1|2`, modalidade `PRESENCIAL|REMOTO|HÍBRIDO`. Audit `COURSE_CONFIG_UPDATED` com diff de/para. **Não recalcula** colações já confirmadas.

Colunas em `curso` (V015). Controller: [`CourseConfigController.kt`](../../backend/modules/academico/src/main/kotlin/br/ufpr/sept/so2/modules/academico/api/CourseConfigController.kt).

CRUD legado permanece em [`CoordenacaoController`](../../backend/modules/academico/src/main/kotlin/br/ufpr/sept/so2/modules/academico/api/CoordenacaoController.kt) (`/academico/cursos`, disciplinas, períodos).

---

## Histórico escolar (critério de colação)

```
GET /academico/alunos/{alunoId}/historico
PUT /academico/alunos/{alunoId}/historico/{disciplinaId}
{ "estado": "CONCLUIDA" }   // CURSANDO | CONCLUIDA | REPROVADA
```

Tabela `historico_escolar` (V015). Usado pelos 5 critérios de colação (T-F5-005).

---

## Checklist

- [x] `GET/PATCH /courses/:id/config` + ownership 403
- [x] Audit diff; não-retroatividade
- [x] Histórico escolar upsert
