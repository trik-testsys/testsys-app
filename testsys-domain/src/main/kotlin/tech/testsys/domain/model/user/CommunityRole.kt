package tech.testsys.domain.model.user

/**
 * Non-fixed role in which a user with non-fixed roles is a member of a community.
 *
 * @since %CURRENT_VERSION%
 */
enum class CommunityRole {

    Administrator,
    Manager,
    Developer,
    Student,
    Judge,
}
