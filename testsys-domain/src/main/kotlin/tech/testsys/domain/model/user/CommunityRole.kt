package tech.testsys.domain.model.user

/**
 * Role in which a user with non-fixed roles is made a member of a community by an invite code or an administrator.
 *
 * @since %CURRENT_VERSION%
 */
enum class CommunityRole {

    Manager,
    Developer,
    Student,
}
