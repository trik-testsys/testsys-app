package tech.testsys.domain.builder.user

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.ObserverData
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.ParticipantData
import tech.testsys.domain.model.user.SingleRoleUser
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.SupervisorData
import java.time.Instant

/**
 * Base class of [SingleRoleUser] builders.
 *
 * @param U the type of the built user.
 * @param Data the type of the user data.
 * @param DataBuilder the builder type of [Data].
 * @since %CURRENT_VERSION%
 */
abstract class SingleRoleUserBuilder<U : SingleRoleUser, Data, DataBuilder : Builder<Data>> :
    UserBuilder<SingleRoleUserId, U, Data, DataBuilder>() {

    override var id: Long? = null
    override var createdAt: Instant? = null
}

/**
 * Builder of [ParticipantData]. Required: [competition], [accessToken], [name].
 *
 * @property competition the id of the competition the participant belongs to, or `null` if not set yet.
 * @property accessToken the access code the participant logs in with, or `null` if not set yet.
 * @property name the name of the participant, or `null` if not set yet.
 * @since %CURRENT_VERSION%
 */
class ParticipantDataBuilder : Builder<ParticipantData> {

    var competition: CompetitionId? = null

    var accessToken: String? = null

    var name: String? = null

    /**
     * Sets [competition] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun competition(competitionId: Long) {
        this.competition = CompetitionId(competitionId)
    }

    override fun build(): ParticipantData {
        val competition = requireField(competition) { ::competition }

        return ParticipantData(
            competition = competition.lazify(),
            accessToken = requireField(accessToken) { ::accessToken },
            name = requireField(name) { ::name },
        )
    }
}

/**
 * Builder of [Participant] entities. Required: [id], [createdAt], [data].
 *
 * @since %CURRENT_VERSION%
 */
class ParticipantBuilder : SingleRoleUserBuilder<Participant, ParticipantData, ParticipantDataBuilder>() {

    override var data: ParticipantData? = null
    override fun dataBuilder() = ParticipantDataBuilder()

    override fun build(): Participant {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return Participant(
            id = SingleRoleUserId(id),
            createdAt = createdAt,
            data = data,
        )
    }
}

/**
 * Builder of [ObserverData]. Required: [community], [accessToken], [name].
 *
 * @property community the id of the community the observer is a member of, or `null` if not set yet.
 * @property competitions the ids of the competitions the observer may view.
 * @property accessToken the access code the observer logs in with, or `null` if not set yet.
 * @property name the name of the observer, or `null` if not set yet.
 * @since %CURRENT_VERSION%
 */
class ObserverDataBuilder : Builder<ObserverData> {

    var community: CommunityId? = null

    var competitions = mutableListOf<CompetitionId>()

    var accessToken: String? = null

    var name: String? = null

    /**
     * Sets [community] from a raw id.
     *
     * @since %CURRENT_VERSION%
     */
    fun community(communityId: Long) {
        this.community = CommunityId(communityId)
    }

    /**
     * Sets [competitions] from raw ids.
     *
     * @since %CURRENT_VERSION%
     */
    fun competitions(competitions: Iterable<Long>) {
        this.competitions = competitions.map { CompetitionId(it) }.toMutableList()
    }

    override fun build(): ObserverData {
        val community = requireField(community) { ::community }

        return ObserverData(
            community = community.lazify(),
            competitions = competitions.lazify(),
            accessToken = requireField(accessToken) { ::accessToken },
            name = requireField(name) { ::name },
        )
    }
}

/**
 * Builder of [Observer] entities. Required: [id], [createdAt], [data].
 *
 * @since %CURRENT_VERSION%
 */
class ObserverBuilder : SingleRoleUserBuilder<Observer, ObserverData, ObserverDataBuilder>() {

    override var data: ObserverData? = null
    override fun dataBuilder() = ObserverDataBuilder()

    override fun build(): Observer {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return Observer(
            id = SingleRoleUserId(id),
            createdAt = createdAt,
            data = data,
        )
    }
}

/**
 * Builder of [SupervisorData]. Required: [accessToken], [name].
 *
 * @property accessToken the access code the supervisor logs in with, or `null` if not set yet.
 * @property name the name of the supervisor, or `null` if not set yet.
 * @since %CURRENT_VERSION%
 */
class SupervisorDataBuilder : Builder<SupervisorData> {

    var accessToken: String? = null

    var name: String? = null

    override fun build() = SupervisorData(
        accessToken = requireField(accessToken) { ::accessToken },
        name = requireField(name) { ::name },
    )
}

/**
 * Builder of [Supervisor] entities. Required: [id], [createdAt], [data].
 *
 * @since %CURRENT_VERSION%
 */
class SupervisorBuilder : SingleRoleUserBuilder<Supervisor, SupervisorData, SupervisorDataBuilder>() {

    override var data: SupervisorData? = null
    override fun dataBuilder() = SupervisorDataBuilder()

    override fun build(): Supervisor {
        val id = requireField(id) { ::id }
        val createdAt = requireField(createdAt) { ::createdAt }
        val data = requireField(data) { ::data }

        return Supervisor(
            id = SingleRoleUserId(id),
            createdAt = createdAt,
            data = data,
        )
    }
}
