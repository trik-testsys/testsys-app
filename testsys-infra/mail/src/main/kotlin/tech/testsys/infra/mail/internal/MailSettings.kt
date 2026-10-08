package tech.testsys.infra.mail.internal

import tech.testsys.infra.localization.bundle.SupportedRegion
import java.time.ZoneId

/**
 * Settings of the letters the mail module sends.
 *
 * @property from the sender address of every letter.
 * @property region the region whose messages render the letters.
 * @property timeZone the time zone of the rendered messages.
 * @since %CURRENT_VERSION%
 */
@InternalMailApi
class MailSettings(
    val from: String,
    val region: SupportedRegion,
    val timeZone: ZoneId,
) {
    init {
        require(from.isNotBlank()) { "Mail sender address must not be blank" }
    }
}
