package tech.testsys.web.app.security

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

internal class MutableClock : Clock() {
    var now: Instant = Instant.parse("2026-01-01T00:00:00Z")

    override fun instant(): Instant = now
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
}
