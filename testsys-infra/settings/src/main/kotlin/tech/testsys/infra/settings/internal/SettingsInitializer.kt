package tech.testsys.infra.settings.internal

import org.springframework.core.env.Environment
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.infra.settings.api.RuntimeSettings
import tech.testsys.infra.settings.api.SettingKey
import tech.testsys.infra.settings.api.SettingsSnapshot
import tech.testsys.infra.settings.api.StoredSettings

@InternalSettingsApi
internal object SettingsInitializer {
    fun initialize(stored: StoredSettings, environment: Environment): RuntimeSettings {
        val values = SettingsResolver(environment).resolve(stored.overrides)
        val snapshot = SettingsSnapshot(stored.revision, values, values.int(SettingKey.MaxMessageBytes))
        val id = environment.getRequiredProperty("testsys.operation.community.public-community-id", Long::class.java)
        return RuntimeSettings(snapshot, CommunityId(id))
    }
}
