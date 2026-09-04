package tech.testsys.infra.database.internal.jpa

import org.hibernate.boot.model.naming.Identifier
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito.mock
import kotlin.test.assertEquals

class TestsysPhysicalNamingStrategyTest {

    private val strategy = TestsysPhysicalNamingStrategy()
    private val jdbcEnvironment = mock(JdbcEnvironment::class.java)

    @ParameterizedTest
    @CsvSource(
        "SubmissionJpaEntity, ts_submission",
        "TrikStudioVersionToTaskContentJpaEntity, ts_trik_studio_version_to_task_content",
        "UserEntity, ts_user",
        "Task, ts_task",
        "Class, ts_class",
    )
    fun `table names drop the entity suffix and get the ts prefix in snake case`(logical: String, expected: String) {
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
    fun `column names are converted to snake case`(logical: String, expected: String) {
        val physical = strategy.toPhysicalColumnName(Identifier.toIdentifier(logical), jdbcEnvironment)

        assertEquals(expected, physical.text)
    }
}
