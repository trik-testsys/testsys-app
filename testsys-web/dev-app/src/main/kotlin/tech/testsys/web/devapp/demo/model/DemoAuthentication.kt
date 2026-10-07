package tech.testsys.web.devapp.demo.model

internal fun DemoState.login(code: String): DemoResult {
    val user = users.firstOrNull {
        it.accessCode == code.trim()
    } ?: return fail("Код-доступа невалиден")
    return copy(sessionUserId = user.id).ok("Вход выполнен: ${user.alias}")
}

internal fun DemoState.register(alias: String, email: String, role: DemoRole?): DemoResult {
    val normalized = email.trim().lowercase()
    if (alias.isBlank() || !EMAIL.matches(normalized) || role == null || role !in listOf(DemoRole.Student, DemoRole.Organizer)) {
        return fail("Укажите псевдоним, корректную почту и роль")
    }
    if (
        users.any {
            it.email?.lowercase() == normalized
        }
    ) {
        return fail("Почта уже привязана к кабинету")
    }
    return copy(
        pendingRegistration = DemoRegistration(alias = alias.trim(), email = normalized, role = role),
    ).ok("Демонстрационное письмо: код 246810")
}

internal fun DemoState.confirmRegistration(code: String): DemoResult {
    val pending = pendingRegistration ?: return fail("Код подтверждения указан неверно")
    if (code.trim() != "246810") {
        return fail("Код подтверждения указан неверно")
    }
    if (
        users.any {
            it.email == pending.email
        }
    ) {
        return fail("Почта уже привязана к кабинету")
    }
    val user = DemoUser(
        id = "user$nextId",
        alias = pending.alias,
        role = pending.role,
        accessCode = "ACCESS-$nextId",
        communityIds = listOf("public"),
        email = pending.email,
    )
    return copy(
        users = users + user,
        sessionUserId = user.id,
        nextId = nextId + 1,
        pendingRegistration = null,
    )
        .ok("Регистрация завершена. Код-доступа: ${user.accessCode}")
}

internal fun DemoState.requestRecovery(email: String): DemoResult {
    val user = users.firstOrNull {
        it.email?.lowercase() == email.trim().lowercase()
    } ?: return fail("Кабинет с такой почтой не найден")
    return copy(recovery = DemoRecovery(userId = user.id, token = "restore-$nextId")).ok("Демонстрационная ссылка: restore-$nextId")
}

internal fun DemoState.restoreAccess(token: String): DemoResult {
    val current = recovery ?: return fail("Ссылка восстановления недействительна")
    if (current.isCompleted || current.token != token) {
        return fail("Ссылка восстановления недействительна")
    }
    val code = "RESTORED-$nextId"
    return copy(
        users = users.map {
            if (it.id == current.userId) {
                it.copy(accessCode = code)
            } else {
                it
            }
        },
        recovery = current.copy(isCompleted = true),
        nextId = nextId + 1,
    ).ok("Новый код-доступа: $code. Старый код невалиден")
}

private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
