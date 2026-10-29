package tech.testsys.web.app.view

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.RouteParameters
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.UserRepository
import tech.testsys.domain.model.user.Administrator
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Student
import tech.testsys.domain.model.user.UserId
import tech.testsys.operation.config.CommunityConfig
import tech.testsys.operation.config.PublicCommunityOwner
import tech.testsys.operation.error.ImpersonationDeniedError
import tech.testsys.operation.error.OperationException
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.StudyFixtures
import tech.testsys.web.app.error.OperationErrorView
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.CabinetSignIn
import tech.testsys.web.app.security.UserKind
import tech.testsys.web.app.service.CommunityVo
import java.time.Instant

@SpringBootTest
class SupervisorUserViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var users: MultipleRoleUserRepository

    @Autowired
    private lateinit var communities: CommunityRepository

    @Autowired
    private lateinit var communityConfig: CommunityConfig

    @Autowired
    private lateinit var study: StudyFixtures

    @Autowired
    private lateinit var userRepository: UserRepository

    private val logger = assertInstanceOf(Logger::class.java, LoggerFactory.getLogger(SupervisorUserView::class.java))
    private val appender = ListAppender<ILoggingEvent>().apply { start() }

    @AfterEach
    fun detachAppender() {
        logger.detachAppender(appender)
    }

    @Test
    fun `should show an observer for viewing without modification actions`() {
        signIn(fixtures.userOf(UserKind.SUPERVISOR))
        val accessToken = fixtures.unique("observer-code")
        val observer = fixtures.observer(fixtures.community(), rawAccessToken = accessToken)

        open(observer.id.value)

        assertEquals(SupervisorUserView::class.java, currentView)
        assertTrue(accessCodeFields().isEmpty())
        assertTrue(UI.getCurrent()._find<TextField>().none { textField -> textField.value == accessToken })
        assertTrue(UI.getCurrent()._find<Button> { text = "Включить в Сообщество" }.isEmpty())
        assertTrue(UI.getCurrent()._find<Button> { text = "Удалить Наблюдателя" }.isEmpty())
        assertTrue(UI.getCurrent()._find<Button> { text = "Назначить Администратором" }.isEmpty())
    }

    @Test
    fun `should show neither an access code nor the password hash of a user with non-fixed roles`() {
        signIn(fixtures.userOf(UserKind.SUPERVISOR))
        val member = fixtures.multipleRoleUser {
            roles {
                developer {
                    memberOf(listOf(communityConfig.publicCommunityId.value))
                    data = developerData {}
                }
            }
        }

        open(member.id.value)

        assertTrue(accessCodeFields().isEmpty())
        assertTrue(UI.getCurrent()._find<TextField>().none { textField -> textField.value == member.data.passwordHash.value })
    }

    @Test
    fun `should not show the access code of a participant`() {
        signIn(fixtures.userOf(UserKind.SUPERVISOR))
        val participant = study.participant(emptyList())

        open(participant.id.value)

        assertTrue(accessCodeFields().isEmpty())
        assertTrue(UI.getCurrent()._find<TextField>().none { textField -> textField.value == participant.data.accessTokenHash.value })
    }

    @Test
    fun `should grant the administrator role to a user who is only in the public community`() {
        signIn(fixtures.userOf(UserKind.SUPERVISOR))
        val name = fixtures.unique("Разработчик")
        val member = fixtures.multipleRoleUser(name = name) {
            roles {
                developer {
                    memberOf(listOf(communityConfig.publicCommunityId.value))
                    data = developerData {}
                }
            }
        }
        open(member.id.value)

        UI.getCurrent()._get<Button> { text = "Назначить Администратором" }._click()
        openDialog()._get<Button> { text = "Назначить" }._click()

        val administrator = checkNotNull(users.findById(member.id)).data.roles.filterIsInstance<Administrator>().single()
        val owned = communities.findByOwner(member.id)
        assertEquals(listOf("Сообщество $name"), owned.map { community -> community.data.name })
        assertEquals(owned.map { community -> community.id }, administrator.memberOf.ids)
        assertFalse(communityConfig.publicCommunityId in administrator.memberOf.ids)
        assertEquals(SupervisorUserView::class.java, currentView)
    }

    @Test
    fun `should not offer the administrator role to a user who already holds it`() {
        signIn(fixtures.userOf(UserKind.SUPERVISOR))
        val administrator = fixtures.administrator()
        fixtures.community(owner = administrator)

        open(administrator.id.value)

        assertEquals(1, UI.getCurrent()._find<Button> { text = "Включить в Сообщество" }.size)
        assertTrue(UI.getCurrent()._find<Button> { text = "Назначить Администратором" }.isEmpty())
    }

    @Test
    fun `should add a user to a foreign community in a newly granted role`() {
        signIn(fixtures.userOf(UserKind.SUPERVISOR))
        val original = fixtures.community()
        val target = fixtures.community(name = "Целевое сообщество")
        val member = fixtures.multipleRoleUser {
            roles {
                developer {
                    memberOf(listOf(original.id.value))
                    data = developerData {}
                }
            }
        }
        open(member.id.value)

        UI.getCurrent()._get<Button> { text = "Включить в Сообщество" }._click()
        choose("Сообщество", CommunityVo::id, target.id)
        choose("Роль", { role: CommunityRole -> role }, CommunityRole.Student)
        UI.getCurrent()._get<Button> { text = "Включить" }._click()

        val updated = checkNotNull(users.findById(member.id))
        val student = updated.data.roles.filterIsInstance<Student>().single()
        assertTrue(target.id in student.memberOf.ids)
        assertTrue(original.id in updated.data.roles.first { it !is Student }.memberOf.ids)
        assertEquals(SupervisorUserView::class.java, currentView)
    }

    @Test
    fun `should title the browser tab of a user and head the page with its kind`() {
        signIn(fixtures.userOf(UserKind.SUPERVISOR))
        val name = fixtures.unique("Наблюдатель")
        val observer = fixtures.observer(fixtures.community(), name = name)
        val participant = study.participant(emptyList())

        open(observer.id.value)
        val observerHead = UI.getCurrent()._get<H1>().text
        open(participant.id.value)

        assertEquals("Пользователь", assertInstanceOf(HasDynamicTitle::class.java, UI.getCurrent().currentView).pageTitle)
        assertEquals("Наблюдатель «$name»", observerHead)
        assertEquals("Участник «${participant.data.name}»", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should sign the supervisor in as a user with non-fixed roles and open the main page`() {
        val supervisor = fixtures.userOf(UserKind.SUPERVISOR)
        signIn(supervisor)
        val member = publicDeveloper()
        open(member.id.value)

        UI.getCurrent()._get<Button> { text = "Войти под Пользователем" }._click()

        assertEquals(CabinetPrincipal.of(member).copy(impersonatedBy = supervisor.id.value), CabinetSignIn.principal())
        assertEquals(MultiMainView::class.java, currentView)
    }

    @Test
    fun `should sign the supervisor in as an observer and open the observer page`() {
        val supervisor = fixtures.userOf(UserKind.SUPERVISOR)
        signIn(supervisor)
        val observer = fixtures.observer(fixtures.community())
        open(observer.id.value)

        UI.getCurrent()._get<Button> { text = "Войти под Пользователем" }._click()

        assertEquals(CabinetPrincipal.of(observer).copy(impersonatedBy = supervisor.id.value), CabinetSignIn.principal())
        assertEquals(ObserverView::class.java, currentView)
    }

    @Test
    fun `should write the supervisor and user ids at INFO when the supervisor signs in as a user`() {
        val supervisor = fixtures.userOf(UserKind.SUPERVISOR)
        signIn(supervisor)
        val member = publicDeveloper()
        open(member.id.value)
        logger.addAppender(appender)

        UI.getCurrent()._get<Button> { text = "Войти под Пользователем" }._click()

        val expected = "Supervisor ${supervisor.id.value} signed in as user ${member.id.value} (MULTIPLE_ROLE)"
        assertEquals(listOf(Level.INFO to expected), appender.list.map { line -> line.level to line.formattedMessage })
    }

    @Test
    fun `should not record a login of the user the supervisor signs in as`() {
        signIn(fixtures.userOf(UserKind.SUPERVISOR))
        val member = publicDeveloper()
        open(member.id.value)

        UI.getCurrent()._get<Button> { text = "Войти под Пользователем" }._click()

        assertEquals(emptyMap<UserId, Instant>(), userRepository.findLastLogins(listOf(member.id)))
    }

    @Test
    fun `should refuse signing in as the technical owner of the public community`() {
        val supervisor = fixtures.userOf(UserKind.SUPERVISOR)
        signIn(supervisor)
        val owner = requireNotNull(users.findByEmail(PublicCommunityOwner.EMAIL))
        open(owner.id.value)

        val failure = assertThrows<OperationException> {
            UI.getCurrent()._get<Button> { text = "Войти под Пользователем" }._click()
        }

        assertEquals(ImpersonationDeniedError(owner.id), failure.error)
        assertEquals(CabinetPrincipal.of(supervisor), CabinetSignIn.principal())
    }

    @Test
    fun `should refuse a supervisor detail route`() {
        val supervisor = fixtures.userOf(UserKind.SUPERVISOR)
        signIn(supervisor)

        open(supervisor.id.value)

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Страница не найдена", UI.getCurrent()._get<H1>().text)
    }

    /** Stores a developer of the public community: the supervisor reads only users with non-fixed roles who are members of a community. */
    private fun publicDeveloper(): MultipleRoleUser = fixtures.multipleRoleUser {
        roles {
            developer {
                memberOf(listOf(communityConfig.publicCommunityId.value))
                data = developerData {}
            }
        }
    }

    private fun open(id: Long) {
        UI.getCurrent().navigate(SupervisorUserView::class.java, RouteParameters(USER_ID_PARAMETER, id.toString()))
    }

    private fun openDialog(): Dialog = UI.getCurrent()._find<Dialog>().single { dialog -> dialog.isOpened }

    private fun accessCodeFields(): List<TextField> =
        UI.getCurrent()._find<TextField>().filter { textField -> textField.ariaLabel.orElse(null) == "Код-доступа" }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any, K> choose(label: String, key: (T) -> K, expected: K) {
        val select = UI.getCurrent()._get<Dialog>()._find<Select<*>>().single { it.ariaLabel.orElse(null) == label }
        val typed = checkNotNull(select as? Select<T?>)
        typed.value = typed.listDataView.items.toList().filterNotNull().single { item -> key(item) == expected }
    }
}
