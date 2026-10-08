package tech.testsys.web.devapp.demo.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class DemoStateTests {
    private val initial = demoFixtures()

    @Nested
    inner class CreateParticipantsTests {
        @Test
        fun `should create selected quantity with unique codes and advance identifiers`() {
            val result = initial.createParticipants(competitionId = "competition1", count = 12)

            assertTrue(result.isSuccess)
            assertEquals(115, result.state.nextId)
            assertEquals(14, result.state.competitions.first().participantIds.size)
            assertEquals("PART-114", result.state.users.last().accessCode)
            assertEquals(result.state.users.size, result.state.users.map { user -> user.accessCode }.distinct().size)
        }

        @ParameterizedTest
        @ValueSource(ints = [0, 101])
        fun `should reject a quantity outside the demonstration limit without changing records`(count: Int) {
            val result = initial.createParticipants(competitionId = "competition1", count = count)

            assertFalse(result.isSuccess)
            assertEquals(initial, result.state)
        }

        @Test
        fun `should reject participants of a competition of another organizer`() {
            val confirmed = confirmedRegistration(DemoRole.Organizer)

            assertFalse(confirmed.createParticipants(competitionId = "competition1", count = 3).isSuccess)
        }
    }

    @Nested
    inner class RegisterTests {
        @Test
        fun `should give a new organizer an access code and no existing competitions`() {
            val confirmed = confirmedRegistration(DemoRole.Organizer)

            val organizer = confirmed.actor(DemoRole.Organizer)
            assertEquals("ACCESS-103", organizer.accessCode)
            assertTrue(confirmed.objects(organizer).competitions.isEmpty())
        }

        @ParameterizedTest
        @CsvSource("Анна, ' ANNA@example.com '", "'', new@example.com", "Новая, invalid")
        fun `should reject a duplicate email, a blank alias or an invalid email`(alias: String, email: String) {
            val result = initial.register(alias = alias, email = email, role = DemoRole.Student)

            assertFalse(result.isSuccess)
        }

        @Test
        fun `should reject a role that cannot register`() {
            val result = initial.register(alias = "Новый", email = "new@example.com", role = DemoRole.Judge)

            assertFalse(result.isSuccess)
        }

        @Test
        fun `should keep users unchanged after a wrong confirmation code`() {
            val pending = initial.register(alias = "Новая", email = "new@example.com", role = DemoRole.Student).state

            val result = pending.confirmRegistration("wrong")

            assertFalse(result.isSuccess)
            assertEquals(initial.users, result.state.users)
        }

        @Test
        fun `should give a new student no classes and no tasks`() {
            val registered = confirmedRegistration(DemoRole.Student)

            val objects = registered.objects(registered.actor(DemoRole.Student))

            assertTrue(objects.classes.isEmpty())
            assertTrue(objects.tasks.isEmpty())
        }

        @Test
        fun `should offer a new organizer the tours of the public community`() {
            val organizer = confirmedRegistration(DemoRole.Organizer)

            val tours = organizer.availableTours(organizer.actor(DemoRole.Organizer))

            assertEquals(listOf("t1"), tours.map { tour -> tour.id })
        }
    }

    @Nested
    inner class RestoreAccessTests {
        private val recovery = initial.requestRecovery("ANNA@example.com").state
        private val token = requireNotNull(recovery.recovery).token

        @Test
        fun `should replace the access code after recovery`() {
            val restored = recovery.restoreAccess(token).state

            assertFalse(restored.login("STUDENT-2026").isSuccess)
            assertTrue(restored.login("RESTORED-103").isSuccess)
        }

        @Test
        fun `should reject a recovery link used twice`() {
            val restored = recovery.restoreAccess(token).state

            assertFalse(restored.restoreAccess(token).isSuccess)
        }
    }

    @Nested
    inner class AddTourTests {
        @Test
        fun `should attach an available tour`() {
            val added = initial.addTour(competitionId = "competition1", tourId = "t2")

            assertTrue(added.isSuccess)
            assertEquals(listOf("t1", "t2"), added.state.competitions.first().tourIds)
        }

        @Test
        fun `should reject a tour that is already attached`() {
            val added = initial.addTour(competitionId = "competition1", tourId = "t2").state

            assertFalse(added.addTour(competitionId = "competition1", tourId = "t2").isSuccess)
        }

        @Test
        fun `should reject an unavailable tour`() {
            assertFalse(initial.addTour(competitionId = "competition1", tourId = "missing").isSuccess)
        }
    }

    @Nested
    inner class CreateCompetitionTests {
        @Test
        fun `should reject a blank competition name`() {
            assertFalse(initial.createCompetition(" ").isSuccess)
        }

        @Test
        fun `should create an empty competition of the organizer with a trimmed name`() {
            val competition = initial.createCompetition(" Новое ").state.competitions.last()

            assertEquals("Новое", competition.name)
            assertEquals("organizer", competition.organizerId)
            assertTrue(competition.participantIds.isEmpty())
            assertTrue(competition.tourIds.isEmpty())
        }
    }

    @Nested
    inner class ObjectsTests {
        @ParameterizedTest
        @CsvSource("Student, 2", "Judge, 4")
        internal fun `should expose the solutions of the role scope`(role: DemoRole, count: Int) {
            assertEquals(count, initial.objects(initial.actor(role)).solutions.size)
        }

        @Test
        fun `should expose the resources of the developer`() {
            assertEquals(3, initial.objects(initial.actor(DemoRole.Developer)).resources.size)
        }

        @Test
        fun `should expose only the assigned tours to an observer`() {
            val tours = initial.objects(initial.actor(DemoRole.Observer)).tours

            assertEquals(listOf("t1"), tours.map { tour -> tour.id })
        }

        @Test
        fun `should keep the identity of a logged in participant`() {
            val participant = initial.login("PART-101").state.actor(DemoRole.Participant)

            assertEquals("p1", participant.id)
            assertEquals(listOf("competition1"), initial.objects(participant).competitions.map { competition -> competition.id })
        }

        @Test
        fun `should hide users of another community from an administrator`() {
            val outsider = initial.users.first().copy(id = "outsider", communityIds = listOf("outside"))
            val extended = initial.copy(users = initial.users + outsider)

            val users = extended.objects(initial.actor(DemoRole.Administrator)).users

            assertFalse(users.any { user -> user.id == "outsider" })
        }
    }

    @Nested
    inner class SubmitSolutionTests {
        @ParameterizedTest
        @CsvSource("student, task3, Python, file.py", "p1, task3, VisualLanguage, file.qrs", "student, task1, Python, ''")
        internal fun `should reject an unavailable task, an unsupported kind or a missing file`(
            userId: String,
            taskId: String,
            kind: DemoSolutionKind,
            fileName: String,
        ) {
            val result = initial.submitSolution(userId = userId, taskId = taskId, kind = kind, fileName = fileName)

            assertFalse(result.isSuccess)
        }

        @Test
        fun `should reject a submission without a kind`() {
            val result = initial.submitSolution(userId = "student", taskId = "task1", kind = null, fileName = "file.py")

            assertFalse(result.isSuccess)
        }

        @Test
        fun `should queue an accepted solution`() {
            val queued = initial.submitSolution(userId = "student", taskId = "task1", kind = DemoSolutionKind.Python, fileName = "file.py")

            assertTrue(queued.isSuccess)
            assertEquals(DemoSolutionStatus.Queue, queued.state.solutions.last().status)
        }

        @Test
        fun `should keep a checked zero score below the earlier best score`() {
            val queued = initial.submitSolution(userId = "student", taskId = "task1", kind = DemoSolutionKind.Python, fileName = "file.py")

            val checked = queued.state.checkSolution("s103").finishSolution(id = "s103", status = DemoSolutionStatus.Checked, score = 0)

            assertEquals(0, checked.solutions.last().score)
            assertEquals(72, bestScore(solutions = checked.solutions, userId = "student", taskId = "task1"))
        }
    }

    @Nested
    inner class BestScoreTests {
        @Test
        fun `should return the best checked score of the participant and task`() {
            assertEquals(72, bestScore(solutions = initial.solutions, userId = "student", taskId = "task1"))
        }

        @Test
        fun `should keep a zero score as a result`() {
            val solutions = initial.solutions.filter { solution -> solution.id == "s1" }

            assertEquals(0, bestScore(solutions = solutions, userId = "student", taskId = "task1"))
        }

        @Test
        fun `should return no score for a solution that is not checked`() {
            assertNull(bestScore(solutions = initial.solutions, userId = "p2", taskId = "task2"))
        }

        @Test
        fun `should ignore the score of a solution in the queue`() {
            val pending = initial.solutions.first().copy(score = 100, status = DemoSolutionStatus.Queue)

            assertNull(bestScore(solutions = listOf(pending), userId = "student", taskId = "task1"))
        }
    }

    @Nested
    inner class ResultsCsvTests {
        @Test
        fun `should list checked scores and attempts of every participant`() {
            val csv = initial.resultsCsv(competitionId = "competition1", tourId = "t1")

            assertTrue(csv.contains("\"85\";\"1\""))
            assertTrue(csv.contains("\"p2\";\"Участник 2\";\"\";\"0\";\"\";\"1\""))
        }

        @Test
        fun `should start with a byte order mark and escape cells`() {
            val users = initial.users.map { user -> if (user.id == "p1") user.copy(alias = "Тест;\"имя\"\nстрока") else user }

            val csv = initial.copy(users = users).resultsCsv(competitionId = "competition1", tourId = "t1")

            assertTrue(csv.startsWith("﻿"))
            assertTrue(csv.contains("\"Тест;\"\"имя\"\"\nстрока\""))
        }

        @Test
        fun `should return nothing for a tour outside the competition`() {
            assertEquals("", initial.resultsCsv(competitionId = "competition1", tourId = "t2"))
        }
    }

    private fun confirmedRegistration(role: DemoRole): DemoState =
        initial.register(alias = "Новый", email = "new@example.com", role = role).state.confirmRegistration("246810").state
}
