package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.contract.persistence.ClassFilter
import tech.testsys.domain.contract.persistence.CompetitionFilter
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.web.app.service.manager.ManagerService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.PageRequest
import tech.testsys.web.components.data.TableHandle
import tech.testsys.web.components.data.TableScope
import tech.testsys.web.components.data.filters
import tech.testsys.web.components.data.table
import tech.testsys.web.components.forms.DateRange
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.dateRangeInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.BlockScope
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.components.texts.UiTexts
import java.time.Instant

/**
 * Cabinet of a Manager (testsys.web.page.manager): the tabs «Классы» and «Соревнования» are the sections of the route; without
 * a section the page opens «Классы».
 *
 * @since %CURRENT_VERSION%
 */
@Route("manager/:section?(classes|competitions)")
@PageTitle("Кабинет Организатора")
@RolesAllowed("MULTIPLE_ROLE")
class ManagerView(texts: UiTexts, private val headers: CabinetHeaders, private val managerService: ManagerService) :
    TestSysView(texts),
    BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        val section = event.routeParameters.get(MANAGER_SECTION_PARAMETER).orElse(null)
        if (section == null) {
            event.forwardTo(ManagerView::class.java, managerSection(CLASSES_SECTION))
            return
        }

        // Checks the manager role before the tables load, so that its absence opens the forbidden screen.
        managerService.viewClasses(Pagination(page = 0, size = 1))
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head("Кабинет Организатора") {
                crumb("Главная", MultiMainView::class.java)
                tabs(matchRouteParameters = true) {
                    tab("Классы", ManagerView::class.java, managerSection(CLASSES_SECTION))
                    tab("Соревнования", ManagerView::class.java, managerSection(COMPETITIONS_SECTION))
                }
            }
            row {
                if (section == COMPETITIONS_SECTION) {
                    block(title = "Соревнования") {
                        groupsTable(
                            fetch = { request, (name, from, to) ->
                                val filter = CompetitionFilter(name = name, createdFrom = from, createdTo = to)
                                val competitions = managerService.viewCompetitions(request.toPagination(), filter)
                                Page(competitions.content, competitions.totalElements.toInt())
                            },
                            columns = {
                                codeColumn("ID", sortKey = "id", size = 6) { competition -> competition.id.value.toString() }
                                textColumn("Название", sortKey = "name", size = 12) { competition -> competition.name }
                                numberColumn("Участников") { competition -> competition.participants.size }
                                empty("Соревнований пока нет", "Создайте соревнование и добавьте в него участников.")
                                onRowClick(isNavigation = true) { competition ->
                                    UI.getCurrent().navigate(ManagerCompetitionView::class.java, competitionParameters(competition.id))
                                }
                            },
                            key = { competition -> competition.id },
                            creation = Creation("Создать соревнование", "Новое соревнование") { name ->
                                managerService.createCompetition(name)
                            },
                        )
                    }
                } else {
                    block(title = "Классы") {
                        groupsTable(
                            fetch = { request, (name, from, to) ->
                                val filter = ClassFilter(name = name, createdFrom = from, createdTo = to)
                                val classes = managerService.viewClasses(request.toPagination(), filter)
                                Page(classes.content, classes.totalElements.toInt())
                            },
                            columns = {
                                codeColumn("ID", sortKey = "id", size = 6) { studyClass -> studyClass.id.value.toString() }
                                textColumn("Название", sortKey = "name", size = 12) { studyClass -> studyClass.name }
                                numberColumn("Учеников") { studyClass -> studyClass.students.size }
                                empty("Классов пока нет", "Создайте класс и раздайте ученикам код-приглашение.")
                                onRowClick(isNavigation = true) { studyClass ->
                                    UI.getCurrent().navigate(ManagerClassView::class.java, classParameters(studyClass.id))
                                }
                            },
                            key = { studyClass -> studyClass.id },
                            creation = Creation("Создать класс", "Новый класс") { name -> managerService.createClass(name) },
                        )
                    }
                }
            }
        }
    }

    /**
     * Fills the block with a table of classes or competitions fetched with the applied [GroupsFilter], the filters by name and
     * creation date and the action of [creation].
     */
    private fun <T> BlockScope.groupsTable(
        fetch: (PageRequest, GroupsFilter) -> Page<T>,
        columns: TableScope<T>.() -> Unit,
        key: (T) -> Any,
        creation: Creation,
    ) {
        var applied = GroupsFilter()
        val rows = table(key = key, fetch = { request -> fetch(request, applied) }, content = columns)

        lateinit var name: ValueInput<String>
        lateinit var dates: ValueInput<DateRange>
        filters(
            onApply = {
                val (from, to) = dates.value
                val isValid = !dates.isInvalid && (from == null || to == null || from <= to)
                if (isValid) {
                    applied = GroupsFilter(name = name.value.ifEmpty { null }, from = from?.let(::startOf), to = to?.let(::endOf))
                }
                isValid
            },
            onReset = {
                name.value = ""
                dates.value = DateRange()
                applied = GroupsFilter()
            },
            onRefresh = { rows.refresh(toFirstPage = true) },
        ) {
            row {
                name = textInput("Название", labelSize = 4, size = 8, hint = "Часть названия, без учёта регистра")
                dates = dateRangeInput("Дата создания", labelSize = 4, size = 8)
            }
        }

        creationAction(creation, rows)
    }

    /** Adds the action of [creation] opening its dialog, which creates a named group and refreshes [rows]. */
    private fun BlockScope.creationAction(creation: Creation, rows: TableHandle<*>) {
        val draft = Binder<NameDraft>()
        val form = dialog(title = creation.title) {
            row {
                textInput("Название", labelSize = 6, size = 18) {
                    draft.forField(this)
                        .nameRules("Укажите название")
                        .bind({ values -> values.name }, { values, name -> values.name = name })
                }
            }
            footer { dialog ->
                action("Отменить") { onClick { dialog.close() } }
                mainAction("Создать") {
                    onClick {
                        val values = NameDraft()
                        if (draft.writeBeanIfValid(values)) {
                            creation.create(values.name)
                            dialog.close()
                            rows.refresh()
                        }
                    }
                }
            }
        }
        actions {
            action(creation.label) {
                onClick {
                    draft.readBean(NameDraft())
                    form.open()
                }
            }
        }
    }

    /** Applied filters of a table of classes or competitions: part of the name and the inclusive creation bounds. */
    private data class GroupsFilter(val name: String? = null, val from: Instant? = null, val to: Instant? = null)

    /** Creation of a class or competition: the [label] of the action, the [title] of its dialog and the [create] call. */
    private class Creation(val label: String, val title: String, val create: (String) -> Unit)

    /** Values of the creation form. */
    private class NameDraft(var name: String = "")
}
