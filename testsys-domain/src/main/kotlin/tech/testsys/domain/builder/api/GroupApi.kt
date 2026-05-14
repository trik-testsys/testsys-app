@file:Suppress("FunctionNaming")

package tech.testsys.domain.builder.api

import tech.testsys.domain.builder.group.ClassBuilder
import tech.testsys.domain.builder.group.ClassDataBuilder
import tech.testsys.domain.builder.group.CommunityBuilder
import tech.testsys.domain.builder.group.CommunityDataBuilder
import tech.testsys.domain.builder.group.CompetitionBuilder
import tech.testsys.domain.builder.group.CompetitionDataBuilder
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassData
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityData
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionData

/**
 * DSL entry point for building [ClassData].
 *
 * @param builder the configuration block applied to [ClassDataBuilder].
 * @return the constructed [ClassData].
 * @since %CURRENT_VERSION%
 */
inline fun classData(builder: ClassDataBuilder.() -> Unit) = ClassDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Class].
 *
 * @param builder the configuration block applied to [ClassBuilder].
 * @return the constructed [Class].
 * @since %CURRENT_VERSION%
 */
inline fun `class`(builder: ClassBuilder.() -> Unit) = ClassBuilder().apply(builder).build()

/**
 * DSL entry point for building [CommunityData].
 *
 * @param builder the configuration block applied to [CommunityDataBuilder].
 * @return the constructed [CommunityData].
 * @since %CURRENT_VERSION%
 */
inline fun communityData(builder: CommunityDataBuilder.() -> Unit) = CommunityDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Community].
 *
 * @param builder the configuration block applied to [CommunityBuilder].
 * @return the constructed [Community].
 * @since %CURRENT_VERSION%
 */
inline fun community(builder: CommunityBuilder.() -> Unit) = CommunityBuilder().apply(builder).build()

/**
 * DSL entry point for building [CompetitionData].
 *
 * @param builder the configuration block applied to [CompetitionDataBuilder].
 * @return the constructed [CompetitionData].
 * @since %CURRENT_VERSION%
 */
inline fun competitionData(builder: CompetitionDataBuilder.() -> Unit) = CompetitionDataBuilder().apply(builder).build()

/**
 * DSL entry point for building a [Competition].
 *
 * @param builder the configuration block applied to [CompetitionBuilder].
 * @return the constructed [Competition].
 * @since %CURRENT_VERSION%
 */
inline fun competition(builder: CompetitionBuilder.() -> Unit) = CompetitionBuilder().apply(builder).build()

private fun ClassData.toBuilder(): ClassDataBuilder {
    val thisData = this
    return ClassDataBuilder().apply {
        owner = thisData.owner.id
        name = thisData.name
        description = thisData.description
        students = thisData.students.ids.toMutableList()
        contests = thisData.contests.ids.toMutableList()
    }
}

/**
 * Creates copy of [Class] with provided modifications.
 *
 * @param builder the configuration block applied to copy of [Class].
 * @return the modified [Class].
 * @since %CURRENT_VERSION%
 */
fun Class.withData(builder: ClassDataBuilder.() -> Unit): Class {
    return Class(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun CommunityData.toBuilder(): CommunityDataBuilder {
    val thisData = this
    return CommunityDataBuilder().apply {
        owner = thisData.owner.id
        name = thisData.name
        description = thisData.description
    }
}

/**
 * Creates copy of [Community] with provided modifications.
 *
 * @param builder the configuration block applied to copy of [Community].
 * @return the modified [Community].
 * @since %CURRENT_VERSION%
 */
fun Community.withData(builder: CommunityDataBuilder.() -> Unit): Community {
    return Community(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}

private fun CompetitionData.toBuilder(): CompetitionDataBuilder {
    val thisData = this
    return CompetitionDataBuilder().apply {
        owner = thisData.owner.id
        name = thisData.name
        description = thisData.description
        participants = thisData.participants.ids.toMutableList()
        contests = thisData.contests.ids.toMutableList()
    }
}

/**
 * Creates copy of [Competition] with provided modifications.
 *
 * @param builder the configuration block applied to copy of [Competition].
 * @return the modified [Competition].
 * @since %CURRENT_VERSION%
 */
fun Competition.withData(builder: CompetitionDataBuilder.() -> Unit): Competition {
    return Competition(this.id, this.createdAt, this.data.toBuilder().apply(builder).build())
}
