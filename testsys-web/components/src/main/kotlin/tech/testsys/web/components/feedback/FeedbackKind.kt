@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.feedback

import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.InternalComponentsApi

/**
 * Meaning of an alert or a toast; the colour and the icon follow it.
 *
 * @since %CURRENT_VERSION%
 */
enum class FeedbackKind(
    internal val alertClass: CssClass,
    internal val alertIcon: IconName,
    internal val toastClass: CssClass,
    internal val toastIcon: IconName,
) {
    Info(
        alertClass = CssClass.AlertInfo,
        alertIcon = IconName.Info,
        toastClass = CssClass.ToastInfo,
        toastIcon = IconName.Info,
    ),
    Warning(
        alertClass = CssClass.AlertWarning,
        alertIcon = IconName.TriangleAlert,
        toastClass = CssClass.ToastWarning,
        toastIcon = IconName.TriangleAlert,
    ),
    Error(
        alertClass = CssClass.AlertDanger,
        alertIcon = IconName.CircleX,
        toastClass = CssClass.ToastError,
        toastIcon = IconName.X,
    ),
    Success(
        alertClass = CssClass.AlertSuccess,
        alertIcon = IconName.CircleCheck,
        toastClass = CssClass.ToastSuccess,
        toastIcon = IconName.Check,
    ),
}
