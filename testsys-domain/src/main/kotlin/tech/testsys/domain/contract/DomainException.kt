package tech.testsys.domain.contract

// TODO

/**
 * Base class of domain-level exceptions. Each carries a [LocalizableMessage] (rendered into [Throwable.message]
 * at construction time) and a machine-readable [ExceptionCode].
 *
 * @property localizableMessage the localizable description of the failure.
 * @property code the machine-readable code identifying the kind of failure.
 * @since %CURRENT_VERSION%
 */
sealed class DomainException(
    val localizableMessage: LocalizableMessage,
    val code: ExceptionCode,
    cause: Throwable? = null,
) : RuntimeException(localizableMessage.localize(), cause) {

    /**
     * Raised when a required entity cannot be found.
     *
     * @since %CURRENT_VERSION%
     */
    class EntityNotFoundException(
        localizableMessage: LocalizableMessage,
        code: ExceptionCode,
        cause: Throwable? = null,
    ) : DomainException(localizableMessage, code, cause)

    /**
     * Raised when input data violates a domain rule or invariant.
     *
     * @since %CURRENT_VERSION%
     */
    class ValidationException(
        localizableMessage: LocalizableMessage,
        code: ExceptionCode,
        cause: Throwable? = null,
    ) : DomainException(localizableMessage, code, cause)

    // TODO append with exceptions
}

/**
 * Marker for machine-readable codes attached to a [DomainException]. Concrete code sets are provided by the modules raising the exceptions.
 *
 * @since %CURRENT_VERSION%
 */
interface ExceptionCode

// TODO remove after completing localization module

/**
 * A message of a [DomainException] that can be rendered as human-readable text.
 *
 * @since %CURRENT_VERSION%
 */
interface LocalizableMessage {

    /**
     * Renders this message as text.
     *
     * @return the localized, human-readable text.
     * @since %CURRENT_VERSION%
     */
    fun localize(): String
}
