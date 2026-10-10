package tech.testsys.web.service.view

import tech.testsys.web.components.navigation.header.CabinetHeader

/**
 * Builds a service header without domain-user navigation or links into the main application.
 *
 * @since %CURRENT_VERSION%
 */
object ServiceHeaders {
    /**
     * Returns the shared brand header with no user-specific controls.
     *
     * @since %CURRENT_VERSION%
     */
    fun header(): CabinetHeader = CabinetHeader()
}
