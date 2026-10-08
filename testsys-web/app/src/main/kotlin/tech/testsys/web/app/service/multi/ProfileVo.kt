package tech.testsys.web.app.service.multi

import tech.testsys.web.app.service.CommunityVo
import tech.testsys.web.app.service.RoleVo

/**
 * Profile of the current user with non-fixed roles for pages.
 *
 * @property name the nickname of the user.
 * @property email the e-mail address of the user.
 * @property roles each role of the user with the communities the user is a member of in it.
 * @since %CURRENT_VERSION%
 */
data class ProfileVo(val name: String, val email: String, val roles: List<Pair<RoleVo, List<CommunityVo>>>)
