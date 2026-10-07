package tech.testsys.web.devapp.demo.ui

import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.select
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.devapp.demo.model.DemoRole
import tech.testsys.web.devapp.demo.model.confirmRegistration
import tech.testsys.web.devapp.demo.model.login
import tech.testsys.web.devapp.demo.model.register
import tech.testsys.web.devapp.demo.model.requestRecovery
import tech.testsys.web.devapp.demo.model.restoreAccess

/** Mock access, email confirmation and recovery use only the UI-owned model. */
internal fun PageScope.demoLogin(context: DemoContext) {
    row {
        block(title = "Вход по коду-доступа", subtitle = "Примеры: STUDENT-2026 · ORG-2026 · PART-101") {
            lateinit var code: ValueInput<String>
            row {
                code = textInput("Код-доступа", labelSize = 4, size = 20)
            }

            footer {
                mainAction("Войти") {
                    onClick {
                        if (context.apply(context.state.login(code.value))) {
                            val role = context.state.users.first { user ->
                                user.id == context.state.sessionUserId
                            }.role
                            context.navigate("${DEMO_CABINETS.first { cabinet -> cabinet.role == role }.key}.overview")
                        } else {
                            context.render()
                        }
                    }
                }
            }
        }
    }

    row {
        block(title = "Регистрация") {
            lateinit var alias: ValueInput<String>
            lateinit var email: ValueInput<String>
            lateinit var role: ValueInput<DemoRole?>
            row {
                alias = textInput("Псевдоним", labelSize = 4, size = 8)
                role = select(
                    "Роль",
                    items = listOf(DemoRole.Student, DemoRole.Organizer),
                    itemLabel = ::demoRoleLabel,
                    labelSize = 4,
                    size = 8,
                ) {
                    value = DemoRole.Student
                }
            }

            row {
                email = textInput("Почта", labelSize = 4, size = 20)
            }

            footer {
                mainAction("Зарегистрироваться") {
                    onClick {
                        context.apply(context.state.register(alias = alias.value, email = email.value, role = role.value))
                        context.render()
                    }
                }
            }
        }
    }

    if (context.state.pendingRegistration != null) {
        row {
            block(title = "Демонстрационное письмо", subtitle = "Код подтверждения: 246810") {
                lateinit var confirmation: ValueInput<String>
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
    }

    row {
        block(title = "Восстановление доступа") {
            lateinit var email: ValueInput<String>
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
}
