package tech.testsys.web.app.view

import tech.testsys.domain.model.user.UserId
import tech.testsys.web.app.service.UserRoleReadService
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.CounterKind
import tech.testsys.web.components.display.badge
import tech.testsys.web.components.display.counter
import tech.testsys.web.components.display.tag
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.texts.currentLocalization

/** Shared role detail blocks for administrator and supervisor user pages. */
internal fun PageScope.tasksBlock(service: UserRoleReadService, userId: UserId) {
    val localization = currentLocalization()
    val texts = localization.pageAdministrator
    val tasks = service.viewUserTasks(userId)
    row {
        block(title = localization.pageCabinet.sectionTasks()) {
            guide(texts.userTasksGuide())
            table(key = { (task, _, _) -> task.id }, fetch = { request -> pageOf(tasks, request) }) {
                textColumn(texts.name(), size = 10) { (task, _, _) -> task.name }
                column(texts.userTasksStatus(), size = 6) { (_, request, _) ->
                    val (text, tone) = validationStatusOf(request?.execution)
                    badge(text, tone)
                }
                textColumn(texts.userTasksCommunities()) { (_, _, shared) -> shared.joinToString(", ") { community -> community.name } }
                empty(texts.userTasksEmpty())
            }
        }
    }
}

internal fun PageScope.contestsBlock(service: UserRoleReadService, userId: UserId) {
    val localization = currentLocalization()
    val texts = localization.pageAdministrator
    val contests = service.viewUserContests(userId)
    row {
        block(title = localization.pageCabinet.sectionContests()) {
            guide(texts.userContestsGuide())
            table(key = { (contest, _) -> contest.id }, fetch = { request -> pageOf(contests, request) }) {
                textColumn(texts.name(), size = 10) { (contest, _) -> contest.name }
                numberColumn(texts.userContestsTasks(), size = 3) { (_, tasks) -> tasks.size }
                column(texts.userContestsSolutions()) { (_, tasks) ->
                    tasks.forEach { (task, submissions) ->
                        tag(task.name)
                        counter(Math.toIntExact(submissions), CounterKind.Neutral)
                    }
                }
                empty(texts.userContestsEmpty())
            }
        }
    }
}

internal fun PageScope.classesBlock(service: UserRoleReadService, userId: UserId) {
    val localization = currentLocalization()
    val texts = localization.pageAdministrator
    val classes = service.viewUserClasses(userId)
    row {
        block(title = localization.pageCabinet.sectionClasses()) {
            guide(texts.userClassesGuide())
            table(key = { (group, _) -> group.id }, fetch = { request -> pageOf(classes, request) }) {
                textColumn(texts.name(), size = 9) { (group, _) -> group.name }
                numberColumn(texts.userClassesStudents(), size = 5) { (group, _) -> group.students.size }
                numberColumn(texts.userSolutions(), size = 5) { (_, count) -> count.submissions }
                numberColumn(texts.userClassesAuthors()) { (_, count) -> count.authors }
                empty(texts.userClassesEmpty())
            }
        }
    }
}

internal fun PageScope.competitionsBlock(service: UserRoleReadService, userId: UserId) {
    val localization = currentLocalization()
    val texts = localization.pageAdministrator
    val competitions = service.viewUserCompetitions(userId)
    row {
        block(title = localization.pageCabinet.sectionCompetitions()) {
            guide(texts.userCompetitionsGuide())
            table(key = { (competition, _) -> competition.id }, fetch = { request -> pageOf(competitions, request) }) {
                textColumn(texts.name(), size = 9) { (competition, _) -> competition.name }
                numberColumn(texts.userCompetitionsParticipants(), size = 5) { (competition, _) -> competition.participants.size }
                numberColumn(texts.userSolutions(), size = 5) { (_, count) -> count.submissions }
                numberColumn(texts.userCompetitionsAuthors()) { (_, count) -> count.authors }
                empty(texts.userCompetitionsEmpty())
            }
        }
    }
}

internal fun PageScope.judgmentsBlock(service: UserRoleReadService, userId: UserId) {
    val texts = currentLocalization().pageAdministrator
    val judgments = service.viewUserJudgments(userId)
    row {
        block(title = texts.userJudgmentsTitle()) {
            guide(texts.userJudgmentsGuide())
            table(key = { (order, _, _) -> order.id }, fetch = { request -> pageOf(judgments, request) }) {
                codeColumn(texts.userJudgmentsSolution(), size = 6) { (_, solution, _) -> solution.value.toString() }
                dateTimeColumn(texts.userJudgmentsIssued(), size = 6) { (order, _, _) -> order.createdAt.toUserDateTime() }
                numberColumn(texts.userJudgmentsPreviousResult(), size = 6) { (_, _, previous) -> previous }
                numberColumn(texts.userJudgmentsNewResult()) { (order, _, _) -> order.score.value }
                empty(texts.userJudgmentsEmpty())
            }
        }
    }
}

internal fun PageScope.assignedContestsBlock(service: UserRoleReadService, userId: UserId) {
    val texts = currentLocalization().pageAdministrator
    val contests = service.viewUserAssignedContests(userId)
    row {
        block(title = texts.userAssignedTitle()) {
            guide(texts.userAssignedGuide())
            table(key = { (contest, _) -> contest.id }, fetch = { request -> pageOf(contests, request) }) {
                textColumn(texts.userAssignedContest(), size = 10) { (contest, _) -> contest.name }
                textColumn(texts.userAssignedCompetitions()) { (_, competitions) ->
                    competitions.joinToString(", ") { competition -> competition.name }
                }
                empty(texts.userAssignedEmpty())
            }
        }
    }
}
