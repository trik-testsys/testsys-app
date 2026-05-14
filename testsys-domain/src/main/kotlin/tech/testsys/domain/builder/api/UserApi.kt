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
 * DSL entry point for building [MultipleRoleUserData].
 *
 * @param builder the configuration block applied to [MultipleRoleUserDataBuilder].
 * @return the constructed [MultipleRoleUserData].
 * @since %CURRENT_VERSION%
 */
inline fun multipleRoleUserData(builder: MultipleRoleUserDataBuilder.() -> Unit) = MultipleRoleUserDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [MultipleRoleUser].
 *
 * @param builder the configuration block applied to [MultipleRoleUserBuilder].
 * @return the constructed [MultipleRoleUser].
 * @since %CURRENT_VERSION%
 */
inline fun multipleRoleUser(builder: MultipleRoleUserBuilder.() -> Unit) = MultipleRoleUserBuilder().apply(builder).build()

/**
 * DSL entry point for building [ObserverData].
 *
 * @param builder the configuration block applied to [ObserverDataBuilder].
 * @return the constructed [ObserverData].
 * @since %CURRENT_VERSION%
 */
inline fun observerData(builder: ObserverDataBuilder.() -> Unit) = ObserverDataBuilder().apply(builder).build()

/**
 * DSL entry point for building an [Observer].
 *
 * @param builder the configuration block applied to [ObserverBuilder].
 * @return the constructed [Observer].
 * @since %CURRENT_VERSION%
 */
inline fun observer(builder: ObserverBuilder.() -> Unit) = ObserverBuilder().apply(builder).build()

/**
 * DSL entry point for building [ParticipantData].
 *
 * @param builder the configuration block applied to [ParticipantDataBuilder].
 * @return the constructed [ParticipantData].
 * @since %CURRENT_VERSION%
 */
inline fun participantData(builder: ParticipantDataBuilder.() -> Unit) = ParticipantDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Participant].
 *
 * @param builder the configuration block applied to [ParticipantBuilder].
 * @return the constructed [Participant].
 * @since %CURRENT_VERSION%
 */
inline fun participant(builder: ParticipantBuilder.() -> Unit) = ParticipantBuilder().apply(builder).build()

/**
 * DSL entry point for building [SupervisorData].
 *
 * @param builder the configuration block applied to [SupervisorDataBuilder].
 * @return the constructed [SupervisorData].
 * @since %CURRENT_VERSION%
 */
inline fun supervisorData(builder: SupervisorDataBuilder.() -> Unit) = SupervisorDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Supervisor].
 *
 * @param builder the configuration block applied to [SupervisorBuilder].
 * @return the constructed [Supervisor].
 * @since %CURRENT_VERSION%
 */
inline fun supervisor(builder: SupervisorBuilder.() -> Unit) = SupervisorBuilder().apply(builder).build()

/**
 * DSL entry point for building [DeveloperData].
 *
 * @param builder the configuration block applied to [DeveloperDataBuilder].
 * @return the constructed [DeveloperData].
 * @since %CURRENT_VERSION%
 */
inline fun developerData(builder: DeveloperDataBuilder.() -> Unit) = DeveloperDataBuilder().apply(builder).build()

/**
 * DSL entry point for building [StudentData].
 *
 * @param builder the configuration block applied to [StudentDataBuilder].
 * @return the constructed [StudentData].
 * @since %CURRENT_VERSION%
 */
inline fun studentData(builder: StudentDataBuilder.() -> Unit) = StudentDataBuilder().apply(builder).build()

/**
 * DSL entry point for building [JudgeData].
 *
 * @param builder the configuration block applied to [JudgeDataBuilder].
 * @return the constructed [JudgeData].
 * @since %CURRENT_VERSION%
 */
inline fun judgeData(builder: JudgeDataBuilder.() -> Unit) = JudgeDataBuilder().apply(builder).build()

/**
 * DSL entry point for building [ManagerData].
 *
 * @param builder the configuration block applied to [ManagerDataBuilder].
 * @return the constructed [ManagerData].
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
 * Creates copy of [Participant] with provided modifications.
 *
 * @param builder the configuration block applied to copy of [Participant].
 * @return the modified [Participant].
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
 * Creates copy of [Observer] with provided modifications.
 *
 * @param builder the configuration block applied to copy of [Observer].
 * @return the modified [Observer].
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
 * Creates copy of [MultipleRoleUser] with provided modifications.
 *
 * @param builder the configuration block applied to copy of [MultipleRoleUser].
 * @return the modified [MultipleRoleUser].
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
 * Creates copy of [Supervisor] with provided modifications.
 *
 * @param builder the configuration block applied to copy of [Supervisor].
 * @return the modified [Supervisor].
 * @since %CURRENT_VERSION%
 */
fun Supervisor.withData(builder: SupervisorDataBuilder.() -> Unit): Supervisor {
    return Supervisor(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}
