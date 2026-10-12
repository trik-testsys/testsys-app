package tech.testsys.infra.settings.api

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import tech.testsys.infra.monitoring.api.TechnicalFailures
import tech.testsys.infra.settings.internal.InternalSettingsApi
import tech.testsys.infra.settings.internal.SettingsCoordinator
import tech.testsys.infra.settings.internal.SettingsInitializer
import tech.testsys.infra.settings.internal.SettingsResolver

/**
 * Restores one shared holder from committed storage before consumers can start; invalid storage aborts startup.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration
@OptIn(InternalSettingsApi::class)
class SettingsConfiguration {
    /**
     * Restores the last durable set; no fallback hides database failures.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun runtimeSettings(store: SettingsStore, environment: Environment): RuntimeSettings =
        SettingsInitializer.initialize(store.read(), environment)

    /**
     * Serializes service changes in the shared infrastructure context and counts their technical failures in [failures].
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun settingsService(
        store: SettingsStore,
        runtime: RuntimeSettings,
        environment: Environment,
        failures: TechnicalFailures? = null,
    ): SettingsService = SettingsCoordinator(
        store = store,
        runtime = runtime,
        resolver = SettingsResolver(environment),
        stored = store.read(),
        failures = failures ?: TechnicalFailures.NONE,
    )
}
