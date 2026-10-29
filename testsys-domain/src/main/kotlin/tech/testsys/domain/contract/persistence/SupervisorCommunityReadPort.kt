package tech.testsys.domain.contract.persistence

import tech.testsys.domain.model.group.Community

/**
 * Read-only selection of every community for the Supervisor cabinet.
 *
 * @since 1.0.0-beta.1
 */
interface SupervisorCommunityReadPort {

    /**
     * Synchronously reads all communities in ascending id order without changing them; storage failures propagate.
     *
     * @return all communities, or an empty list if none exist.
     * @since 1.0.0-beta.1
     */
    fun findAllCommunities(): List<Community>
}
