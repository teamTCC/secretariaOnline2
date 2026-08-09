package br.ufpr.sept.so2.modules.iam.domain

import java.util.UUID

data class Role(
    val id: UUID,
    val code: String,
    val descricao: String,
    val authorities: Set<Authority>,
)

data class Authority(
    val id: UUID,
    val code: String,
    val descricao: String,
)

object AuthorityCodes {
    // IAM
    const val AUTH_FIRST_ACCESS = "auth.first_access"
    const val USER_UPDATE_OWN_PROFILE = "user.update_own_profile"
    const val USER_UPDATE_OWN_PASSWORD = "user.update_own_password"
    const val USER_MANAGE_STUDENTS = "user.manage_students"
    const val USER_MANAGE_ALL = "user.manage_all"
    const val USER_RESET_PASSWORD = "user.reset_password"
    const val IAM_MANAGE_ROLES = "iam.manage_roles"

    // Dashboard
    const val DASHBOARD_VIEW_OWN = "dashboard.view_own"
    const val DASHBOARD_VIEW_PROFESSOR = "dashboard.view_self_professor"
    const val DASHBOARD_VIEW_SECRETARY = "dashboard.view_secretary"

    // Requests
    const val REQUEST_OPEN = "request.open"
    const val REQUEST_VIEW_OWN = "request.view_own"
    const val REQUEST_INTERNAL_OPEN = "request.internal_open"
    const val REQUEST_DELIBERATE = "request.deliberate"
    const val REQUEST_VIEW_CURSO = "request.view_curso"
    const val REQUEST_REOPEN = "request.reopen"

    // Events
    const val EVENT_MANAGE = "event.manage"
    const val EVENT_HOST = "event.host"
    const val ATTENDANCE_VIEW_OPEN = "attendance.view_open"
    const val ATTENDANCE_CHECK_IN = "attendance.check_in"

    // Formativas
    const val FORMATIVE_SUBMIT = "formative.submit"
    const val FORMATIVE_VIEW_OWN = "formative.view_own"
    const val FORMATIVE_REVIEW = "formative.review"

    // Estágio
    const val INTERNSHIP_VIEW_OWN = "internship.view_own"
    const val INTERNSHIP_UPLOAD_DOC_OWN = "internship.upload_doc_own"
    const val INTERNSHIP_REVIEW = "internship.review"
    const val INTERNSHIP_SUPERVISE = "internship.supervise"

    // TCC
    const val TCC_VIEW_OWN = "tcc.view_own"
    const val TCC_UPLOAD_FINAL = "tcc.upload_final"
    const val TCC_SUPERVISE = "tcc.supervise"
    const val TCC_EXAMINE = "tcc.examine"

    // Comunicação
    const val COMMUNICATION_READ = "communication.read"
    const val COMMUNICATION_PUBLISH_CLASS = "communication.publish_class"
    const val COMMUNICATION_PUBLISH = "communication.publish"

    // Certificados
    const val CERTIFICATE_VIEW_OWN = "certificate.view_own"

    // Sistema
    const val SYSTEM_ADMIN = "system.admin"
    const val SYSTEM_OBSERVE = "system.observe"
    const val AUDIT_READ = "audit.read"
}

object RoleCodes {
    const val ALUNO = "ALUNO"
    const val EGRESSO = "EGRESSO"
    const val PROFESSOR = "PROFESSOR"
    const val COORDENADOR = "COORDENADOR"
    const val SECRETARIO = "SECRETARIO"
    const val CAAF = "CAAF"
    const val COE = "COE"
    const val ADMIN = "ADMIN"
}
