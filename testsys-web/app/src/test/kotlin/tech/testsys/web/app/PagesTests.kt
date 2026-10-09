package tech.testsys.web.app

import com.github.mvysny.kaributesting.v10.Routes
import com.github.mvysny.kaributesting.v10._errorMessage
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.router.InternalServerError
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.server.VaadinService
import jakarta.annotation.security.RolesAllowed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.judgeData
import tech.testsys.domain.builder.api.managerData
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.User
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.CabinetSignIn
import tech.testsys.web.app.security.UserKind
import tech.testsys.web.app.view.ADMIN_SECTION_PARAMETER
import tech.testsys.web.app.view.AdminCommunityView
import tech.testsys.web.app.view.AdminUserSection
import tech.testsys.web.app.view.AdminUserView
import tech.testsys.web.app.view.AdminView
import tech.testsys.web.app.view.COMMUNITY_ID_PARAMETER
import tech.testsys.web.app.view.OBSERVER_ID_PARAMETER
import tech.testsys.web.app.view.ParticipantContestView
import tech.testsys.web.app.view.ParticipantTaskView
import tech.testsys.web.app.view.STUDY_CLASS_ID_PARAMETER
import tech.testsys.web.app.view.STUDY_CONTEST_ID_PARAMETER
import tech.testsys.web.app.view.STUDY_TASK_ID_PARAMETER
import tech.testsys.web.app.view.StudentClassView
import tech.testsys.web.app.view.StudentContestView
import tech.testsys.web.app.view.StudentTaskView
import tech.testsys.web.app.view.StudentView
import tech.testsys.web.app.view.USER_ID_PARAMETER
import tech.testsys.web.app.view.studentClassParameters
import tech.testsys.web.app.view.studentContestParameters
import tech.testsys.web.app.view.userParameters

/** Route parameters of a page built from the fixtures and the signed-in user. */
private typealias ParametersOf = (AppFixtures, User<*>?) -> RouteParameters

/**
 * Opens every page of the application, signed in as a user of the kind the page requires, so that a page build error fails the build.
 * A user with non-fixed roles holds the administrator role, which the Cabinet of an Administrator requires.
 */
@Import(PostgresTestConfiguration::class)
@SpringBootTest
class PagesTests : MockSpringVaadinTests() {
    @ParameterizedTest(name = "{argumentSetName}", allowZeroInvocations = true)
    @MethodSource("pages")
    fun `should open the page without a build error`(page: Class<out Component>, parameters: ParametersOf) {
        val user = page.getAnnotation(RolesAllowed::class.java)?.let { access ->
            signedInUsers.getValue(UserKind.valueOf(access.value.single()))(fixtures).also { user -> signIn(user) }
        }

        UI.getCurrent().navigate(page, parameters(fixtures, user))

        assertEquals(page, currentView) {
            UI.getCurrent().internals.activeRouterTargetsChain
                .filterIsInstance<InternalServerError>()
                .joinToString { error -> error._errorMessage }
        }
    }

    companion object {
        private val signedInUsers: Map<UserKind, (AppFixtures) -> User<*>> =
            UserKind.entries.associateWith { kind -> { fixtures: AppFixtures -> fixtures.userOf(kind) } } +
                (UserKind.MULTIPLE_ROLE to AppFixtures::administrator)

        private val parameterSets: Map<Class<out Component>, Map<String, ParametersOf>> = mapOf(
            AdminView::class.java to mapOf(
                "" to parametersOf { _, _ -> RouteParameters.empty() },
                "communities" to parametersOf { _, _ -> RouteParameters(ADMIN_SECTION_PARAMETER, "communities") },
                "users" to parametersOf { _, _ -> RouteParameters(ADMIN_SECTION_PARAMETER, "users") },
            ),
            AdminCommunityView::class.java to mapOf(
                "community" to parametersOf { fixtures, user ->
                    RouteParameters(COMMUNITY_ID_PARAMETER, fixtures.community(owner = administrator(user)).id.value.toString())
                },
            ),
            AdminUserView::class.java to mapOf(
                "user" to parametersOf { fixtures, user ->
                    val administrator = administrator(user)
                    fixtures.community(owner = administrator)
                    RouteParameters(USER_ID_PARAMETER, administrator.id.value.toString())
                },
                "observer" to parametersOf { fixtures, user ->
                    val observer = fixtures.observer(fixtures.community(owner = administrator(user)))
                    RouteParameters(OBSERVER_ID_PARAMETER, observer.id.value.toString())
                },
            ) + AdminUserSection.entries.associate { section ->
                section.value to parametersOf { fixtures, user -> userParameters(memberWithAllSections(fixtures, user).id, section) }
            },
            StudentView::class.java to mapOf(
                "" to parametersOf { _, _ ->
                    signedIn(study().student())
                    RouteParameters.empty()
                },
            ),
            StudentClassView::class.java to mapOf(
                "class" to parametersOf { _, _ ->
                    val student = signedIn(study().student())
                    val contest = study().runningContest()
                    val studyClass = study().studentClass(listOf(student), listOf(contest))
                    study().studentEntry(student, studyClass, contest)
                    studentClassParameters(studyClass.id)
                },
            ),
            StudentContestView::class.java to mapOf(
                "not entered" to parametersOf { _, _ -> studentTaskParameters(isEntered = false).first },
                "entered" to parametersOf { _, _ -> studentTaskParameters(isEntered = true).first },
            ),
            StudentTaskView::class.java to mapOf("task" to parametersOf { _, _ -> studentTaskParameters(isEntered = true).second }),
            ParticipantContestView::class.java to mapOf(
                "not entered" to parametersOf { _, _ -> participantTaskParameters(isEntered = false).first },
                "entered" to parametersOf { _, _ -> participantTaskParameters(isEntered = true).first },
            ),
            ParticipantTaskView::class.java to mapOf("task" to parametersOf { _, _ -> participantTaskParameters(isEntered = true).second }),
        )

        /** Returns the fixtures of the study pages from the Spring context of the running UI. */
        private fun study(): StudyFixtures = VaadinService.getCurrent().instantiator.getOrCreate(StudyFixtures::class.java)

        /** Signs [user] in instead of the user the page access requires by kind. */
        private fun <T : User<*>> signedIn(user: T): T = user.also { CabinetSignIn.signIn(CabinetPrincipal.of(user)) }

        /** Signs a new student in and returns the parameters of the contest page and the task page of a contest of its class. */
        private fun studentTaskParameters(isEntered: Boolean): Pair<RouteParameters, RouteParameters> {
            val student = signedIn(study().student())
            val task = study().task()
            val contest = study().runningContest(tasks = listOf(task))
            val studyClass = study().studentClass(listOf(student), listOf(contest))
            if (isEntered) study().studentEntry(student, studyClass, contest)
            val contestParameters = studentContestParameters(studyClass.id, contest.id)
            val taskParameters = RouteParameters(
                mapOf(
                    STUDY_CLASS_ID_PARAMETER to studyClass.id.value.toString(),
                    STUDY_CONTEST_ID_PARAMETER to contest.id.value.toString(),
                    STUDY_TASK_ID_PARAMETER to task.id.value.toString(),
                ),
            )
            return contestParameters to taskParameters
        }

        /** Signs a new participant in and returns the parameters of the contest page and the task page of its contest. */
        private fun participantTaskParameters(isEntered: Boolean): Pair<RouteParameters, RouteParameters> {
            val task = study().task()
            val contest = study().runningContest(tasks = listOf(task))
            val participant = signedIn(study().participant(listOf(contest)))
            if (isEntered) study().participantEntry(participant, contest)
            val contestParameters = RouteParameters(STUDY_CONTEST_ID_PARAMETER, contest.id.value.toString())
            val taskParameters = RouteParameters(
                mapOf(STUDY_CONTEST_ID_PARAMETER to contest.id.value.toString(), STUDY_TASK_ID_PARAMETER to task.id.value.toString()),
            )
            return contestParameters to taskParameters
        }

        /** Returns a member of a community of the signed-in administrator [user] with the roles of all user page tabs. */
        private fun memberWithAllSections(fixtures: AppFixtures, user: User<*>?): MultipleRoleUser {
            val communityId = listOf(fixtures.community(owner = administrator(user)).id.value)
            return fixtures.multipleRoleUser {
                roles {
                    developer {
                        memberOf(communityId)
                        data = developerData {}
                    }
                    manager {
                        memberOf(communityId)
                        data = managerData {}
                    }
                    judge {
                        memberOf(communityId)
                        data = judgeData {}
                    }
                }
            }
        }

        @JvmStatic
        fun pages(): List<Arguments> = Routes().autoDiscoverViews(MockSpringVaadinTests::class.java.packageName).routes
            .filter { page -> page.isAnnotationPresent(Route::class.java) }
            .sortedBy { page -> page.name }
            .flatMap { page ->
                parameterSets.getOrDefault(page, mapOf("" to parametersOf { _, _ -> RouteParameters.empty() }))
                    .map { (name, parameters) -> argumentSet("${page.simpleName} $name".trim(), page, parameters) }
            }

        private fun parametersOf(parameters: ParametersOf): ParametersOf = parameters

        private fun administrator(user: User<*>?): MultipleRoleUser =
            checkNotNull(user as? MultipleRoleUser) { "The page of an administrator needs a signed-in user with non-fixed roles" }
    }
}
