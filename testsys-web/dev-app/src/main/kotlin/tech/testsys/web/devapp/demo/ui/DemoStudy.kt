package tech.testsys.web.devapp.demo.ui

import com.vaadin.flow.signals.local.ValueSignal
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.display.text
import tech.testsys.web.components.forms.UploadLimits
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.fileDrop
import tech.testsys.web.components.forms.select
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.devapp.demo.model.DemoObjects
import tech.testsys.web.devapp.demo.model.DemoRole
import tech.testsys.web.devapp.demo.model.DemoRow
import tech.testsys.web.devapp.demo.model.DemoSolution
import tech.testsys.web.devapp.demo.model.DemoSolutionKind
import tech.testsys.web.devapp.demo.model.DemoTask
import tech.testsys.web.devapp.demo.model.DemoUser
import tech.testsys.web.devapp.demo.model.actor
import tech.testsys.web.devapp.demo.model.bestScore
import tech.testsys.web.devapp.demo.model.objects
import tech.testsys.web.devapp.demo.model.submitSolution

private const val DEMO_FILE_BYTES = 1_048_576

/** Accessible class, tour and task choice and the existing mock submission workflow. */
internal fun PageScope.demoStudy(context: DemoContext, actor: DemoUser, objects: DemoObjects, section: String) {
    if (objects.classes.isNotEmpty()) {
        row {
            block(title = "Классы") {
                demoTable(
                    context.table("${actor.id}:classes"),
                    objects.classes.map { item ->
                        DemoRow(id = item.id, title = item.name)
                    },
                ) { selected ->
                    context.select(key = "${actor.id}:class", id = selected.id)
                }
            }
        }
    }
    val selectedClass = objects.classes.firstOrNull {
        it.id == context.selection(key = "${actor.id}:class", fallback = objects.classes.firstOrNull()?.id)
    }
    val tours = objects.tours.filter {
        selectedClass == null || it.id in selectedClass.tourIds
    }
    row {
        block(title = "Доступные туры") {
            demoTable(
                context.table("${actor.id}:${selectedClass?.id}:tours"),
                tours.map { tour ->
                    tour.row()
                },
            ) { selected ->
                context.select(key = "${actor.id}:study-tour", id = selected.id)
            }
        }
    }
    val tour = tours.firstOrNull {
        it.id == context.selection(key = "${actor.id}:study-tour", fallback = tours.firstOrNull()?.id)
    } ?: return
    demoTourMaterials(context, tour)
    val tasks = objects.tasks.filter {
        it.id in tour.taskIds
    }
    val taskKey = "${actor.id}:${tour.id}:task"
    row {
        block(title = "Выбор задачи") {
            row {
                select("Задача", items = tasks, itemLabel = DemoTask::name, labelSize = 4, size = 20) {
                    value = tasks.firstOrNull { task ->
                        task.id == context.selection(key = taskKey, fallback = tasks.firstOrNull()?.id)
                    }
                    addValueChangeListener { event ->
                        event.value?.let { task ->
                            context.select(key = taskKey, id = task.id)
                        }
                    }
                }
            }
        }
    }
    val task = tasks.firstOrNull {
        it.id == context.selection(key = taskKey, fallback = tasks.firstOrNull()?.id)
    } ?: return
    row {
        block(title = task.name) {
            row {
                text(task.description)
            }
            row {
                val score = bestScore(solutions = context.state.solutions, userId = actor.id, taskId = task.id)
                text("Лучший балл: ${score?.toString() ?: "Нет проверенных решений"}")
            }
            actions {
                demoDownload(filename = "${task.id}-statement.txt", content = "${task.name}\n${task.description}\nДемонстрационное условие")
                demoDownload(filename = "${task.id}-exercise-demo.qrs", content = "TestSys demo exercise: ${task.name}")
            }
            if (section != "solutions" || actor.role == DemoRole.Participant) {
                lateinit var kind: ValueInput<DemoSolutionKind?>
                val filename = ValueSignal("")
                row {
                    kind = select(
                        "Вид решения",
                        items = task.authorSolutionKinds,
                        itemLabel = ::demoKindLabel,
                        labelSize = 4,
                        size = 20,
                    ) {
                        value = task.authorSolutionKinds.first()
                    }
                }
                row {
                    fileDrop(
                        "Файл решения (демонстрация)",
                        UploadLimits(maxFiles = 1, maxFileBytes = DEMO_FILE_BYTES, maxMemoryBytes = DEMO_FILE_BYTES.toLong()),
                        consume = { file ->
                            filename.set(file.filename)
                        },
                    )
                }
                footer {
                    mainAction("Отправить решение") {
                        onClick {
                            if (
                                context.apply(
                                    context.state.submitSolution(
                                        userId = actor.id,
                                        taskId = task.id,
                                        kind = kind.value,
                                        fileName = filename.peek(),
                                    ),
                                )
                            ) {
                                val id = context.state.solutions.last().id
                                context.session.check(id, context.render)
                            }
                            context.render()
                        }
                    }
                }
            } else {
                footer {
                    action("Открыть задачу и отправку решения") {
                        onClick {
                            context.navigate("student.study")
                        }
                    }
                }
            }
        }
    }
    demoSolutionTable(
        context,
        actor,
        context.state.solutions.filter {
            it.userId == actor.id && it.taskId == task.id
        },
    )
}

/** Existing history and statuses; filtering cannot broaden the supplied role-scoped rows. */
internal fun PageScope.demoSolutionTable(
    context: DemoContext,
    actor: DemoUser,
    solutions: List<DemoSolution>,
    onSelect: (DemoRow) -> Unit = {},
) {
    row {
        block(
            title = if (actor.role == DemoRole.Judge) {
                "Решения учеников и участников"
            } else {
                "Мои решения"
            },
        ) {
            demoTable(
                context.table("${actor.id}:${context.screen}:solutions"),
                solutions.map { solution ->
                    DemoRow(
                        id = solution.id,
                        title = solution.fileName,
                        category = demoStatusLabel(solution.status),
                        date = demoDate(solution.submittedAt),
                        detail = "${solution.taskId} · ${solution.score?.toString() ?: "—"}",
                    )
                },
                onSelect = onSelect,
            )
        }
    }
}
