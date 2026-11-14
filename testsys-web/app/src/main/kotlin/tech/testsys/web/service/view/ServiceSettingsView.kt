package tech.testsys.web.service.view

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.infra.localization.PageService
import tech.testsys.infra.settings.api.RuntimeSettings
import tech.testsys.infra.settings.api.SettingsSnapshot
import tech.testsys.infra.settings.api.catalog.SettingKey
import tech.testsys.infra.settings.api.service.SettingsChangeResult
import tech.testsys.infra.settings.api.service.SettingsPersistence
import tech.testsys.infra.settings.api.service.SettingsService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.alert
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.texts.currentLocalization

/**
 * Service-only settings editor with a block per settings group; a block saves its changed settings atomically, and
 * the page is refreshed on navigation and after successful changes.
 *
 * @since 1.0.0-beta.1
 */
@Route("settings")
@RolesAllowed("SERVICE")
class ServiceSettingsView(
    private val headers: ServiceHeaders,
    private val settings: SettingsService,
    private val runtime: RuntimeSettings,
) : TestSysView(),
    BeforeEnterObserver,
    HasDynamicTitle {
    private val texts: PageService get() = currentLocalization().pageService

    override fun getPageTitle(): String = texts.settingsTitle()

    override fun beforeEnter(event: BeforeEnterEvent) = showSettings()

    private fun showSettings() {
        val overview = settings.overview()
        val snapshot = overview.snapshot
        val isKnown = overview.persistence == SettingsPersistence.Known
        page(headers.header(active = ServiceHeaders.MENU_SECTION)) {
            head(texts.settingsTitle())
            // Two blocks a row: the groups are short, and full-width blocks leave the page mostly empty.
            GROUPS.withIndex().chunked(BLOCKS_IN_ROW).forEach { groups ->
                row {
                    groups.forEach { (index, entry) ->
                        val (group, keys) = entry
                        block(title = groupTitle(group), size = BLOCK_SIZE) {
                            // Each block binds only its keys, so its form changes only them.
                            val form = ServiceSettingsForm(snapshot)
                            if (isKnown) {
                                editing(onSave = {
                                    val changes = form.changes()
                                    if (changes == null) false else applyChanges(overview.revision, changes, form)
                                }, onCancel = form::cancel)
                            } else if (index == 0) {
                                row { alert(FeedbackKind.Warning, texts.settingsUncertain()) }
                            }

                            keys.forEach { key ->
                                row {
                                    val input = textInput(label(key), labelSize = 10, size = 12, hint = key.property)
                                    form.bind(key, input)
                                    input.isEditable = isKnown
                                    horizontal(size = 2) {
                                        action(label = settingHelp(key, snapshot), icon = IconName.Info, iconOnly = true)
                                    }
                                }
                            }

                            val savedMaxMessageBytes = snapshot.values.int(SettingKey.MaxMessageBytes)
                            if (SettingKey.MaxMessageBytes in keys && savedMaxMessageBytes != snapshot.activeMaxMessageBytes) {
                                row {
                                    alert(
                                        FeedbackKind.Info,
                                        texts.settingsRestartPending(active = snapshot.activeMaxMessageBytes, saved = savedMaxMessageBytes),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            row {
                block(title = texts.settingsGroupCommunities(), size = BLOCK_SIZE) {
                    row {
                        textInput(texts.settingsPublicCommunity(), labelSize = 10, size = 12) {
                            value = runtime.publicCommunityId.value.toString()
                            isEditable = false
                        }
                    }
                }
            }
        }
    }

    private fun applyChanges(revision: Long, changes: Map<SettingKey, String>, form: ServiceSettingsForm): Boolean {
        return when (val result = settings.change(revision, changes)) {
            is SettingsChangeResult.Applied -> {
                toast(FeedbackKind.Success, texts.settingsApplied())
                showSettings()
                true
            }
            is SettingsChangeResult.Invalid -> {
                form.errors(result.errors)
                false
            }
            SettingsChangeResult.Stale -> {
                toast(FeedbackKind.Error, texts.settingsStale())
                false
            }
            SettingsChangeResult.Failed -> {
                toast(FeedbackKind.Error, texts.settingsFailed())
                false
            }
            SettingsChangeResult.Uncertain -> {
                toast(FeedbackKind.Error, texts.settingsUncertainResult())
                showSettings()
                false
            }
        }
    }

    private fun settingHelp(key: SettingKey, snapshot: SettingsSnapshot): String = when (key) {
        SettingKey.MaxParticipants -> texts.settingsHelpMaxParticipants()
        SettingKey.ConfirmationCodeLifetime -> texts.settingsHelpConfirmationCodeLifetime()
        SettingKey.MaxConfirmationAttempts -> texts.settingsHelpMaxConfirmationAttempts()
        SettingKey.CommunityInviteTtl -> texts.settingsHelpCommunityInviteTtl()
        SettingKey.ClassInviteTtl -> texts.settingsHelpClassInviteTtl()
        SettingKey.GradingMaxAttempts -> texts.settingsHelpGradingMaxAttempts()
        SettingKey.TotalTimeout -> texts.settingsHelpTotalTimeout()
        SettingKey.RpcTimeout -> texts.settingsHelpRpcTimeout()
        SettingKey.ShouldRecordVideo -> texts.settingsHelpShouldRecordVideo()
        SettingKey.PollInterval -> texts.settingsHelpPollInterval()
        SettingKey.StatusTimeout -> texts.settingsHelpStatusTimeout()
        SettingKey.MaxMessageBytes -> texts.settingsHelpMaxMessageBytes(active = snapshot.activeMaxMessageBytes)
        SettingKey.MaxTimeLimitMillis -> texts.settingsHelpMaxTimeLimitMillis()
        SettingKey.ScorePrefix -> texts.settingsHelpScorePrefix()
        SettingKey.CleanupMinimumAge -> texts.settingsHelpCleanupMinimumAge()
        SettingKey.CleanupBatchSize -> texts.settingsHelpCleanupBatchSize()
    }

    private fun label(key: SettingKey): String = when (key) {
        SettingKey.MaxParticipants -> texts.settingsLabelMaxParticipants()
        SettingKey.ConfirmationCodeLifetime -> texts.settingsLabelConfirmationCodeLifetime()
        SettingKey.MaxConfirmationAttempts -> texts.settingsLabelMaxConfirmationAttempts()
        SettingKey.CommunityInviteTtl -> texts.settingsLabelCommunityInviteTtl()
        SettingKey.ClassInviteTtl -> texts.settingsLabelClassInviteTtl()
        SettingKey.GradingMaxAttempts -> texts.settingsLabelGradingMaxAttempts()
        SettingKey.TotalTimeout -> texts.settingsLabelTotalTimeout()
        SettingKey.RpcTimeout -> texts.settingsLabelRpcTimeout()
        SettingKey.ShouldRecordVideo -> texts.settingsLabelShouldRecordVideo()
        SettingKey.PollInterval -> texts.settingsLabelPollInterval()
        SettingKey.StatusTimeout -> texts.settingsLabelStatusTimeout()
        SettingKey.MaxMessageBytes -> texts.settingsLabelMaxMessageBytes()
        SettingKey.MaxTimeLimitMillis -> texts.settingsLabelMaxTimeLimitMillis()
        SettingKey.ScorePrefix -> texts.settingsLabelScorePrefix()
        SettingKey.CleanupMinimumAge -> texts.settingsLabelCleanupMinimumAge()
        SettingKey.CleanupBatchSize -> texts.settingsLabelCleanupBatchSize()
    }

    private fun groupTitle(group: SettingsGroup): String = when (group) {
        SettingsGroup.Competitions -> texts.settingsGroupCompetitions()
        SettingsGroup.EmailConfirmation -> texts.settingsGroupEmailConfirmation()
        SettingsGroup.Invites -> texts.settingsGroupInvites()
        SettingsGroup.Grading -> texts.settingsGroupGrading()
        SettingsGroup.Diagnostics -> texts.settingsGroupDiagnostics()
        SettingsGroup.FileCleanup -> texts.settingsGroupFileCleanup()
    }

    /** Settings blocks in the order of the page. */
    private enum class SettingsGroup { Competitions, EmailConfirmation, Invites, Grading, Diagnostics, FileCleanup }

    private companion object {
        private const val BLOCKS_IN_ROW = 2
        private const val BLOCK_SIZE = 12

        private val GROUPS = listOf(
            SettingsGroup.Competitions to listOf(SettingKey.MaxParticipants),
            SettingsGroup.EmailConfirmation to listOf(SettingKey.ConfirmationCodeLifetime, SettingKey.MaxConfirmationAttempts),
            SettingsGroup.Invites to listOf(SettingKey.CommunityInviteTtl, SettingKey.ClassInviteTtl),
            SettingsGroup.Grading to listOf(
                SettingKey.GradingMaxAttempts,
                SettingKey.TotalTimeout,
                SettingKey.RpcTimeout,
                SettingKey.ShouldRecordVideo,
                SettingKey.PollInterval,
                SettingKey.StatusTimeout,
                SettingKey.MaxMessageBytes,
            ),
            SettingsGroup.Diagnostics to listOf(SettingKey.MaxTimeLimitMillis, SettingKey.ScorePrefix),
            SettingsGroup.FileCleanup to listOf(SettingKey.CleanupMinimumAge, SettingKey.CleanupBatchSize),
        )
    }
}
