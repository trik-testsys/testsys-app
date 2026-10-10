package tech.testsys.web.app.service.manager

import tech.testsys.domain.contract.persistence.ContestTaskResult
import tech.testsys.domain.model.group.ClassInvite
import tech.testsys.domain.model.group.ClassInviteId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.group.InviteCodeHash
import tech.testsys.domain.model.user.AccessTokenHash
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.UserId
import tech.testsys.operation.user.ManagerOperations
import tech.testsys.web.app.service.CompetitionVo
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.MultipleRoleUserVo
import tech.testsys.web.app.service.TaskVo
import tech.testsys.web.app.service.student.ClassVo
import tech.testsys.web.app.service.student.toVo
import tech.testsys.web.app.service.toVo
import java.time.Instant

/**
 * Class invite code data for pages.
 *
 * @property id the identifier of the invite.
 * @property createdAt the moment the invite was created.
 * @property codeHash the stored invite code with its hashing algorithm.
 * @property expiresAt the moment the invite stops being valid.
 * @since %CURRENT_VERSION%
 */
data class ClassInviteVo(
    val id: ClassInviteId,
    val createdAt: Instant,
    val codeHash: InviteCodeHash,
    val expiresAt: Instant,
)

/**
 * Participant data for pages, with links replaced by identifiers.
 *
 * @property id the identifier of the participant.
 * @property createdAt the moment the participant was created.
 * @property accessTokenHash the stored access code with its hashing algorithm.
 * @property name the nickname of the participant.
 * @property competition the identifier of the competition of the participant.
 * @since %CURRENT_VERSION%
 */
data class ParticipantVo(
    val id: SingleRoleUserId,
    val createdAt: Instant,
    val accessTokenHash: AccessTokenHash,
    val name: String,
    val competition: CompetitionId,
)

/**
 * Class of the manager with its invite, its students with their last logins and its contests.
 *
 * @property studyClass the class.
 * @property invite the invite code of the class.
 * @property students the students in stored order, each with its last login, or `null` if never logged in.
 * @property contests the added contests in stored order.
 * @since %CURRENT_VERSION%
 */
data class ClassDetailsVo(
    val studyClass: ClassVo,
    val invite: ClassInviteVo,
    val students: List<Pair<MultipleRoleUserVo, Instant?>>,
    val contests: List<ContestVo>,
)

/**
 * Competition of the manager with its participants and their last logins and its contests.
 *
 * @property competition the competition.
 * @property participants the participants in stored order, each with its last login, or `null` if never logged in.
 * @property contests the added contests in stored order.
 * @since %CURRENT_VERSION%
 */
data class CompetitionDetailsVo(
    val competition: CompetitionVo,
    val participants: List<Pair<ParticipantVo, Instant?>>,
    val contests: List<ContestVo>,
)

/**
 * Results of a contest within a class or competition of the manager.
 *
 * @property contest the contest.
 * @property tasks the tasks of the contest in stored order.
 * @property members the identifiers and nicknames of the class students or competition participants in stored order.
 * @property results the results of member and task pairs having submissions.
 * @since %CURRENT_VERSION%
 */
data class ContestResultsVo(
    val contest: ContestVo,
    val tasks: List<TaskVo>,
    val members: List<Pair<UserId, String>>,
    val results: List<ContestTaskResult>,
)

internal fun ClassInvite.toVo(): ClassInviteVo =
    ClassInviteVo(id = id, createdAt = createdAt, codeHash = data.codeHash, expiresAt = data.expiresAt)

internal fun Participant.toVo(): ParticipantVo = ParticipantVo(
    id = id,
    createdAt = createdAt,
    accessTokenHash = data.accessTokenHash,
    name = data.name,
    competition = data.competition.id,
)

internal fun ManagerOperations.ClassDetails.toVo(): ClassDetailsVo = ClassDetailsVo(
    studyClass = studyClass.toVo(),
    invite = invite.toVo(),
    students = students.map { (student, lastLogin) -> student.toVo() to lastLogin },
    contests = contests.map { contest -> contest.toVo() },
)

internal fun ManagerOperations.CompetitionDetails.toVo(): CompetitionDetailsVo = CompetitionDetailsVo(
    competition = competition.toVo(),
    participants = participants.map { (participant, lastLogin) -> participant.toVo() to lastLogin },
    contests = contests.map { contest -> contest.toVo() },
)

internal fun ManagerOperations.ContestResults.toVo(): ContestResultsVo = ContestResultsVo(
    contest = contest.toVo(),
    tasks = tasks.map { task -> task.toVo() },
    members = members.map { member ->
        when (member) {
            is MultipleRoleUser -> member.id to member.data.name
            is Participant -> member.id to member.data.name
            is Observer, is Supervisor -> error("User id=${member.id.value} of a kind that is no group member was returned")
        }
    },
    results = results,
)
