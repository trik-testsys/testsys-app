@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.internal

import com.google.protobuf.ByteString
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.builder.api.developerSolutionValidationInput
import tech.testsys.domain.builder.api.task
import tech.testsys.domain.builder.api.taskValidationSnapshot
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.builder.data
import tech.testsys.domain.contract.StoredBlobRef
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.task.FileContent
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.FileStorageKind
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.RecordingId
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikStudioVersion
import java.time.Instant
import tech.testsys.domain.model.task.Test as Polygon
import trik.testsys.grading.GradingNodeOuterClass as Proto

class GradingPersistenceServiceTests {
    @Nested
    inner class PrepareTests {
        @Test
        fun `should encode stored solution bytes through the reader`() {
            val repository = RepositoryFixture()
            val loaded = repository.solutions.load(repository.initial.data.solution)
            val storedFile =
                FileData("solution.py", FileContent.Stored(StoredBlobRef("solution"), FileStorageKind.Solution))
            every { repository.solutions.load(repository.initial.data.solution) } returns loaded.withData { file(storedFile) }
            every { repository.fileContentReader.read(storedFile) } returns "stored program".toByteArray()

            val result = repository.persistence.prepare(repository.initial, shouldRecordVideo = false)

            assertEquals("stored program", result.message.pythonSubmission.file.content.toStringUtf8())
            verify(exactly = 1) { repository.fileContentReader.read(storedFile) }
        }

        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `should select immutable snapshot polygons for linked author submissions after task edits or commit`(committed: Boolean) {
            val repository = RepositoryFixture()
            val submitted = repository.initial
            val request = validationRequest()
            every { repository.validationRequests.findBySubmissionId(submitted.id) } returns request
            every { repository.tasks.load(submitted.data.task) } returns changedTask(committed)

            val prepared = repository.persistence.prepare(submitted, shouldRecordVideo = true)

            assertEquals(listOf(TestId(4)), prepared.testIds)
            assertEquals("4", prepared.message.task.fieldsList.single().name)
            assertEquals("world", prepared.message.task.fieldsList.single().content.toStringUtf8())
            assertEquals("2025.1", prepared.message.options.dockerImage)
            verify(exactly = 0) { repository.tasks.load(submitted.data.task) }
        }

        @Test
        fun `should reject a linked submission that does not match its snapshot author solution and version`() {
            val repository = RepositoryFixture()
            val request = validationRequest().withData {
                snapshot = taskValidationSnapshot {
                    tests(listOf(4))
                    developerSolutions = mutableListOf(
                        developerSolutionValidationInput {
                            developerSolution(7)
                            solution(99)
                            expectedScore = Score(5)
                        },
                    )
                    supportedTrikStudioVersions = mutableListOf(TrikStudioVersion("2025.1"))
                }
            }
            every { repository.validationRequests.findBySubmissionId(repository.initial.id) } returns request

            assertThrows(IllegalStateException::class.java) {
                repository.persistence.prepare(repository.initial, shouldRecordVideo = true)
            }

            verify(exactly = 0) { repository.submissions.update(any<Submission>()) }
        }

        @Test
        fun `should select committed polygons for a regular submission`() {
            val submitted = testSubmission().withData { kind.grading { contest(10) } }
            val repository = RepositoryFixture(submitted)
            every { repository.tasks.load(submitted.data.task) } returns editedTask()

            val prepared = repository.persistence.prepare(submitted, shouldRecordVideo = true)

            assertEquals(listOf(TestId(4)), prepared.testIds)
            assertEquals("4", prepared.message.task.fieldsList.single().name)
            assertEquals("contest-version", prepared.message.options.dockerImage)
        }

        @Test
        fun `should reject an author submission without a validation request before queueing`() {
            val repository = RepositoryFixture()
            every { repository.validationRequests.findBySubmissionId(repository.initial.id) } returns null

            val failure = assertThrows(IllegalStateException::class.java) {
                repository.persistence.prepare(repository.initial, shouldRecordVideo = true)
            }

            assertEquals("Author submission 42 has no linked validation request", failure.message)
            verify(exactly = 0) { repository.tasks.load(any<LazyEntity<TaskId, Task>>()) }
            verify(exactly = 0) { repository.tests.load(any<LazyEntityList<TestId, Polygon>>()) }
            verify(exactly = 0) { repository.submissions.update(any<Submission>()) }
        }

        @Test
        fun `should prepare a submission in one transaction`() {
            val submitted = testSubmission().withData { kind.grading { contest(10) } }
            val repository = RepositoryFixture(submitted)
            every { repository.tasks.load(submitted.data.task) } returns editedTask()
            val solution = repository.solutions.load(submitted.data.solution)
            val polygons = repository.tests.load(LazyEntityList(listOf(TestId(4))))
            val callsInTransaction = mutableListOf<Boolean>()
            every { repository.solutions.load(submitted.data.solution) } answers {
                callsInTransaction.add(repository.transactions.isActive.get())
                solution
            }
            every { repository.tests.load(any<LazyEntityList<TestId, Polygon>>()) } answers {
                callsInTransaction.add(repository.transactions.isActive.get())
                polygons
            }
            every { repository.submissions.update(any<Submission>()) } answers {
                callsInTransaction.add(repository.transactions.isActive.get())
                firstArg<Submission>().also(repository.current::set)
            }

            repository.persistence.prepare(submitted, shouldRecordVideo = true)

            assertEquals(1, repository.transactions.count.get())
            assertEquals(listOf(true, true, true), callsInTransaction)
            assertTrue(repository.hasCommitted.get())
        }

        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `should encode the configured video recording flag`(shouldRecordVideo: Boolean) {
            val repository = RepositoryFixture()

            val prepared = repository.persistence.prepare(repository.initial, shouldRecordVideo)

            assertEquals(shouldRecordVideo, prepared.message.options.recordVideo)
        }
    }

    @Nested
    inner class SaveResultTests {
        @ParameterizedTest
        @ValueSource(strings = ["logs", "video"])
        fun `should save a grading error without artifacts when an uploaded name exceeds the limit`(kind: String) {
            val repository = RepositoryFixture()
            val field = field().toBuilder().apply {
                when (kind) {
                    "logs" -> verdict = verdict.toBuilder().setName("😀".repeat(513)).build()
                    "video" -> video = verdict.toBuilder().setName("😀".repeat(513)).build()
                }
            }.build()
            val checked = checkResult(
                result = result(fields = listOf(field)),
                expectedId = repository.initial.id,
                expectedTests = listOf(TestId(4)),
                parser = JsonLogParser(),
            )

            repository.persistence.saveResult(repository.initial, checked)

            val status = assertInstanceOf(SubmissionStatus.Graded::class.java, repository.current.get().data.status)
            assertInstanceOf(GradingResult.GradingError::class.java, status.grade)
            assertEquals(0, repository.savedVerdicts.size)
            assertEquals(0, repository.savedLogs.size)
            assertEquals(0, repository.savedRecordings.size)
        }

        @Test
        @Tag("regression")
        fun `should save a grading error without artifacts when overflowing logs include an error`() {
            val repository = RepositoryFixture()
            val content = """[{"level":"info","message":"Набрано баллов: 2147483648"},{"level":"error","message":"failed"}]"""
            val video = Proto.File.newBuilder().setName("run.webm").setContent(ByteString.copyFromUtf8("video")).build()
            val checked = checkResult(
                result = result(fields = listOf(field(content = content).toBuilder().setVideo(video).build())),
                expectedId = repository.initial.id,
                expectedTests = listOf(TestId(4)),
                parser = JsonLogParser(),
            )

            repository.persistence.saveResult(repository.initial, checked)

            val status = assertInstanceOf(SubmissionStatus.Graded::class.java, repository.current.get().data.status)
            assertInstanceOf(GradingResult.GradingError::class.java, status.grade)
            assertEquals(0, repository.savedVerdicts.size)
            assertEquals(0, repository.savedLogs.size)
            assertEquals(0, repository.savedRecordings.size)
        }

        @Test
        fun `should persist zero scores together with logs for a normal solution failure`() {
            val repository = RepositoryFixture()
            val content = """[{"level":"info","message":"Набрано баллов: 100"},{"level":"error","message":"failed"}]"""
            val checked = checkResult(
                result(fields = listOf(field(content = content))),
                repository.initial.id,
                listOf(TestId(4)),
                JsonLogParser(),
            )

            repository.persistence.saveResult(repository.initial, checked)

            assertEquals(0, repository.savedVerdicts.single().testVerdicts.single().score.value)
            val bytes = assertInstanceOf(FileContent.Inline::class.java, repository.savedLogs.single().file.content).bytes
            assertArrayEquals(content.toByteArray(), bytes)
        }

        @Test
        fun `should persist video bytes and link the recording to its polygon verdict`() {
            val repository = RepositoryFixture()
            val video = Proto.File.newBuilder().setName("run.webm")
                .setContent(ByteString.copyFrom(byteArrayOf(0, 1, -1, 42))).build()
            val checked = checkResult(
                result = result(fields = listOf(field().toBuilder().setVideo(video).build())),
                expectedId = repository.initial.id,
                expectedTests = listOf(TestId(4)),
                parser = JsonLogParser(),
            )

            repository.persistence.saveResult(repository.initial, checked)

            val saved = repository.savedRecordings.single()
            assertEquals("run.webm", saved.file.uploadedFilename)
            assertArrayEquals(byteArrayOf(0, 1, -1, 42), assertInstanceOf(FileContent.Inline::class.java, saved.file.content).bytes)
            val testVerdict = repository.savedVerdicts.single().testVerdicts.single()
            assertEquals(TestId(4), testVerdict.test.id)
            assertEquals(RecordingId(6), testVerdict.recording?.id)
        }

        @Test
        fun `should save no verdict or logs for incomplete results`() {
            val repository = RepositoryFixture()
            val checked = checkResult(
                result = result(fields = emptyList()),
                expectedId = repository.initial.id,
                expectedTests = listOf(TestId(4)),
                parser = JsonLogParser(),
            )

            repository.persistence.saveResult(repository.initial, checked)

            assertEquals(0, repository.savedVerdicts.size)
            assertEquals(0, repository.savedLogs.size)
            verify(exactly = 1) { repository.submissions.update(any<Submission>()) }
        }
    }

    private fun editedTask() = task {
        id = 3
        createdAt = Instant.EPOCH
        data {
            owner(1)
            name = "Task"
            description = ""
            content.uncommitted(
                wipBuilder = { tests(listOf(5)) },
                lastCommittedBuilder = {
                    tests(listOf(4))
                    exercises(listOf(6))
                    statement(7)
                },
            )
        }
    }

    private fun changedTask(committed: Boolean) = task {
        id = 3
        createdAt = Instant.EPOCH
        data {
            owner(1)
            name = "Changed task"
            description = ""
            if (committed) {
                content.committed {
                    tests(listOf(5))
                    exercises(listOf(6))
                    statement(7)
                }
            } else {
                content.new { tests(listOf(5)) }
            }
        }
    }
}
