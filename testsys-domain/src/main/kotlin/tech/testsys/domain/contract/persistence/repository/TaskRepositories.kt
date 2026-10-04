package tech.testsys.domain.contract.persistence.repository

import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestData
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.DeveloperSolution
import tech.testsys.domain.model.task.DeveloperSolutionData
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseData
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.JudgmentOrderData
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.Logs
import tech.testsys.domain.model.task.LogsData
import tech.testsys.domain.model.task.LogsId
import tech.testsys.domain.model.task.Recording
import tech.testsys.domain.model.task.RecordingData
import tech.testsys.domain.model.task.RecordingId
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.SolutionData
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.Statement
import tech.testsys.domain.model.task.StatementData
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionData
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.Test
import tech.testsys.domain.model.task.TestData
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictData
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.domain.model.task.VersionBucket

/**
 * Persistence port for [Contest] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface ContestRepository : EntityRepository<ContestData, ContestId, Contest>

/**
 * Persistence port for [DeveloperSolution] entities. The solution and expected score are fixed on creation: `update`
 * with another value throws [UnsupportedOperationException], a new version is saved as a new developer solution in
 * the same version bucket.
 *
 * @since %CURRENT_VERSION%
 */
interface DeveloperSolutionRepository : EntityRepository<DeveloperSolutionData, DeveloperSolutionId, DeveloperSolution> {

    /**
     * Finds the latest version in [versionBucket], ordered by creation time and then by id, both descending.
     * Metadata updates do not change this order.
     *
     * @param versionBucket the resource version chain to search.
     * @return the latest version, or `null` if the chain has no versions.
     * @since %CURRENT_VERSION%
     */
    fun findLatestByVersionBucket(versionBucket: VersionBucket): DeveloperSolution?
}

/**
 * Persistence port for [Exercise] entities. The file and language are fixed on creation: `update` with another value
 * throws [UnsupportedOperationException], a new version is saved as a new exercise in the same version bucket.
 *
 * @since %CURRENT_VERSION%
 */
interface ExerciseRepository : EntityRepository<ExerciseData, ExerciseId, Exercise> {

    /**
     * Finds the latest version in [versionBucket], ordered by creation time and then by id, both descending.
     * Metadata updates do not change this order.
     *
     * @param versionBucket the resource version chain to search.
     * @return the latest version, or `null` if the chain has no versions.
     * @since %CURRENT_VERSION%
     */
    fun findLatestByVersionBucket(versionBucket: VersionBucket): Exercise?
}

/**
 * Persistence port for [JudgmentOrder] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface JudgmentOrderRepository : EntityRepository<JudgmentOrderData, JudgmentOrderId, JudgmentOrder>

/**
 * Persistence port for [Logs] entities. Logs are fixed on creation, so `update` is not supported.
 *
 * @since %CURRENT_VERSION%
 */
interface LogsRepository : EntityRepository<LogsData, LogsId, Logs>

/**
 * Persistence port for [Recording] entities. A recording is fixed on creation, so `update` is not supported.
 *
 * @since %CURRENT_VERSION%
 */
interface RecordingRepository : EntityRepository<RecordingData, RecordingId, Recording>

/**
 * Persistence port for [Solution] entities. A solution is fixed on creation, so `update` is not supported.
 *
 * @since %CURRENT_VERSION%
 */
interface SolutionRepository : EntityRepository<SolutionData, SolutionId, Solution>

/**
 * Persistence port for [Statement] entities. The file is fixed on creation: `update` with another file
 * throws [UnsupportedOperationException], a new version is saved as a new statement in the same version bucket.
 *
 * @since %CURRENT_VERSION%
 */
interface StatementRepository : EntityRepository<StatementData, StatementId, Statement> {

    /**
     * Finds the latest version in [versionBucket], ordered by creation time and then by id, both descending.
     * Metadata updates do not change this order.
     *
     * @param versionBucket the resource version chain to search.
     * @return the latest version, or `null` if the chain has no versions.
     * @since %CURRENT_VERSION%
     */
    fun findLatestByVersionBucket(versionBucket: VersionBucket): Statement?
}

/**
 * Persistence port for [Submission] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface SubmissionRepository : EntityRepository<SubmissionData, SubmissionId, Submission>

/**
 * Persistence port for [Verdict] entities. A verdict is fixed on creation, so `update` is not supported.
 *
 * @since %CURRENT_VERSION%
 */
interface VerdictRepository : EntityRepository<VerdictData, VerdictId, Verdict>

/**
 * Persistence port for [Task] entities.
 *
 * @since %CURRENT_VERSION%
 */
interface TaskRepository : EntityRepository<TaskData, TaskId, Task>

/**
 * Persistence port for [Test] entities. The file is fixed on creation: `update` with another file
 * throws [UnsupportedOperationException], a new version is saved as a new test in the same version bucket.
 *
 * @since %CURRENT_VERSION%
 */
interface TestRepository : EntityRepository<TestData, TestId, Test> {

    /**
     * Finds the latest version in [versionBucket], ordered by creation time and then by id, both descending.
     * Metadata updates do not change this order.
     *
     * @param versionBucket the resource version chain to search.
     * @return the latest version, or `null` if the chain has no versions.
     * @since %CURRENT_VERSION%
     */
    fun findLatestByVersionBucket(versionBucket: VersionBucket): Test?
}
