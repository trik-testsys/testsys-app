package tech.testsys.web.app

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import tech.testsys.domain.builder.api.communityData
import tech.testsys.domain.builder.api.communityInviteData
import tech.testsys.domain.builder.api.competitionData
import tech.testsys.domain.builder.api.multipleRoleUserData
import tech.testsys.domain.builder.api.observerData
import tech.testsys.domain.builder.api.participantData
import tech.testsys.domain.builder.api.supervisorData
import tech.testsys.domain.builder.user.MultipleRoleUserDataBuilder
import tech.testsys.domain.contract.UserMailSender
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ObserverRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.SupervisorRepository
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInviteData
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.User
import tech.testsys.operation.config.CommunityConfig
import tech.testsys.web.app.security.UserKind
import java.time.Instant
import java.util.UUID

/** Test beans of the application: stored fixtures, the public community made of them and recorded letters. */
@TestConfiguration
class AppTestConfiguration {
    @Bean
    fun appFixtures(
        multipleRoleUsers: MultipleRoleUserRepository,
        participants: ParticipantRepository,
        observers: ObserverRepository,
        supervisors: SupervisorRepository,
        communities: CommunityRepository,
        competitions: CompetitionRepository,
    ): AppFixtures = AppFixtures(multipleRoleUsers, participants, observers, supervisors, communities, competitions)

    @Bean
    @Primary
    fun publicCommunityConfig(fixtures: AppFixtures): CommunityConfig = object : CommunityConfig {
        override val publicCommunityId: CommunityId by lazy { fixtures.community().id }
    }

    @Bean
    @Primary
    fun recordedMail(): RecordedMail = RecordedMail()
}

/** Stores users and groups with unique access codes, names and e-mail addresses. */
class AppFixtures(
    private val multipleRoleUsers: MultipleRoleUserRepository,
    private val participants: ParticipantRepository,
    private val observers: ObserverRepository,
    private val supervisors: SupervisorRepository,
    private val communities: CommunityRepository,
    private val competitions: CompetitionRepository,
) {
    fun unique(prefix: String): String = "$prefix-${UUID.randomUUID()}"

    /** Returns a confirmation code of the same length that differs from [code] in every digit. */
    fun otherCode(code: String): String = code.map { digit -> '0' + (digit - '0' + 1) % DIGITS }.joinToString("")

    fun multipleRoleUser(
        name: String = unique("User"),
        rawAccessToken: String = unique("token"),
        email: String = "${unique("user")}@example.com",
        roles: MultipleRoleUserDataBuilder.() -> Unit = {},
    ): MultipleRoleUser = multipleRoleUsers.save(
        multipleRoleUserData {
            accessToken(rawAccessToken, algorithm = HashAlgorithm.Identity)
            this.name = name
            this.email = email
            roles()
        },
    )

    fun userOf(kind: UserKind, name: String = unique("User"), rawAccessToken: String = unique("token")): User<*> = when (kind) {
        UserKind.MULTIPLE_ROLE -> multipleRoleUser(name = name, rawAccessToken = rawAccessToken)
        UserKind.PARTICIPANT -> {
            val competitionId = competitions.save(
                competitionData {
                    owner(multipleRoleUser().id.value)
                    this.name = unique("Competition")
                    description = "Competition"
                },
            ).id.value
            participants.save(
                participantData {
                    competition(competitionId)
                    accessToken(rawAccessToken, algorithm = HashAlgorithm.Identity)
                    this.name = name
                },
            )
        }
        UserKind.OBSERVER -> observer(of = community(), name = name, rawAccessToken = rawAccessToken)
        UserKind.SUPERVISOR -> supervisors.save(
            supervisorData {
                accessToken(rawAccessToken, algorithm = HashAlgorithm.Identity)
                this.name = name
            },
        )
    }

    /** Returns a new user holding only the administrator role. */
    fun administrator(): MultipleRoleUser = multipleRoleUser { roles { administrator {} } }

    fun observer(of: Community, name: String = unique("Observer"), rawAccessToken: String = unique("token")): Observer = observers.save(
        observerData {
            community(of.id.value)
            accessToken(rawAccessToken, algorithm = HashAlgorithm.Identity)
            this.name = name
        },
    )

    fun community(owner: MultipleRoleUser = multipleRoleUser(), name: String = unique("Community")): Community {
        val ownerId = owner.id.value
        return communities.saveWithInvites(managerInvite = invite(), developerInvite = invite()) { managerInviteId, developerInviteId ->
            communityData {
                owner(ownerId)
                this.name = name
                description = "Community"
                managerInvite = managerInviteId
                developerInvite = developerInviteId
            }
        }
    }

    private fun invite(): CommunityInviteData = communityInviteData {
        code(unique("code"), HashAlgorithm.Identity)
        expiresAt = Instant.parse("2100-01-01T00:00:00Z")
    }

    private companion object {
        const val DIGITS = 10
    }
}

/** Mail port that keeps the codes it was asked to send instead of sending letters. */
class RecordedMail : UserMailSender {
    val confirmationCodes = mutableMapOf<String, String>()
    val accessTokens = mutableMapOf<String, String>()

    override fun sendRegistrationConfirmationCode(email: String, confirmationCode: String) {
        confirmationCodes[email] = confirmationCode
    }

    override fun sendAccessToken(email: String, name: String, accessToken: String) {
        accessTokens[email] = accessToken
    }

    override fun sendEmailChangeConfirmationCode(email: String, confirmationCode: String) {
        confirmationCodes[email] = confirmationCode
    }

    override fun sendEmailChangedNotice(email: String, name: String) = Unit
}
