package tech.testsys.infra.localization.runtime

import tech.testsys.infra.localization.InternalLocalizationApi
import java.time.ZoneId

/**
 * Where a date argument of a message takes its time zone from, when it is not the context zone.
 */
@InternalLocalizationApi
internal sealed interface DateZone {
    /** A zone fixed by the message: a literal `timeZone` option. */
    data class Fixed(val zone: ZoneId) : DateZone

    /** The zone passed as the `ZoneId` message argument [argument] (`timeZone=$argument`). */
    data class Argument(val argument: String) : DateZone
}
