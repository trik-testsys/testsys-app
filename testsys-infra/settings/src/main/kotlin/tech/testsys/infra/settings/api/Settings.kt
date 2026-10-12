package tech.testsys.infra.settings.api

import org.springframework.core.env.Environment

/**
 * Marks a class bound from settings by its constructor; unlike `@ConfigurationProperties`, Spring Boot does not rebind it.
 *
 * @property prefix the property prefix of the bound class.
 * @since %CURRENT_VERSION%
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SettingsProperties(val prefix: String)

/**
 * Source of the current value of a [SettingsProperties] class [T]; inject it instead of [T], which is not a bean.
 *
 * @param T the properties class.
 * @since %CURRENT_VERSION%
 */
fun interface Settings<T : Any> {
    /**
     * Captures the current value; call once per operation and pass the result on.
     *
     * @since %CURRENT_VERSION%
     */
    fun current(): T
}

/**
 * Binds [type] once to reject invalid configuration at startup, then reads it from the current snapshot of [runtime]
 * over [environment], or returns the value bound from [environment] alone without the shared holder.
 * With [runtime], it registers a preparation that binds [type] to every candidate snapshot before it is saved.
 *
 * @param T the properties class.
 * @throws org.springframework.boot.context.properties.bind.BindException if a value cannot be bound or is rejected.
 * @since %CURRENT_VERSION%
 */
fun <T : Any> settingsOf(type: Class<T>, environment: Environment, runtime: RuntimeSettings?): Settings<T> {
    if (runtime == null) {
        val value = bindSettings(type, environment)
        return Settings { value }
    }

    runtime.register { candidate ->
        candidate.get(type, environment)
        AutoCloseable {}
    }
    return Settings { runtime.snapshot().get(type, environment) }
}
