package tech.testsys.operation.util

import tech.testsys.domain.builder.api.*
import tech.testsys.domain.builder.api.multipleRoleUser
import tech.testsys.domain.builder.user.AdministratorBuilder
import tech.testsys.domain.builder.user.DeveloperBuilder
import tech.testsys.domain.builder.user.JudgeBuilder
import tech.testsys.domain.builder.user.ManagerBuilder
import tech.testsys.domain.builder.user.MultipleRoleUserDataBuilder
import tech.testsys.domain.builder.user.StudentBuilder
import tech.testsys.domain.builder.util.chooser.TaskContentChooser
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.Statement
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.Task
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
        accessToken = "token"
        builder()
    }
}

fun testDeveloper(builder: DeveloperBuilder.() -> Unit): MultipleRoleUser =
    testMultipleRoleUser { roles { developer(builder) } }

fun testStudent(builder: StudentBuilder.() -> Unit): MultipleRoleUser =
    testMultipleRoleUser { roles { student(builder) } }

fun testJudge(builder: JudgeBuilder.() -> Unit): MultipleRoleUser =
    testMultipleRoleUser { roles { judge(builder) } }

fun testManager(builder: ManagerBuilder.() -> Unit): MultipleRoleUser =
    testMultipleRoleUser { roles { manager(builder) } }

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
    }
}

fun testNewTask(): Task = testTask {
    new {}
}

fun testUncommittedTask(): Task = testTask {
    uncommitted(
        wipBuilder = {},
        lastCommittedBuilder = {
            exercise = ExerciseId(1L)
            statement = StatementId(1L)
        }
    )
}

fun testCommitedTask(): Task = testTask {
    committed {
        exercise = ExerciseId(1L)
        statement = StatementId(1L)
    }
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

fun testStatement(): Statement = statement {
    id = 0L
    createdAt = Instant.MIN
    version = EntityVersion(0)
    data = statementData {
        name = "name"
        description = "description"
        versionBucket = UUID.fromString("00000000-0000-0000-0000-000000000000")
        file("file.pdf", "".toByteArray())
    }
}