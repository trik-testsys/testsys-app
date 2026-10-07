package tech.testsys.web.components.forms

import com.vaadin.flow.component.page.PendingJavaScriptResult
import com.vaadin.flow.dom.Element

internal const val DATE_RANGE_CALENDAR_TAG: String = "testsys-date-range-calendar"
internal const val DATE_RANGE_CALENDAR_MODULE: String = "./testsys-ui/date-range-calendar.tsx"
internal const val FILE_TRANSFER_MODULE: String = "./testsys-ui/file-drop-transfers.ts"
internal const val SEGMENTED_CHOICE_MODULE: String = "./testsys-ui/segmented-choice.ts"
internal const val CODE_EDITOR_MODULE: String = "./testsys-ui/code-editor.ts"
internal const val NATIVE_CODE_AREA_TAG: String = "textarea"
internal const val POPUP_FOCUS_MODULE: String = "./testsys-ui/popup-focus.ts"

/**
 * Returns focus to the trigger only if it stayed in [popup] or was lost, so a click elsewhere keeps its target.
 * The owner component loads [POPUP_FOCUS_MODULE].
 */
internal fun Element.restoreTriggerFocus(popup: Element): PendingJavaScriptResult =
    executeJs("window.testsysPopupFocus.restore(this, $0)", popup)
