package tech.testsys.infra.settings.internal

import org.springframework.boot.context.properties.bind.Binder
import org.springframework.core.env.Environment
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.infra.settings.api.RuntimeSettings
import tech.testsys.infra.settings.api.SettingsSnapshot
import tech.testsys.infra.settings.api.catalog.SettingKey
import tech.testsys.infra.settings.api.store.StoredSettings

@InternalSettingsApi
internal object SettingsInitializer {
    private const val PUBLIC_COMMUNITY_ID = "testsys.operation.community.public-community-id"

    fun initialize(stored: StoredSettings, environment: Environment): RuntimeSettings {
        val values = SettingsResolver(environment).resolve(stored.overrides)
        val snapshot = SettingsSnapshot(stored.revision, values, values.int(SettingKey.MaxMessageBytes))
        val id = checkNotNull(Binder.get(environment).bind(PUBLIC_COMMUNITY_ID, Long::class.java).orElse(null)) {
            "$PUBLIC_COMMUNITY_ID is required"
        }
        return RuntimeSettings(snapshot, CommunityId(id))
    }
}
