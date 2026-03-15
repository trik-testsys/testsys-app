package tech.testsys.infra.database.jpa.entity.user

import jakarta.persistence.Entity
import jakarta.persistence.MappedSuperclass
import tech.testsys.infra.database.jpa.entity.JpaEntity

/**
 * @author Viktor Karasev
 * @author Vyacheslav Buchin
 * @author Roman Shishkin
 *
 * @since %CURRENT_VERSION%
 */
@MappedSuperclass
abstract class RoleEntity(
    val userId: Long
) : JpaEntity()

/**
 * @author Viktor Karasev
 * @author Vyacheslav Buchin
 * @author Roman Shishkin
 *
 * @since %CURRENT_VERSION%
 */
@Entity
class DeveloperRoleJpaEntity(
    userId: Long
) : RoleEntity(userId)

/**
 * @author Viktor Karasev
 * @author Vyacheslav Buchin
 * @author Roman Shishkin
 *
 * @since %CURRENT_VERSION%
 */
@Entity
class StudentRoleJpaEntity(
    userId: Long
) : RoleEntity(userId)

/**
 * @author Viktor Karasev
 * @author Vyacheslav Buchin
 * @author Roman Shishkin
 *
 * @since %CURRENT_VERSION%
 */
@Entity
class AdministratorRoleJpaEntity(
    userId: Long
) : RoleEntity(userId)

/**
 * @author Viktor Karasev
 * @author Vyacheslav Buchin
 * @author Roman Shishkin
 *
 * @since %CURRENT_VERSION%
 */
@Entity
class ManagerRoleJpaEntity(
    userId: Long
) : RoleEntity(userId)