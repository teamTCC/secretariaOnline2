package br.ufpr.sept.so2.modules.iam.domain.exceptions

class InvalidCredentialsException : RuntimeException("Credenciais inválidas. Verifique seus dados e tente novamente.")

class InvalidTokenException(
    message: String = "Token inválido ou expirado",
) : RuntimeException(message)

class UserNotFoundException(
    identifier: String,
) : NoSuchElementException("Usuário não encontrado: $identifier")

class AccountBlockedException(
    minutesRemaining: Long,
) : RuntimeException(
        "Conta bloqueada temporariamente. Tente novamente em $minutesRemaining minuto(s).",
    )

class WeakPasswordException(
    reason: String,
) : IllegalArgumentException("Senha não atende os requisitos: $reason")

class PasswordReuseException : IllegalArgumentException("A nova senha não pode ser igual a uma senha recentemente utilizada.")

class UserAlreadyExistsException(
    identifier: String,
) : IllegalStateException("Usuário já cadastrado: $identifier")
