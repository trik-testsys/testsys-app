package tech.testsys.infra.mail.internal

import org.springframework.boot.context.properties.bind.Name
import tech.testsys.infra.localization.bundle.SupportedRegion
import java.time.Duration
import java.time.ZoneId

/**
 * Parameters of the mail module bound from `testsys.mail`, except the password, which is bound on its own so that
 * a failure report never shows it.
 *
 * @property host the SMTP host.
 * @property port the SMTP port.
 * @property username the SMTP user name, empty without authorization.
 * @property isSmtpAuth whether the server requires authorization.
 * @property isSsl whether the connection uses SSL from the start.
 * @property isStarttls whether the connection switches to TLS with STARTTLS.
 * @property timeout the timeout of connecting, reading and writing.
 * @property from the sender address of every letter.
 * @property region the region whose messages render the letters.
 * @property timeZone the time zone of the rendered messages.
 * @since 1.0.0-beta.1
 */
@InternalMailApi
class MailProperties(
    val host: String,
    val port: Int,
    val username: String?,
    @Name("smtp-auth") val isSmtpAuth: Boolean,
    @Name("ssl") val isSsl: Boolean,
    @Name("starttls") val isStarttls: Boolean,
    val timeout: Duration,
    val from: String,
    val region: SupportedRegion,
    val timeZone: ZoneId,
)
