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
 * Builds [ClassData] with a [ClassDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun classData(builder: ClassDataBuilder.() -> Unit) = ClassDataBuilder().apply(builder).build()

/**
 * Builds a [Class] with a [ClassBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun `class`(builder: ClassBuilder.() -> Unit) = ClassBuilder().apply(builder).build()

/**
 * Builds [CommunityData] with a [CommunityDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun communityData(builder: CommunityDataBuilder.() -> Unit) = CommunityDataBuilder().apply(builder).build()

/**
 * Builds a [Community] with a [CommunityBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun community(builder: CommunityBuilder.() -> Unit) = CommunityBuilder().apply(builder).build()

/**
 * Builds [CompetitionData] with a [CompetitionDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun competitionData(builder: CompetitionDataBuilder.() -> Unit) = CompetitionDataBuilder().apply(builder).build()

/**
 * Builds a [Competition] with a [CompetitionBuilder] block.
 *
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
 * Returns a copy of this class with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun Class.withData(builder: ClassDataBuilder.() -> Unit): Class {
    return Class(this.id, this.createdAt, this.version, this.data.toBuilder().apply(builder).build())
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
 * Returns a copy of this community with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun Community.withData(builder: CommunityDataBuilder.() -> Unit): Community {
    return Community(this.id, this.createdAt, this.version, this.data.toBuilder().apply(builder).build())
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
 * Returns a copy of this competition with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun Competition.withData(builder: CompetitionDataBuilder.() -> Unit): Competition {
    return Competition(this.id, this.createdAt, this.version, this.data.toBuilder().apply(builder).build())
}
