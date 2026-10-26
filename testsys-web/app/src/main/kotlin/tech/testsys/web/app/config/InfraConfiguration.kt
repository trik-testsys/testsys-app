package tech.testsys.web.app.config

import org.springframework.aop.Advisor
import org.springframework.aop.ClassFilter
import org.springframework.aop.support.DefaultPointcutAdvisor
import org.springframework.aop.support.StaticMethodMatcherPointcut
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Role
import org.springframework.core.Ordered
import org.springframework.util.ClassUtils
import tech.testsys.infra.database.api.DatabaseConfiguration
import tech.testsys.infra.database.api.service.ServiceTrikStudioVersions
import tech.testsys.infra.database.api.service.ServiceUserSearch
import tech.testsys.infra.diagnostics.api.DiagnosticsConfiguration
import tech.testsys.infra.grpc.api.GradingNodeRegistry
import tech.testsys.infra.grpc.api.GrpcConfiguration
import tech.testsys.infra.mail.api.MailConfiguration
import tech.testsys.infra.monitoring.api.MonitoringConfiguration
import tech.testsys.infra.settings.api.SettingsConfiguration
import tech.testsys.infra.settings.api.registry.GradingNodeStore
import tech.testsys.infra.settings.api.service.SettingsService
import tech.testsys.infra.settings.api.store.SettingsStore
import java.lang.reflect.Method

/**
 * Connects the configurations of the infrastructure modules that implement the ports of the operations and count
 * technical failures. The companion object registers the call log of the infrastructure beans.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration
@Import(
    DatabaseConfiguration::class,
    SettingsConfiguration::class,
    GrpcConfiguration::class,
    DiagnosticsConfiguration::class,
    MailConfiguration::class,
    MonitoringConfiguration::class,
)
class InfraConfiguration {
    /**
     * Static infrastructure beans: they are created before the auto-proxy creator wraps the application beans.
     *
     * @since %CURRENT_VERSION%
     */
    companion object {
        private const val CONTRACT_PACKAGE = "tech.testsys.domain.contract"
        private const val INFRA_PACKAGE = "tech.testsys.infra."
        private val loggedInterfaces = setOf(
            SettingsService::class.java,
            SettingsStore::class.java,
            GradingNodeStore::class.java,
            GradingNodeRegistry::class.java,
            ServiceUserSearch::class.java,
            ServiceTrikStudioVersions::class.java,
        )

        /**
         * Writes at DEBUG the calls of the domain ports and service interfaces implemented by the infrastructure
         * modules, outside their transactions and transaction repeats.
         *
         * @since %CURRENT_VERSION%
         */
        @JvmStatic
        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        fun infrastructureCallLoggingAdvisor(): Advisor {
            val pointcut = object : StaticMethodMatcherPointcut() {
                override fun matches(method: Method, targetClass: Class<*>): Boolean =
                    loggedInterfacesOf(targetClass).any { type -> type.methods.any { declared -> declared.name == method.name } }
            }

            pointcut.classFilter = ClassFilter { type ->
                type.name.startsWith(INFRA_PACKAGE) && loggedInterfacesOf(type).isNotEmpty()
            }

            return DefaultPointcutAdvisor(pointcut, CallLoggingInterceptor()).apply { order = Ordered.HIGHEST_PRECEDENCE }
        }

        private fun loggedInterfacesOf(type: Class<*>): List<Class<*>> = ClassUtils.getAllInterfacesForClassAsSet(type)
            .filter { candidate -> candidate in loggedInterfaces || candidate.packageName.startsWith(CONTRACT_PACKAGE) }
    }
}
