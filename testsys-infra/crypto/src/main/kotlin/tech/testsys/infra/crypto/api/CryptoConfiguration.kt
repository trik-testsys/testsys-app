package tech.testsys.infra.crypto.api

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

/**
 * Registers the password hashing adapter of the module.
 *
 * @since 1.0.0-beta.1
 */
@Configuration
@ComponentScan(basePackages = ["tech.testsys.infra.crypto.api"])
class CryptoConfiguration
