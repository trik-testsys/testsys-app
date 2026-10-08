package tech.testsys.operation.config

import tech.testsys.domain.model.group.CommunityId
import tech.testsys.operation.annotation.OperationConfig

/**
 * Configuration of the communities operations rely on.
 *
 * @property publicCommunityId the id of the public community every self-registered user joins.
 * @since %CURRENT_VERSION%
 */
@OperationConfig("community")
interface CommunityConfig {

    val publicCommunityId: CommunityId
}
