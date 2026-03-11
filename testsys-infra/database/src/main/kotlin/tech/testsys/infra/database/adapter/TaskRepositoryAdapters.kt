package tech.testsys.infra.database.adapter

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Repository
import tech.testsys.domain.contract.EntityRepository
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.LazyNullableEntity
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestData
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.DeveloperSolution
import tech.testsys.domain.model.task.DeveloperSolutionData
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseData
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.SolutionData
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.Test
import tech.testsys.domain.model.task.TestData
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.infra.database.jpa.entity.ContestJpaEntity
import tech.testsys.infra.database.jpa.entity.DeveloperSolutionJpaEntity
import tech.testsys.infra.database.jpa.entity.ExerciseJpaEntity
import tech.testsys.infra.database.jpa.entity.SolutionJpaEntity
import tech.testsys.infra.database.jpa.entity.TaskJpaEntity
import tech.testsys.infra.database.jpa.entity.TestJpaEntity
import tech.testsys.infra.database.jpa.entity.TrikStudioVersionEmbeddable
import tech.testsys.infra.database.jpa.spring.ContestSpringRepository
import tech.testsys.infra.database.jpa.spring.DeveloperSolutionSpringRepository
import tech.testsys.infra.database.jpa.spring.ExerciseSpringRepository
import tech.testsys.infra.database.jpa.spring.SolutionSpringRepository
import tech.testsys.infra.database.jpa.spring.TaskSpringRepository
import tech.testsys.infra.database.jpa.spring.TestSpringRepository
import java.time.Duration

@Repository
class ContestRepositoryAdapter(
    private val contestRepo: ContestSpringRepository,
    private val userEntityLoader: UserEntityLoader,
) : EntityRepository<ContestData, ContestId, Contest> {

    override fun load(field: LazyEntity<ContestId, Contest>): Contest {
        val jpa: ContestJpaEntity = contestRepo.findById(field.id.value).orElseThrow {
            EntityNotFoundException("Contest ${field.id.value} not found")
        }
        return toDomain(jpa)
    }

    override fun load(field: LazyNullableEntity<ContestId, Contest>): Contest? =
        contestRepo.findById(field.id.value).orElse(null)?.let { toDomain(it) }

    override fun load(list: LazyEntityList<ContestId, Contest>, pageSize: Int, page: Int): List<Contest> {
        val ids = list.ids.map { it.value }.drop(page * pageSize).take(pageSize)
        return contestRepo.findAllById(ids).map { toDomain(it) }
    }

    override fun save(data: ContestData): Contest {
        val jpa = ContestJpaEntity(
            ownerId = data.owner.id.value,
            name = data.name,
            description = data.description,
            taskIds = data.tasksOrder.map { it.value }.toMutableList(),
            startsAt = data.startsAt,
            contestDurationSeconds = data.contestDuration.seconds,
            attemptDurationSeconds = data.attemptDuration.seconds,
            trikStudioVersionImage = data.trikStudioVersion.image,
            trikStudioVersionTag = data.trikStudioVersion.tag,
            createdAt = data.createdAt,
        )
        val saved = contestRepo.save(jpa)
        return toDomain(saved)
    }

    override fun save(data: List<ContestData>): List<Contest> = data.map { save(it) }

    private fun toDomain(jpa: ContestJpaEntity): Contest {
        val owner = userEntityLoader.loadById(jpa.ownerId) as MultipleRoleUser
        val taskIdList = jpa.taskIds.map { TaskId(it) }
        return Contest(
            id = ContestId(jpa.id),
            data = ContestData(
                owner = owner,
                name = jpa.name,
                description = jpa.description,
                tasks = LazyEntityList(taskIdList),
                tasksOrder = taskIdList,
                startsAt = jpa.startsAt,
                contestDuration = Duration.ofSeconds(jpa.contestDurationSeconds),
                attemptDuration = Duration.ofSeconds(jpa.attemptDurationSeconds),
                createdAt = jpa.createdAt,
                trikStudioVersion = TrikStudioVersion(
                    image = jpa.trikStudioVersionImage,
                    tag = jpa.trikStudioVersionTag,
                ),
            )
        )
    }
}

@Repository
class TaskRepositoryAdapter(
    private val taskRepo: TaskSpringRepository,
    private val userEntityLoader: UserEntityLoader,
) : EntityRepository<TaskData, TaskId, Task> {

    override fun load(field: LazyEntity<TaskId, Task>): Task {
        val jpa: TaskJpaEntity = taskRepo.findById(field.id.value).orElseThrow {
            EntityNotFoundException("Task ${field.id.value} not found")
        }
        return toDomain(jpa)
    }

    override fun load(field: LazyNullableEntity<TaskId, Task>): Task? =
        taskRepo.findById(field.id.value).orElse(null)?.let { toDomain(it) }

    override fun load(list: LazyEntityList<TaskId, Task>, pageSize: Int, page: Int): List<Task> {
        val ids = list.ids.map { it.value }.drop(page * pageSize).take(pageSize)
        return taskRepo.findAllById(ids).map { toDomain(it) }
    }

    override fun save(data: TaskData): Task {
        val jpa = TaskJpaEntity(
            ownerId = data.owner.id.value,
            name = data.name,
            description = data.description,
            exerciseId = data.exercise.id.value,
            testIds = data.tests.ids.map { it.value }.toMutableList(),
            developerSolutionIds = data.developerSolutions.ids.map { it.value }.toMutableList(),
            supportedTrikStudioVersions = data.supportedTrikStudioVersions.map {
                TrikStudioVersionEmbeddable(image = it.image, tag = it.tag)
            }.toMutableList(),
        )
        val saved = taskRepo.save(jpa)
        return toDomain(saved)
    }

    override fun save(data: List<TaskData>): List<Task> = data.map { save(it) }

    private fun toDomain(jpa: TaskJpaEntity): Task {
        val owner = userEntityLoader.loadById(jpa.ownerId) as MultipleRoleUser
        return Task(
            id = TaskId(jpa.id),
            data = TaskData(
                owner = owner,
                name = jpa.name,
                description = jpa.description,
                tests = LazyEntityList(jpa.testIds.map { TestId(it) }),
                exercise = LazyEntity(ExerciseId(jpa.exerciseId)),
                developerSolutions = LazyEntityList(jpa.developerSolutionIds.map { DeveloperSolutionId(it) }),
                supportedTrikStudioVersions = jpa.supportedTrikStudioVersions.map {
                    TrikStudioVersion(image = it.image, tag = it.tag)
                },
            )
        )
    }
}

@Repository
class TestRepositoryAdapter(
    private val testRepo: TestSpringRepository,
) : EntityRepository<TestData, TestId, Test> {

    override fun load(field: LazyEntity<TestId, Test>): Test {
        val jpa: TestJpaEntity = testRepo.findById(field.id.value).orElseThrow {
            EntityNotFoundException("Test ${field.id.value} not found")
        }
        return toDomain(jpa)
    }

    override fun load(field: LazyNullableEntity<TestId, Test>): Test? =
        testRepo.findById(field.id.value).orElse(null)?.let { toDomain(it) }

    override fun load(list: LazyEntityList<TestId, Test>, pageSize: Int, page: Int): List<Test> {
        val ids = list.ids.map { it.value }.drop(page * pageSize).take(pageSize)
        return testRepo.findAllById(ids).map { toDomain(it) }
    }

    override fun save(data: TestData): Test {
        val jpa = TestJpaEntity(
            polygon = data.polygon,
            language = data.language.toDbString(),
        )
        val saved = testRepo.save(jpa)
        return toDomain(saved)
    }

    override fun save(data: List<TestData>): List<Test> = data.map { save(it) }

    private fun toDomain(jpa: TestJpaEntity): Test = Test(
        id = TestId(jpa.id),
        data = TestData(
            polygon = jpa.polygon,
            language = trikLanguageFromDb(jpa.language)
        )
    )
}

@Repository
class SolutionRepositoryAdapter(
    private val solutionRepo: SolutionSpringRepository,
) : EntityRepository<SolutionData, SolutionId, Solution> {

    override fun load(field: LazyEntity<SolutionId, Solution>): Solution {
        val jpa: SolutionJpaEntity = solutionRepo.findById(field.id.value).orElseThrow {
            EntityNotFoundException("Solution ${field.id.value} not found")
        }
        return toDomain(jpa)
    }

    override fun load(field: LazyNullableEntity<SolutionId, Solution>): Solution? =
        solutionRepo.findById(field.id.value).orElse(null)?.let { toDomain(it) }

    override fun load(list: LazyEntityList<SolutionId, Solution>, pageSize: Int, page: Int): List<Solution> {
        val ids = list.ids.map { it.value }.drop(page * pageSize).take(pageSize)
        return solutionRepo.findAllById(ids).map { toDomain(it) }
    }

    override fun save(data: SolutionData): Solution {
        val jpa = SolutionJpaEntity(
            content = data.content,
            language = data.language.toDbString(),
        )
        val saved = solutionRepo.save(jpa)
        return toDomain(saved)
    }

    override fun save(data: List<SolutionData>): List<Solution> = data.map { save(it) }

    private fun toDomain(jpa: SolutionJpaEntity): Solution = Solution(
        id = SolutionId(jpa.id),
        data = SolutionData(
            content = jpa.content,
            language = trikLanguageFromDb(jpa.language),
        )
    )
}

@Repository
class ExerciseRepositoryAdapter(
    private val exerciseRepo: ExerciseSpringRepository,
) : EntityRepository<ExerciseData, ExerciseId, Exercise> {

    override fun load(field: LazyEntity<ExerciseId, Exercise>): Exercise {
        val jpa: ExerciseJpaEntity = exerciseRepo.findById(field.id.value).orElseThrow {
            EntityNotFoundException("Exercise ${field.id.value} not found")
        }
        return toDomain(jpa)
    }

    override fun load(field: LazyNullableEntity<ExerciseId, Exercise>): Exercise? =
        exerciseRepo.findById(field.id.value).orElse(null)?.let { toDomain(it) }

    override fun load(list: LazyEntityList<ExerciseId, Exercise>, pageSize: Int, page: Int): List<Exercise> {
        val ids = list.ids.map { it.value }.drop(page * pageSize).take(pageSize)
        return exerciseRepo.findAllById(ids).map { toDomain(it) }
    }

    override fun save(data: ExerciseData): Exercise {
        val jpa = ExerciseJpaEntity(
            content = data.content,
            language = data.language.toDbString(),
        )
        val saved = exerciseRepo.save(jpa)
        return toDomain(saved)
    }

    override fun save(data: List<ExerciseData>): List<Exercise> = data.map { save(it) }

    private fun toDomain(jpa: ExerciseJpaEntity): Exercise = Exercise(
        id = ExerciseId(jpa.id),
        data = ExerciseData(
            content = jpa.content,
            language = trikLanguageFromDb(jpa.language),
        )
    )
}

@Repository
class DeveloperSolutionRepositoryAdapter(
    private val devSolutionRepo: DeveloperSolutionSpringRepository,
) : EntityRepository<DeveloperSolutionData, DeveloperSolutionId, DeveloperSolution> {

    override fun load(field: LazyEntity<DeveloperSolutionId, DeveloperSolution>): DeveloperSolution {
        val jpa: DeveloperSolutionJpaEntity = devSolutionRepo.findById(field.id.value).orElseThrow {
            EntityNotFoundException("DeveloperSolution ${field.id.value} not found")
        }
        return toDomain(jpa)
    }

    override fun load(field: LazyNullableEntity<DeveloperSolutionId, DeveloperSolution>): DeveloperSolution? =
        devSolutionRepo.findById(field.id.value).orElse(null)?.let { toDomain(it) }

    override fun load(
        list: LazyEntityList<DeveloperSolutionId, DeveloperSolution>,
        pageSize: Int,
        page: Int,
    ): List<DeveloperSolution> {
        val ids = list.ids.map { it.value }.drop(page * pageSize).take(pageSize)
        return devSolutionRepo.findAllById(ids).map { toDomain(it) }
    }

    override fun save(data: DeveloperSolutionData): DeveloperSolution {
        val jpa = DeveloperSolutionJpaEntity(
            solutionId = data.solution.id.value,
            expectedVerdictId = data.expectedVerdict.id.value,
        )
        val saved = devSolutionRepo.save(jpa)
        return toDomain(saved)
    }

    override fun save(data: List<DeveloperSolutionData>): List<DeveloperSolution> = data.map { save(it) }

    private fun toDomain(jpa: DeveloperSolutionJpaEntity): DeveloperSolution = DeveloperSolution(
        id = DeveloperSolutionId(jpa.id),
        data = DeveloperSolutionData(
            solution = LazyEntity(SolutionId(jpa.solutionId)),
            expectedVerdict = LazyEntity(VerdictId(jpa.expectedVerdictId)),
        )
    )
}