package tech.testsys.infra.settings.api

/**
 * Prepares gated resources without changing the active snapshot; preparation failures occur before persistence.
 *
 * @since %CURRENT_VERSION%
 */
fun interface SettingsChangePreparation {
    /**
     * Prepares resources gated by the identity of [candidate]; close releases them after abort or supersession.
     *
     * @since %CURRENT_VERSION%
     */
    fun prepare(candidate: SettingsSnapshot): AutoCloseable
}
