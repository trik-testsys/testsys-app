package tech.testsys.web.devapp.demo.ui

import com.vaadin.flow.data.binder.Binder
import tech.testsys.web.components.actions.DownloadContent
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.downloadAction
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.forms.integerInput
import tech.testsys.web.components.forms.select
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.devapp.demo.model.DEMO_PARTICIPANT_LIMIT
import tech.testsys.web.devapp.demo.model.DemoRow
import tech.testsys.web.devapp.demo.model.DemoTour
import tech.testsys.web.devapp.demo.model.DemoUser
import tech.testsys.web.devapp.demo.model.actor
import tech.testsys.web.devapp.demo.model.addTour
import tech.testsys.web.devapp.demo.model.availableTours
import tech.testsys.web.devapp.demo.model.createCompetition
import tech.testsys.web.devapp.demo.model.createParticipants
import tech.testsys.web.devapp.demo.model.objects
import tech.testsys.web.devapp.demo.model.resultsCsv
import java.io.ByteArrayInputStream

/** Writable draft of a creation dialog; every open starts from its defaults. */
private class DemoCreation(var name: String = "", var count: Int? = 3, var tourId: String? = null)

/** Organizer list actions open forms in the corresponding block header. */
internal fun PageScope.demoOrganizer(context: DemoContext, actor: DemoUser, section: String) {
    val competitions = context.state.objects(actor).competitions
    val selectionKey = "${actor.id}:competition"
    val selectedId = context.selection(key = selectionKey, fallback = competitions.firstOrNull()?.id)
    val competition = competitions.firstOrNull {
        it.id == selectedId
    }
    val createBinder = Binder<DemoCreation>()
    val create = dialog("Создать соревнование") {
        val binder = createBinder
        row {
            textInput("Название", labelSize = 8, size = 16) {
                binder.forField(this).asRequired("Введите название соревнования").bind(
                    { draft ->
                        draft.name
                    },
                    { target, value ->
                        target.name = value
                    },
                )
            }
        }
        footer { handle ->
            action("Отменить") {
                onClick {
                    handle.close()
                }
            }
            mainAction("Создать") {
                onClick {
                    val draft = DemoCreation()
                    if (binder.writeBeanIfValid(draft)) {
                        val id = "competition${context.state.nextId}"
                        if (context.created(result = context.state.createCompetition(draft.name), selectionKey = selectionKey, id = id)) {
                            handle.close()
                        }
                    }
                }
            }
        }
    }
    if (section in listOf("overview", "competitions")) {
        block("Соревнования") {
            actions {
                mainAction("Создать соревнование") {
                    onClick {
                        createBinder.readBean(DemoCreation())
                        create.open()
                    }
                }
            }
            demoTable(
                context.table("${actor.id}:competitions"),
                competitions.map { item ->
                    DemoRow(id = item.id, title = item.name)
                },
            ) { selected ->
                context.select(key = selectionKey, id = selected.id)
            }
        }
    }
    if (competition == null) {
        return
    }
    block("Выбранное соревнование") {
        row {
            textInput("Название", labelSize = 4, size = 20) {
                value = competition.name
                isEditable = false
            }
        }
    }
    if (section in listOf("overview", "participants")) {
        val participants = context.state.users.filter {
            it.id in competition.participantIds
        }
        val participantsBinder = Binder<DemoCreation>()
        val createParticipants = dialog(
            "Создать участников",
            subtitle = "До 100 за один вызов — только предел демонстрации",
        ) {
            val binder = participantsBinder
            row {
                integerInput("Количество", labelSize = 8, size = 16, min = 1, max = DEMO_PARTICIPANT_LIMIT) {
                    value = 3
                    binder.forField(this).asRequired("Укажите количество")
                        .withValidator(
                            { count ->
                                count != null && count in 1..DEMO_PARTICIPANT_LIMIT
                            },
                            "Укажите целое количество от 1 до 100",
                        )
                        .bind(
                            { draft ->
                                draft.count
                            },
                            { target, value ->
                                target.count = value
                            },
                        )
                }
            }
            footer { handle ->
                action("Отменить") {
                    onClick {
                        handle.close()
                    }
                }
                mainAction("Создать") {
                    onClick {
                        val draft = DemoCreation()
                        if (
                            binder.writeBeanIfValid(draft) && context.created(
                                result = context.state.createParticipants(
                                    competition.id,
                                    checkNotNull(draft.count) { "Validated participant count of ${competition.id} is missing" },
                                ),
                                selectionKey = selectionKey,
                                id = competition.id,
                            )
                        ) {
                            handle.close()
                        }
                    }
                }
            }
        }
        block("Участники") {
            actions {
                mainAction("Создать участников") {
                    onClick {
                        participantsBinder.readBean(DemoCreation())
                        createParticipants.open()
                    }
                }
            }
            demoTable(
                context.table("${actor.id}:${competition.id}:participants"),
                participants.map { participant ->
                    DemoRow(id = participant.id, title = participant.alias, detail = participant.accessCode)
                },
            )
        }
    }
    if (section in listOf("overview", "tours", "results")) {
        val available = context.state.availableTours(actor).filter {
            it.id !in competition.tourIds
        }
        val tourBinder = Binder<DemoCreation>()
        val add = dialog("Добавить доступный тур") {
            val binder = tourBinder
            row {
                select("Тур", items = available, itemLabel = DemoTour::name, labelSize = 8, size = 16) {
                    binder.forField(this).asRequired("Выберите тур").bind(
                        { draft ->
                            available.firstOrNull { tour ->
                                tour.id == draft.tourId
                            }
                        },
                        { target, value ->
                            target.tourId = value?.id
                        },
                    )
                }
            }
            footer { handle ->
                action("Отменить") {
                    onClick {
                        handle.close()
                    }
                }
                mainAction("Добавить") {
                    onClick {
                        val draft = DemoCreation()
                        if (
                            binder.writeBeanIfValid(draft) && context.created(
                                result = context.state.addTour(
                                    competitionId = competition.id,
                                    tourId = checkNotNull(draft.tourId) { "Validated tour of ${competition.id} is missing" },
                                ),
                                selectionKey = "${actor.id}:tour",
                                id = checkNotNull(draft.tourId) { "Validated tour of ${competition.id} is missing" },
                            )
                        ) {
                            handle.close()
                        }
                    }
                }
            }
        }
        val tours = context.state.tours.filter {
            it.id in competition.tourIds
        }
        val tourKey = "${actor.id}:tour"
        block("Туры") {
            actions {
                mainAction("Добавить доступный тур") {
                    onClick {
                        tourBinder.readBean(DemoCreation())
                        add.open()
                    }
                }
            }
            demoTable(
                context.table("${actor.id}:${competition.id}:tours"),
                tours.map { tour ->
                    tour.row()
                },
            ) { selected ->
                context.select(key = tourKey, id = selected.id)
            }
        }
        val tour = tours.firstOrNull {
            it.id == context.selection(key = tourKey, fallback = tours.firstOrNull()?.id)
        }
        if (tour != null) {
            demoTourMaterials(context, tour)
            block("Результаты", subtitle = "CSV содержит полную матрицу выбранного тура") {
                actions {
                    downloadAction(
                        "Скачать CSV",
                        produce = {
                            val bytes = context.state.resultsCsv(competitionId = competition.id, tourId = tour.id)
                                .toByteArray(Charsets.UTF_8)
                            DownloadContent(
                                filename = "results.csv",
                                contentType = "text/csv;charset=utf-8",
                                length = bytes.size.toLong(),
                            ) {
                                ByteArrayInputStream(bytes)
                            }
                        },
                    )
                }
                val participants = competition.participantIds.map { id -> context.state.users.first { user -> user.id == id } }
                demoResultMatrix(context, actor, tour, participants)
            }
        }
    }
}
