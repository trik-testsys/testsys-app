@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.internal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.domain.contract.GradingNodeStatus
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TestId

class GradingFunctionsTests {
    @Nested
    inner class ParseTests {
        @ParameterizedTest
        @ValueSource(
            strings = [
                "[]",
                "[{\"level\":\"error\",\"message\":\"Набрано баллов: 100\"}]",
                "[{\"level\":\"info\",\"message\":\"failed\"}]",
            ],
        )
        fun `should award zero for normal failures or missing scores`(content: String) {
            val result = JsonLogParser().parse(content.toByteArray())

            assertEquals(Score(0), result)
        }

        @ParameterizedTest
        @ValueSource(strings = ["broken", "{}", "[null]", "[] []", "[{\"level\":\"info\",\"message\":\"Набрано баллов: 2147483648\"}]"])
        fun `should reject malformed logs and overflowing scores`(content: String) {
            val result = JsonLogParser().parse(content.toByteArray())

            assertEquals(null, result)
        }

        @ParameterizedTest
        @Tag("regression")
        @ValueSource(
            strings = [
                """[{"level":"info","message":"Набрано баллов: 2147483648"},{"level":"error","message":"failed"}]""",
                """[{"level":"error","message":"failed"},{"level":"info","message":"Набрано баллов: 2147483648"}]""",
            ],
        )
        fun `should reject overflowing info scores when an error is present`(content: String) {
            val result = JsonLogParser().parse(content.toByteArray())

            assertEquals(null, result)
        }

        @Test
        fun `should take the maximum info score and ignore unknown properties`() {
            val content = """[{"level":"info","message":"Набрано баллов: -20","extra":1},{"level":"info","message":"Набрано баллов: -5"}]"""

            val result = JsonLogParser().parse(content.toByteArray())

            assertEquals(Score(-5), result)
        }

        @Test
        fun `should award zero when error accompanies a nonzero info score`() {
            val content = """[{"level":"info","message":"Набрано баллов: 100"},{"level":"error","message":"failed"}]"""

            val result = JsonLogParser().parse(content.toByteArray())

            assertEquals(Score(0), result)
        }
    }

    @Nested
    inner class CheckResultTests {
        @Test
        fun `should reject duplicate or missing polygons`() {
            val result = checkResult(
                result = result(fields = listOf(field(), field())),
                expectedId = SubmissionId(42),
                expectedTests = listOf(TestId(4), TestId(5)),
                parser = JsonLogParser(),
            )

            assertInstanceOf(CheckedResult.Failure::class.java, result)
        }

        @Test
        fun `should reject a foreign submission id`() {
            val result = checkResult(result(id = 43), SubmissionId(42), listOf(TestId(4)), JsonLogParser())

            assertInstanceOf(CheckedResult.Failure::class.java, result)
        }
    }

    @Nested
    inner class SelectNodeTests {
        @Test
        fun `should select available capacity using both local and reported load`() {
            val first = GradingNodeAddress("first")
            val second = GradingNodeAddress("second")
            val statuses = mapOf(
                first to GradingNodeStatus.Available(queued = 0, capacity = 1),
                second to GradingNodeStatus.Available(queued = 1, capacity = 3),
            )

            val result = selectNode(statuses, mapOf(first to 1))

            assertEquals(second, result)
        }
    }
}
