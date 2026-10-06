package tech.testsys.infra.database.internal.mapping.task

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.builder.api.submissionData
import tech.testsys.domain.model.task.SubmissionKind
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionKindJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.task.SubmissionStatusJpaEnum
import tech.testsys.infra.database.internal.mapping.EntityMappingTests
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

@InternalDatabaseApi
class SubmissionMappingTests : EntityMappingTests<SubmissionMapping>() {

    override val mapping = SubmissionMapping

    @Test
    fun `should restore developer solution test version without a contest`() {
        val row = row(SubmissionKindJpaEnum.DEVELOPER_SOLUTION_TEST, versionId = 3)

        val restored = mapping.toDomain(row, MultipleRoleUserId(1), TrikStudioVersion("3.0.0"), emptyList())

        assertEquals(
            TrikStudioVersion("3.0.0"),
            assertIs<SubmissionKind.DeveloperSolutionTest>(restored.data.kind).trikStudioVersion,
        )
    }

    @ParameterizedTest
    @CsvSource("false,false", "false,true", "true,false")
    fun `should reject developer solution test if version id or resolved version is missing`(
        hasVersionId: Boolean,
        hasResolvedVersion: Boolean,
    ) {
        val row = row(SubmissionKindJpaEnum.DEVELOPER_SOLUTION_TEST, versionId = 3L.takeIf { hasVersionId })
        val version = TrikStudioVersion("3.0.0").takeIf { hasResolvedVersion }

        assertFailsWith<IllegalArgumentException> {
            mapping.toDomain(row, MultipleRoleUserId(1), version, emptyList())
        }
    }

    @Test
    fun `should reject developer solution test if contest is set`() {
        val row = row(SubmissionKindJpaEnum.DEVELOPER_SOLUTION_TEST, versionId = 3, contestId = 4)

        assertFailsWith<IllegalArgumentException> {
            mapping.toDomain(row, MultipleRoleUserId(1), TrikStudioVersion("3.0.0"), emptyList())
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = [3])
    fun `should restore grading contest with absent or legacy version column`(versionId: Long?) {
        val row = row(SubmissionKindJpaEnum.GRADING, versionId = versionId, contestId = 4)

        val restored = mapping.toDomain(row, MultipleRoleUserId(1), null, emptyList())

        assertEquals(4L, assertIs<SubmissionKind.Grading>(restored.data.kind).contest.id.value)
    }

    @Test
    fun `should reject grading if contest is missing`() {
        val row = row(SubmissionKindJpaEnum.GRADING)

        assertFailsWith<IllegalStateException> {
            mapping.toDomain(row, MultipleRoleUserId(1), null, emptyList())
        }
    }

    @Test
    fun `should encode developer solution test with version id and no contest`() {
        val data = submissionData {
            author(1)
            solution(2)
            task(3)
            status.queued()
            kind.developerSolutionTest { trikStudioVersion("3.0.0") }
        }

        val row = mapping.toJpaEntity(data, trikStudioVersionId = 7)

        assertEquals(SubmissionKindJpaEnum.DEVELOPER_SOLUTION_TEST, row.kind)
        assertEquals(7L, row.trikStudioVersionId)
        assertNull(row.gradingContestId)
    }

    @Test
    fun `should reject encoding developer solution test if version id is missing`() {
        val data = submissionData {
            author(1)
            solution(2)
            task(3)
            status.queued()
            kind.developerSolutionTest { trikStudioVersion("3.0.0") }
        }

        assertFailsWith<IllegalArgumentException> { mapping.toJpaEntity(data, trikStudioVersionId = null) }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = [7])
    fun `should encode grading with contest and no separate version`(versionId: Long?) {
        val data = submissionData {
            author(1)
            solution(2)
            task(3)
            status.queued()
            kind.grading { contest(4) }
        }

        val row = mapping.toJpaEntity(data, trikStudioVersionId = versionId)

        assertEquals(SubmissionKindJpaEnum.GRADING, row.kind)
        assertEquals(4L, row.gradingContestId)
        assertNull(row.trikStudioVersionId)
    }

    private fun row(kind: SubmissionKindJpaEnum, versionId: Long? = null, contestId: Long? = null) = SubmissionJpaEntity(
        id = 10,
        authorId = 1,
        solutionId = 2,
        taskId = 3,
        trikStudioVersionId = versionId,
        status = SubmissionStatusJpaEnum.QUEUED,
        gradingResult = null,
        gradingVerdictId = null,
        gradingErrorDescription = null,
        kind = kind,
        gradingContestId = contestId,
    )
}
