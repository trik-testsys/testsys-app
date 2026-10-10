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
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.model.user.CommunityRole
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
import tech.testsys.web.app.view.CLASSES_SECTION
import tech.testsys.web.app.view.COMMUNITY_ID_PARAMETER
import tech.testsys.web.app.view.COMPETITIONS_SECTION
import tech.testsys.web.app.view.CONTESTS_SECTION
import tech.testsys.web.app.view.DeveloperContestView
import tech.testsys.web.app.view.DeveloperFixtures
import tech.testsys.web.app.view.DeveloperResourceView
import tech.testsys.web.app.view.DeveloperTaskView
import tech.testsys.web.app.view.DeveloperTestingView
import tech.testsys.web.app.view.DeveloperView
import tech.testsys.web.app.view.JudgeSolutionView
import tech.testsys.web.app.view.JudgeView
import tech.testsys.web.app.view.ManagerClassView
import tech.testsys.web.app.view.ManagerCompetitionView
import tech.testsys.web.app.view.ManagerContestView
import tech.testsys.web.app.view.ManagerView
import tech.testsys.web.app.view.OBSERVER_ID_PARAMETER
import tech.testsys.web.app.view.ParticipantContestView
import tech.testsys.web.app.view.ParticipantTaskView
import tech.testsys.web.app.view.ParticipantView
import tech.testsys.web.app.view.STUDY_CLASS_ID_PARAMETER
import tech.testsys.web.app.view.STUDY_CONTEST_ID_PARAMETER
import tech.testsys.web.app.view.STUDY_TASK_ID_PARAMETER
import tech.testsys.web.app.view.SUBMISSION_ID_PARAMETER
import tech.testsys.web.app.view.StudentClassView
import tech.testsys.web.app.view.StudentContestView
import tech.testsys.web.app.view.StudentTaskView
import tech.testsys.web.app.view.StudentView
import tech.testsys.web.app.view.TASKS_SECTION
import tech.testsys.web.app.view.USER_ID_PARAMETER
import tech.testsys.web.app.view.classContestParameters
import tech.testsys.web.app.view.classParameters
import tech.testsys.web.app.view.competitionContestParameters
import tech.testsys.web.app.view.competitionParameters
import tech.testsys.web.app.view.contestParameters
import tech.testsys.web.app.view.developerSectionParameters
import tech.testsys.web.app.view.judgeSubmissionsParameters
import tech.testsys.web.app.view.managerSection
import tech.testsys.web.app.view.resourceParameters
import tech.testsys.web.app.view.studentClassParameters
import tech.testsys.web.app.view.studentContestParameters
import tech.testsys.web.app.view.taskParameters
import tech.testsys.web.app.view.testingParameters
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
            ParticipantView::class.java to mapOf(
                "" to parametersOf { _, _ -> RouteParameters.empty() },
                "contests" to parametersOf { _, _ -> RouteParameters("section", "contests") },
            ),
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
            ManagerView::class.java to mapOf(
                "" to parametersOf { fixtures, user ->
                    manager(fixtures, user)
                    RouteParameters.empty()
                },
                CLASSES_SECTION to parametersOf { fixtures, user ->
                    manager(fixtures, user)
                    managerSection(CLASSES_SECTION)
                },
                COMPETITIONS_SECTION to parametersOf { fixtures, user ->
                    manager(fixtures, user)
                    managerSection(COMPETITIONS_SECTION)
                },
            ),
            ManagerClassView::class.java to mapOf(
                "class" to parametersOf { fixtures, user -> classParameters(fixtures.studyClass(owner = manager(fixtures, user)).id) },
            ),
            ManagerCompetitionView::class.java to mapOf(
                "competition" to parametersOf { fixtures, user ->
                    competitionParameters(fixtures.competition(owner = manager(fixtures, user)).id)
                },
            ),
            ManagerContestView::class.java to mapOf(
                "class" to parametersOf { fixtures, user ->
                    val contest = fixtures.contest()
                    classContestParameters(fixtures.studyClass(owner = manager(fixtures, user), contests = listOf(contest)).id, contest.id)
                },
                "competition" to parametersOf { fixtures, user ->
                    val contest = fixtures.contest()
                    val competition = fixtures.competition(owner = manager(fixtures, user), contests = listOf(contest))
                    competitionContestParameters(competition.id, contest.id)
                },
            ),
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
            JudgeView::class.java to mapOf(
                "" to parametersOf { fixtures, _ -> RouteParameters.empty().also { signInJudge(fixtures) } },
                "submissions" to parametersOf { fixtures, _ -> judgeSubmissionsParameters().also { signInJudge(fixtures) } },
            ),
            JudgeSolutionView::class.java to mapOf(
                "submission" to parametersOf { fixtures, _ ->
                    signInJudge(fixtures)
                    val author = fixtures.multipleRoleUser { roles { student { data = studentData {} } } }
                    RouteParameters(SUBMISSION_ID_PARAMETER, fixtures.gradingSubmission(author).id.value.toString())
                },
            ),
            DeveloperView::class.java to mapOf(
                "" to developerParameters { _ -> RouteParameters.empty() },
                TASKS_SECTION to developerParameters { _ -> developerSectionParameters(TASKS_SECTION) },
                CONTESTS_SECTION to developerParameters { _ -> developerSectionParameters(CONTESTS_SECTION) },
            ),
            DeveloperTaskView::class.java to mapOf("task" to developerParameters { developers -> taskParameters(developers.task().id) }),
            DeveloperTestingView::class.java to mapOf(
                "testing" to developerParameters { developers ->
                    val task = developers.task()
                    testingParameters(task.id, developers.testing(task).id)
                },
            ),
            DeveloperResourceView::class.java to mapOf(
                "resource" to developerParameters { developers ->
                    val task = developers.task()
                    resourceParameters(task.id, developers.statement(task.id).versionBucket)
                },
            ),
            DeveloperContestView::class.java to mapOf(
                "contest" to developerParameters { developers -> contestParameters(developers.contest(developers.trikStudioVersion()).id) },
            ),
        )

        /** Grants the manager role to the signed-in administrator [user], which the Cabinet of a Manager requires. */
        private fun manager(fixtures: AppFixtures, user: User<*>?): MultipleRoleUser =
            fixtures.grantRole(administrator(user), CommunityRole.Manager)

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

        /** Signs a new judge in instead of the signed-in user: the pages of a judge require the judge role. */
        private fun signInJudge(fixtures: AppFixtures) {
            CabinetSignIn.signIn(CabinetPrincipal.of(fixtures.judge()))
        }

        /** Signs in a new developer, whose pages need the role, and returns the route parameters [parameters] builds for it. */
        private fun developerParameters(parameters: (DeveloperFixtures) -> RouteParameters): ParametersOf = { _, _ ->
            val developers = VaadinService.getCurrent().instantiator.getOrCreate(DeveloperFixtures::class.java)
            developers.signInDeveloper()
            parameters(developers)
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
