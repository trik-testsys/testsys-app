package tech.testsys.web.devapp.demo.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DemoStateTests {
    @Test
    fun `should create selected quantity with unique codes and advance identifiers`() {
        val initial = demoFixtures()
        val result = initial.createParticipants("competition1", 12)

        assertTrue(result.isSuccess)
        assertEquals(115, result.state.nextId)
        assertEquals(14, result.state.competitions.first().participantIds.size)
        assertEquals("PART-114", result.state.users.last().accessCode)
        assertEquals(result.state.users.size, result.state.users.map { it.accessCode }.distinct().size)
    }

    @Test
    fun `should reject quantities outside the demonstration limit without changing records`() {
        val initial = demoFixtures()

        assertFalse(initial.createParticipants("competition1", 0).isSuccess)
        assertFalse(initial.createParticipants("competition1", 101).isSuccess)
        assertEquals(initial, initial.createParticipants("competition1", 0).state)
    }

    @Test
    fun `should keep new organizer isolated from existing competitions`() {
        val registered = demoFixtures().register("Новый", "new@example.com", "Организатор").state
        val confirmed = registered.confirmRegistration("246810").state

        assertTrue(confirmed.objects(confirmed.actor("Организатор")).competitions.isEmpty())
        assertFalse(confirmed.createParticipants("competition1", 3).isSuccess)
        assertEquals("ACCESS-103", confirmed.actor("Организатор").accessCode)
    }

    @Test
    fun `should invalidate the old access code after one use recovery`() {
        val recovery = demoFixtures().requestRecovery("ANNA@example.com").state
        val restored = recovery.restoreAccess(recovery.recovery!!.token).state

        assertFalse(restored.login("STUDENT-2026").isSuccess)
        assertTrue(restored.login("RESTORED-103").isSuccess)
        assertFalse(restored.restoreAccess(recovery.recovery.token).isSuccess)
    }

    @Test
    fun `should attach only an available tour once`() {
        val initial = demoFixtures()
        val added = initial.addTour("competition1", "t2")

        assertTrue(added.isSuccess)
        assertEquals(listOf("t1", "t2"), added.state.competitions.first().tourIds)
        assertFalse(added.state.addTour("competition1", "t2").isSuccess)
        assertFalse(initial.addTour("competition1", "missing").isSuccess)
    }

    @Test
    fun `should expose role scoped records and retain zero scores in full csv`() {
        val initial = demoFixtures()

        assertEquals(2, initial.objects(initial.actor("Ученик")).solutions.size)
        assertEquals(3, initial.objects(initial.actor("Разработчик")).resources.size)
        assertEquals(4, initial.objects(initial.actor("Судья")).solutions.size)
        assertEquals(listOf("t1"), initial.objects(initial.actor("Наблюдатель")).tours.map { it.id })
        assertTrue(initial.resultsCsv("competition1", "t1").contains("\"85\";\"1\""))
        assertTrue(initial.resultsCsv("competition1", "t1").contains("\"p2\""))
        assertEquals(72, bestScore(initial.solutions, "student", "task1"))
    }

    @Test
    fun `should validate submission access and author supported kinds`() {
        val initial = demoFixtures()

        assertFalse(initial.submitSolution("student", "task3", "Python", "file.py").isSuccess)
        assertFalse(initial.submitSolution("p1", "task3", "VisualLanguage", "file.qrs").isSuccess)
        val queued = initial.submitSolution("student", "task1", "Python", "file.py").state
        assertEquals("Queue", queued.solutions.last().status)
        val checked = queued.checkSolution("s103").finishSolution("s103", "Checked", 0)
        assertEquals(0, checked.solutions.last().score)
        assertEquals(72, bestScore(checked.solutions, "student", "task1"))
    }

    @Test
    fun `should reject duplicate emails invalid confirmation and empty creation`() {
        val initial = demoFixtures()
        assertFalse(initial.register("Анна", " ANNA@example.com ", "Ученик").isSuccess)
        assertFalse(initial.register("", "new@example.com", "Ученик").isSuccess)
        assertFalse(initial.register("Новая", "invalid", "Ученик").isSuccess)
        val pending = initial.register("Новая", "new@example.com", "Ученик").state
        assertFalse(pending.confirmRegistration("wrong").isSuccess)
        assertEquals(initial.users, pending.confirmRegistration("wrong").state.users)
        assertFalse(initial.createCompetition(" ").isSuccess)
        val competition = initial.createCompetition(" Новое ").state.competitions.last()
        assertEquals("Новое", competition.name)
        assertEquals("organizer", competition.organizerId)
        assertTrue(competition.participantIds.isEmpty())
        assertTrue(competition.tourIds.isEmpty())
    }

    @Test
    fun `should preserve participant identity and restrict administrator community and new student groups`() {
        val initial = demoFixtures()
        val participant = initial.login("PART-101").state.actor("Участник")
        assertEquals("p1", participant.id)
        assertEquals(listOf("competition1"), initial.objects(participant).competitions.map { it.id })
        val outsider = initial.users.first().copy(id = "outsider", communityIds = listOf("outside"))
        val extended = initial.copy(users = initial.users + outsider)
        assertFalse(extended.objects(initial.actor("Администратор")).users.any { it.id == "outsider" })
        val registered = initial.register("Новая", "new@example.com", "Ученик").state.confirmRegistration("246810").state
        assertTrue(registered.objects(registered.actor("Ученик")).classes.isEmpty())
        assertTrue(registered.objects(registered.actor("Ученик")).tasks.isEmpty())
        val organizer = initial.register("Новый", "new@example.com", "Организатор").state.confirmRegistration("246810").state
        assertEquals(listOf("t1"), organizer.availableTours(organizer.actor("Организатор")).map { it.id })
    }

    @Test
    fun `should distinguish zero score absence and checking and escape full csv cells`() {
        val initial = demoFixtures()
        assertEquals(0, bestScore(initial.solutions.filter { it.id == "s1" }, "student", "task1"))
        assertEquals(null, bestScore(initial.solutions, "p2", "task2"))
        val pending = initial.solutions.first().copy(score = 100, status = "Queue")
        assertEquals(null, bestScore(listOf(pending), "student", "task1"))
        val users = initial.users.map { if (it.id == "p1") it.copy(alias = "Тест;\"имя\"\nстрока") else it }
        val csv = initial.copy(users = users).resultsCsv("competition1", "t1")
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("\"Тест;\"\"имя\"\"\nстрока\""))
        assertTrue(csv.contains("\"p2\";\"Участник 2\";\"\";\"0\";\"\";\"1\""))
        assertEquals("", initial.resultsCsv("competition1", "t2"))
    }
}
