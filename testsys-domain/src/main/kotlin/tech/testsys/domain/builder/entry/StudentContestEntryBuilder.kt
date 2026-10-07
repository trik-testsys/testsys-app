package tech.testsys.domain.builder.entry

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DomainEntityWithDataBuilder
import tech.testsys.domain.builder.util.applyVersion
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.entry.StudentContestEntry
import tech.testsys.domain.model.entry.StudentContestEntryData
import tech.testsys.domain.model.entry.StudentContestEntryId
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUserId
import java.time.Instant

/**
 * Builder of [StudentContestEntryData]. Required: [user], [studyClass], [contest], [enteredAt].
 *
 * @property user the context identifier, or null if not set yet.
 * @property studyClass the context identifier, or null if not set yet.
 * @property contest the context identifier, or null if not set yet.
 * @property enteredAt the first entry moment, or null if not set yet.
 * @since %CURRENT_VERSION%
 */
class StudentContestEntryDataBuilder : Builder<StudentContestEntryData> {

    var user: MultipleRoleUserId? = null

    var studyClass: ClassId? = null

    var contest: ContestId? = null

    var enteredAt: Instant? = null

    /**
     * Sets [user] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun user(user: Long) {
        this.user = MultipleRoleUserId(user)
    }

    /**
     * Sets [studyClass] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun studyClass(studyClass: Long) {
        this.studyClass = ClassId(studyClass)
    }

    /**
     * Sets [contest] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun contest(contest: Long) {
        this.contest = ContestId(contest)
    }

    override fun build(): StudentContestEntryData = StudentContestEntryData(
        user = requireField(user) { ::user }.lazify(),
        studyClass = requireField(studyClass) { ::studyClass }.lazify(),
        contest = requireField(contest) { ::contest }.lazify(),
        enteredAt = requireField(enteredAt) { ::enteredAt },
    )
}

/**
 * Builder of [StudentContestEntry] entities. Required: [id], [createdAt], [data].
 *
 * @since %CURRENT_VERSION%
 */
class StudentContestEntryBuilder :
    DomainEntityWithDataBuilder<StudentContestEntry, StudentContestEntryData, StudentContestEntryDataBuilder>() {

    override fun dataBuilder() = StudentContestEntryDataBuilder()

    override fun build(): StudentContestEntry = StudentContestEntry(
        id = StudentContestEntryId(requireField(id) { ::id }),
        createdAt = requireField(createdAt) { ::createdAt },
        data = requireField(data) { ::data },
    ).applyVersion(version)
}
