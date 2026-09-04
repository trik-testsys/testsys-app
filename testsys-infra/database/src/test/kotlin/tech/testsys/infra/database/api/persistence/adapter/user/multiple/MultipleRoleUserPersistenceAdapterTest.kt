package tech.testsys.infra.database.api.persistence.adapter.user.multiple

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.judgeData
import tech.testsys.domain.builder.api.managerData
import tech.testsys.domain.builder.api.multipleRoleUserData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.user.Administrator
import tech.testsys.domain.model.user.CompatibleUserRole
import tech.testsys.domain.model.user.Developer
import tech.testsys.domain.model.user.Judge
import tech.testsys.domain.model.user.Manager
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserData
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.Student
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTest
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.multiple.AdministratorDataJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.multiple.DeveloperDataJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.multiple.MultipleRoleToUserJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.multiple.StudentDataJpaEntityRepository
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(InternalDatabaseApi::class)
class MultipleRoleUserPersistenceAdapterTest :
    PersistenceAdapterContractTest<MultipleRoleUserData, MultipleRoleUserId, MultipleRoleUser>() {

    @Autowired
    override lateinit var repository: MultipleRoleUserRepository

    @Autowired
    private lateinit var userJpaEntityRepository: UserJpaEntityRepository

    @Autowired
    private lateinit var multipleRoleToUserJpaEntityRepository: MultipleRoleToUserJpaEntityRepository

    @Autowired
    private lateinit var administratorDataJpaEntityRepository: AdministratorDataJpaEntityRepository

    @Autowired
    private lateinit var developerDataJpaEntityRepository: DeveloperDataJpaEntityRepository

    @Autowired
    private lateinit var studentDataJpaEntityRepository: StudentDataJpaEntityRepository

    override fun newData(): MultipleRoleUserData {
        val communityId = fixtures.community().id.value
        return multipleRoleUserData {
            accessToken = fixtures.unique("token")
            name = fixtures.unique("User")
            email = fixtures.email("user")
            roles {
                developer {
                    memberOf(listOf(communityId))
                    data = developerData {}
                }
                administrator {}
            }
        }
    }

    override fun modified(entity: MultipleRoleUser): MultipleRoleUser {
        val communityId = fixtures.community().id.value
        return entity.withData {
            accessToken = fixtures.unique("token")
            name = fixtures.unique("Renamed user")
            email = fixtures.email("renamed")
            roles {
                clear()
                student {
                    memberOf(listOf(communityId))
                    data = studentData {}
                }
                judge { data = judgeData {} }
            }
        }
    }

    override fun idOf(value: Long) = MultipleRoleUserId(value)

    override fun assertSameData(expected: MultipleRoleUser, actual: MultipleRoleUser) {
        assertEquals(expected.data.accessToken, actual.data.accessToken)
        assertEquals(expected.data.name, actual.data.name)
        assertEquals(expected.data.email, actual.data.email)
        assertSameRoles(expected.data.roles, actual.data.roles)
    }

    private fun assertSameRoles(expected: List<CompatibleUserRole>, actual: List<CompatibleUserRole>) {
        assertEquals(expected.map { it::class }.toSet(), actual.map { it::class }.toSet())
        expected.forEach { role ->
            val counterpart = actual.single { it::class == role::class }
            assertEquals(role.memberOf.ids.toSet(), counterpart.memberOf.ids.toSet(), "memberships of ${role::class.simpleName}")
            when (role) {
                is Developer -> {
                    val actualRole = assertIs<Developer>(counterpart)
                    assertEquals(role.data.tasks.ids.toSet(), actualRole.data.tasks.ids.toSet())
                    assertEquals(role.data.contests.ids.toSet(), actualRole.data.contests.ids.toSet())
                }
                is Student -> {
                    val actualRole = assertIs<Student>(counterpart)
                    assertEquals(role.data.classes.ids.toSet(), actualRole.data.classes.ids.toSet())
                    assertEquals(role.data.submissions.ids.toSet(), actualRole.data.submissions.ids.toSet())
                }
                is Judge -> assertEquals(role.data.judgmentOrders.ids.toSet(), assertIs<Judge>(counterpart).data.judgmentOrders.ids.toSet())
                is Manager -> {
                    val actualRole = assertIs<Manager>(counterpart)
                    assertEquals(role.data.classes.ids.toSet(), actualRole.data.classes.ids.toSet())
                    assertEquals(role.data.competitions.ids.toSet(), actualRole.data.competitions.ids.toSet())
                }
                is Administrator -> Unit
            }
        }
    }

    private inline fun <reified Role : CompatibleUserRole> MultipleRoleUser.role(): Role = data.roles.filterIsInstance<Role>().single()

    @Test
    fun `every role survives a round trip with its memberships`() {
        val first = fixtures.community().id.value
        val second = fixtures.community().id.value
        val data = multipleRoleUserData {
            accessToken = fixtures.unique("token")
            name = fixtures.unique("Jack of all trades")
            email = fixtures.email("jack")
            roles {
                administrator { memberOf(listOf(first)) }
                developer {
                    memberOf(listOf(first, second))
                    data = developerData {}
                }
                student {
                    memberOf(listOf(second))
                    data = studentData {}
                }
                judge { data = judgeData {} }
                manager {
                    memberOf(listOf(first))
                    data = managerData {}
                }
            }
        }

        val saved = repository.save(data)
        val found = assertNotNull(repository.findById(saved.id))

        assertSameRoles(data.roles, saved.data.roles)
        assertSameRoles(data.roles, found.data.roles)
        assertEquals(5, multipleRoleToUserJpaEntityRepository.findAllByUserId(saved.id.value).size)
    }

    @Test
    fun `update adds and drops roles and memberships`() {
        val saved = repository.save(newData())
        val kept = saved.role<Developer>().memberOf.ids.single()
        val added = fixtures.community().id.value

        repository.update(
            saved.withData {
                roles {
                    clear()
                    developer {
                        memberOf(listOf(kept.value, added))
                        data = developerData {}
                    }
                    student { data = studentData {} }
                }
            },
        )

        val found = assertNotNull(repository.findById(saved.id))
        assertEquals(setOf(Developer::class, Student::class), found.data.roles.map { it::class }.toSet())
        assertEquals(setOf(kept.value, added), found.role<Developer>().memberOf.ids.map { it.value }.toSet())
        assertNull(administratorDataJpaEntityRepository.findByUserId(saved.id.value))
        assertNotNull(studentDataJpaEntityRepository.findByUserId(saved.id.value))
        assertEquals(2, multipleRoleToUserJpaEntityRepository.findAllByUserId(saved.id.value).size)
    }

    @Test
    fun `developer role projects the owned tasks and contests`() {
        val developer = fixtures.developer()
        val task = fixtures.task(developer)
        val contest = fixtures.contest(developer)
        fixtures.task()

        val found = assertNotNull(repository.findById(developer.id))

        assertEquals(listOf(task.id), found.role<Developer>().data.tasks.ids)
        assertEquals(listOf(contest.id), found.role<Developer>().data.contests.ids)
    }

    @Test
    fun `student role projects the enrolled classes and authored submissions`() {
        val student = fixtures.student()
        val studentClass = fixtures.studentClass(students = listOf(student))
        val submission = fixtures.submission(author = student)
        fixtures.studentClass()

        val found = assertNotNull(repository.findById(student.id))

        assertEquals(listOf(studentClass.id), found.role<Student>().data.classes.ids)
        assertEquals(listOf(submission.id), found.role<Student>().data.submissions.ids)
    }

    @Test
    fun `judge role projects the issued judgment orders`() {
        val judge = fixtures.judge()
        val order = fixtures.judgmentOrder(judge = judge)
        fixtures.judgmentOrder()

        val found = assertNotNull(repository.findById(judge.id))

        assertEquals(listOf(order.id), found.role<Judge>().data.judgmentOrders.ids)
    }

    @Test
    fun `manager role projects the owned classes and competitions`() {
        val manager = fixtures.manager()
        val studentClass = fixtures.studentClass(owner = manager)
        val competition = fixtures.competition(owner = manager)
        fixtures.competition()

        val found = assertNotNull(repository.findById(manager.id))

        assertEquals(listOf(studentClass.id), found.role<Manager>().data.classes.ids)
        assertEquals(listOf(competition.id), found.role<Manager>().data.competitions.ids)
    }

    @Test
    fun `role projections given on save are ignored`() {
        val saved = repository.save(
            multipleRoleUserData {
                accessToken = fixtures.unique("token")
                name = fixtures.unique("User")
                email = fixtures.email("user")
                roles {
                    developer { data = developerData { tasks(listOf(UNKNOWN_ID)) } }
                }
            },
        )

        assertEquals(emptyList(), saved.role<Developer>().data.tasks.ids)
        assertEquals(emptyList(), assertNotNull(repository.findById(saved.id)).role<Developer>().data.tasks.ids)
    }

    @Test
    fun `removeById deletes the user together with its role and membership rows`() {
        val saved = repository.save(newData())

        repository.removeById(saved.id)

        assertNull(repository.findById(saved.id))
        assertNull(developerDataJpaEntityRepository.findByUserId(saved.id.value))
        assertNull(administratorDataJpaEntityRepository.findByUserId(saved.id.value))
        assertTrue(multipleRoleToUserJpaEntityRepository.findAllByUserId(saved.id.value).isEmpty())
        assertTrue(userJpaEntityRepository.findById(saved.id.value).isEmpty)
    }

    @Test
    fun `single-role users are invisible to the adapter`() {
        val supervisor = fixtures.supervisor()
        val user = repository.save(newData())
        val foreignId = MultipleRoleUserId(supervisor.id.value)

        assertNull(repository.findById(foreignId))
        assertEquals(listOf(user.id), repository.findByIds(listOf(foreignId, user.id)).map { it.id })
        repository.removeById(foreignId)
        assertTrue(userJpaEntityRepository.findById(supervisor.id.value).isPresent)
    }
}
