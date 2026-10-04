package tech.testsys.domain.model.task

import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import java.time.Instant

/**
 * Identifier of a [Test].
 *
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class TestId(
    override val value: Long,
) : DomainId

/**
 * Data of a [Test].
 *
 * @property file the uploaded TRIK Studio world model file; fixed on creation; update fails if it differs.
 * @property name the name of the test.
 * @property description the description of the test.
 * @property versionBucket the version chain shared by all versions of the same logical test;
 *   fixed on creation and ignored on update.
 * @since %CURRENT_VERSION%
 */
data class TestData(
    val file: FileData,
    val name: String,
    val description: String,
    val versionBucket: VersionBucket,
)

/**
 * A TRIK Studio world model (polygon) that solutions are run in when graded.
 *
 * @property data the data of the test.
 * @since %CURRENT_VERSION%
 */
class Test(
    id: TestId,
    createdAt: Instant,
    val data: TestData,
) : DomainEntity<TestId>(id, createdAt)
