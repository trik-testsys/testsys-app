package tech.testsys.web.components.feedback

import tech.testsys.web.components.core.IconName

/**
 * Meaning of an alert or a toast; the colour and the icon follow it.
 *
 * @since %CURRENT_VERSION%
 */
enum class FeedbackKind(
    internal val alertTone: String,
    internal val alertIcon: IconName,
    internal val toastTone: String,
    internal val toastIcon: IconName,
) {
    Info("info", IconName.Info, "info", IconName.Info),
    Warning("warning", IconName.TriangleAlert, "warning", IconName.TriangleAlert),
    Error("danger", IconName.CircleX, "error", IconName.X),
    Success("success", IconName.CircleCheck, "success", IconName.Check),
}
