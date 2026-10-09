package tech.testsys.infra.database.api.persistence.adapter.user.multiple

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.judgeData
import tech.testsys.domain.builder.api.managerData
import tech.testsys.domain.builder.api.multipleRoleUser
import tech.testsys.domain.builder.api.multipleRoleUserData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.user.Administrator
import tech.testsys.domain.model.user.CompatibleUserRole
import tech.testsys.domain.model.user.Developer
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.Judge
import tech.testsys.domain.model.user.Manager
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserData
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.Student
import tech.testsys.infra.database.api.persistence.adapter.UpdatablePersistenceAdapterContractTests
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.HashAlgorithmJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.user.UserTypeJpaEnum
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.multiple.AdministratorDataJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.multiple.DeveloperDataJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.multiple.MultipleRoleToUserJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.multiple.StudentDataJpaEntityRepository
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(InternalDatabaseApi::class)
class MultipleRoleUserPersistenceAdapterTests :
    UpdatablePersistenceAdapterContractTests<MultipleRoleUserData, MultipleRoleUserId, MultipleRoleUser>() {

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

    @Autowired
    private lateinit var classes: ClassRepository

    override fun newData() = newData(fixtures.unique("token"))

    private fun newData(rawAccessToken: String): MultipleRoleUserData {
        val communityId = fixtures.community().id.value
        return multipleRoleUserData {
            accessToken(rawAccessToken, algorithm = HashAlgorithm.Identity)
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
            accessToken(fixtures.unique("token"), algorithm = HashAlgorithm.Identity)
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

    override fun detached(entity: MultipleRoleUser) = multipleRoleUser {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
    }

    override fun idOf(value: Long) = MultipleRoleUserId(value)

    override fun assertSameData(expected: MultipleRoleUser, actual: MultipleRoleUser) {
        assertEquals(expected.data.accessTokenHash, actual.data.accessTokenHash)
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
    fun `should keep every role with its memberships through a round trip`() {
        val first = fixtures.community().id.value
        val second = fixtures.community().id.value
        val data = multipleRoleUserData {
            accessToken(fixtures.unique("token"), algorithm = HashAlgorithm.Identity)
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
    fun `should add and drop roles and memberships on update`() {
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
    fun `should project the owned tasks and contests for the Developer role`() {
        val developer = fixtures.developer()
        val task = fixtures.task(developer)
        val contest = fixtures.contest(developer)
        fixtures.task()

        val found = assertNotNull(repository.findById(developer.id))

        assertEquals(listOf(task.id), found.role<Developer>().data.tasks.ids)
        assertEquals(listOf(contest.id), found.role<Developer>().data.contests.ids)
    }

    @Test
    fun `should project the enrolled classes and authored submissions for the Student role`() {
        val student = fixtures.student()
        val studentClass = fixtures.studentClass(students = listOf(student))
        val submission = fixtures.submission(author = student)
        fixtures.studentClass()

        val found = assertNotNull(repository.findById(student.id))

        assertEquals(listOf(studentClass.id), found.role<Student>().data.classes.ids)
        assertEquals(listOf(submission.id), found.role<Student>().data.submissions.ids)
    }

    @Test
    fun `should project the issued judgment orders for the Judge role`() {
        val judge = fixtures.judge()
        val order = fixtures.judgmentOrder(judge = judge)
        fixtures.judgmentOrder()

        val found = assertNotNull(repository.findById(judge.id))

        assertEquals(listOf(order.id), found.role<Judge>().data.judgmentOrders.ids)
    }

    @Test
    fun `should project the owned classes and competitions for the Manager role`() {
        val manager = fixtures.manager()
        val studentClass = fixtures.studentClass(owner = manager)
        val competition = fixtures.competition(owner = manager)
        fixtures.competition()

        val found = assertNotNull(repository.findById(manager.id))

        assertEquals(listOf(studentClass.id), found.role<Manager>().data.classes.ids)
        assertEquals(listOf(competition.id), found.role<Manager>().data.competitions.ids)
    }

    @Test
    fun `should ignore role projections given on save`() {
        val saved = repository.save(
            multipleRoleUserData {
                accessToken(fixtures.unique("token"), algorithm = HashAlgorithm.Identity)
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
    fun `should store the access code and its algorithm through save and update`() {
        val data = newData()

        val saved = repository.save(data)
        val modified = modified(saved)
        val updated = repository.update(modified)
        val found = requireNotNull(repository.findById(updated.id))
        val row = userJpaEntityRepository.findById(updated.id.value).orElseThrow()

        assertEquals(data.accessTokenHash.value, saved.data.accessTokenHash.value)
        assertEquals(HashAlgorithm.Identity, saved.data.accessTokenHash.algorithm)
        assertEquals(modified.data.accessTokenHash.value, found.data.accessTokenHash.value)
        assertEquals(HashAlgorithm.Identity, found.data.accessTokenHash.algorithm)
        assertEquals(modified.data.accessTokenHash.value, row.accessToken)
        assertEquals(HashAlgorithmJpaEnum.IDENTITY, row.accessTokenHashAlgorithm)
    }

    @Test
    fun `should delete the user together with its role and membership rows by id`() {
        val saved = repository.save(newData())

        repository.removeById(saved.id)

        assertNull(repository.findById(saved.id))
        assertNull(developerDataJpaEntityRepository.findByUserId(saved.id.value))
        assertNull(administratorDataJpaEntityRepository.findByUserId(saved.id.value))
        assertTrue(multipleRoleToUserJpaEntityRepository.findAllByUserId(saved.id.value).isEmpty())
        assertTrue(userJpaEntityRepository.findById(saved.id.value).isEmpty)
    }

    @Test
    fun `should not find single-role users`() {
        val supervisor = fixtures.supervisor()
        val user = repository.save(newData())
        val foreignId = MultipleRoleUserId(supervisor.id.value)

        assertNull(repository.findById(foreignId))
        assertEquals(listOf(user.id), repository.findByIds(listOf(foreignId, user.id)).map { it.id })
        repository.removeById(foreignId)
        assertTrue(userJpaEntityRepository.findById(supervisor.id.value).isPresent)
    }

    @Test
    fun `should grant a missing role with the community membership on addCommunityMembership`() {
        val user = fixtures.student()
        val community = fixtures.community(fixtures.administrator())

        val updated = repository.addCommunityMembership(user.id, community.id, CommunityInvite.Kind.Manager)

        val manager = updated.data.roles.filterIsInstance<Manager>().single()
        assertEquals(listOf(community.id), manager.memberOf.ids)
        assertEquals(1, updated.data.roles.filterIsInstance<Student>().size)
        val stored = assertNotNull(repository.findById(user.id))
        assertEquals(listOf(community.id), stored.data.roles.filterIsInstance<Manager>().single().memberOf.ids)
    }

    @Test
    fun `should add a membership to an existing role and keep other memberships on addCommunityMembership`() {
        val saved = repository.save(newData())
        val existingCommunities = saved.data.roles.filterIsInstance<Developer>().single().memberOf.ids
        val community = fixtures.community(fixtures.administrator())

        val updated = repository.addCommunityMembership(saved.id, community.id, CommunityInvite.Kind.Developer)

        val developer = updated.data.roles.filterIsInstance<Developer>().single()
        assertEquals((existingCommunities + community.id).toSet(), developer.memberOf.ids.toSet())
        assertEquals(1, updated.data.roles.filterIsInstance<Administrator>().size)
    }

    @Test
    fun `should not add a second membership row if the user is already a member in the role`() {
        val user = fixtures.developer()
        val community = fixtures.community(fixtures.administrator())
        repository.addCommunityMembership(user.id, community.id, CommunityInvite.Kind.Developer)

        val updated = repository.addCommunityMembership(user.id, community.id, CommunityInvite.Kind.Developer)

        assertEquals(listOf(community.id), updated.data.roles.filterIsInstance<Developer>().single().memberOf.ids)
        assertEquals(1, multipleRoleToUserJpaEntityRepository.findAllByUserId(user.id.value).size)
    }

    @Test
    fun `should increment the user version on addCommunityMembership`() {
        val user = fixtures.developer()
        val community = fixtures.community(fixtures.administrator())

        val updated = repository.addCommunityMembership(user.id, community.id, CommunityInvite.Kind.Developer)

        assertEquals(assertNotNull(user.version).value + 1, assertNotNull(updated.version).value)
        assertEquals(updated.version, assertNotNull(repository.findById(user.id)).version)
    }

    @Test
    fun `should increment the user version on an update of the roles only`() {
        val user = fixtures.developer()

        val updated = repository.update(
            user.withData {
                roles {
                    student { data = studentData {} }
                }
            },
        )

        assertEquals(assertNotNull(user.version).value + 1, assertNotNull(updated.version).value)
        assertEquals(updated.version, assertNotNull(repository.findById(user.id)).version)
    }

    @Test
    fun `should reject an update of a user row of another kind`() {
        val supervisor = fixtures.supervisor()
        val user = fixtures.developer()
        val foreign = multipleRoleUser {
            id = supervisor.id.value
            createdAt = supervisor.createdAt
            version = supervisor.version
            data = user.data
        }

        assertFailsWith<IllegalArgumentException> { repository.update(foreign) }

        val row = userJpaEntityRepository.findById(supervisor.id.value).orElseThrow()
        assertEquals(UserTypeJpaEnum.SINGLE_ROLE, row.type)
        assertEquals(supervisor.data.name, row.name)
    }

    @Test
    fun `should increment the versions of the student's classes on removeById`() {
        val student = fixtures.student()
        val first = fixtures.studentClass(students = listOf(student))
        val second = fixtures.studentClass(students = listOf(student))

        repository.removeById(student.id)

        assertEquals(assertNotNull(first.version).value + 1, assertNotNull(classes.findById(first.id)?.version).value)
        assertEquals(assertNotNull(second.version).value + 1, assertNotNull(classes.findById(second.id)?.version).value)
        assertEquals(emptyList(), assertNotNull(classes.findById(first.id)).data.students.ids)
    }

    @Test
    fun `should reject removing the student role of a user enrolled in a class`() {
        val studyClass = fixtures.studentClass(students = listOf(fixtures.student()))
        // Enrolment increments the student version, so the student is read after it.
        val student = assertNotNull(repository.findById(studyClass.data.students.ids.single()))

        assertFailsWith<IllegalArgumentException> { repository.update(asDeveloperOnly(student)) }

        assertNotNull(studentDataJpaEntityRepository.findByUserId(student.id.value))
        assertEquals(listOf(studyClass.id), assertNotNull(repository.findById(student.id)).role<Student>().data.classes.ids)
    }

    @Test
    fun `should remove the student role of a user enrolled in no class`() {
        val student = fixtures.student()

        val updated = repository.update(asDeveloperOnly(student))

        assertEquals(listOf(Developer::class), updated.data.roles.map { role -> role::class })
        assertNull(studentDataJpaEntityRepository.findByUserId(student.id.value))
    }

    @Test
    fun `should fail to add a community membership for a missing user`() {
        val community = fixtures.community(fixtures.administrator())

        assertFailsWith<IllegalArgumentException> {
            repository.addCommunityMembership(MultipleRoleUserId(UNKNOWN_ID), community.id, CommunityInvite.Kind.Manager)
        }
    }

    @Test
    fun `should find user by its access code`() {
        val token = fixtures.unique("token")
        val saved = repository.save(newData(token))

        val found = repository.findByAccessToken(token)

        assertNotNull(found)
        assertEquals(saved.id, found.id)
        assertSameData(saved, found)
    }

    @Test
    fun `should not find user of another kind by its access code`() {
        val token = fixtures.unique("token")
        fixtures.supervisor(rawAccessToken = token)

        val found = repository.findByAccessToken(token)

        assertNull(found)
    }

    @Test
    fun `should not find user by unknown access code`() {
        repository.save(newData())

        val found = repository.findByAccessToken(fixtures.unique("unknown"))

        assertNull(found)
    }

    @Test
    fun `should not find user by access code with different letter case`() {
        val token = fixtures.unique("token")
        repository.save(newData(token))

        val found = repository.findByAccessToken(token.uppercase())

        assertNull(found)
    }

    @Test
    fun `should not find user by access code with surrounding whitespace`() {
        val token = fixtures.unique("token")
        repository.save(newData(token))

        val found = repository.findByAccessToken(" $token ")

        assertNull(found)
    }

    @Test
    fun `should not find user by previous access code after it is replaced`() {
        val previous = fixtures.unique("token")
        val saved = repository.save(newData(previous))
        repository.update(saved.withData { accessToken(fixtures.unique("token"), algorithm = HashAlgorithm.Identity) })

        val found = repository.findByAccessToken(previous)

        assertNull(found)
    }

    @Test
    fun `should find user by new access code after it is replaced`() {
        val saved = repository.save(newData())
        val replacement = fixtures.unique("token")
        val updated = repository.update(saved.withData { accessToken(replacement, algorithm = HashAlgorithm.Identity) })

        val found = repository.findByAccessToken(replacement)

        assertEquals(updated.id, assertNotNull(found).id)
    }

    @Test
    fun `should find user by its email`() {
        val saved = repository.save(newData())

        val found = repository.findByEmail(saved.data.email)

        assertNotNull(found)
        assertEquals(saved.id, found.id)
        assertSameData(saved, found)
    }

    @Test
    fun `should not find user by unknown email`() {
        repository.save(newData())

        val found = repository.findByEmail(fixtures.email("unknown"))

        assertNull(found)
    }

    @Test
    fun `should not find user by email with different letter case`() {
        val saved = repository.save(newData())

        val found = repository.findByEmail(saved.data.email.uppercase())

        assertNull(found)
    }

    @Test
    fun `should find users holding every role by ids with the same statement count for one and twenty ids`() {
        val communityId = fixtures.community().id.value
        val saved = List(20) {
            repository.save(
                multipleRoleUserData {
                    accessToken(fixtures.unique("token"), algorithm = HashAlgorithm.Identity)
                    name = fixtures.unique("User")
                    email = fixtures.email("user")
                    roles {
                        administrator { memberOf(listOf(communityId)) }
                        developer {
                            memberOf(listOf(communityId))
                            data = developerData {}
                        }
                        student { data = studentData {} }
                        judge { data = judgeData {} }
                        manager { data = managerData {} }
                    }
                },
            )
        }
        val taskIds = saved.map { user -> fixtures.workingTask(user).id }
        val ids = saved.map { user -> user.id }

        val (one, oneIdStatements) = withStatementCount { repository.findByIds(ids.take(1)) }
        val (twenty, twentyIdsStatements) = withStatementCount { repository.findByIds(ids) }

        assertEquals(ids.take(1), one.map { user -> user.id })
        assertEquals(ids.toSet(), twenty.map { user -> user.id }.toSet())
        assertEquals(List(20) { 5 }, twenty.map { user -> user.data.roles.size })
        assertEquals(ids.zip(taskIds).toMap(), twenty.associate { user -> user.id to user.role<Developer>().data.tasks.ids.single() })
        assertEquals(oneIdStatements, twentyIdsStatements)
    }

    /** A copy of [user] holding only the Developer role, which drops every other role on update. */
    private fun asDeveloperOnly(user: MultipleRoleUser): MultipleRoleUser = user.withData {
        roles {
            clear()
            developer { data = developerData {} }
        }
    }
}
