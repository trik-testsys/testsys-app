package tech.testsys.infra.database.api.service

import tech.testsys.domain.model.task.TrikStudioVersion

/**
 * Service listing, registration, tag change and deletion of TRIK Studio versions for the service cabinet.
 *
 * @since %CURRENT_VERSION%
 */
interface ServiceTrikStudioVersions {

    /**
     * Finds every registered TRIK Studio version.
     *
     * @return the registered versions ordered by Docker image tag, or an empty list when none is registered.
     * @since %CURRENT_VERSION%
     */
    fun findAll(): List<TrikStudioVersion>

    /**
     * Registers a TRIK Studio version with the Docker image [tag] and the version number [version], both trimmed.
     *
     * @param tag the Docker image tag; at most 512 Unicode code points after trimming.
     * @param version the version number; at most 255 Unicode code points after trimming.
     * @return the registered version, or the first reason of refusal, checking the version number before the tag;
     * nothing is stored on refusal.
     * @since %CURRENT_VERSION%
     */
    fun register(tag: String, version: String): TrikStudioVersionRegistration

    /**
     * Finds the tags of the registered versions that a contest, a submission, a task revision or a task validation
     * request uses.
     *
     * @since %CURRENT_VERSION%
     */
    fun findUsedTags(): Set<String>

    /**
     * Changes the Docker image tag of the version with [tag] to [newTag], trimmed; the version number stays.
     *
     * @param newTag the new Docker image tag; at most 512 Unicode code points after trimming.
     * @return the changed version, the reason of refusal of [newTag] or [TrikStudioVersionTagChange.NotFound];
     * nothing is stored on refusal.
     * @since %CURRENT_VERSION%
     */
    fun changeTag(tag: String, newTag: String): TrikStudioVersionTagChange

    /**
     * Deletes the version with [tag] unless something uses it, see [findUsedTags].
     *
     * @since %CURRENT_VERSION%
     */
    fun delete(tag: String): TrikStudioVersionDeletion
}

/**
 * Result of [ServiceTrikStudioVersions.changeTag].
 *
 * @since %CURRENT_VERSION%
 */
sealed interface TrikStudioVersionTagChange {

    /**
     * The new tag is stored.
     *
     * @property version the version with the new tag.
     * @since %CURRENT_VERSION%
     */
    data class Changed(val version: TrikStudioVersion) : TrikStudioVersionTagChange

    /**
     * The new tag is not stored.
     *
     * @property reason the failed check of the new tag: [TrikStudioVersionRejection.BlankTag],
     * [TrikStudioVersionRejection.TagTooLong] or [TrikStudioVersionRejection.TagAlreadyRegistered].
     * @since %CURRENT_VERSION%
     */
    data class Rejected(val reason: TrikStudioVersionRejection) : TrikStudioVersionTagChange

    /**
     * No version has the tag.
     *
     * @since %CURRENT_VERSION%
     */
    data object NotFound : TrikStudioVersionTagChange
}

/**
 * Result of [ServiceTrikStudioVersions.delete].
 *
 * @since %CURRENT_VERSION%
 */
enum class TrikStudioVersionDeletion {

    /**
     * The version is deleted.
     *
     * @since %CURRENT_VERSION%
     */
    Deleted,

    /**
     * The version is used and stays.
     *
     * @since %CURRENT_VERSION%
     */
    InUse,

    /**
     * No version has the tag.
     *
     * @since %CURRENT_VERSION%
     */
    NotFound,
}

/**
 * Result of [ServiceTrikStudioVersions.register].
 *
 * @since %CURRENT_VERSION%
 */
sealed interface TrikStudioVersionRegistration {

    /**
     * The version is stored.
     *
     * @property version the registered version with trimmed values.
     * @since %CURRENT_VERSION%
     */
    data class Registered(val version: TrikStudioVersion) : TrikStudioVersionRegistration

    /**
     * The version is not stored.
     *
     * @property reason the first failed check.
     * @since %CURRENT_VERSION%
     */
    data class Rejected(val reason: TrikStudioVersionRejection) : TrikStudioVersionRegistration
}

/**
 * Reason why [ServiceTrikStudioVersions.register] refused a version.
 *
 * @since %CURRENT_VERSION%
 */
enum class TrikStudioVersionRejection {

    /**
     * The version number is empty after trimming.
     *
     * @since %CURRENT_VERSION%
     */
    BlankVersion,

    /**
     * The version number is longer than 255 code points.
     *
     * @since %CURRENT_VERSION%
     */
    VersionTooLong,

    /**
     * Another registered version has the same version number.
     *
     * @since %CURRENT_VERSION%
     */
    VersionAlreadyRegistered,

    /**
     * The tag is empty after trimming.
     *
     * @since %CURRENT_VERSION%
     */
    BlankTag,

    /**
     * The tag is longer than 512 code points.
     *
     * @since %CURRENT_VERSION%
     */
    TagTooLong,

    /**
     * Another registered version has the same tag.
     *
     * @since %CURRENT_VERSION%
     */
    TagAlreadyRegistered,
}
