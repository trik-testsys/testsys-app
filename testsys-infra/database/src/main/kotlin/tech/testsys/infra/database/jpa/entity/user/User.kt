package tech.testsys.infra.database.jpa.entity.user

import jakarta.persistence.Entity
import tech.testsys.infra.database.jpa.entity.JpaEntity

/**
 * @author Viktor Karasev
 * @author Vyacheslav Buchin
 * @author Roman Shishkin
 *
 * @since %CURRENT_VERSION%
 */
@Entity
class UserJpaEntity(
    val accessToken: String,
) : JpaEntity()