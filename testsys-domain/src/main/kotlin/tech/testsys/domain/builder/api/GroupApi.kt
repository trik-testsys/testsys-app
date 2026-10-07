@file:Suppress("FunctionNaming")

package tech.testsys.domain.builder.api

import tech.testsys.domain.builder.group.ClassBuilder
import tech.testsys.domain.builder.group.ClassDataBuilder
import tech.testsys.domain.builder.group.ClassInviteBuilder
import tech.testsys.domain.builder.group.ClassInviteDataBuilder
import tech.testsys.domain.builder.group.CommunityBuilder
import tech.testsys.domain.builder.group.CommunityDataBuilder
import tech.testsys.domain.builder.group.CommunityInviteDataBuilder
import tech.testsys.domain.builder.group.CompetitionBuilder
import tech.testsys.domain.builder.group.CompetitionDataBuilder
import tech.testsys.domain.builder.group.DeveloperCommunityInviteBuilder
import tech.testsys.domain.builder.group.ManagerCommunityInviteBuilder
import tech.testsys.domain.builder.util.applyVersion
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassData
import tech.testsys.domain.model.group.ClassInvite
import tech.testsys.domain.model.group.ClassInviteData
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityData
import tech.testsys.domain.model.group.CommunityInvite
import tech.testsys.domain.model.group.CommunityInviteData
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

/**
 * Builds [ClassInviteData] with a [ClassInviteDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun classInviteData(builder: ClassInviteDataBuilder.() -> Unit) = ClassInviteDataBuilder().apply(builder).build()

/**
 * Builds a [ClassInvite] with a [ClassInviteBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun classInvite(builder: ClassInviteBuilder.() -> Unit) = ClassInviteBuilder().apply(builder).build()

/**
 * Builds [CommunityInviteData] with a [CommunityInviteDataBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun communityInviteData(builder: CommunityInviteDataBuilder.() -> Unit) = CommunityInviteDataBuilder().apply(builder).build()

/**
 * Builds a [CommunityInvite.Manager] with a [ManagerCommunityInviteBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun managerCommunityInvite(builder: ManagerCommunityInviteBuilder.() -> Unit) =
    ManagerCommunityInviteBuilder().apply(builder).build()

/**
 * Builds a [CommunityInvite.Developer] with a [DeveloperCommunityInviteBuilder] block.
 *
 * @since %CURRENT_VERSION%
 */
inline fun developerCommunityInvite(builder: DeveloperCommunityInviteBuilder.() -> Unit) =
    DeveloperCommunityInviteBuilder().apply(builder).build()

private fun ClassData.toBuilder(): ClassDataBuilder {
    val thisData = this
    return ClassDataBuilder().apply {
        owner = thisData.owner.id
        name = thisData.name
        description = thisData.description
        students = thisData.students.ids.toMutableList()
        contests = thisData.contests.ids.toMutableList()
        invite = thisData.invite.id
    }
}

/**
 * Returns a copy of this class with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun Class.withData(builder: ClassDataBuilder.() -> Unit): Class {
    return Class(
        id = this.id,
        createdAt = this.createdAt,
        data = this.data.toBuilder().apply(builder).build(),
    ).applyVersion(this.version)
}

private fun CommunityData.toBuilder(): CommunityDataBuilder {
    val thisData = this
    return CommunityDataBuilder().apply {
        owner = thisData.owner.id
        name = thisData.name
        description = thisData.description
        managerInvite = thisData.managerInvite.id
        developerInvite = thisData.developerInvite.id
    }
}

/**
 * Returns a copy of this community with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun Community.withData(builder: CommunityDataBuilder.() -> Unit): Community {
    return Community(
        id = this.id,
        createdAt = this.createdAt,
        data = this.data.toBuilder().apply(builder).build(),
    ).applyVersion(this.version)
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
    return Competition(
        id = this.id,
        createdAt = this.createdAt,
        data = this.data.toBuilder().apply(builder).build(),
    ).applyVersion(this.version)
}

private fun ClassInviteData.toBuilder(): ClassInviteDataBuilder {
    val thisData = this
    return ClassInviteDataBuilder().apply {
        storedCode(thisData.codeHash)
        expiresAt = thisData.expiresAt
    }
}

/**
 * Returns a copy of this class invite with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun ClassInvite.withData(builder: ClassInviteDataBuilder.() -> Unit): ClassInvite {
    return ClassInvite(
        id = this.id,
        createdAt = this.createdAt,
        data = this.data.toBuilder().apply(builder).build(),
    ).applyVersion(this.version)
}

private fun CommunityInviteData.toBuilder(): CommunityInviteDataBuilder {
    val thisData = this
    return CommunityInviteDataBuilder().apply {
        storedCode(thisData.codeHash)
        expiresAt = thisData.expiresAt
    }
}

/**
 * Returns a copy of this manager-role community invite with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun CommunityInvite.Manager.withData(builder: CommunityInviteDataBuilder.() -> Unit): CommunityInvite.Manager {
    return CommunityInvite.Manager(
        id = this.id,
        createdAt = this.createdAt,
        data = this.data.toBuilder().apply(builder).build(),
    ).applyVersion(this.version)
}

/**
 * Returns a copy of this developer-role community invite with its data modified by [builder].
 *
 * @since %CURRENT_VERSION%
 */
fun CommunityInvite.Developer.withData(builder: CommunityInviteDataBuilder.() -> Unit): CommunityInvite.Developer {
    return CommunityInvite.Developer(
        id = this.id,
        createdAt = this.createdAt,
        data = this.data.toBuilder().apply(builder).build(),
    ).applyVersion(this.version)
}
