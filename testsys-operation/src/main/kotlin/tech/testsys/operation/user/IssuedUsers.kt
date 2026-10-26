package tech.testsys.operation.user

import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.Participant

/**
 * A participant created by the manager together with its original access code, which cannot be read back from storage.
 *
 * @property participant the saved participant.
 * @property accessToken the original access code issued to [participant].
 * @since %CURRENT_VERSION%
 */
data class IssuedParticipant(
    val participant: Participant,
    val accessToken: String,
)

/**
 * An observer created in a community together with its original access code, which cannot be read back from storage.
 *
 * @property observer the saved observer.
 * @property accessToken the original access code issued to [observer].
 * @since %CURRENT_VERSION%
 */
data class IssuedObserver(
    val observer: Observer,
    val accessToken: String,
)
