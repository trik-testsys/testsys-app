package tech.testsys.operation.config

/**
 * Configuration of operations on competitions.
 *
 * @property maxParticipants the maximum total number of participants in a competition.
 * @since %CURRENT_VERSION%
 */
interface CompetitionConfig {

    val maxParticipants: Int
}
