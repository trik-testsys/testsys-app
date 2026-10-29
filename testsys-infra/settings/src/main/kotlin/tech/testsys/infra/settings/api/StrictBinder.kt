package tech.testsys.infra.settings.api

import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver
import org.springframework.boot.context.properties.source.ConfigurationPropertySources
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.Environment
import org.springframework.util.PropertyPlaceholderHelper
import org.springframework.util.SystemPropertyUtils

/**
 * Binder over [environment] that fails with a `BindException` on an unresolvable placeholder instead of binding its text,
 * as `Binder.get` does; [environment] must be a `ConfigurableEnvironment`.
 *
 * @since 1.0.0-beta.1
 */
fun strictBinder(environment: Environment): Binder {
    val helper = PropertyPlaceholderHelper(
        SystemPropertyUtils.PLACEHOLDER_PREFIX,
        SystemPropertyUtils.PLACEHOLDER_SUFFIX,
        SystemPropertyUtils.VALUE_SEPARATOR,
        SystemPropertyUtils.ESCAPE_CHARACTER,
        false,
    )
    val sources = (environment as ConfigurableEnvironment).propertySources
    return Binder(ConfigurationPropertySources.get(environment), PropertySourcesPlaceholdersResolver(sources, helper))
}
