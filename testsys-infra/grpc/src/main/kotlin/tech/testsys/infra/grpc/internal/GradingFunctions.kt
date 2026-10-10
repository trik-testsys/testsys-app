@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.internal

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.protobuf.ByteString
import tech.testsys.domain.contract.FileContentReader
import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.domain.contract.GradingNodeStatus
import tech.testsys.domain.model.TextLimits
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.Solution
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.Test
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.TrikSupportedLanguage
import trik.testsys.grading.GradingNodeOuterClass as Proto

@InternalGrpcApi
internal data class CheckedTest(val testId: TestId, val score: Score, val logs: Proto.File, val recording: Proto.File?)

@InternalGrpcApi
internal sealed interface CheckedResult {
    data class Success(val tests: List<CheckedTest>) : CheckedResult
    data class Failure(val description: String) : CheckedResult
    data object Timeout : CheckedResult
}

@InternalGrpcApi
internal fun encodeSubmission(
    id: SubmissionId,
    solution: Solution,
    tests: List<Test>,
    trikStudioVersion: TrikStudioVersion,
    shouldRecordVideo: Boolean,
    fileContentReader: FileContentReader,
): Proto.Submission {
    val file = Proto.File.newBuilder().setName(solution.data.file.uploadedFilename)
        .setContent(ByteString.copyFrom(fileContentReader.read(solution.data.file))).build()
    val builder = Proto.Submission.newBuilder().setId(id.value)
        .setTask(
            Proto.Task.newBuilder().addAllFields(
                tests.map { test ->
                    Proto.File.newBuilder().setName(test.id.value.toString())
                        .setContent(ByteString.copyFrom(fileContentReader.read(test.data.file))).build()
                },
            ),
        )
        .setOptions(
            Proto.Options.newBuilder().setDockerImage(trikStudioVersion.version).setRecordVideo(shouldRecordVideo),
        )
    when (solution.data.language) {
        TrikSupportedLanguage.Python -> builder.setPythonSubmission(Proto.PythonSubmission.newBuilder().setFile(file))
        TrikSupportedLanguage.JavaScript -> builder.setJavascriptSubmission(Proto.JavaScriptSubmission.newBuilder().setFile(file))
        TrikSupportedLanguage.VisualLanguage -> builder.setVisualLanguageSubmission(
            Proto.VisualLanguageSubmission.newBuilder().setFile(file),
        )
    }
    return builder.build()
}

@InternalGrpcApi
internal fun selectNode(
    statuses: Map<GradingNodeAddress, GradingNodeStatus>,
    outstanding: Map<GradingNodeAddress, Int>,
): GradingNodeAddress? = statuses.entries.mapNotNull { (address, status) ->
    val available = status as? GradingNodeStatus.Available ?: return@mapNotNull null
    val load = maxOf(available.queued, outstanding[address] ?: 0)
    if (available.capacity <= 0 || load >= available.capacity) {
        null
    } else {
        address to load.toDouble() / available.capacity
    }
}.minWithOrNull(compareBy<Pair<GradingNodeAddress, Double>> { entry -> entry.second }.thenBy { entry -> entry.first.target })?.first

@InternalGrpcApi
internal fun checkResult(result: Proto.Result, expectedId: SubmissionId, expectedTests: List<TestId>, parser: LogParser): CheckedResult {
    if (result.id != expectedId.value) {
        return CheckedResult.Failure("Unexpected submission id ${result.id}, expected ${expectedId.value}")
    }
    return when (result.resultCase) {
        Proto.Result.ResultCase.ERROR -> if (result.error.kind == NODE_TIMEOUT_KIND) {
            CheckedResult.Timeout
        } else {
            CheckedResult.Failure("Node error ${result.error.kind}: ${result.error.description}")
        }
        Proto.Result.ResultCase.OK -> checkFields(result.ok.resultsList, expectedTests, parser)
        Proto.Result.ResultCase.RESULT_NOT_SET, null -> CheckedResult.Failure("Node returned no result for ${result.id}")
    }
}

private fun checkFields(fields: List<Proto.FieldResult>, expectedTests: List<TestId>, parser: LogParser): CheckedResult {
    val names = fields.map { field -> field.name }
    val expectedNames = expectedTests.map { id -> id.value.toString() }
    if (expectedNames.isEmpty() || names.size != expectedNames.size || names.toSet() != expectedNames.toSet()) {
        return CheckedResult.Failure("Unexpected polygon results: $names, expected $expectedNames")
    }
    val results = fields.map { field ->
        if (!field.hasVerdict()) {
            return CheckedResult.Failure("Missing logs for polygon ${field.name}")
        }
        if (!TextLimits.isValidUploadedFilename(field.verdict.name) ||
            (field.hasVideo() && !TextLimits.isValidUploadedFilename(field.video.name))
        ) {
            return CheckedResult.Failure("File name exceeds 512 Unicode code points for polygon ${field.name}")
        }
        val score = parser.parse(field.verdict.content.toByteArray())
            ?: return CheckedResult.Failure("Invalid logs for polygon ${field.name}")
        CheckedTest(
            testId = TestId(field.name.toLong()),
            score = score,
            logs = field.verdict,
            recording = if (field.hasVideo()) field.video else null,
        )
    }
    return CheckedResult.Success(results)
}

/**
 * Pure log scoring extension for the grading adapter.
 *
 * @since %CURRENT_VERSION%
 */
@InternalGrpcApi
fun interface LogParser {
    /**
     * Determines the score from grading logs without IO or modifying the input.
     *
     * @param content the raw bytes of the node's log file.
     * @return the score, or `null` if the format or numeric value is invalid.
     * @since %CURRENT_VERSION%
     */
    fun parse(content: ByteArray): Score?
}

/** Scoring algorithm used by the previous web application, with explicit zero scores for normal failures. */
@InternalGrpcApi
internal class JsonLogParser : LogParser {
    private val mapper = ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
    private val pattern = Regex("Набрано баллов:\\s*(-?\\d+)")

    override fun parse(content: ByteArray): Score? {
        val elements = try {
            mapper.readTree(content)
        } catch (_: JsonProcessingException) {
            return null
        }
        if (elements == null || !elements.isArray || elements.any { element -> !element.isObject }) {
            return null
        }
        val scores = elements.filter { element -> element.path("level").asText() == "info" }.mapNotNull { element ->
            val match = pattern.find(element.path("message").asText()) ?: return@mapNotNull null
            match.groupValues[1].toIntOrNull() ?: return null
        }
        if (elements.any { element -> element.path("level").asText() == "error" }) {
            return Score(0)
        }
        return Score(scores.maxOrNull() ?: 0)
    }
}

private const val NODE_TIMEOUT_KIND = 4
