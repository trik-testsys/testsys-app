package tech.testsys.domain.builder.user

import tech.testsys.domain.builder.Builder
import tech.testsys.domain.builder.DataCapable
import tech.testsys.domain.builder.util.lazify
import tech.testsys.domain.builder.util.requireField
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.ObserverData
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.ParticipantData
import tech.testsys.domain.model.user.SingleRoleUser
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.UserId
import java.time.Instant

/**
 * Abstract base builder for [SingleRoleUser] entities.
 * Provides default `null` initial values for [id] and [createdAt].
 *
 * @param U the concrete single-role user type being built.
 * @since %CURRENT_VERSION%
 */
abstract class SingleRoleUserBuilder<U: SingleRoleUser> : UserBuilder<U>() {

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
        val competition = requireField(competition, "competition")

        return ParticipantData(
            competition = competition.lazify(),
        )
    }

}

/**
 * Builder for constructing [Participant] domain entities.
 * Supports configuring participant data via [DataCapable].
 *
 * @since %CURRENT_VERSION%
 */
class ParticipantBuilder : SingleRoleUserBuilder<Participant>(), DataCapable<ParticipantData, ParticipantDataBuilder> {

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
        val id = requireField(id, "id")
        val createdAt = requireField(createdAt, "createdAt")
        val accessToken = requireField(accessToken, "accessToken")
        val data = requireField(data, "data")

        return Participant(
            id = UserId(id),
            createdAt = createdAt,
            accessToken = accessToken,
            data = data,
        )
    }

}

/**
 * DSL entry point for building [ParticipantData].
 *
 * @param builder the configuration block applied to [ParticipantDataBuilder].
 * @return the constructed [ParticipantData].
 * @since %CURRENT_VERSION%
 */
inline fun buildParticipantData(builder: ParticipantDataBuilder.() -> Unit) =
    ParticipantDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Participant].
 *
 * @param builder the configuration block applied to [ParticipantBuilder].
 * @return the constructed [Participant].
 * @since %CURRENT_VERSION%
 */
inline fun buildParticipant(builder: ParticipantBuilder.() -> Unit) =
    ParticipantBuilder().apply(builder).build()

/**
 * Builder for constructing [ObserverData].
 *
 * @since %CURRENT_VERSION%
 */
class ObserverDataBuilder : Builder<ObserverData> {

    /**
     * The list of competitions this observer can view.
     *
     * @since %CURRENT_VERSION%
     */
    var competitions = mutableListOf<CompetitionId>()

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
     * @since %CURRENT_VERSION%
     */
    override fun build() = ObserverData(
        competitions = competitions.lazify(),
    )

}

/**
 * Builder for constructing [Observer] domain entities.
 * Supports configuring observer data via [DataCapable].
 *
 * @since %CURRENT_VERSION%
 */
class ObserverBuilder : SingleRoleUserBuilder<Observer>(), DataCapable<ObserverData, ObserverDataBuilder> {

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
        val id = requireField(id, "id")
        val createdAt = requireField(createdAt, "createdAt")
        val accessToken = requireField(accessToken, "accessToken")
        val data = requireField(data, "data")

        return Observer(
            id = UserId(id),
            createdAt = createdAt,
            accessToken = accessToken,
            data = data,
        )
    }
}

/**
 * DSL entry point for building [ObserverData].
 *
 * @param builder the configuration block applied to [ObserverDataBuilder].
 * @return the constructed [ObserverData].
 * @since %CURRENT_VERSION%
 */
inline fun buildObserverData(builder: ObserverDataBuilder.() -> Unit) = ObserverDataBuilder().apply(builder).build()

/**
 * DSL entry point for building an [Observer].
 *
 * @param builder the configuration block applied to [ObserverBuilder].
 * @return the constructed [Observer].
 * @since %CURRENT_VERSION%
 */
inline fun buildObserver(builder: ObserverBuilder.() -> Unit) = ObserverBuilder().apply(builder).build()

/**
 * Builder for constructing [Supervisor] domain entities.
 * Supervisors have no additional data beyond the base user fields.
 *
 * @since %CURRENT_VERSION%
 */
class SupervisorBuilder : SingleRoleUserBuilder<Supervisor>() {

    /**
     * Builds the [Supervisor] instance.
     *
     * @return the constructed [Supervisor].
     * @throws IllegalArgumentException if any required field is not set.
     * @since %CURRENT_VERSION%
     */
    override fun build(): Supervisor {
        val id = requireField(id, "id")
        val createdAt = requireField(createdAt, "createdAt")
        val accessToken = requireField(accessToken, "accessToken")

        return Supervisor(
            id = UserId(id),
            createdAt = createdAt,
            accessToken = accessToken,
        )
    }

}

/**
 * DSL entry point for building a [Supervisor].
 *
 * @param builder the configuration block applied to [SupervisorBuilder].
 * @return the constructed [Supervisor].
 * @since %CURRENT_VERSION%
 */
inline fun buildSupervisor(builder: SupervisorBuilder.() -> Unit) = SupervisorBuilder().apply(builder).build()
