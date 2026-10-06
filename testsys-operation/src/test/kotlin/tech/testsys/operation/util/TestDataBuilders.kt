package tech.testsys.operation.util

import tech.testsys.domain.builder.api.*
import tech.testsys.domain.builder.api.multipleRoleUser
import tech.testsys.domain.builder.task.ContestDataBuilder
import tech.testsys.domain.builder.user.AdministratorBuilder
import tech.testsys.domain.builder.user.DeveloperBuilder
import tech.testsys.domain.builder.user.JudgeBuilder
import tech.testsys.domain.builder.user.ManagerBuilder
import tech.testsys.domain.builder.user.MultipleRoleUserDataBuilder
import tech.testsys.domain.builder.user.StudentBuilder
import tech.testsys.domain.builder.util.chooser.TaskContentChooser
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.Statement
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Instant
import java.util.UUID

fun testMultipleRoleUser(builder: MultipleRoleUserDataBuilder.() -> Unit): MultipleRoleUser = multipleRoleUser {
    id = 0L
    createdAt = Instant.MIN
    version = EntityVersion(0)
    data = multipleRoleUserData {
        name = "Name"
        email = "email"
        accessToken("token", algorithm = HashAlgorithm.Identity)
        builder()
    }
}

fun testDeveloper(builder: DeveloperBuilder.() -> Unit): MultipleRoleUser = testMultipleRoleUser { roles { developer(builder) } }

fun testStudent(builder: StudentBuilder.() -> Unit): MultipleRoleUser = testMultipleRoleUser { roles { student(builder) } }

fun testJudge(builder: JudgeBuilder.() -> Unit): MultipleRoleUser = testMultipleRoleUser { roles { judge(builder) } }

fun testManager(builder: ManagerBuilder.() -> Unit): MultipleRoleUser = testMultipleRoleUser { roles { manager(builder) } }

fun testAdministrator(builder: AdministratorBuilder.() -> Unit): MultipleRoleUser =
    testMultipleRoleUser { roles { administrator(builder) } }

fun testTask(choose: TaskContentChooser.() -> Unit): Task = task {
    id = 0L
    createdAt = Instant.MIN
    version = EntityVersion(0)
    data = taskData {
        owner = MultipleRoleUserId(0)
        name = "name"
        description = "description"
        content.choose()
        uploadedResources = mutableSetOf(testStatement().data.versionBucket)
    }
}

fun testNewTask(): Task = testTask {
    new {}
}

fun testUncommittedTask(): Task = testTask {
    uncommitted(
        wipBuilder = {},
        lastCommittedBuilder = {
            exercises = mutableListOf(ExerciseId(1L))
            statement = StatementId(1L)
        },
    )
}

fun testCommitedTask(): Task = testTask {
    committed {
        exercises = mutableListOf(ExerciseId(1L))
        statement = StatementId(1L)
    }
}

val savedTaskVersion = EntityVersion(1)

fun testSavedTask(updatedTask: Task): Task = task {
    id = updatedTask.id.value
    createdAt = updatedTask.createdAt
    version = savedTaskVersion
    data = updatedTask.data
}

fun testContest(builder: ContestDataBuilder.() -> Unit = {}): Contest = contest {
    id = 19L
    createdAt = Instant.parse("2019-01-01T00:00:00Z")
    version = EntityVersion(0)
    data = contestData {
        owner = MultipleRoleUserId(0)
        name = "Contest"
        description = "Description"
        trikStudioVersion = TrikStudioVersion("3.0.0")
        builder()
    }
}

fun testSavedContest(updatedContest: Contest): Contest = contest {
    id = updatedContest.id.value
    createdAt = updatedContest.createdAt
    version = EntityVersion(1)
    data = updatedContest.data
}

fun testCommunity(communityId: Long): Community = community {
    id = communityId
    createdAt = Instant.MIN
    version = EntityVersion(0)
    data = communityData {
        owner = MultipleRoleUserId(0)
        name = "name"
        description = "description"
    }
}

fun testStatement(statementId: Long = 0L): Statement = statement {
    id = statementId
    createdAt = Instant.MIN
    version = EntityVersion(0)
    data = statementData {
        name = "name"
        description = "description"
        versionBucket = VersionBucket(UUID(0, 0))
        file("file.pdf", "".toByteArray())
    }
}
