// Placeholder for the operations class listed in testsys-operation/README.md, kept until its first feature.
@file:Suppress("EmptyKotlinFile")

package tech.testsys.operation.user

import tech.testsys.domain.contract.persistence.ObserverContestFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.SingleRoleUser
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.error.CompetitionAccessDeniedError
import tech.testsys.operation.error.CompetitionNotExistsError
import tech.testsys.operation.error.DownloadObserverResultError
import tech.testsys.operation.error.MissedObserverRoleError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.ViewObserverContestsError
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.ensure
import tech.testsys.operation.error.operation

/**
 * Operations performed by observers for their assigned contests and competitions containing them.
 *
 * @since %CURRENT_VERSION%
 */
@OptIn(InternalOperationsApi::class)
class ObserverOperations(
    private val contestRepository: ContestRepository,
    private val competitionRepository: CompetitionRepository,
) {

    /**
     * Returns a unique page of contests assigned to [user], applying [filter] and [pagination] without recording entry.
     * Future and completed contests retain their time limits; technical storage exceptions propagate.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.single.observer.viewContests")
    fun viewContests(
        user: SingleRoleUser,
        pagination: Pagination,
        filter: ObserverContestFilter = ObserverContestFilter(),
    ): OperationResult<Page<Contest>, ViewObserverContestsError> = operation<Page<Contest>, ViewObserverContestsError> {
        ensure(user is Observer, MissedObserverRoleError)
        return contestRepository.findAvailableToObserver(
            contestIds = user.data.contests.ids.toSet(),
            pagination = pagination,
            filter = filter,
        ).asSuccess()
    }

    /**
     * Returns a CSV file for [competitionId] limited to its contests assigned to [user], without changing stored data.
     * The file is currently empty; technical storage exceptions propagate.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.single.observer.downloadResult")
    fun downloadResult(user: SingleRoleUser, competitionId: CompetitionId): OperationResult<FileData, DownloadObserverResultError> =
        operation<FileData, DownloadObserverResultError> {
            ensure(user is Observer, MissedObserverRoleError)
            val competition = competitionRepository.findById(competitionId)
            ensure(competition != null) { CompetitionNotExistsError(competitionId) }
            val assigned = user.data.contests.ids.toSet()
            val observedContests = competition.data.contests.ids.filter { contestId -> contestId in assigned }.toSet()
            ensure(observedContests.isNotEmpty()) { CompetitionAccessDeniedError(competitionId) }
            return generateResultCsv(competition, observedContests).asSuccess()
        }

    /**
     * Builds the results file of [competition] restricted to [observedContests]; content generation is not defined yet.
     */
    @Suppress("UnusedParameter")
    private fun generateResultCsv(competition: Competition, observedContests: Set<ContestId>): FileData = FileData(
        uploadedFilename = "competition-${competition.id.value}-results.csv",
        content = ByteArray(0),
    )
}
