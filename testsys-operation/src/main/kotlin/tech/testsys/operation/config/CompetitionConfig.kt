package tech.testsys.operation.config

import tech.testsys.operation.annotation.ConfigProperty
import tech.testsys.operation.annotation.OperationConfig

/**
 * Configuration of operations on competitions.
 *
 * @property maxParticipants the maximum total number of participants in a competition.
 * @since %CURRENT_VERSION%
 */
@OperationConfig("competition")
interface CompetitionConfig {

    @ConfigProperty(dynamic = true)
    val maxParticipants: Int
}
