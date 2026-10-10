package tech.testsys.infra.database.internal.jpa

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import tech.testsys.infra.database.DatabaseIntegrationTests
import java.sql.SQLException

class SchemaConstraintsTests : DatabaseIntegrationTests() {
    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @BeforeEach
    fun seedReferencedRows() {
        val prerequisites = rows.filterKeys { it != "ts_multiple_role_to_user" }
        jdbc.batchUpdate(*prerequisites.map { (table, values) -> insertSql(table, values) }.toTypedArray())
        insertExtra("ts_user", mapOf("id" to "2", "email" to "'other@test'", "access_token" to "'other'"))
        insertExtra("ts_community_invite", mapOf("id" to "2", "code" to "'developer'", "role" to "'DEVELOPER'"))
        jdbc.execute(
            "INSERT INTO ts_community (id, name, description, owner_id, manager_invite_id, developer_invite_id) " +
                "VALUES (1, 'community', '', 1, 1, 2)",
        )
        insertExtra("ts_multiple_role_to_user", emptyMap())
        insertExtra("ts_task_content", mapOf("id" to "2"))
        insertExtra("ts_task_content", mapOf("id" to "3"))
        insertExtra("ts_file_data", mapOf("id" to "2"))
        insertExtra("ts_task_validation_request", mapOf("id" to "2"))
    }

    @ParameterizedTest(name = "{0}: {1} {2}")
    @MethodSource("acceptedRows")
    fun `should accept valid rows on insert and update`(table: String, mode: String, values: Map<String, String>) {
        val changed = writeRow(table, mode, values)

        assertEquals(1, changed)
    }

    @ParameterizedTest(name = "{0}: {1} {2}")
    @MethodSource("rejectedRows")
    fun `should reject invalid rows on insert and update`(table: String, mode: String, values: Map<String, String>, constraint: String) {
        val error = assertThrows<DataIntegrityViolationException> { writeRow(table, mode, values) }

        val cause = error.mostSpecificCause
        assertTrue(cause is SQLException)
        assertEquals("23514", (cause as? SQLException)?.sqlState)
        assertTrue(cause.message.orEmpty().contains(constraint), cause.message)
    }

    @ParameterizedTest
    @CsvSource("ts_task,name,255", "ts_contest,name,255", "ts_test,name,255", "ts_exercise,name,255", "ts_file_data,uploaded_file_name,512")
    fun `should store non BMP names at the PostgreSQL column limit`(table: String, column: String, limit: Int) {
        val name = "😀".repeat(limit)

        val changed = jdbc.update("UPDATE $table SET $column = ? WHERE id = 1", name)

        assertEquals(1, changed)
        assertEquals(name, jdbc.queryForObject("SELECT $column FROM $table WHERE id = 1", String::class.java))
    }

    @ParameterizedTest
    @CsvSource("ts_task,name,256", "ts_contest,name,256", "ts_test,name,256", "ts_exercise,name,256", "ts_file_data,uploaded_file_name,513")
    fun `should reject non BMP names over the PostgreSQL column limit`(table: String, column: String, length: Int) {
        val name = "😀".repeat(length)

        val error =
            assertThrows<DataIntegrityViolationException> { jdbc.update("UPDATE $table SET $column = ? WHERE id = 1", name) }

        assertEquals("22001", (error.mostSpecificCause as? SQLException)?.sqlState)
    }

    private fun insertExtra(table: String, values: Map<String, String>) {
        jdbc.execute(insertSql(table, rows.getValue(table) + values))
    }

    private fun writeRow(table: String, mode: String, values: Map<String, String>): Int {
        val original = rows.getValue(table)
        return when (mode) {
            "insert" -> {
                if (table == "ts_task") jdbc.update("UPDATE ts_task SET wip_content_id = 3 WHERE id = 1")
                if (table == "ts_diagnostic_report") jdbc.update("UPDATE ts_diagnostic_report SET position = 10 WHERE id = 1")
                val identity = when (table) {
                    "ts_file_data", "ts_task_validation_request" -> mapOf("id" to "3")
                    "ts_solution", "ts_exercise", "ts_test" -> mapOf("id" to "2", "file_data_id" to "2")
                    "ts_user" -> mapOf("id" to "3", "email" to "'new@test'", "access_token" to "'new'")
                    "ts_single_role_to_user", "ts_multiple_role_to_user" -> mapOf("user_id" to "2")
                    "ts_submission_to_task_validation_request" -> mapOf("request_id" to "2")
                    "ts_registration_request" -> mapOf("id" to "2", "email" to "'new@test'")
                    "ts_email_change_request" -> mapOf("id" to "2", "user_id" to "2")
                    "ts_class_invite", "ts_community_invite" -> mapOf("id" to "3", "code" to "'new-code'")
                    "ts_diagnostic_report" -> mapOf("id" to "2", "position" to "1")
                    else -> mapOf("id" to "2")
                }
                jdbc.update(insertSql(table, original + identity + values))
            }
            "update" -> {
                val key = when (table) {
                    "ts_single_role_to_user", "ts_multiple_role_to_user" -> "user_id"
                    "ts_submission_to_task_validation_request" -> "request_id"
                    else -> "id"
                }
                val assignments = values.entries.joinToString { (column, value) -> "$column = $value" }
                jdbc.update("UPDATE $table SET $assignments WHERE $key = 1")
            }
            else -> error("Unknown SQL operation $mode")
        }
    }

    private data class RowCase(val table: String, val values: Map<String, String>, val isAccepted: Boolean, val constraint: String)

    companion object {
        private const val TIME = "'2026-01-01T00:00:00Z'"
        private const val OTHER_TIME = "'2026-01-02T00:00:00Z'"
        private val rows = linkedMapOf(
            "ts_user" to mapOf(
                "id" to "1",
                "name" to "'user'",
                "access_token" to "'token'",
                "access_token_hash_algorithm" to "'IDENTITY'",
                "type" to "'MULTIPLE_ROLE'",
                "email" to "'user@test'",
            ),
            "ts_file_data" to mapOf(
                "id" to "1",
                "uploaded_file_name" to "'file'",
                "stored_file_name" to "'key'",
                "content_hash" to "'hash'",
            ),
            "ts_trik_studio_version" to mapOf(
                "id" to "1",
                "tag" to "'version'",
            ),
            "ts_solution" to mapOf(
                "id" to "1",
                "file_data_id" to "1",
                "language" to "'PYTHON'",
            ),
            "ts_exercise" to mapOf(
                "id" to "1",
                "file_data_id" to "1",
                "name" to "'exercise'",
                "description" to "''",
                "version_bucket" to "'00000000-0000-0000-0000-000000000001'",
                "language" to "'PYTHON'",
            ),
            "ts_test" to mapOf(
                "id" to "1",
                "file_data_id" to "1",
                "name" to "'polygon'",
                "description" to "''",
                "version_bucket" to "'00000000-0000-0000-0000-000000000002'",
            ),
            "ts_task_content" to mapOf(
                "id" to "1",
            ),
            "ts_task" to mapOf(
                "id" to "1",
                "name" to "'task'",
                "description" to "''",
                "owner_id" to "1",
                "status" to "'NEW'",
                "wip_content_id" to "1",
                "committed_content_id" to "NULL",
            ),
            "ts_contest" to mapOf(
                "id" to "1",
                "name" to "'contest'",
                "description" to "''",
                "owner_id" to "1",
                "trik_studio_version_id" to "1",
            ),
            "ts_submission" to mapOf(
                "id" to "1",
                "author_id" to "1",
                "solution_id" to "1",
                "task_id" to "1",
                "status" to "'QUEUED'",
                "kind" to "'GRADING'",
                "grading_contest_id" to "1",
                "trik_studio_version_id" to "NULL",
                "grading_result" to "NULL",
                "grading_verdict_id" to "NULL",
                "grading_error_description" to "NULL",
            ),
            "ts_verdict" to mapOf(
                "id" to "1",
                "task_id" to "1",
                "submission_id" to "1",
            ),
            "ts_task_validation_request" to mapOf(
                "id" to "1",
                "task_id" to "1",
                "requested_by_id" to "1",
                "execution" to "'PENDING_DIAGNOSTICS'",
                "are_diagnostics_complete" to "false",
                "are_submissions_created" to "false",
                "completed_at" to "NULL",
                "failure_description" to "NULL",
                "failure_occurred_at" to "NULL",
            ),
            "ts_test_diagnostic_result" to mapOf(
                "request_id" to "1",
                "test_id" to "1",
            ),
            "ts_diagnostic_report" to mapOf(
                "id" to "1",
                "request_id" to "1",
                "test_id" to "1",
                "position" to "0",
                "severity" to "'INFO'",
                "reason" to "'MissingTimeLimit'",
                "parameters" to "''",
            ),
            "ts_submission_to_task_validation_request" to mapOf(
                "submission_id" to "1",
                "request_id" to "1",
                "position" to "0",
                "failure" to "NULL",
                "actual_score" to "NULL",
            ),
            "ts_single_role_to_user" to mapOf(
                "single_role" to "'PARTICIPANT'",
                "user_id" to "1",
            ),
            "ts_multiple_role_to_user" to mapOf(
                "community_id" to "1",
                "multiple_role" to "'DEVELOPER'",
                "user_id" to "1",
            ),
            "ts_class_invite" to mapOf(
                "id" to "1",
                "code" to "'class'",
                "code_hash_algorithm" to "'IDENTITY'",
                "expires_at" to TIME,
            ),
            "ts_community_invite" to mapOf(
                "id" to "1",
                "code" to "'community'",
                "code_hash_algorithm" to "'IDENTITY'",
                "role" to "'MANAGER'",
                "expires_at" to TIME,
            ),
            "ts_registration_request" to mapOf(
                "id" to "1",
                "email" to "'registration@test'",
                "confirmation_code" to "'code'",
                "expires_at" to TIME,
                "attempts_left" to "3",
            ),
            "ts_email_change_request" to mapOf(
                "id" to "1",
                "user_id" to "1",
                "email" to "'change@test'",
                "confirmation_code" to "'code'",
                "expires_at" to TIME,
                "attempts_left" to "3",
            ),
            "ts_judgment_order" to mapOf(
                "id" to "1",
                "judge_id" to "1",
                "submission_id" to "1",
                "score" to "0",
                "reason" to "''",
            ),
        )

        private fun insertSql(table: String, values: Map<String, String>): String {
            val row = mapOf("created_at" to TIME, "updated_at" to TIME, "version" to "0") + values
            return "INSERT INTO $table (${row.keys.joinToString()}) VALUES (${row.values.joinToString()})"
        }

        @JvmStatic
        fun acceptedRows(): List<Arguments> = cases().filter { it.isAccepted }.flatMap { row ->
            listOf("insert", "update").map { mode -> Arguments.of(row.table, mode, row.values) }
        }

        @JvmStatic
        fun rejectedRows(): List<Arguments> = cases().filterNot { it.isAccepted }.flatMap { row ->
            listOf("insert", "update").map { mode -> Arguments.of(row.table, mode, row.values, row.constraint) }
        }

        private fun cases(): List<RowCase> = submissionStates() + submissionKinds() + taskStates() + validationStates() +
            authorFailures() + scalarConstraints()

        private fun submissionStates(): List<RowCase> = buildList {
            val expected = listOf(
                listOf("QUEUED", "NULL", "NULL", "NULL"),
                listOf("IN_PROGRESS", "NULL", "NULL", "NULL"),
                listOf("GRADED", "'SUCCESS'", "1", "NULL"),
                listOf("GRADED", "'GRADING_ERROR'", "NULL", "'failure'"),
                listOf("GRADED", "'TIMEOUT'", "NULL", "NULL"),
            )
            for (status in listOf("QUEUED", "IN_PROGRESS", "GRADED", "UNKNOWN")) {
                for (result in listOf("NULL", "'SUCCESS'", "'GRADING_ERROR'", "'TIMEOUT'", "'UNKNOWN'")) {
                    for (verdict in listOf("NULL", "1")) {
                        for (description in listOf("NULL", "'failure'")) {
                            add(
                                RowCase(
                                    table = "ts_submission",
                                    values = mapOf(
                                        "status" to "'$status'",
                                        "grading_result" to result,
                                        "grading_verdict_id" to verdict,
                                        "grading_error_description" to description,
                                    ),
                                    isAccepted = listOf(status, result, verdict, description) in expected,
                                    constraint = "ck_ts_submission_status_result",
                                ),
                            )
                        }
                    }
                }
            }
        }

        private fun submissionKinds(): List<RowCase> = buildList {
            val expected = listOf(Triple("GRADING", "1", "NULL"), Triple("DEVELOPER_SOLUTION_TEST", "NULL", "1"))
            for (kind in listOf("GRADING", "DEVELOPER_SOLUTION_TEST", "UNKNOWN")) {
                for (contest in listOf("NULL", "1")) {
                    for (version in listOf("NULL", "1")) {
                        add(
                            RowCase(
                                table = "ts_submission",
                                values = mapOf("kind" to "'$kind'", "grading_contest_id" to contest, "trik_studio_version_id" to version),
                                isAccepted = Triple(kind, contest, version) in expected,
                                constraint = "ck_ts_submission_kind_context",
                            ),
                        )
                    }
                }
            }
        }

        private fun taskStates(): List<RowCase> = buildList {
            for (state in listOf("NEW", "UNCOMMITTED", "COMMITTED", "UNKNOWN")) {
                for (committed in listOf("NULL", "1", "2")) {
                    add(
                        RowCase(
                            table = "ts_task",
                            values = mapOf("status" to "'$state'", "committed_content_id" to committed),
                            isAccepted = (state to committed) in listOf("NEW" to "NULL", "UNCOMMITTED" to "2", "COMMITTED" to "1"),
                            constraint = "ck_ts_task_status_content",
                        ),
                    )
                }
            }
        }

        private fun validationStates(): List<RowCase> = buildList {
            val expected = listOf(
                listOf("PENDING_DIAGNOSTICS", "false", "false", "NULL"),
                listOf("AWAITING_SUBMISSIONS", "true", "false", "NULL"),
                listOf("STOPPED_BY_DIAGNOSTICS", "true", "false", TIME),
                listOf("SUBMISSIONS_CREATED", "true", "true", "NULL"),
                listOf("COMPLETED", "true", "true", TIME),
            )
            for (state in expected.map { it.first() } + "UNKNOWN") {
                for (diagnostics in listOf("false", "true")) {
                    for (submissions in listOf("false", "true")) {
                        for (completed in listOf("NULL", TIME)) {
                            add(
                                RowCase(
                                    table = "ts_task_validation_request",
                                    values = validationValues(
                                        state = state,
                                        diagnostics = diagnostics,
                                        submissions = submissions,
                                        completed = completed,
                                    ),
                                    isAccepted = listOf(state, diagnostics, submissions, completed) in expected,
                                    constraint = "ck_ts_task_validation_request_execution_state",
                                ),
                            )
                        }
                    }
                }
            }
            addAll(technicalFailures())
            for (state in expected) {
                for (failure in listOf(mapOf("failure_description" to "'failure'"), mapOf("failure_occurred_at" to TIME))) {
                    add(
                        RowCase(
                            table = "ts_task_validation_request",
                            values = validationValues(
                                state = state[0],
                                diagnostics = state[1],
                                submissions = state[2],
                                completed = state[3],
                            ) + failure,
                            isAccepted = false,
                            constraint = "ck_ts_task_validation_request_execution_state",
                        ),
                    )
                }
            }
        }

        private fun technicalFailures(): List<RowCase> = buildList {
            for (diagnostics in listOf("false", "true")) {
                for (submissions in listOf("false", "true")) {
                    for (completed in listOf("NULL", TIME, OTHER_TIME)) {
                        for (failureTime in listOf("NULL", TIME)) {
                            for (description in listOf("NULL", "'failure'")) {
                                add(
                                    RowCase(
                                        table = "ts_task_validation_request",
                                        values = validationValues(
                                            state = "TECHNICAL_FAILURE",
                                            diagnostics = diagnostics,
                                            submissions = submissions,
                                            completed = completed,
                                        ) + mapOf(
                                            "failure_description" to description,
                                            "failure_occurred_at" to failureTime,
                                        ),
                                        isAccepted = (diagnostics to submissions) in listOf(
                                            "false" to "false",
                                            "true" to "false",
                                            "true" to "true",
                                        ) && completed == TIME && failureTime == TIME && description != "NULL",
                                        constraint = "ck_ts_task_validation_request_execution_state",
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }

        private fun validationValues(state: String, diagnostics: String, submissions: String, completed: String): Map<String, String> =
            mapOf(
                "execution" to "'$state'",
                "are_diagnostics_complete" to diagnostics,
                "are_submissions_created" to submissions,
                "completed_at" to completed,
            )

        private fun authorFailures(): List<RowCase> = buildList {
            for (failure in listOf("NULL", "'SCORE_MISMATCH'", "'GRADING_FAILED'", "'UNKNOWN'")) {
                for (score in listOf("NULL", "-1", "0", "1")) {
                    add(
                        RowCase(
                            table = "ts_submission_to_task_validation_request",
                            values = mapOf("failure" to failure, "actual_score" to score),
                            isAccepted = (failure == "'SCORE_MISMATCH'" && score != "NULL") ||
                                (failure in listOf("NULL", "'GRADING_FAILED'") && score == "NULL"),
                            constraint = "ck_ts_submission_to_task_validation_request_failure_score",
                        ),
                    )
                }
            }
        }

        private fun scalarConstraints(): List<RowCase> = buildList {
            addAll(enumCases("ts_user", "access_token_hash_algorithm", listOf("IDENTITY")))
            addAll(enumCases("ts_single_role_to_user", "single_role", listOf("PARTICIPANT", "OBSERVER", "SUPERVISOR")))
            addAll(
                enumCases("ts_multiple_role_to_user", "multiple_role", listOf("DEVELOPER", "STUDENT", "ADMINISTRATOR", "JUDGE", "MANAGER")),
            )
            addAll(enumCases("ts_solution", "language", listOf("PYTHON", "JAVA_SCRIPT", "VISUAL_LANGUAGE")))
            addAll(enumCases("ts_exercise", "language", listOf("PYTHON", "JAVA_SCRIPT", "VISUAL_LANGUAGE")))
            addAll(enumCases("ts_class_invite", "code_hash_algorithm", listOf("IDENTITY")))
            addAll(enumCases("ts_community_invite", "code_hash_algorithm", listOf("IDENTITY")))
            addAll(enumCases("ts_community_invite", "role", listOf("MANAGER", "DEVELOPER")))
            addAll(enumCases("ts_diagnostic_report", "severity", listOf("INFO", "WARNING", "ERROR")))
            addAll(
                enumCases(
                    table = "ts_diagnostic_report",
                    column = "reason",
                    values = listOf(
                        "UnknownElement", "MalformedXml", "MissingChild", "MissingAttribute", "InvalidAttributeValue",
                        "InvalidChildCount", "MissingTimeLimit", "MultipleTimeLimits", "NegativeTimeLimit",
                        "ExcessiveTimeLimit", "InvalidEventId", "MissingScoreOutput",
                    ),
                ),
            )
            val nonnegativeColumns = listOf(
                "ts_registration_request" to "attempts_left",
                "ts_email_change_request" to "attempts_left",
                "ts_judgment_order" to "score",
                "ts_submission_to_task_validation_request" to "position",
                "ts_diagnostic_report" to "position",
            )
            for ((table, column) in nonnegativeColumns) {
                for (value in listOf("-1", "0", "1")) {
                    add(
                        RowCase(
                            table = table,
                            values = mapOf(column to value),
                            isAccepted = value != "-1",
                            constraint = "ck_${table}_$column",
                        ),
                    )
                }
            }
            for (type in listOf("MULTIPLE_ROLE", "SINGLE_ROLE", "UNKNOWN")) {
                for (email in listOf("NULL", "'new@test'")) {
                    add(
                        RowCase(
                            table = "ts_user",
                            values = mapOf("type" to "'$type'", "email" to email),
                            isAccepted = (type to email) in listOf("MULTIPLE_ROLE" to "'new@test'", "SINGLE_ROLE" to "NULL"),
                            constraint = "ck_ts_user_type_email",
                        ),
                    )
                }
            }
        }

        private fun enumCases(table: String, column: String, values: List<String>): List<RowCase> = (values + "UNKNOWN").map { value ->
            RowCase(table = table, values = mapOf(column to "'$value'"), isAccepted = value in values, constraint = "ck_${table}_$column")
        }
    }
}
