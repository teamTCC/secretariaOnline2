# T-F6-001 — Configurar Parâmetros do Curso

> **Diagrama de referência:** [`foundationDocs/sequenceDiagrams/F6 — Coordenação/US-F6-001-CONFIGURAR-CURSO.md`](../../foundationDocs/sequenceDiagrams/F6%20—%20Coordenação/US-F6-001-CONFIGURAR-CURSO.md)  
> **Status:** ⏳ Não implementado — `CourseController` e use cases de configuração pendentes

---

## O que os diagramas especificam

### F6.1-D01 — `GET /courses/:id/config` (Carregar configuração)

```
GET /courses/tads/config
Authorization: Bearer eyJhbGci...  (hasAuthority('course.config'))
```

Carrega parâmetros configuráveis do curso. O use case verifica `course.coordenador_id = userId` antes de retornar (ownership check — não é só capability).

**JSON de saída (200):**

```json
{
  "courseId": "tads",
  "horasFormativasMinimas": 120,
  "duracaoCalendario": "15_SEMANAS",
  "bancaMembrosExternos": 2,
  "bancaModalidade": "HÍBRIDO",
  "regimento": "O curso de TADS segue o regimento...",
  "_links": {
    "self": "/courses/tads/config",
    "update": "/courses/tads/config"
  }
}
```

> `_links.update` (rel `course:update-config`) só presente quando o coordenador tem `course.config` **e** é o dono do curso. O frontend usa `useActions(resource)` para habilitar o formulário.

---

### F6.1-D02 — `PATCH /courses/:id/config` (Salvar configuração)

```
PATCH /courses/tads/config
Authorization: Bearer eyJhbGci...  (course.config + ownership)
Content-Type: application/json

{
  "horasFormativasMinimas": 150
}
```

PATCH semântico — apenas campos alterados são enviados. Transação atômica: `SELECT FOR UPDATE` para capturar valor anterior → `UPDATE` → `INSERT audit_log` com o diff.

**Transação:**

```sql
BEGIN;
SELECT horasFormativasMinimas FROM course_config WHERE course_id='tads' FOR UPDATE;
-- retorna: 120
UPDATE course_config SET horasFormativasMinimas=150 WHERE course_id='tads';
INSERT INTO audit_log (entidade, campo, de, para, alteradoPor, alteradoEm)
  VALUES ('course_config', 'horasFormativasMinimas', '120', '150', :userId, now());
COMMIT;
```

**JSON de saída (200):**

```json
{
  "courseId": "tads",
  "horasFormativasMinimas": 150,
  "duracaoCalendario": "15_SEMANAS",
  "bancaMembrosExternos": 2,
  "bancaModalidade": "HÍBRIDO",
  "_links": {
    "self": "/courses/tads/config",
    "update": "/courses/tads/config"
  }
}
```

**Regra de não-retroatividade:** ao alterar `horasFormativasMinimas`, o UC **não recalcula** elegibilidades existentes. Alunos já elegíveis pelo limiar anterior permanecem elegíveis. Novos cálculos usarão 150.

**Campos configuráveis:**

| Campo | Tipo | Validação |
|-------|------|-----------|
| `horasFormativasMinimas` | Int | `[0, 1000]` |
| `duracaoCalendario` | Enum | `15_SEMANAS` ou `18_SEMANAS` |
| `bancaMembrosExternos` | Int | `1` ou `2` |
| `bancaModalidade` | Enum | `PRESENCIAL`, `REMOTO`, `HÍBRIDO` |
| `regimento` | String | `max: 10000 chars` |

---

### F6.1-ERRO — 403 Ownership: coordenador acessa curso alheio

```
GET /courses/ec/config
Authorization: Bearer eyJhbGci...  (course.config ✓, mas coordenador_id ≠ userId)
```

```json
HTTP/1.1 403 Forbidden
Content-Type: application/problem+json

{
  "type": "https://secretariaonline.ufpr.br/errors/course_ownership_denied",
  "title": "Acesso negado",
  "status": 403,
  "detail": "Você não é coordenador deste curso."
}
```

> O `@PreAuthorize("hasAuthority('course.config')")` passa. O 403 é lançado pelo use case após consultar `course.coordenador_id` no banco — é uma restrição de **escopo de dado**, não de capability.

---

## DTOs esperados

```kotlin
// Request DTO
data class UpdateCourseConfigRequest(
    val horasFormativasMinimas: Int? = null,    // @Min(0) @Max(1000)
    val duracaoCalendario: DuracaoCalendario? = null,
    val bancaMembrosExternos: Int? = null,      // @Min(1) @Max(2)
    val bancaModalidade: BancaModalidade? = null,
    val regimento: String? = null               // @Size(max = 10000)
)

// Response DTO
data class CourseConfigDto(
    val courseId: String,
    val horasFormativasMinimas: Int,
    val duracaoCalendario: DuracaoCalendario,
    val bancaMembrosExternos: Int,
    val bancaModalidade: BancaModalidade,
    val regimento: String?,
    val links: Map<String, String>              // HATEOAS via Spring HATEOAS
)
```

---

## O que precisa ser implementado

| Arquivo a criar | Descrição |
|----------------|-----------|
| `modules/academico/api/CourseController.kt` | Endpoints `GET` e `PATCH /courses/:id/config` |
| `modules/academico/application/GetCourseConfigUseCase.kt` | Load config + ownership check |
| `modules/academico/application/UpdateCourseConfigUseCase.kt` | PATCH + audit_log diff + não-retroatividade |
| `modules/academico/domain/CourseConfig.kt` | Value object com validações de domínio |
| `modules/academico/infrastructure/CourseConfigEntity.kt` | JPA entity |
| Migração | `course_config(course_id, horas_formativas_minimas, duracao_calendario, ...)` |

---

## Checklist de Verificação

- [ ] `GET /courses/tads/config` → `200` com todos os parâmetros + `_links`
- [ ] `PATCH /courses/tads/config` → TX atômica: update + audit_log com diff de/para
- [ ] Ownership check: `coordenador_id ≠ userId` → `403 course_ownership_denied`
- [ ] Não-retroatividade: alterar `horasFormativasMinimas` não recalcula elegibilidades
- [ ] `_links.update` ausente se coordenador não tem `course.config`
- [ ] Validação 422 backend: `horasFormativasMinimas` fora de `[0, 1000]`
