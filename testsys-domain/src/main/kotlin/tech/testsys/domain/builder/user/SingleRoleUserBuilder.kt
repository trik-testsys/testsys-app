package tech.testsys.domain.builder.user

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DataCapable
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
 * Abstract base builder for [SingleRoleUser] entities.
 * Provides default `null` initial values for [id] and [createdAt].
 *
 * @param U the concrete single-role user type being built.
 * @param Data the type of associated data object.
 * @param DataBuilder the builder type used to construct [Data].
 * @since %CURRENT_VERSION%
 */
abstract class SingleRoleUserBuilder<U: SingleRoleUser, Data, DataBuilder: Builder<Data>>
    : UserBuilder<SingleRoleUserId, U, Data, DataBuilder>() {

    override var id: Long? = null
    override var createdAt: Instant? = null

}

/**
 * Builder for constructing [ParticipantData].
 *
 * @since %CURRENT_VERSION%
 */
class ParticipantDataBuilder : Builder<ParticipantData> {

    /**
     * The competition this participant belongs to.
     *
     * @since %CURRENT_VERSION%
     */
    var competition: CompetitionId? = null

    var accessToken: String? = null

    /**
     * The display name of the participant.
     *
     * @since %CURRENT_VERSION%
     */
    var name: String? = null

    /**
     * Sets the [competition] from a raw ID value.
     *
     * @param competitionId the raw competition ID.
     * @since %CURRENT_VERSION%
     */
    fun competition(competitionId: Long) {
        this.competition = CompetitionId(competitionId)
    }

    /**
     * Builds the [ParticipantData] instance.
     *
     * @return the constructed [ParticipantData].
     * @throws IllegalArgumentException if [competition] is not set.
     * @since %CURRENT_VERSION%
     */
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
 * Builder for constructing [Participant] domain entities.
 * Supports configuring participant data via [DataCapable].
 *
 * @since %CURRENT_VERSION%
 */
class ParticipantBuilder : SingleRoleUserBuilder<Participant, ParticipantData, ParticipantDataBuilder>() {

    override var data: ParticipantData? = null
    override fun dataBuilder() = ParticipantDataBuilder()

    /**
     * Builds the [Participant] instance.
     *
     * @return the constructed [Participant].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
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
 * Builder for constructing [ObserverData].
 *
 * @since %CURRENT_VERSION%
 */
class ObserverDataBuilder : Builder<ObserverData> {

    /**
     * The community this observer is scoped to.
     *
     * @since %CURRENT_VERSION%
     */
    var community: CommunityId? = null

    /**
     * The list of competitions this observer can view.
     *
     * @since %CURRENT_VERSION%
     */
    var competitions = mutableListOf<CompetitionId>()

    var accessToken: String? = null

    /**
     * The display name of the observer.
     *
     * @since %CURRENT_VERSION%
     */
    var name: String? = null

    /**
     * Sets the [community] from a raw ID value.
     *
     * @param communityId the raw community ID.
     * @since %CURRENT_VERSION%
     */
    fun community(communityId: Long) {
        this.community = CommunityId(communityId)
    }

    /**
     * Sets the [competitions] list from raw ID values.
     *
     * @param competitions the raw competition IDs.
     * @since %CURRENT_VERSION%
     */
    fun competitions(competitions: Iterable<Long>) {
        this.competitions = competitions.map { CompetitionId(it) }.toMutableList()
    }

    /**
     * Builds the [ObserverData] instance.
     *
     * @return the constructed [ObserverData].
     * @throws IllegalArgumentException if [community] is not set.
     * @since %CURRENT_VERSION%
     */
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
 * Builder for constructing [Observer] domain entities.
 * Supports configuring observer data via [DataCapable].
 *
 * @since %CURRENT_VERSION%
 */
class ObserverBuilder : SingleRoleUserBuilder<Observer, ObserverData, ObserverDataBuilder>() {

    override var data: ObserverData? = null
    override fun dataBuilder() = ObserverDataBuilder()

    /**
     * Builds the [Observer] instance.
     *
     * @return the constructed [Observer].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
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

class SupervisorDataBuilder : Builder<SupervisorData> {


    var accessToken: String? = null

    /**
     * The display name of the supervisor.
     *
     * @since %CURRENT_VERSION%
     */
    var name: String? = null

    /**
     * Builds the [SupervisorData] instance.
     *
     * @return the constructed [SupervisorData].
     * @since %CURRENT_VERSION%
     */
    override fun build() = SupervisorData(
        accessToken = requireField(accessToken) { ::accessToken },
        name = requireField(name) { ::name },
    )

}

/**
 * Builder for constructing [Supervisor] domain entities.
 * Supervisors have no additional data beyond the base user fields.
 *
 * @since %CURRENT_VERSION%
 */
class SupervisorBuilder : SingleRoleUserBuilder<Supervisor, SupervisorData, SupervisorDataBuilder>() {

    override var data: SupervisorData? = null
    override fun dataBuilder() = SupervisorDataBuilder()

    /**
     * Builds the [Supervisor] instance.
     *
     * @return the constructed [Supervisor].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
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
