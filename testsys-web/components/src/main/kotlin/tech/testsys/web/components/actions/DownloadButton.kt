@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.actions

import com.vaadin.flow.component.Html
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.dom.Element
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.server.streams.DownloadEvent
import com.vaadin.flow.server.streams.DownloadHandler
import com.vaadin.flow.signals.Signal
import com.vaadin.flow.signals.local.ValueSignal
import org.slf4j.LoggerFactory
import tech.testsys.web.components.Background
import tech.testsys.web.components.Bindable
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.UiTexts
import tech.testsys.web.components.core.AriaLive
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.CssDisplay
import tech.testsys.web.components.core.CssUnit
import tech.testsys.web.components.core.ElementRole
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.core.clickClient
import tech.testsys.web.components.core.removeClassName
import tech.testsys.web.components.core.setAriaBusy
import tech.testsys.web.components.core.setAriaHidden
import tech.testsys.web.components.core.setAriaLive
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setClassName
import tech.testsys.web.components.core.setDisplay
import tech.testsys.web.components.core.setDownloadOffset
import tech.testsys.web.components.core.setRole
import tech.testsys.web.components.core.setWidth
import tech.testsys.web.components.core.svgIcon
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.io.OutputStream
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

private const val HTTP_GONE = 410
private const val HTTP_SERVER_ERROR = 500
private const val PERCENT_MAX = 100.0
private const val RING_CIRCUMFERENCE = 81.7
private const val COPY_BUFFER_BYTES = 8192
private const val PROGRESS_BYTES_INTERVAL = 65_536
private const val PROGRESS_NANOS_INTERVAL = 100_000_000L

/**
 * Streaming content for one download attempt; [openStream] creates a fresh input without buffering the file.
 *
 * @property filename the nonempty browser filename without newlines.
 * @property contentType the MIME content type.
 * @property length the optional nonnegative expected length.
 * @property openStream the stream factory, called outside the session lock for this attempt only.
 * @since %CURRENT_VERSION%
 */
data class DownloadContent(
    val filename: String,
    val contentType: String,
    val length: Long? = null,
    val openStream: () -> InputStream,
) {
    init {
        require(
            filename.isNotBlank() && '\n' !in filename && '\r' !in filename,
        ) {
            "Download filename must be nonempty and must not contain newlines"
        }
        require(contentType.isNotBlank() && (length == null || length >= 0)) { "Download type must be nonempty and length nonnegative" }
    }
}

/**
 * Cooperative cancellation context of one producer and its streaming attempt.
 *
 * @property isCancelled whether preparation or transfer has been cancelled.
 * @since %CURRENT_VERSION%
 */
class DownloadContext internal constructor() {
    private val isCancellationRequested = AtomicBoolean()
    private val input = AtomicReference<InputStream?>()
    val isCancelled: Boolean
        get() = isCancellationRequested.get()

    /**
     * Checks cancellation before the producer continues its work.
     *
     * @throws InterruptedIOException if the attempt was cancelled.
     * @since %CURRENT_VERSION%
     */
    fun ensureActive() {
        if (isCancelled) throw InterruptedIOException("Download attempt was cancelled")
    }

    internal fun cancel() {
        isCancellationRequested.set(true)
    }

    internal fun own(stream: InputStream) {
        input.set(stream)
        if (isCancelled) {
            closeStream()
            ensureActive()
        }
    }

    internal fun releaseStream(stream: InputStream) {
        input.compareAndSet(stream, null)
    }

    internal fun closeStream() {
        try {
            input.getAndSet(null)?.close()
        } catch (_: IOException) {
            // The state already describes cancellation or the transfer error.
        }
    }
}

/**
 * Honest server-side download lifecycle, without claiming a file was saved on the client's disk.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface DownloadState {
    /**
     * No current attempt.
     *
     * @since %CURRENT_VERSION%
     */
    data object Idle : DownloadState

    /**
     * The application producer is preparing content.
     *
     * @since %CURRENT_VERSION%
     */
    data object Preparing : DownloadState

    /**
     * Actual bytes are being written, with optional expected total.
     *
     * @property bytes the actual transferred byte count.
     * @property total the expected byte count, or null if unknown.
     * @since %CURRENT_VERSION%
     */
    data class Downloading(val bytes: Long, val total: Long?) : DownloadState

    /**
     * The server finished writing and closed its streams.
     *
     * @property bytes the actual transferred byte count.
     * @since %CURRENT_VERSION%
     */
    data class Done(val bytes: Long) : DownloadState

    /**
     * Preparation or streaming failed.
     *
     * @property cause the exception for application diagnostics.
     * @since %CURRENT_VERSION%
     */
    data class Error(val cause: Exception) : DownloadState
}

/**
 * Handle of a real streaming download with repeat, retry and cooperative cancellation.
 *
 * @property isEnabled whether new attempts may start; disabling cancels the current attempt.
 * @property state the read-only signal of the current attempt.
 * @since %CURRENT_VERSION%
 */
class DownloadHandle internal constructor(private val download: DownloadDisplay) : ElementHandle(download) {
    private val enabled = Bindable(download.element, true, download::allow)
    var isEnabled: Boolean
        get() = enabled.value
        set(value) {
            enabled.value = value
        }
    val state: Signal<DownloadState> = download.state.asReadonly()

    /**
     * Binds whether attempts may start to [signal].
     *
     * @since %CURRENT_VERSION%
     */
    fun bindEnabled(signal: Signal<Boolean>): SignalBinding<Boolean> = enabled.bind(signal)

    /**
     * Starts a fresh attempt if attached, visible, enabled and no attempt is active.
     *
     * @since %CURRENT_VERSION%
     */
    fun start() {
        download.start()
    }

    /**
     * Cancels preparation or transfer and removes its registered resource.
     *
     * @since %CURRENT_VERSION%
     */
    fun cancel() {
        download.cancel()
    }
}

/**
 * Adds a download action named [label]; [produce] prepares its streaming content off the session lock.
 *
 * @param produce the application producer cooperating with the supplied cancellation context.
 * @param configure the returned download action configuration.
 * @since %CURRENT_VERSION%
 */
fun ContentScope.downloadAction(
    label: String,
    produce: (DownloadContext) -> DownloadContent,
    configure: DownloadHandle.() -> Unit = {},
): DownloadHandle = download(label, isIconOnly = false, produce, configure)

/**
 * Adds an icon-only real download action; [label] is its required accessible name.
 *
 * @param produce the application producer cooperating with the supplied cancellation context.
 * @param configure the returned download action configuration.
 * @since %CURRENT_VERSION%
 */
fun ContentScope.iconDownloadAction(
    label: String,
    produce: (DownloadContext) -> DownloadContent,
    configure: DownloadHandle.() -> Unit = {},
): DownloadHandle = download(label, isIconOnly = true, produce, configure)

/**
 * Adds a real download action on [size] columns, or the remaining columns.
 *
 * @param produce the application producer cooperating with its cancellation context.
 * @param configure the returned download action configuration.
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.downloadAction(
    label: String,
    size: Int? = null,
    produce: (DownloadContext) -> DownloadContent,
    configure: DownloadHandle.() -> Unit = {},
): DownloadHandle = placeContent(size, Div()).downloadAction(label, produce, configure)

private fun ContentScope.download(
    label: String,
    isIconOnly: Boolean,
    produce: (DownloadContext) -> DownloadContent,
    configure: DownloadHandle.() -> Unit,
): DownloadHandle {
    require(label.isNotBlank()) { "Download action needs an accessible label" }
    val display = DownloadDisplay(texts, label, isIconOnly, placement.isCompact, produce)
    add(display)
    return DownloadHandle(display).apply(configure)
}

internal class DownloadAttempt(val context: DownloadContext = DownloadContext()) {
    val isConsumed = AtomicBoolean()
}

internal class DownloadDisplay(
    private val texts: UiTexts,
    private val label: String,
    private val isIconOnly: Boolean,
    compact: Boolean,
    private val produce: (DownloadContext) -> DownloadContent,
) : Div() {
    private val logger = LoggerFactory.getLogger(DownloadDisplay::class.java)
    val state = ValueSignal<DownloadState>(DownloadState.Idle)
    private val trigger = NativeButton().apply {
        addClassNames(CssClass.Btn, CssClass.BtnSecondary)
        if (compact) addClassName(CssClass.BtnSm)
        if (isIconOnly) addClassName(CssClass.BtnIcon)
    }

    private var renderedVariant: CssClass? = null
    private val captionNode = Span()
    private val percentageNode = Span().apply { addClassName(CssClass.Mono) }
    private val progressLabel = Span().apply { addClassName(CssClass.BtnLabel) }
    private val fill = Span().apply { addClassName(CssClass.BtnFill) }
    private val spinner = Span().apply {
        addClassName(CssClass.Spinner)
        element.setAriaHidden(true)
    }
    private val ring = Html(
        """
        <svg width="32" height="32" viewBox="0 0 32 32" aria-hidden="true">
          <circle cx="16" cy="16" r="13" fill="none" stroke="var(--muted)" stroke-width="3"/>
          <circle cx="16" cy="16" r="13" fill="none" stroke="var(--accent)" stroke-width="3"
            stroke-dasharray="$RING_CIRCUMFERENCE" style="stroke-dashoffset:var(--ts-download-offset)" transform="rotate(-90 16 16)"/>
          <rect x="12.5" y="12.5" width="7" height="7" rx="1.5" fill="var(--accent)"/>
        </svg>
        """.trimIndent(),
    )

    private val anchor = Anchor().apply {
        element.style.setDisplay(CssDisplay.None)
        element.setAriaHidden(true)
        element.setAttribute(HtmlAttribute.TabIndex, "-1")
        setRouterIgnore(true)
    }

    private val status = Span().apply {
        element.setRole(ElementRole.Status)
        element.setAriaLive(AriaLive.Polite)
    }

    private var attempt: DownloadAttempt? = null
    private var executor: Executor? = null
    private var isAllowed = true

    init {
        trigger.addClickListener { if (attempt != null) cancel() else start() }
        addClassName(CssClass.Download)
        status.setClassName(CssClass.SrOnly)
        add(trigger, anchor, status)
        addAttachListener {
            executor = Background.executor()
            render(state.peek())
        }
        addDetachListener {
            cancel()
            executor = null
        }
        status.element.bindText(
            state.map { value ->
                when (value) {
                    DownloadState.Idle -> ""
                    DownloadState.Preparing -> texts.components.preparing
                    is DownloadState.Downloading -> texts.components.transferBytes(value.bytes)
                    is DownloadState.Done -> texts.components.done
                    is DownloadState.Error -> texts.components.failed
                }
            },
        )
        trigger.element.bindDownloadState(state)
        // Rendering through the signal also covers background access updates without keeping a custom subscription.
        com.vaadin.flow.dom.ElementEffect.bind(element, state) { _, value -> render(value) }
        render(DownloadState.Idle)
    }

    fun allow(value: Boolean) {
        isAllowed = value
        if (!value) cancel()
        render(state.peek())
    }

    fun start() {
        if (!isAllowed || !isAttached || !isVisible || attempt != null) return
        anchor.removeHref()
        val current = DownloadAttempt()
        attempt = current
        val ui = ui.orElseThrow()
        state.set(DownloadState.Preparing)
        checkNotNull(executor) { "Attached download has no background executor" }.execute {
            try {
                current.context.ensureActive()
                val content = produce(current.context)
                current.context.ensureActive()
                Background.inUi(ui) {
                    if (isAttached && attempt === current && !current.context.isCancelled) {
                        anchor.setHref(DownloadHandler { event -> transfer(event, current, content, ui) })
                        anchor.element.clickClient()
                    }
                }
            } catch (failure: Exception) {
                // The application producer may use checked exception types unknown to this module.
                publish(ui, current, DownloadState.Error(failure))
            }
        }
    }

    fun cancel() {
        val current = attempt
        attempt = null
        current?.context?.cancel()
        if (current != null) executor?.execute { current.context.closeStream() }
        anchor.removeHref()
        state.set(DownloadState.Idle)
    }

    internal fun activeAttempt(): DownloadAttempt? = attempt

    internal fun transfer(event: DownloadEvent, current: DownloadAttempt, content: DownloadContent, ui: UI) {
        if (current.context.isCancelled || !current.isConsumed.compareAndSet(false, true)) {
            event.response.setStatus(HTTP_GONE)
            return
        }
        event.response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate")
        event.setFileName(content.filename)
        event.setContentType(content.contentType)
        content.length?.let(event::setContentLength)
        try {
            val bytes = streamDownload(
                content,
                current.context,
                event.outputStream,
                onFailure = { event.response.setStatus(HTTP_SERVER_ERROR) },
            ) { count ->
                publish(ui, current, DownloadState.Downloading(bytes = count, total = content.length))
            }
            publish(ui, current, DownloadState.Done(bytes))
        } catch (failure: IOException) {
            event.response.setStatus(HTTP_SERVER_ERROR)
            publish(ui, current, DownloadState.Error(failure))
            throw failure
        } catch (failure: RuntimeException) {
            event.response.setStatus(HTTP_SERVER_ERROR)
            publish(ui, current, DownloadState.Error(failure))
            throw failure
        }
    }

    private fun publish(ui: UI, current: DownloadAttempt, value: DownloadState) {
        Background.inUi(ui) {
            if (isAttached && attempt === current && !current.context.isCancelled) {
                if (value is DownloadState.Error) logger.error("Download action '{}' failed", label, value.cause)
                state.set(value)
                if (value is DownloadState.Done || value is DownloadState.Error) {
                    attempt = null
                    anchor.removeHref()
                }
            }
        }
    }

    private fun render(value: DownloadState) {
        val isBusy = value == DownloadState.Preparing || value is DownloadState.Downloading
        trigger.isEnabled = isAllowed
        val percent = (value as? DownloadState.Downloading)?.let { transfer ->
            transfer.total?.takeIf { total -> total > 0 }?.let { total ->
                (transfer.bytes * PERCENT_MAX / total).coerceIn(minimumValue = 0.0, maximumValue = PERCENT_MAX)
            }
        }
        val caption = when (value) {
            DownloadState.Preparing -> texts.components.preparing
            is DownloadState.Downloading -> texts.components.cancel
            is DownloadState.Done -> texts.components.downloadAgain
            is DownloadState.Error -> texts.components.retry
            DownloadState.Idle -> label
        }
        val variant = when {
            value == DownloadState.Preparing || value is DownloadState.Downloading && percent == null -> CssClass.BtnBusy
            value is DownloadState.Downloading -> if (isIconOnly) CssClass.BtnGhost else CssClass.BtnProgress
            value is DownloadState.Done -> CssClass.BtnSuccessSoft
            value is DownloadState.Error -> CssClass.BtnDangerSoft
            else -> CssClass.BtnSecondary
        }
        val accessible = texts.components.downloadLabel(label, caption)
        trigger.element.setAttribute(HtmlAttribute.AriaLabel, accessible)
        trigger.element.setAttribute(HtmlAttribute.Title, accessible)
        trigger.element.setAriaBusy(isBusy)
        captionNode.text = caption
        percent?.let { progress ->
            percentageNode.text = texts.components.percent(progress.toInt())
            fill.element.style.setWidth(progress, CssUnit.Percent)
            ring.element.style.setDownloadOffset(RING_CIRCUMFERENCE * (1 - progress / PERCENT_MAX))
        }
        // Progress updates keep the animated node attached and preserve fill/ring transitions.
        if (renderedVariant == variant) return
        trigger.removeAll()
        listOf(
            CssClass.BtnSecondary,
            CssClass.BtnBusy,
            CssClass.BtnSuccessSoft,
            CssClass.BtnDangerSoft,
            CssClass.BtnProgress,
            CssClass.BtnGhost,
        ).forEach { modifier ->
            trigger.removeClassName(modifier)
        }
        trigger.addClassName(variant)
        when (variant) {
            CssClass.BtnBusy -> {
                trigger.add(spinner)
                if (!isIconOnly) trigger.add(captionNode)
            }
            CssClass.BtnGhost -> trigger.add(ring)
            CssClass.BtnProgress -> {
                progressLabel.add(percentageNode, captionNode)
                trigger.add(fill, progressLabel)
            }
            else -> {
                trigger.add(
                    svgIcon(
                        when (value) {
                            is DownloadState.Done -> IconName.Check
                            is DownloadState.Error -> IconName.RefreshCw
                            else -> IconName.Download
                        },
                        tech.testsys.web.components.core.ICON_SIZE,
                    ),
                )
                if (!isIconOnly) trigger.add(captionNode)
            }
        }
        renderedVariant = variant
    }
}

internal fun streamDownload(
    content: DownloadContent,
    context: DownloadContext,
    output: OutputStream,
    onFailure: (Exception) -> Unit = {},
    progress: (Long) -> Unit,
): Long = output.use { destination ->
    try {
        context.ensureActive()
        val input = content.openStream()
        var count = 0L
        var reported = 0L
        var reportedAt = System.nanoTime()
        try {
            input.use { source ->
                context.own(source)
                val buffer = ByteArray(COPY_BUFFER_BYTES)
                progress(0)
                while (true) {
                    context.ensureActive()
                    val read = source.read(buffer)
                    if (read < 0) break
                    if (read == 0) continue
                    context.ensureActive()
                    if (content.length != null && count + read > content.length) {
                        throw IOException("Download '${content.filename}' exceeds declared length ${content.length}")
                    }
                    destination.write(buffer, 0, read)
                    count += read
                    val now = System.nanoTime()
                    if (count - reported >= PROGRESS_BYTES_INTERVAL || now - reportedAt >= PROGRESS_NANOS_INTERVAL) {
                        progress(count)
                        reported = count
                        reportedAt = now
                    }
                }
                if (content.length != null && count != content.length) {
                    throw IOException("Download '${content.filename}' ended at $count bytes, expected ${content.length}")
                }
                context.ensureActive()
                destination.flush()
                progress(count)
            }
        } finally {
            context.releaseStream(input)
        }
        count
    } catch (failure: IOException) {
        onFailure(failure)
        throw failure
    } catch (failure: RuntimeException) {
        onFailure(failure)
        throw failure
    }
}

private fun Element.bindDownloadState(state: Signal<DownloadState>): SignalBinding<String> = bindAttribute(
    "data-state",
    state.map { value ->
        when (value) {
            DownloadState.Idle -> "Idle"
            DownloadState.Preparing -> "Preparing"
            is DownloadState.Downloading -> "Downloading"
            is DownloadState.Done -> "Done"
            is DownloadState.Error -> "Error"
        }
    },
)
