package tech.testsys.infra.database.api.service

import tech.testsys.domain.model.task.TrikStudioVersion

/**
 * Service listing, registration, tag change and deletion of TRIK Studio versions for the service cabinet.
 *
 * @since 1.0.0-beta.1
 */
interface ServiceTrikStudioVersions {

    /**
     * Finds every registered TRIK Studio version.
     *
     * @return the registered versions ordered by Docker image tag, or an empty list when none is registered.
     * @since 1.0.0-beta.1
     */
    fun findAll(): List<TrikStudioVersion>

    /**
     * Registers a TRIK Studio version with the Docker image [tag] and the version number [version], both trimmed.
     *
     * @param tag the Docker image tag; at most 512 Unicode code points after trimming.
     * @param version the version number; at most 255 Unicode code points after trimming.
     * @return the registered version, or the first reason of refusal, checking the version number before the tag;
     * nothing is stored on refusal.
     * @since 1.0.0-beta.1
     */
    fun register(tag: String, version: String): TrikStudioVersionRegistration

    /**
     * Finds the tags of the registered versions that a contest, a submission, a task revision or a task validation
     * request uses.
     *
     * @since 1.0.0-beta.1
     */
    fun findUsedTags(): Set<String>

    /**
     * Changes the Docker image tag of the version with [tag] to [newTag], trimmed; the version number stays.
     *
     * @param newTag the new Docker image tag; at most 512 Unicode code points after trimming.
     * @return the changed version, the reason of refusal of [newTag] or [TrikStudioVersionTagChange.NotFound];
     * nothing is stored on refusal.
     * @since 1.0.0-beta.1
     */
    fun changeTag(tag: String, newTag: String): TrikStudioVersionTagChange

    /**
     * Deletes the version with [tag] unless something uses it, see [findUsedTags].
     *
     * @since 1.0.0-beta.1
     */
    fun delete(tag: String): TrikStudioVersionDeletion
}

/**
 * Result of [ServiceTrikStudioVersions.changeTag].
 *
 * @since 1.0.0-beta.1
 */
sealed interface TrikStudioVersionTagChange {

    /**
     * The new tag is stored.
     *
     * @property version the version with the new tag.
     * @since 1.0.0-beta.1
     */
    data class Changed(val version: TrikStudioVersion) : TrikStudioVersionTagChange

    /**
     * The new tag is not stored.
     *
     * @property reason the failed check of the new tag: [TrikStudioVersionRejection.BlankTag],
     * [TrikStudioVersionRejection.TagTooLong] or [TrikStudioVersionRejection.TagAlreadyRegistered].
     * @since 1.0.0-beta.1
     */
    data class Rejected(val reason: TrikStudioVersionRejection) : TrikStudioVersionTagChange

    /**
     * No version has the tag.
     *
     * @since 1.0.0-beta.1
     */
    data object NotFound : TrikStudioVersionTagChange
}

/**
 * Result of [ServiceTrikStudioVersions.delete].
 *
 * @since 1.0.0-beta.1
 */
enum class TrikStudioVersionDeletion {

    /**
     * The version is deleted.
     *
     * @since 1.0.0-beta.1
     */
    Deleted,

    /**
     * The version is used and stays.
     *
     * @since 1.0.0-beta.1
     */
    InUse,

    /**
     * No version has the tag.
     *
     * @since 1.0.0-beta.1
     */
    NotFound,
}

/**
 * Result of [ServiceTrikStudioVersions.register].
 *
 * @since 1.0.0-beta.1
 */
sealed interface TrikStudioVersionRegistration {

    /**
     * The version is stored.
     *
     * @property version the registered version with trimmed values.
     * @since 1.0.0-beta.1
     */
    data class Registered(val version: TrikStudioVersion) : TrikStudioVersionRegistration

    /**
     * The version is not stored.
     *
     * @property reason the first failed check.
     * @since 1.0.0-beta.1
     */
    data class Rejected(val reason: TrikStudioVersionRejection) : TrikStudioVersionRegistration
}

/**
 * Reason why [ServiceTrikStudioVersions.register] refused a version.
 *
 * @since 1.0.0-beta.1
 */
enum class TrikStudioVersionRejection {

    /**
     * The version number is empty after trimming.
     *
     * @since 1.0.0-beta.1
     */
    BlankVersion,

    /**
     * The version number is longer than 255 code points.
     *
     * @since 1.0.0-beta.1
     */
    VersionTooLong,

    /**
     * Another registered version has the same version number.
     *
     * @since 1.0.0-beta.1
     */
    VersionAlreadyRegistered,

    /**
     * The tag is empty after trimming.
     *
     * @since 1.0.0-beta.1
     */
    BlankTag,

    /**
     * The tag is longer than 512 code points.
     *
     * @since 1.0.0-beta.1
     */
    TagTooLong,

    /**
     * Another registered version has the same tag.
     *
     * @since 1.0.0-beta.1
     */
    TagAlreadyRegistered,
}
