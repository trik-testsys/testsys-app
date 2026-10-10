package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._clickItemWithCaption
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._setValue
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.contextmenu.ContextMenu
import com.vaadin.flow.component.datetimepicker.DateTimePicker
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.component.html.TableBody
import com.vaadin.flow.component.html.TableRow
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.RouterLink
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.builder.api.contestData
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.judgeData
import tech.testsys.domain.builder.api.managerData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.builder.api.taskData
import tech.testsys.domain.builder.api.taskValidationTechnicalFailure
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.UserRepository
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.AuthorSubmissionFailure
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.Developer
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Student
import tech.testsys.operation.config.CommunityConfig
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.error.OperationErrorView
import tech.testsys.web.app.service.CommunityVo
import tech.testsys.web.app.service.developer.TaskValidationExecutionVo
import tech.testsys.web.components.display.Tone
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.sql.DataSource

@SpringBootTest
class AdminUserViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var multipleRoleUsers: MultipleRoleUserRepository

    @Autowired
    private lateinit var users: UserRepository

    @Autowired
    private lateinit var observers: ObserverRepository

    @Autowired
    private lateinit var communities: CommunityRepository

    @Autowired
    private lateinit var communityConfig: CommunityConfig

    @Autowired
    private lateinit var tasks: TaskRepository

    @Autowired
    private lateinit var contests: ContestRepository

    @Autowired
    private lateinit var dataSource: DataSource

    @Test
    fun `should show the common data and the roles of a member in the communities of the administrator`() {
        val community = fixtures.community(owner = signInAdministrator(), name = "Кружок")
        val member = developerOf(community, name = "Разработчик Иван")

        openUser(member)

        assertEquals(AdminUserView::class.java, currentView)
        assertEquals(member.id.value.toString(), textField("ID").value)
        assertEquals("Разработчик Иван", textField("Псевдоним").value)
        assertTrue("КружокРазработчик" in rolesText(), rolesText())
    }

    @Test
    fun `should show all roles of a member in one community in one row`() {
        val community = fixtures.community(owner = signInAdministrator(), name = "Кружок")
        val member = fixtures.multipleRoleUser {
            roles {
                developer {
                    memberOf(listOf(community.id.value))
                    data = developerData {}
                }
                student {
                    memberOf(listOf(community.id.value))
                    data = studentData {}
                }
            }
        }

        openUser(member)

        val row = UI.getCurrent()._get<Table>()._get<TableBody>()._get<TableRow>()
        assertTrue("Кружок" in row.element.textRecursively)
        assertEquals(listOf("Разработчик", "Ученик"), row._find<Span> { classes = "ts-tag" }.map { tag -> tag.text })
    }

    @Test
    fun `should show the details in read-only form fields`() {
        val member = developerOf(fixtures.community(owner = signInAdministrator()))

        openUser(member)

        assertTrue(textField("ID").isReadOnly)
        assertTrue(textField("Псевдоним").isReadOnly)
        assertTrue(UI.getCurrent()._get<DateTimePicker>().isReadOnly)
    }

    @Test
    fun `should show the access code of an observer at its own address without role granting`() {
        val community = fixtures.community(owner = signInAdministrator())
        val observer = fixtures.observer(community, rawAccessToken = "observer-code")

        UI.getCurrent().navigate(AdminUserView::class.java, RouteParameters(OBSERVER_ID_PARAMETER, observer.id.value.toString()))

        val accessToken = textField("Код-доступа")
        assertEquals("observer-code", accessToken.value)
        assertTrue(accessToken.isReadOnly)
        assertEquals(emptyList<Button>(), UI.getCurrent()._find<Button> { text = "Включить в сообщество" })
    }

    @Test
    fun `should show the not found screen for a missing user`() {
        signInAdministrator()

        UI.getCurrent().navigate(AdminUserView::class.java, RouteParameters(USER_ID_PARAMETER, Long.MAX_VALUE.toString()))

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Страница не найдена", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should show the forbidden screen for a user of another administrator`() {
        signInAdministrator()
        val stranger = developerOf(fixtures.community())

        openUser(stranger)

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Нет доступа", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should grant the chosen role in a community of the administrator and show it`() {
        val administrator = signInAdministrator()
        val member = developerOf(fixtures.community(owner = administrator))
        val other = fixtures.community(owner = administrator, name = "Второе сообщество")
        openUser(member)

        UI.getCurrent()._get<Button> { text = "Включить в сообщество" }._click()
        choose("Сообщество", CommunityVo::id, other.id)
        choose("Роль", { role: CommunityRole -> role }, CommunityRole.Student)
        UI.getCurrent()._get<Button> { text = "Включить" }._click()

        val student = checkNotNull(multipleRoleUsers.findById(member.id)).data.roles.filterIsInstance<Student>().single()
        assertEquals(setOf(communityConfig.publicCommunityId, other.id), student.memberOf.ids.toSet())
        assertTrue("Второе сообществоУченик" in rolesText(), rolesText())
        assertEquals("Пользователь включён в сообщество", lastToastTitle())
    }

    @Test
    fun `should show the tabs of the roles held in the communities of the administrator`() {
        val community = fixtures.community(owner = signInAdministrator())
        val member = fixtures.multipleRoleUser {
            roles {
                developer {
                    memberOf(listOf(community.id.value))
                    data = developerData {}
                }
                judge {
                    memberOf(listOf(community.id.value))
                    data = judgeData {}
                }
            }
        }

        openUser(member)

        assertEquals(listOf("Сведения", "Разработчик", "Судья"), tabLabels())
        assertEquals(emptyList<String>(), sectionEmptyTitles())
    }

    @Test
    fun `should show the sections of a role on its tab`() {
        val member = developerOf(fixtures.community(owner = signInAdministrator()))

        openUser(member, AdminUserSection.Developer)

        assertEquals(listOf("Задач нет", "Туров нет"), sectionEmptyTitles())
        assertTrue(UI.getCurrent()._find<Table>().none { table -> "Роли" in table.element.textRecursively })
    }

    @Test
    fun `should open the common data for the tab of a role the user does not hold`() {
        val member = developerOf(fixtures.community(owner = signInAdministrator()))

        openUser(member, AdminUserSection.Judge)

        assertEquals(currentLocation(), "admin/users/${member.id.value}")
        assertTrue("Разработчик" in rolesText())
    }

    @Test
    fun `should show the tasks and contests of a developer with their validation status and submission counts`() {
        val member = developerOf(fixtures.community(owner = signInAdministrator()))
        val task = tasks.save(
            taskData {
                owner = member.id
                name = "Движение по линии"
                description = "Task"
                content.new {}
            },
        )
        contests.save(
            contestData {
                owner(member.id.value)
                name = "Весенний тур"
                description = ""
                trikStudioVersion(trikStudioVersion())
                tasks(listOf(task.id.value))
            },
        )

        openUser(member, AdminUserSection.Developer)

        assertTrue("Движение по линииНе запускалась" in tableText("Статус проверки"), tableText("Статус проверки"))
        assertTrue("Весенний тур1Движение по линии0" in tableText("Решения по задачам"), tableText("Решения по задачам"))
    }

    @ParameterizedTest
    @MethodSource("validationStatuses")
    fun `should show the validation status of the latest request for each execution state`(
        execution: TaskValidationExecutionVo?,
        expected: Pair<String, Tone>,
    ) {
        assertEquals(expected, validationStatusOf(execution))
    }

    @Test
    fun `should hide the sections of the roles held only in communities of other administrators`() {
        val community = fixtures.community(owner = signInAdministrator())
        val foreign = fixtures.community()
        val member = fixtures.multipleRoleUser {
            roles {
                student {
                    memberOf(listOf(community.id.value))
                    data = studentData {}
                }
                manager {
                    memberOf(listOf(foreign.id.value))
                    data = managerData {}
                }
            }
        }

        openUser(member)

        assertEquals(emptyList<String>(), tabLabels())
    }

    @Test
    fun `should remove the member from the community in the role after the confirmation and stay on the page`() {
        val administrator = signInAdministrator()
        val removed = fixtures.community(owner = administrator, name = "Первое")
        val kept = fixtures.community(owner = administrator, name = "Второе")
        val member = developerOf(first = removed, second = kept)
        openUser(member)

        rolesMenu("Первое")._clickItemWithCaption("Исключить в роли Разработчик")
        openDialog()._get<Button> { text = "Исключить" }._click()

        val developer = checkNotNull(multipleRoleUsers.findById(member.id)).data.roles.filterIsInstance<Developer>().single()
        assertEquals(listOf(kept.id), developer.memberOf.ids)
        assertEquals(AdminUserView::class.java, currentView)
        assertFalse("Первое" in rolesText(), rolesText())
        assertEquals("Пользователь исключён из сообщества", lastToastTitle())
    }

    @Test
    fun `should open the users of the administrator after removing the last membership in their communities`() {
        val community = fixtures.community(owner = signInAdministrator(), name = "Единственное")
        val member = developerOf(community)
        openUser(member)

        rolesMenu("Единственное")._clickItemWithCaption("Исключить в роли Разработчик")
        openDialog()._get<Button> { text = "Исключить" }._click()

        val developer = checkNotNull(multipleRoleUsers.findById(member.id)).data.roles.filterIsInstance<Developer>().single()
        assertEquals(emptyList<CommunityId>(), developer.memberOf.ids)
        assertEquals(AdminView::class.java, currentView)
        assertEquals("Пользователь исключён из сообщества", lastToastTitle())
    }

    @Test
    fun `should announce removal of the judge role after navigating to the users list`() {
        val community = fixtures.community(owner = signInAdministrator(), name = "Кружок")
        val member = fixtures.multipleRoleUser {
            roles {
                judge {
                    memberOf(listOf(community.id.value))
                    data = judgeData {}
                }
            }
        }
        openUser(member)
        rolesMenu("Кружок")._clickItemWithCaption("Исключить в роли Судья")

        openDialog()._get<Button> { text = "Исключить" }._click()

        assertEquals(AdminView::class.java, currentView)
        assertEquals("Пользователь исключён из сообщества", lastToastTitle())
        assertEquals("Роль: Судья.", lastToastDescription())
    }

    @Test
    fun `should keep the membership if the removal is cancelled`() {
        val community = fixtures.community(owner = signInAdministrator(), name = "Кружок")
        val member = developerOf(community)
        openUser(member)

        rolesMenu("Кружок")._clickItemWithCaption("Исключить в роли Разработчик")
        openDialog()._get<Button> { text = "Отменить" }._click()

        val developer = checkNotNull(multipleRoleUsers.findById(member.id)).data.roles.filterIsInstance<Developer>().single()
        assertEquals(listOf(community.id), developer.memberOf.ids)
        assertEquals(0, UI.getCurrent()._find<Span> { classes = "ts-toast__title" }.size)
    }

    @Test
    fun `should offer no removal in the row of the public community`() {
        val administrator = signInPublicCommunityOwner()
        val community = fixtures.community(owner = administrator, name = "Кружок")
        val member = developerOf(community)
        multipleRoleUsers.addCommunityMembership(member.id, communityConfig.publicCommunityId, CommunityRole.Developer)
        val publicName = checkNotNull(communities.findById(communityConfig.publicCommunityId)).data.name
        openUser(member)

        val items = rolesMenu(publicName).items.map { item -> item.text to item.isEnabled }

        assertEquals(listOf("Нет ролей для исключения" to false), items)
    }

    @Test
    fun `should offer no removal in a row with the administrator role only`() {
        val community = fixtures.community(owner = signInAdministrator(), name = "Кружок")
        val member = fixtures.multipleRoleUser { roles { administrator { memberOf(listOf(community.id.value)) } } }
        openUser(member)

        val items = rolesMenu("Кружок").items.map { item -> item.text to item.isEnabled }

        assertEquals(listOf("Нет ролей для исключения" to false), items)
    }

    @Test
    fun `should delete the observer after the confirmation and open its community`() {
        val community = fixtures.community(owner = signInAdministrator())
        val observer = fixtures.observer(community)
        openObserver(observer.id.value)

        UI.getCurrent()._get<Button> { text = "Удалить наблюдателя" }._click()
        assertFalse(openDialog()._get<Button> { text = "Удалить" }.isEnabled)
        openDialog()._get<TextField>()._setValue(observer.data.name)
        openDialog()._get<Button> { text = "Удалить" }._click()

        assertNull(observers.findById(observer.id))
        assertEquals(AdminCommunityView::class.java, currentView)
        assertEquals("Наблюдатель удалён", lastToastTitle())
    }

    @Test
    fun `should show the assigned contests section of an observer`() {
        val observer = fixtures.observer(fixtures.community(owner = signInAdministrator()))

        openObserver(observer.id.value)

        assertEquals(listOf("Назначенных туров нет"), sectionEmptyTitles())
    }

    @Test
    fun `should show the date and time of the last login in the details`() {
        val member = developerOf(fixtures.community(owner = signInAdministrator()))
        users.recordLogin(member.id, LocalDateTime.of(2026, 3, 14, 9, 30).atZone(ZoneId.systemDefault()).toInstant())

        openUser(member)

        assertEquals(LocalDateTime.of(2026, 3, 14, 9, 30), UI.getCurrent()._get<DateTimePicker>().value)
    }

    @Test
    fun `should leave the last login empty for a user who has never signed in`() {
        val member = developerOf(fixtures.community(owner = signInAdministrator()))

        openUser(member)

        assertNull(UI.getCurrent()._get<DateTimePicker>().value)
    }

    private fun signInAdministrator(): MultipleRoleUser = fixtures.administrator().also { administrator -> signIn(administrator) }

    /** Signs in the creator of the public community, made an administrator, since only it sees that community. */
    private fun signInPublicCommunityOwner(): MultipleRoleUser {
        val ownerId = checkNotNull(communities.findById(communityConfig.publicCommunityId)).data.owner.id
        val owner = checkNotNull(multipleRoleUsers.findById(ownerId))
        return multipleRoleUsers.update(owner.withData { roles { administrator {} } }).also { administrator -> signIn(administrator) }
    }

    private fun developerOf(community: Community, name: String = fixtures.unique("Developer")): MultipleRoleUser =
        developerOf(community, name = name, others = emptyList())

    private fun developerOf(first: Community, second: Community): MultipleRoleUser =
        developerOf(first, name = fixtures.unique("Developer"), others = listOf(second))

    private fun developerOf(community: Community, name: String, others: List<Community>): MultipleRoleUser =
        fixtures.multipleRoleUser(name = name) {
            roles {
                developer {
                    memberOf((listOf(community) + others).map { member -> member.id.value })
                    data = developerData {}
                }
            }
        }

    private fun openUser(user: MultipleRoleUser, section: AdminUserSection? = null) {
        UI.getCurrent().navigate(AdminUserView::class.java, userParameters(user.id, section))
    }

    private fun tabLabels(): List<String> = UI.getCurrent()._find<RouterLink>()
        .filter { link -> link.hasClassName("ts-tab") }
        .map { link -> link.text }

    private fun currentLocation(): String = UI.getCurrent().internals.activeViewLocation.path

    private fun openObserver(observerId: Long) {
        UI.getCurrent().navigate(AdminUserView::class.java, RouteParameters(OBSERVER_ID_PARAMETER, observerId.toString()))
    }

    private fun rolesText(): String =
        UI.getCurrent()._find<Table>().first { table -> "Роли" in table.element.textRecursively }.element.textRecursively

    /** Returns the text of the first table with the column [title]. */
    private fun tableText(title: String): String =
        UI.getCurrent()._find<Table>().first { table -> title in table.element.textRecursively }.element.textRecursively

    /** Adds a TRIK Studio version and returns its tag. */
    private fun trikStudioVersion(): String {
        // Versions come from the grader; no port stores them, so the test adds the row the contest refers to.
        val tag = fixtures.unique("tsv")
        dataSource.connection.use { connection ->
            val insert = "insert into ts_trik_studio_version (id, tag) select coalesce(max(id), 0) + 1, ? from ts_trik_studio_version"
            connection.prepareStatement(insert).use { statement ->
                statement.setString(1, tag)
                statement.executeUpdate()
            }
        }
        return tag
    }

    /** Returns the empty-state titles of the role section tables, in the order of the sections. */
    private fun sectionEmptyTitles(): List<String> = UI.getCurrent()._find<Table>()
        .map { table -> table.element.textRecursively }
        .mapNotNull { text -> SECTION_EMPTY_TITLES.firstOrNull { title -> title in text } }

    private fun rolesMenu(community: String): ContextMenu {
        val trigger = UI.getCurrent()._find<Button>().single { button ->
            button.element.getAttribute("aria-label") == "Роли в сообществе «$community»"
        }
        return checkNotNull(ComponentUtil.getData(trigger, ContextMenu::class.java))
    }

    private fun openDialog(): Dialog = UI.getCurrent()._find<Dialog>().single { dialog -> dialog.isOpened }

    private fun textField(label: String): TextField =
        UI.getCurrent()._find<TextField>().single { field -> field.ariaLabel.orElse(null) == label }

    /** Chooses the item of the select [label] of the open dialog whose [key] is [expected]. */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any, K> choose(label: String, key: (T) -> K, expected: K) {
        val select = UI.getCurrent()._get<Dialog>()._find<Select<*>>().single { candidate -> candidate.ariaLabel.orElse(null) == label }
        val typed = checkNotNull(select as? Select<T?>)
        typed.value = typed.listDataView.items.toList().filterNotNull().single { item -> key(item) == expected }
    }

    private companion object {
        val SECTION_EMPTY_TITLES = listOf(
            "Задач нет",
            "Туров нет",
            "Классов нет",
            "Соревнований нет",
            "Судейских вердиктов нет",
            "Назначенных туров нет",
        )

        @JvmStatic
        fun validationStatuses(): List<Arguments> {
            val failure = taskValidationTechnicalFailure {
                description = "Grader is unavailable"
                occurredAt = Instant.EPOCH
            }
            return listOf(
                Arguments.of(null, "Не запускалась" to Tone.Neutral),
                Arguments.of(TaskValidationExecutionVo.PendingDiagnostics, "Идёт" to Tone.Info),
                Arguments.of(TaskValidationExecutionVo.AwaitingSubmissions(diagnostics = emptyList()), "Идёт" to Tone.Info),
                Arguments.of(
                    TaskValidationExecutionVo.SubmissionsCreated(diagnostics = emptyList(), submissions = listOf(SubmissionId(1))),
                    "Идёт" to Tone.Info,
                ),
                Arguments.of(
                    TaskValidationExecutionVo.StoppedByDiagnostics(diagnostics = emptyList(), completedAt = Instant.EPOCH),
                    "Неуспех" to Tone.Danger,
                ),
                Arguments.of(
                    TaskValidationExecutionVo.Completed(
                        diagnostics = emptyList(),
                        submissions = listOf(SubmissionId(1)),
                        failures = emptyList(),
                        completedAt = Instant.EPOCH,
                    ),
                    "Успех" to Tone.Success,
                ),
                Arguments.of(
                    TaskValidationExecutionVo.Completed(
                        diagnostics = emptyList(),
                        submissions = listOf(SubmissionId(1)),
                        failures = listOf(AuthorSubmissionFailure.GradingFailed(SubmissionId(1))),
                        completedAt = Instant.EPOCH,
                    ),
                    "Неуспех" to Tone.Danger,
                ),
                Arguments.of(TaskValidationExecutionVo.IncompleteDiagnostics(failure), "Техническая остановка" to Tone.Warning),
                Arguments.of(
                    TaskValidationExecutionVo.CompletedDiagnostics(diagnostics = emptyList(), failure = failure),
                    "Техническая остановка" to Tone.Warning,
                ),
                Arguments.of(
                    TaskValidationExecutionVo.CreatedSubmissions(
                        diagnostics = emptyList(),
                        submissions = listOf(SubmissionId(1)),
                        failure = failure,
                    ),
                    "Техническая остановка" to Tone.Warning,
                ),
            )
        }
    }
}
