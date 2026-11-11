package tech.testsys.operation.config

/**
 * Configuration of operations on competitions.
 *
 * @property maxParticipants the maximum total number of participants in a competition.
 * @since 1.0.0-beta.1
 */
interface CompetitionConfig {

    val maxParticipants: Int
}
