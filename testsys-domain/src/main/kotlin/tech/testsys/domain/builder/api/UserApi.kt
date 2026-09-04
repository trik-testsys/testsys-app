package tech.testsys.domain.builder.api

import tech.testsys.domain.builder.user.DeveloperDataBuilder
import tech.testsys.domain.builder.user.JudgeDataBuilder
import tech.testsys.domain.builder.user.ManagerDataBuilder
import tech.testsys.domain.builder.user.MultipleRoleUserBuilder
import tech.testsys.domain.builder.user.MultipleRoleUserDataBuilder
import tech.testsys.domain.builder.user.ObserverBuilder
import tech.testsys.domain.builder.user.ObserverDataBuilder
import tech.testsys.domain.builder.user.ParticipantBuilder
import tech.testsys.domain.builder.user.ParticipantDataBuilder
import tech.testsys.domain.builder.user.StudentDataBuilder
import tech.testsys.domain.builder.user.SupervisorBuilder
import tech.testsys.domain.builder.user.SupervisorDataBuilder
import tech.testsys.domain.model.user.DeveloperData
import tech.testsys.domain.model.user.JudgeData
import tech.testsys.domain.model.user.ManagerData
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserData
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.ObserverData
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.ParticipantData
import tech.testsys.domain.model.user.StudentData
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.SupervisorData

/**
 * Builds [MultipleRoleUserData] with a [MultipleRoleUserDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun multipleRoleUserData(builder: MultipleRoleUserDataBuilder.() -> Unit) = MultipleRoleUserDataBuilder().apply(builder).build()

/**
 * Builds a [MultipleRoleUser] with a [MultipleRoleUserBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun multipleRoleUser(builder: MultipleRoleUserBuilder.() -> Unit) = MultipleRoleUserBuilder().apply(builder).build()

/**
 * Builds [ObserverData] with an [ObserverDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun observerData(builder: ObserverDataBuilder.() -> Unit) = ObserverDataBuilder().apply(builder).build()

/**
 * Builds an [Observer] with an [ObserverBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun observer(builder: ObserverBuilder.() -> Unit) = ObserverBuilder().apply(builder).build()

/**
 * Builds [ParticipantData] with a [ParticipantDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun participantData(builder: ParticipantDataBuilder.() -> Unit) = ParticipantDataBuilder().apply(builder).build()

/**
 * Builds a [Participant] with a [ParticipantBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun participant(builder: ParticipantBuilder.() -> Unit) = ParticipantBuilder().apply(builder).build()

/**
 * Builds [SupervisorData] with a [SupervisorDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun supervisorData(builder: SupervisorDataBuilder.() -> Unit) = SupervisorDataBuilder().apply(builder).build()

/**
 * Builds a [Supervisor] with a [SupervisorBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun supervisor(builder: SupervisorBuilder.() -> Unit) = SupervisorBuilder().apply(builder).build()

/**
 * Builds [DeveloperData] with a [DeveloperDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun developerData(builder: DeveloperDataBuilder.() -> Unit) = DeveloperDataBuilder().apply(builder).build()

/**
 * Builds [StudentData] with a [StudentDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun studentData(builder: StudentDataBuilder.() -> Unit) = StudentDataBuilder().apply(builder).build()

/**
 * Builds [JudgeData] with a [JudgeDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun judgeData(builder: JudgeDataBuilder.() -> Unit) = JudgeDataBuilder().apply(builder).build()

/**
 * Builds [ManagerData] with a [ManagerDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun managerData(builder: ManagerDataBuilder.() -> Unit) = ManagerDataBuilder().apply(builder).build()

private fun ParticipantData.toBuilder(): ParticipantDataBuilder {
    val thisData = this
    return ParticipantDataBuilder().apply {
        competition = thisData.competition.id
        accessToken = thisData.accessToken
        name = thisData.name
    }
}

/**
 * Returns a copy of this participant with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun Participant.withData(builder: ParticipantDataBuilder.() -> Unit): Participant {
    return Participant(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun ObserverData.toBuilder(): ObserverDataBuilder {
    val thisData = this
    return ObserverDataBuilder().apply {
        community = thisData.community.id
        accessToken = thisData.accessToken
        competitions = thisData.competitions.ids.toMutableList()
        name = thisData.name
    }
}

/**
 * Returns a copy of this observer with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun Observer.withData(builder: ObserverDataBuilder.() -> Unit): Observer {
    return Observer(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun MultipleRoleUserData.toBuilder(): MultipleRoleUserDataBuilder {
    val thisData = this
    return MultipleRoleUserDataBuilder().apply {
        accessToken = thisData.accessToken
        name = thisData.name
        email = thisData.email
        roles { addAll(thisData.roles) }
    }
}

/**
 * Returns a copy of this user with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun MultipleRoleUser.withData(builder: MultipleRoleUserDataBuilder.() -> Unit): MultipleRoleUser {
    return MultipleRoleUser(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun SupervisorData.toBuilder(): SupervisorDataBuilder {
    val thisData = this
    return SupervisorDataBuilder().apply {
        accessToken = thisData.accessToken
        name = thisData.name
    }
}

/**
 * Returns a copy of this supervisor with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun Supervisor.withData(builder: SupervisorDataBuilder.() -> Unit): Supervisor {
    return Supervisor(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}
