package tech.testsys.infra.database.api.service

import tech.testsys.domain.model.task.TrikStudioVersion

/**
 * Service listing and registration of TRIK Studio versions for the service cabinet.
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
