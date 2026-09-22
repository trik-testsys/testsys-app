package tech.testsys.infra.database.internal.jpa

import io.mockk.mockk
import org.hibernate.boot.model.naming.Identifier
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.infra.database.internal.InternalDatabaseApi
import kotlin.test.assertEquals

@OptIn(InternalDatabaseApi::class)
class TestsysPhysicalNamingStrategyTests {

    private val strategy = TestsysPhysicalNamingStrategy()
    private val jdbcEnvironment = mockk<JdbcEnvironment>()

    @ParameterizedTest
    @CsvSource(
        "SubmissionJpaEntity, ts_submission",
        "TrikStudioVersionToTaskContentJpaEntity, ts_trik_studio_version_to_task_content",
        "UserEntity, ts_user",
        "Task, ts_task",
        "Class, ts_class",
    )
    fun `should drop the entity suffix and add the ts prefix in snake case for table names`(logical: String, expected: String) {
        val physical = strategy.toPhysicalTableName(Identifier.toIdentifier(logical), jdbcEnvironment)

        assertEquals(expected, physical.text)
    }

    @ParameterizedTest
    @CsvSource(
        "taskId, task_id",
        "gradingErrorDescription, grading_error_description",
        "id, id",
        "contestDurationMillis, contest_duration_millis",
    )
    fun `should convert column names to snake case`(logical: String, expected: String) {
        val physical = strategy.toPhysicalColumnName(Identifier.toIdentifier(logical), jdbcEnvironment)

        assertEquals(expected, physical.text)
    }
}
