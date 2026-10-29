package tech.testsys.infra.settings.api

/**
 * Prepares gated resources without changing the active snapshot; preparation failures occur before persistence.
 *
 * @since 1.0.0-beta.1
 */
fun interface SettingsChangePreparation {
    /**
     * Prepares resources gated by the identity of [candidate]; close releases them after abort or supersession.
     *
     * @since 1.0.0-beta.1
     */
    fun prepare(candidate: SettingsSnapshot): AutoCloseable
}
