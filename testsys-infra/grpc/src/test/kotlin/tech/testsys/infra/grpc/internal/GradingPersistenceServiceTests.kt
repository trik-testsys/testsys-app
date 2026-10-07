@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.internal

import com.google.protobuf.ByteString
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.builder.api.task
import tech.testsys.domain.builder.api.test
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.RecordingId
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.VersionBucket
import java.time.Instant
import java.util.UUID
import tech.testsys.domain.model.task.Test as Polygon
import trik.testsys.grading.GradingNodeOuterClass as Proto

class GradingPersistenceServiceTests {
    @Nested
    inner class PrepareTests {
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
        fun `should select WIP polygons for a developer test`() {
            val repository = RepositoryFixture()
            every { repository.tasks.load(repository.initial.data.task) } returns editedTask()
            every { repository.tests.load(any<LazyEntityList<TestId, Polygon>>()) } returns listOf(
                test {
                    id = 5
                    createdAt = Instant.EPOCH
                    data {
                        name = "WIP"
                        description = ""
                        versionBucket = VersionBucket(UUID(0, 0))
                        file("world.xml", "wip-world".toByteArray())
                    }
                },
            )

            val prepared = repository.persistence.prepare(repository.initial, shouldRecordVideo = true)

            assertEquals(listOf(TestId(5)), prepared.testIds)
            assertEquals("5", prepared.message.task.fieldsList.single().name)
            assertEquals("2025.1", prepared.message.options.dockerImage)
        }

        @Test
        fun `should select committed polygons for a developer test without WIP`() {
            val repository = RepositoryFixture()
            every { repository.tasks.load(repository.initial.data.task) } returns task {
                id = 3
                createdAt = Instant.EPOCH
                data {
                    owner(1)
                    name = "Task"
                    description = ""
                    content.committed {
                        tests(listOf(4))
                        exercises(listOf(6))
                        statement(7)
                    }
                }
            }

            val prepared = repository.persistence.prepare(repository.initial, shouldRecordVideo = true)

            assertEquals(listOf(TestId(4)), prepared.testIds)
            assertEquals("4", prepared.message.task.fieldsList.single().name)
            assertEquals("world", prepared.message.task.fieldsList.single().content.toStringUtf8())
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
            assertArrayEquals(content.toByteArray(), repository.savedLogs.single().file.content)
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
            assertArrayEquals(byteArrayOf(0, 1, -1, 42), saved.file.content)
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
            verify(exactly = 1) { repository.submissions.update(any<tech.testsys.domain.model.task.Submission>()) }
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
}
