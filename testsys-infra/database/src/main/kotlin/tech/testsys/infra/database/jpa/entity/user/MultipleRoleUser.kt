package tech.testsys.infra.database.jpa.entity.user

import jakarta.persistence.Entity

/**
 * JPA entity representing a developer role domain entity.
 *
 * @see tech.testsys.domain.model.user.Developer
 * @see tech.testsys.domain.model.user.DeveloperData
 * @since %CURRENT_VERSION%
 */
@Entity
class DeveloperRoleJpaEntity(
    userId: Long
) : RoleEntity(userId)

/**
 * JPA entity representing a student role domain entity.
 *
 * @see tech.testsys.domain.model.user.Student
 * @see tech.testsys.domain.model.user.StudentData
 * @since %CURRENT_VERSION%
 */
@Entity
class StudentRoleJpaEntity(
    userId: Long
) : RoleEntity(userId)

/**
 * JPA entity representing an administrator role domain entity.
 *
 * @see tech.testsys.domain.model.user.Administrator
 * @since %CURRENT_VERSION%
 */
@Entity
class AdministratorRoleJpaEntity(
    userId: Long
) : RoleEntity(userId)

/**
 * JPA entity representing a judge role domain entity.
 *
 * @see tech.testsys.domain.model.user.Judge
 * @see tech.testsys.domain.model.user.JudgeData
 * @since %CURRENT_VERSION%
 */
@Entity
class JudgeRoleJpaEntity(
    userId: Long
) : RoleEntity(userId)

/**
 * JPA entity representing a manager role domain entity.
 *
 * @see tech.testsys.domain.model.user.Manager
 * @see tech.testsys.domain.model.user.ManagerData
 * @since %CURRENT_VERSION%
 */
@Entity
class ManagerRoleJpaEntity(
    userId: Long
) : RoleEntity(userId)
