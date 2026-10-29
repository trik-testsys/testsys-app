package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.contract.persistence.ClassFilter
import tech.testsys.domain.contract.persistence.CompetitionFilter
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.infra.localization.PageManager
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
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.DateRange
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.dateRangeInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.BlockScope
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.components.texts.currentLocalization
import java.time.Instant

/**
 * Cabinet of a Manager (testsys.web.page.manager): the tabs with the classes and the competitions are the sections of the
 * route; without a section the page opens the classes.
 *
 * @since %CURRENT_VERSION%
 */
@Route("manager/:section?(classes|competitions)")
@RolesAllowed("MULTIPLE_ROLE")
class ManagerView(private val headers: CabinetHeaders, private val managerService: ManagerService) :
    TestSysView(),
    BeforeEnterObserver,
    HasDynamicTitle {
    override fun getPageTitle(): String = currentLocalization().pageManager.title()

    override fun beforeEnter(event: BeforeEnterEvent) {
        val section = event.routeParameters.get(MANAGER_SECTION_PARAMETER).orElse(null)
        if (section == null) {
            event.forwardTo(ManagerView::class.java, managerSection(CLASSES_SECTION))
            return
        }

        // Checks the manager role before the tables load, so that its absence opens the forbidden screen.
        managerService.viewClasses(Pagination(page = 0, size = 1))
        val localization = currentLocalization()
        val cabinet = localization.pageCabinet
        val texts = localization.pageManager
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head(texts.title()) {
                crumb(cabinet.home(), MultiMainView::class.java)
                tabs(matchRouteParameters = true) {
                    tab(cabinet.sectionClasses(), ManagerView::class.java, managerSection(CLASSES_SECTION))
                    tab(cabinet.sectionCompetitions(), ManagerView::class.java, managerSection(COMPETITIONS_SECTION))
                }
            }
            guide(texts.guide())
            row {
                if (section == COMPETITIONS_SECTION) {
                    block(title = cabinet.sectionCompetitions()) {
                        guide(texts.competitionsGuide())
                        groupsTable(
                            fetch = { request, (name, from, to) ->
                                val filter = CompetitionFilter(name = name, createdFrom = from, createdTo = to)
                                val competitions = managerService.viewCompetitions(request.toPagination(), filter)
                                Page(competitions.content, competitions.totalElements.toInt())
                            },
                            columns = {
                                codeColumn(texts.id(), sortKey = "id", size = 6) { competition -> competition.id.value.toString() }
                                textColumn(texts.name(), sortKey = "name", size = 12) { competition -> competition.name }
                                numberColumn(texts.participantCount()) { competition -> competition.participants.size }
                                empty(texts.competitionsEmpty(), texts.competitionsEmptyHint())
                                onRowClick(isNavigation = true) { competition ->
                                    UI.getCurrent().navigate(ManagerCompetitionView::class.java, competitionParameters(competition.id))
                                }
                            },
                            key = { competition -> competition.id },
                            creation = Creation(
                                label = texts.competitionsCreate(),
                                title = texts.competitionsDialogTitle(),
                                created = PageManager::competitionsCreated,
                            ) { name -> managerService.createCompetition(name) },
                        )
                    }
                } else {
                    block(title = cabinet.sectionClasses()) {
                        guide(texts.classesGuide())
                        groupsTable(
                            fetch = { request, (name, from, to) ->
                                val filter = ClassFilter(name = name, createdFrom = from, createdTo = to)
                                val classes = managerService.viewClasses(request.toPagination(), filter)
                                Page(classes.content, classes.totalElements.toInt())
                            },
                            columns = {
                                codeColumn(texts.id(), sortKey = "id", size = 6) { studyClass -> studyClass.id.value.toString() }
                                textColumn(texts.name(), sortKey = "name", size = 12) { studyClass -> studyClass.name }
                                numberColumn(texts.studentCount()) { studyClass -> studyClass.students.size }
                                empty(texts.classesEmpty(), texts.classesEmptyHint())
                                onRowClick(isNavigation = true) { studyClass ->
                                    UI.getCurrent().navigate(ManagerClassView::class.java, classParameters(studyClass.id))
                                }
                            },
                            key = { studyClass -> studyClass.id },
                            creation = Creation(
                                label = texts.classesCreate(),
                                title = texts.classesDialogTitle(),
                                created = PageManager::classesCreated,
                            ) { name -> managerService.createClass(name) },
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
        val texts = currentLocalization().pageManager
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
                name = textInput(texts.name(), labelSize = 3, size = 9, hint = texts.filterNameHint())
                dates = dateRangeInput(texts.filterCreated(), labelSize = 3, size = 9)
            }
        }

        creationAction(creation, rows)
    }

    /** Adds the action of [creation] opening its dialog, which creates a named group and refreshes [rows]. */
    private fun BlockScope.creationAction(creation: Creation, rows: TableHandle<*>) {
        val localization = currentLocalization()
        val texts = localization.pageManager
        val draft = Binder<NameDraft>()
        val form = dialog(title = creation.title) {
            row {
                textInput(texts.name(), labelSize = 6, size = 18) {
                    draft.forField(this)
                        .nameRules(texts.nameRequired())
                        .bind({ values -> values.name }, { values, name -> values.name = name })
                }
            }
            footer { dialog ->
                action(localization.uiOverlay.dialogCancel()) { onClick { dialog.close() } }
                mainAction(texts.create()) {
                    onClick {
                        val values = NameDraft()
                        if (draft.writeBeanIfValid(values)) {
                            creation.create(values.name)
                            toast(FeedbackKind.Success, creation.created(currentLocalization().pageManager))
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

    /**
     * Creation of a class or competition: the [label] of the action, the [title] of its dialog, the [created] toast message
     * and the [create] call.
     */
    private class Creation(val label: String, val title: String, val created: (PageManager) -> String, val create: (String) -> Unit)

    /** Values of the creation form. */
    private class NameDraft(var name: String = "")
}
