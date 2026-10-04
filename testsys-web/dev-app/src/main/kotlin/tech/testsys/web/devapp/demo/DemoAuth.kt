package tech.testsys.web.devapp.demo

import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.forms.select
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageScope

/** Mock access, email confirmation and recovery use only the UI-owned model. */
internal fun PageScope.demoLogin(context: DemoContext) {
    block("Вход по коду-доступа", subtitle = "Примеры: STUDENT-2026 · ORG-2026 · PART-101") {
        lateinit var code: tech.testsys.web.components.forms.ValueInput<String>
        row {
            code = textInput("Код-доступа", labelSize = 4, size = 20)
        }
        footer {
            mainAction("Войти") {
                onClick {
                    if (context.apply(context.state.login(code.value))) {
                        val role = context.state.users.first {
                            it.id == context.state.sessionUserId
                        }.role
                        context.navigate("${DEMO_CABINETS.first { it.role == role }.key}.overview")
                    } else {
                        context.render()
                    }
                }
            }
        }
    }
    block("Регистрация") {
        lateinit var alias: tech.testsys.web.components.forms.ValueInput<String>
        lateinit var email: tech.testsys.web.components.forms.ValueInput<String>
        lateinit var role: tech.testsys.web.components.forms.ValueInput<String?>
        row {
            alias = textInput("Псевдоним", labelSize = 4, size = 8)
            role = select(
                "Роль",
                items = listOf("Ученик", "Организатор"),
                itemLabel = {
                    it
                },
                labelSize = 4,
                size = 8,
            ) {
                value = "Ученик"
            }
        }
        row {
            email = textInput("Почта", labelSize = 4, size = 20)
        }
        footer {
            mainAction("Зарегистрироваться") {
                onClick {
                    context.apply(context.state.register(alias = alias.value, email = email.value, role = role.value.orEmpty()))
                    context.render()
                }
            }
        }
    }
    if (context.state.pendingRegistration != null) {
        block("Демонстрационное письмо", subtitle = "Код подтверждения: 246810") {
            lateinit var confirmation: tech.testsys.web.components.forms.ValueInput<String>
            row {
                confirmation = textInput("Код подтверждения", labelSize = 4, size = 20)
            }
            footer {
                mainAction("Подтвердить") {
                    onClick {
                        context.apply(context.state.confirmRegistration(confirmation.value))
                        context.render()
                    }
                }
            }
        }
    }
    block("Восстановление доступа") {
        lateinit var email: tech.testsys.web.components.forms.ValueInput<String>
        row {
            email = textInput("Почта", labelSize = 4, size = 20)
        }
        footer {
            action("Подготовить письмо") {
                onClick {
                    context.apply(context.state.requestRecovery(email.value))
                    context.render()
                }
            }
            context.state.recovery?.let { recovery ->
                action("Открыть демонстрационную ссылку") {
                    onClick {
                        context.apply(context.state.restoreAccess(recovery.token))
                        context.render()
                    }
                }
            }
        }
    }
}
