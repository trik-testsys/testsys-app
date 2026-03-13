package tech.testsys.domain.model.user

import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.Test
import tech.testsys.domain.model.task.TestId
import java.time.Instant

data class MultipleRoleUserData(
    val roles: List<CompatibleUserRole>
)

sealed class MultipleRoleUser(
    id: UserId,
    accessToken: String,
    val data: MultipleRoleUserData,
    createdAt: Instant = Instant.now(),
) : User(id, accessToken, createdAt)

sealed class CompatibleUserRole(
    val memberOf: LazyEntityList<CommunityId, Community>
)

data class DeveloperData(
    val tasks: LazyEntityList<TaskId, Task>,
    val contest: LazyEntityList<ContestId, Contest>,
    val polygons: LazyEntityList<TestId, Test>,
    val solutions: LazyEntityList<SolutionId, Solution>,
    val exercises: LazyEntityList<ExerciseId, Exercise>
)

class Developer(
    memberOf: LazyEntityList<CommunityId, Community>,
    val data: DeveloperData
) : CompatibleUserRole(memberOf)

data class StudentData(
    val classes: LazyEntityList<ClassId, Class>,
    val submissions: LazyEntityList<SubmissionId, Submission>
)

class Student(
    memberOf: LazyEntityList<CommunityId, Community>,
    val data: StudentData
) : CompatibleUserRole(memberOf)

class Administrator(
    memberOf: LazyEntityList<CommunityId, Community>,
) : CompatibleUserRole(memberOf)

data class JudgeData(
    val judgmentOrders: LazyEntityList<JudgmentOrderId, JudgmentOrder>
)

class Judge(
    memberOf: LazyEntityList<CommunityId, Community>,
    val data: JudgeData
) : CompatibleUserRole(memberOf)

data class ManagerData(
    val classes: LazyEntityList<ClassId, Class>,
    val competitions: LazyEntityList<CompetitionId, Competition>
)

class Manager(
    memberOf: LazyEntityList<CommunityId, Community>,
    val data: ManagerData
) : CompatibleUserRole(memberOf)
