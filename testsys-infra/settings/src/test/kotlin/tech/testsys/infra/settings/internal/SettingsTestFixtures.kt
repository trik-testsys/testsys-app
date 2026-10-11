@file:OptIn(InternalSettingsApi::class)

package tech.testsys.infra.settings.internal

import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import tech.testsys.infra.settings.api.SettingsStore
import tech.testsys.infra.settings.api.StoredSettings
import java.util.UUID

internal fun settingsEnvironment(): StandardEnvironment = StandardEnvironment()
    .withProperty("testsys.operation.competition.max-participants", "100")
    .withProperty("testsys.operation.email-confirmation.confirmation-code-lifetime", "PT5M")
    .withProperty("testsys.operation.email-confirmation.max-confirmation-attempts", "5")
    .withProperty("testsys.operation.community-invite.ttl", "PT24H")
    .withProperty("testsys.operation.class-invite.ttl", "PT24H")
    .withProperty("testsys.operation.community.public-community-id", "42")

internal class MemorySettingsStore : SettingsStore {
    var stored = StoredSettings(0, emptyMap(), null)
    var beforeWrite: () -> Unit = {}
    var afterWrite: () -> Unit = {}
    var beforeRead: () -> Unit = {}

    override fun read(): StoredSettings {
        beforeRead()
        return stored
    }

    override fun compareAndSet(
        expectedRevision: Long,
        overrides: Map<String, String>,
        changeId: UUID,
        expectedChangeId: UUID?,
    ): StoredSettings? {
        beforeWrite()
        if (stored.revision != expectedRevision || (expectedChangeId != null && stored.changeId != expectedChangeId)) return null
        stored = StoredSettings(expectedRevision + 1, overrides.toMap(), changeId)
        afterWrite()
        return stored
    }
}

internal fun StandardEnvironment.withProperty(key: String, value: String): StandardEnvironment = apply {
    propertySources.addFirst(MapPropertySource(key, mapOf(key to value)))
}
