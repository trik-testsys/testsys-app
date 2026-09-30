package tech.testsys.web.dev

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.signals.local.ValueSignal
import org.springframework.core.env.Environment
import tech.testsys.web.ui.TestSysView
import tech.testsys.web.ui.UiTexts
import tech.testsys.web.ui.actions.DownloadContent
import tech.testsys.web.ui.actions.DownloadContext
import tech.testsys.web.ui.actions.downloadAction
import tech.testsys.web.ui.actions.iconDownloadAction
import tech.testsys.web.ui.display.text
import tech.testsys.web.ui.forms.UploadLimits
import tech.testsys.web.ui.forms.codeEditor
import tech.testsys.web.ui.forms.dateRangeInput
import tech.testsys.web.ui.forms.fileDrop
import tech.testsys.web.ui.forms.multiSelect
import tech.testsys.web.ui.forms.radio
import tech.testsys.web.ui.forms.segmentedControl
import tech.testsys.web.ui.forms.switchInput
import tech.testsys.web.ui.layout.PageScope
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.LockSupport

private const val LABEL_COLUMNS = 4
private const val VALUE_COLUMNS = 20
private const val COMPACT_EDITOR_LINES = 3
private const val MAX_FILE_BYTES = 1_048_576
private const val MAX_MEMORY_BYTES = 2_097_152L
private const val PREPARATION_STEPS = 5
private const val PREPARATION_NANOS = 100_000_000L
private const val TRANSFER_BYTES = 262_144L
private const val TRANSFER_CHUNK_BYTES = 1024
private const val TRANSFER_CHUNK_NANOS = 20_000_000L
private const val UNSIGNED_BYTE_MASK = 255

/**
 * Showcase of the remaining fields and real bounded file transfers, available only in dev.
 *
 * @since %CURRENT_VERSION%
 */
@Route("dev/showcase/forms")
@PageTitle("Поля и файлы")
class ShowcaseFormsView(texts: UiTexts, private val environment: Environment) : TestSysView(texts), BeforeEnterObserver {
    init {
        page(showcaseHeader()) {
            showcaseHead("Поля и файлы")
            ordinaryFields()
            block(title = "Режимы поля") {
                editing(onSave = { true }, onCancel = {})
                row {
                    segmentedControl(
                        label = "Изменяемое",
                        labelSize = LABEL_COLUMNS,
                        size = VALUE_COLUMNS,
                        items = listOf("A", "B"),
                        itemLabel = { value -> value },
                    ) { value = "A" }
                }
                row {
                    multiSelect("Только чтение", labelSize = LABEL_COLUMNS, size = VALUE_COLUMNS, listOf("A", "B"), { value -> value }) {
                        value = setOf("A")
                        isEditable = false
                    }
                }
                row {
                    codeEditor("Выключенное", labelSize = LABEL_COLUMNS, size = VALUE_COLUMNS, minLines = COMPACT_EDITOR_LINES) {
                        value = "readonly example"
                        isEnabled = false
                    }
                }
            }
            fileExamples()
        }
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        if (!environment.matchesProfiles(DEV_PROFILE)) event.rerouteToError(NotFoundException::class.java)
    }
}

private fun PageScope.ordinaryFields() {
    block(title = "Обычные поля") {
        row {
            radio(
                label = "Формат",
                labelSize = LABEL_COLUMNS,
                size = VALUE_COLUMNS,
                items = listOf("Практика", "Контест"),
                itemLabel = { value -> value },
            ) { value = "Практика" }
        }
        row { switchInput("Публикация", labelSize = LABEL_COLUMNS, size = VALUE_COLUMNS) { value = true } }
        row {
            segmentedControl(
                label = "Режим",
                labelSize = LABEL_COLUMNS,
                size = VALUE_COLUMNS,
                items = listOf("День", "Неделя", "Месяц"),
                itemLabel = { value -> value },
            ) {
                value = "Неделя"
            }
        }
        row { codeEditor("Код", labelSize = LABEL_COLUMNS, size = VALUE_COLUMNS) { value = "fun main() {\n    println(42)\n}" } }
        row {
            multiSelect(
                label = "Группы",
                labelSize = LABEL_COLUMNS,
                size = VALUE_COLUMNS,
                items = listOf("Первая", "Вторая", "Третья", "Четвёртая"),
                itemLabel = { value -> value },
            ) {
                value = setOf("Первая", "Третья")
            }
        }
        row { dateRangeInput("Период", labelSize = LABEL_COLUMNS, size = VALUE_COLUMNS) }
    }
}

private fun PageScope.fileExamples() {
    block(title = "Настоящий приём файлов", subtitle = "До 2 TXT-файлов по 1 МБ; fail.txt демонстрирует ошибку обработчика") {
        val ui = UI.getCurrent()
        val status = ValueSignal("Файл ещё не получен")
        row {
            fileDrop(
                label = "Файлы",
                limits = UploadLimits(
                    maxFiles = 2,
                    maxFileBytes = MAX_FILE_BYTES,
                    maxMemoryBytes = MAX_MEMORY_BYTES,
                    mimeTypes = setOf("text/*"),
                    extensions = setOf(".txt"),
                ),
                consume = { file ->
                    file.ensureActive()
                    val content = file.openStream().use { stream -> stream.readAllBytes() }
                    if (file.filename == "fail.txt") throw IOException("Demonstration handler failure")
                    ui.access { status.set("Получено: ${file.filename}, ${content.size} байт") }
                },
            )
        }
        row { vertical { text("Статус").bindText(status) } }
    }
    block(title = "Настоящее скачивание", subtitle = "Небольшие генерируемые файлы; повторный клик отменяет текущую передачу") {
        row {
            horizontal {
                downloadAction("Пример TXT", produce = { context -> demoDownload(context, knownLength = true) })
                downloadAction("Без известной длины", produce = { context -> demoDownload(context, knownLength = false) })
                iconDownloadAction("Скачать пример со значком", produce = { context -> demoDownload(context, knownLength = true) })
                val attempts = AtomicInteger()
                downloadAction("Ошибка и повтор", produce = { context ->
                    context.ensureActive()
                    if (attempts.getAndIncrement() == 0) throw IOException("Demonstration producer failure")
                    demoDownload(context, knownLength = true)
                })
            }
        }
    }
}

private fun demoDownload(context: DownloadContext, knownLength: Boolean): DownloadContent {
    repeat(PREPARATION_STEPS) {
        context.ensureActive()
        LockSupport.parkNanos(PREPARATION_NANOS)
    }
    return DownloadContent(
        filename = "testsys-example.txt",
        contentType = "text/plain; charset=UTF-8",
        length = TRANSFER_BYTES.takeIf { knownLength },
    ) {
        object : InputStream() {
            private var remaining = TRANSFER_BYTES
            private val sample = "TestSys streaming example\n".toByteArray()
            private var position = 0

            override fun read(): Int {
                context.ensureActive()
                if (remaining == 0L) return -1
                remaining--
                return sample[(position++ % sample.size)].toInt() and UNSIGNED_BYTE_MASK
            }

            override fun read(bytes: ByteArray, offset: Int, size: Int): Int {
                context.ensureActive()
                if (remaining == 0L) return -1
                LockSupport.parkNanos(TRANSFER_CHUNK_NANOS)
                val count = minOf(size, TRANSFER_CHUNK_BYTES, remaining.toInt())
                repeat(count) { index -> bytes[offset + index] = read().toByte() }
                return count
            }
        }
    }
}
