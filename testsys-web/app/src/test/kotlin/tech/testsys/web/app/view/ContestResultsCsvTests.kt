package tech.testsys.web.app.view

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.domain.contract.persistence.ContestTaskResult
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.TaskVo
import tech.testsys.web.app.service.manager.ContestResultsVo
import java.time.Instant

class ContestResultsCsvTests {
    @Test
    fun `should start with a byte order mark and the header of the member and task columns`() {
        val csv = contestResultsCsv(results(tasks = listOf(task(5, "Движение")), members = emptyList()))

        assertEquals("﻿ID;Псевдоним;Движение — балл;Движение — посылок\r\n", csv)
    }

    @Test
    fun `should write the best result and the submission count of each member by task`() {
        val results = results(
            tasks = listOf(task(5, "A"), task(6, "B")),
            members = listOf(MultipleRoleUserId(1) to "Иван"),
            results = listOf(
                ContestTaskResult(authorId = MultipleRoleUserId(1), taskId = TaskId(5), bestScore = Score(70), submissionCount = 3),
                ContestTaskResult(authorId = MultipleRoleUserId(1), taskId = TaskId(6), bestScore = null, submissionCount = 2),
            ),
        )

        val rows = contestResultsCsv(results).removePrefix("﻿").split("\r\n")

        assertEquals("1;Иван;70;3;;2", rows[1])
    }

    @Test
    fun `should write zero submissions and an empty result for a member without submissions`() {
        val results = results(tasks = listOf(task(5, "A")), members = listOf(MultipleRoleUserId(2) to "Анна"))

        val rows = contestResultsCsv(results).removePrefix("﻿").split("\r\n")

        assertEquals("2;Анна;;0", rows[1])
    }

    @Test
    fun `should quote values with separators, quotes or line breaks and double their quotes`() {
        val quoted = task(5, "Тур; \"финал\"")
        val results = results(tasks = listOf(quoted), members = listOf(MultipleRoleUserId(3) to "a\nb"))

        val csv = contestResultsCsv(results)

        assertTrue("\"Тур; \"\"финал\"\" — балл\"" in csv, csv)
        assertTrue("3;\"a\nb\";;0" in csv, csv)
    }

    private fun results(
        tasks: List<TaskVo>,
        members: List<Pair<MultipleRoleUserId, String>>,
        results: List<ContestTaskResult> = emptyList(),
    ): ContestResultsVo = ContestResultsVo(contest = contest(), tasks = tasks, members = members, results = results)

    private fun contest(): ContestVo = ContestVo(
        id = ContestId(19),
        createdAt = Instant.EPOCH,
        owner = MultipleRoleUserId(0),
        name = "Тур",
        description = "",
        tasks = emptyList(),
        startsAt = null,
        contestDuration = null,
        attemptDuration = null,
        trikStudioVersion = TrikStudioVersion("3.0.0"),
        sharedTo = emptyList<CommunityId>(),
        endsAt = null,
    )

    private fun task(id: Long, name: String): TaskVo = TaskVo(
        id = TaskId(id),
        createdAt = Instant.EPOCH,
        owner = MultipleRoleUserId(0),
        name = name,
        description = "",
        sharedTo = emptyList(),
        wip = null,
        lastCommitted = null,
        uploadedResources = emptySet(),
    )
}
