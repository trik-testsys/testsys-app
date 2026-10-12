package tech.testsys.infra.settings.api

import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver
import org.springframework.boot.context.properties.source.ConfigurationPropertySources
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource
import org.springframework.core.env.Environment
import tech.testsys.infra.settings.api.catalog.SettingKey
import tech.testsys.infra.settings.api.catalog.SettingsValues
import java.util.concurrent.ConcurrentHashMap

/**
 * Complete effective settings publication, including the transport limit fixed at process startup.
 *
 * @property revision the durable revision known at publication.
 * @property values the saved, resolved values.
 * @property activeMaxMessageBytes the inbound limit used by every channel in this process.
 * @since %CURRENT_VERSION%
 */
@Suppress("DataClassContainsFunctions")
data class SettingsSnapshot(val revision: Long, val values: SettingsValues, val activeMaxMessageBytes: Int) {
    private val bound = ConcurrentHashMap<Pair<Class<*>, Environment>, Any>()

    /**
     * Binds the [SettingsProperties] class [type] from the saved values over [environment] once per [environment];
     * a failed binding is retried on the next call. The limit fixed until restart is bound as [activeMaxMessageBytes].
     *
     * @param T the properties class.
     * @throws org.springframework.boot.context.properties.bind.BindException if a value cannot be bound or is rejected.
     * @since %CURRENT_VERSION%
     */
    fun <T : Any> get(type: Class<T>, environment: Environment): T = type.cast(
        bound.computeIfAbsent(type to environment) {
            val saved = values.properties() + (SettingKey.MaxMessageBytes.property to activeMaxMessageBytes.toString())
            bindSettings(type, environment, saved)
        },
    )

    /**
     * Binds the [SettingsProperties] class [T] from the saved values over [environment] once per [environment].
     *
     * @param T the properties class.
     * @since %CURRENT_VERSION%
     */
    inline fun <reified T : Any> get(environment: Environment): T = get(T::class.java, environment)
}

/** Binds the [SettingsProperties] class [type] from [saved] values over [environment], resolving placeholders only in [environment]. */
internal fun <T : Any> bindSettings(type: Class<T>, environment: Environment, saved: Map<String, String> = emptyMap()): T {
    val annotation = requireNotNull(type.getAnnotation(SettingsProperties::class.java)) {
        "${type.name} is not annotated with @SettingsProperties"
    }
    val placeholders = PropertySourcesPlaceholdersResolver(environment)
    val sources = listOf(MapConfigurationPropertySource(saved.mapValues { (_, text) -> SavedValue(text) })) +
        ConfigurationPropertySources.get(environment)
    val binder = Binder(sources) { value ->
        if (value is SavedValue) value.text else placeholders.resolvePlaceholders(value)
    }
    return binder.bindOrCreate(annotation.prefix, Bindable.of(type))
}

// Saved values are literal text: the marker keeps the resolver from expanding `${...}` in them.
private class SavedValue(val text: String)
