package tech.testsys.infra.database.jpa.entity.user

import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.JpaEntity

/**
 * @since %CURRENT_VERSION%
 */
@Entity
class UserJpaEntity(
    val accessToken: String,
) : JpaEntity()