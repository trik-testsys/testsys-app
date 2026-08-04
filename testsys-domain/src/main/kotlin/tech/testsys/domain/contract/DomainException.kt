package tech.testsys.domain.contract

// TODO
sealed class DomainException(
    val localizableMessage: LocalizableMessage,
    val code: ExceptionCode,
    cause: Throwable? = null,
) : RuntimeException(localizableMessage.localize(), cause) {

    class EntityNotFoundException(
        localizableMessage: LocalizableMessage,
        code: ExceptionCode,
        cause: Throwable? = null,
    ) : DomainException(localizableMessage, code, cause)

    class ValidationException(
        localizableMessage: LocalizableMessage,
        code: ExceptionCode,
        cause: Throwable? = null,
    ) : DomainException(localizableMessage, code, cause)

    // TODO append with exceptions
}

interface ExceptionCode

// TODO remove after completing localization module
interface LocalizableMessage {

    fun localize(): String
}
