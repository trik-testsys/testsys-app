@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.forms

import com.vaadin.flow.component.UI
import com.vaadin.flow.component.dependency.JsModule
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.page.PendingJavaScriptResult
import com.vaadin.flow.component.upload.Upload
import com.vaadin.flow.component.upload.UploadI18N
import com.vaadin.flow.dom.Element
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.server.streams.UploadEvent
import com.vaadin.flow.server.streams.UploadHandler
import com.vaadin.flow.signals.Signal
import com.vaadin.flow.signals.local.ValueSignal
import tech.testsys.web.components.FieldHandle
import tech.testsys.web.components.actions.ActionHandle
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.core.AriaLive
import tech.testsys.web.components.core.Background
import tech.testsys.web.components.core.Bindable
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.DomEvent
import tech.testsys.web.components.core.DomEventData
import tech.testsys.web.components.core.ElementRole
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.core.addEventData
import tech.testsys.web.components.core.addEventListener
import tech.testsys.web.components.core.get
import tech.testsys.web.components.core.setAriaLive
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setRole
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.Placement
import tech.testsys.web.components.texts.UiTexts
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

private const val PROGRESS_BYTES_INTERVAL = 65_536
private const val PROGRESS_NANOS_INTERVAL = 100_000_000L

/**
 * Server-enforced limits including all buffers retained until application handlers finish.
 *
 * @property maxFiles the number of accepted files since clearing the component.
 * @property maxFileBytes the positive maximum per-file buffer size, at most the JVM byte-array limit.
 * @property maxMemoryBytes the total isReserved memory of concurrently running handlers.
 * @property mimeTypes isAllowed MIME types, empty for any; a trailing slash-star matches a MIME family.
 * @property extensions isAllowed dot-prefixed filename extensions, empty for any; MIME and extension constraints combine with AND.
 * @since %CURRENT_VERSION%
 */
data class UploadLimits(
    val maxFiles: Int,
    val maxFileBytes: Int,
    val maxMemoryBytes: Long,
    val mimeTypes: Set<String> = emptySet(),
    val extensions: Set<String> = emptySet(),
) {
    init {
        require(maxFiles > 0) { "Upload limits must permit at least one file, got maxFiles $maxFiles" }
        require(maxFileBytes > 0) { "Upload limits must permit a positive file size, got maxFileBytes $maxFileBytes" }
        require(maxMemoryBytes >= maxFileBytes) {
            "Upload memory $maxMemoryBytes must hold at least one file of $maxFileBytes bytes"
        }
        val invalidExtensions = extensions.filterNot { extension -> extension.startsWith('.') && extension.length > 1 }
        require(invalidExtensions.isEmpty()) { "Upload extensions must include their leading dot, got $invalidExtensions" }
    }
}

/**
 * Temporary file content valid only during the application handler; the buffer is wiped afterwards.
 *
 * @property filename the client-provided filename, never used as a server path.
 * @property contentType the client-provided MIME type checked against configured limits.
 * @property size the actual number of received bytes.
 * @property isCancelled whether the owning transfer has been cancelled.
 * @since %CURRENT_VERSION%
 */
class UploadedFile internal constructor(
    val filename: String,
    val contentType: String,
    val size: Int,
    private var buffer: ByteArray?,
    private val cancelled: AtomicBoolean,
) {
    val isCancelled: Boolean
        get() = cancelled.get()

    /**
     * Opens a new bounded stream while the handler owns the temporary content.
     *
     * @throws IOException if the handler has finished or the transfer was cancelled.
     * @since %CURRENT_VERSION%
     */
    fun openStream(): InputStream {
        ensureActive()
        val bytes = checkNotNull(buffer) { "Upload '$filename' content is no longer owned by its handler" }
        return object : InputStream() {
            private val source = ByteArrayInputStream(bytes, 0, size)
            private var isClosed = false
            override fun read(): Int {
                ensureReadable()
                return source.read()
            }

            override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
                ensureReadable()
                return source.read(bytes, offset, length)
            }

            override fun skip(count: Long): Long {
                ensureReadable()
                return source.skip(count)
            }

            override fun available(): Int {
                ensureReadable()
                return source.available()
            }

            override fun close() {
                isClosed = true
            }

            private fun ensureReadable() {
                ensureActive()
                if (isClosed) throw IOException("Temporary upload stream was closed")
            }
        }
    }

    /**
     * Checks cooperative cancellation before application processing continues.
     *
     * @since %CURRENT_VERSION%
     */
    fun ensureActive() {
        if (isCancelled || buffer == null) throw InterruptedIOException("Upload '$filename' is cancelled or expired")
    }

    internal fun release() {
        buffer?.fill(0)
        buffer = null
    }
}

/**
 * Latest lifecycle event of the real file transfer; each event names the file it concerns.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface FileUploadState {
    /**
     * No current transfer.
     *
     * @since %CURRENT_VERSION%
     */
    data object Idle : FileUploadState

    /**
     * A file is being read, with optional known total.
     *
     * @property filename the transferring file.
     * @property bytes the actual received byte count.
     * @property total the declared length, or null if unknown.
     * @since %CURRENT_VERSION%
     */
    data class Uploading(val filename: String, val bytes: Long, val total: Long?) : FileUploadState

    /**
     * The application is processing temporary content.
     *
     * @property filename the processed file.
     * @since %CURRENT_VERSION%
     */
    data class Processing(val filename: String) : FileUploadState

    /**
     * The application handler completed and temporary content was released.
     *
     * @property filename the completed file.
     * @property bytes its actual byte count.
     * @since %CURRENT_VERSION%
     */
    data class Done(val filename: String, val bytes: Long) : FileUploadState

    /**
     * A rejected or failed transfer; no content is retained.
     *
     * @property filename the failed file.
     * @property cause the diagnostic exception for application logging.
     * @since %CURRENT_VERSION%
     */
    data class Error(val filename: String, val cause: Exception) : FileUploadState

    /**
     * User cancellation without retaining temporary bytes.
     *
     * @since %CURRENT_VERSION%
     */
    data object Cancelled : FileUploadState
}

/**
 * File reception action, independent of Binder values and permanent storage.
 *
 * @property isEnabled whether transfers may start.
 * @property isEditable whether this action accepts files; row actions also follow their block mode.
 * @property state the read-only signal of the latest transfer lifecycle event.
 * @since %CURRENT_VERSION%
 */
class FileDropHandle internal constructor(private val drop: FileDropDisplay) : FieldHandle(component = drop, valueArea = drop) {
    private var isBlockEditable = true
    private val enabled: Bindable<Boolean>
    private val editable: Bindable<Boolean>

    init {
        enabled = Bindable(drop.element, true) { value -> drop.allow(value && editable.value && isBlockEditable) }
        editable = Bindable(drop.element, true) { value -> drop.allow(value && enabled.value && isBlockEditable) }
    }

    var isEnabled: Boolean
        get() = enabled.value
        set(value) {
            enabled.value = value
        }
    var isEditable: Boolean
        get() = editable.value
        set(value) {
            editable.value = value
        }
    val state: Signal<FileUploadState> = drop.state.asReadonly()

    /**
     * Binds whether transfers can start to [signal].
     *
     * @since %CURRENT_VERSION%
     */
    fun bindEnabled(signal: Signal<Boolean>): SignalBinding<Boolean> = enabled.bind(signal)

    /**
     * Binds the action edit mode to [signal].
     *
     * @since %CURRENT_VERSION%
     */
    fun bindEditable(signal: Signal<Boolean>): SignalBinding<Boolean> = editable.bind(signal)

    /**
     * Cancels running reception and requests cooperative handler cancellation.
     *
     * @since %CURRENT_VERSION%
     */
    fun cancel() {
        drop.cancel()
    }

    /**
     * Cancels running work, clears the visible file list and resets the file-count quota.
     *
     * @since %CURRENT_VERSION%
     */
    fun clear() {
        drop.clear()
    }

    internal fun followBlock(state: tech.testsys.web.components.layout.BlockEditState) {
        state.follow { value ->
            isBlockEditable = value
            drop.allow(value && enabled.value && editable.value)
        }
    }
}

/**
 * Adds a real bounded file receiver named [label]; [consume] receives temporary content off the session lock.
 *
 * @param consume the synchronous handler, which must finish reading before returning and cooperate with cancellation.
 * @param configure the returned action's state and bindings.
 * @since %CURRENT_VERSION%
 */
fun ContentScope.fileDrop(
    label: String,
    limits: UploadLimits,
    consume: (UploadedFile) -> Unit,
    configure: FileDropHandle.() -> Unit = {},
): FileDropHandle {
    val drop = FileDropDisplay(texts, label, limits, consume)
    add(drop)
    return FileDropHandle(drop).apply(configure)
}

/**
 * Adds a file receiver on [size] columns, or the remaining columns, following the block edit mode.
 *
 * @param consume the synchronous temporary-content handler.
 * @param configure the returned action configuration.
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.fileDrop(
    label: String,
    limits: UploadLimits,
    size: Int? = null,
    consume: (UploadedFile) -> Unit,
    configure: FileDropHandle.() -> Unit = {},
): FileDropHandle {
    val drop = placeWithSize(size) { FileDropDisplay(texts, label, limits, consume) }
    return FileDropHandle(drop).apply {
        followBlock(this@fileDrop.editState)
        configure()
    }
}

@JsModule(FILE_TRANSFER_MODULE)
internal class FileDropDisplay(
    texts: UiTexts,
    label: String,
    limits: UploadLimits,
    consume: (UploadedFile) -> Unit,
) : Div() {
    val state = ValueSignal<FileUploadState>(FileUploadState.Idle)
    private var attachedUi: UI? = null
    private var isAllowed = true
    val engine = BoundedUploads(limits, consume) { value, isCurrent ->
        attachedUi?.let { ui -> Background.inUi(ui) { if (isAttached && isCurrent()) state.set(value) } }
    }

    private val status = Span().apply {
        addClassName(CssClass.FiledropStatus)
        element.setRole(ElementRole.Status)
        element.setAriaLive(AriaLive.Polite)
    }

    private val upload = Upload(
        object : UploadHandler {
            override fun handleUploadRequest(event: UploadEvent) {
                try {
                    engine.receive(
                        transferId = event.request.getHeader(UploadIdentity.HEADER).orEmpty(),
                        filename = event.fileName,
                        mime = event.contentType,
                        declared = event.fileSize,
                        stream = event.inputStream,
                    )
                } catch (_: UploadRejectedException) {
                    event.reject(texts.components.uploadRejected)
                } catch (_: Exception) {
                    event.reject(texts.components.failed)
                }
            }

            override fun getFileSizeMax(): Long = limits.maxFileBytes.toLong()
            override fun getFileCountMax(): Long = limits.maxFiles.toLong()
        },
    ).apply {
        addClassName(CssClass.Drop)
        setDropLabelIcon(
            tech.testsys.web.components.core.svgIcon(
                tech.testsys.web.components.core.IconName.Upload,
                tech.testsys.web.components.core.ICON_SIZE,
            ),
        )
        maxFiles = limits.maxFiles
        maxFileSize = limits.maxFileBytes
        setAcceptedMimeTypes(*limits.mimeTypes.toTypedArray())
        setAcceptedFileExtensions(*limits.extensions.toTypedArray())
        setDropLabel(Span(texts.components.drop).apply { addClassName(CssClass.DropTitle) })
        val selectAction = com.vaadin.flow.component.html.NativeButton(texts.components.upload).apply {
            addClassNames(CssClass.Btn, CssClass.BtnSecondary, CssClass.BtnSm)
        }
        setUploadButton(selectAction)
        element.setAttribute(HtmlAttribute.AriaLabel, label)
        i18n = UploadI18N()
            .setDropFiles(UploadI18N.DropFiles().setOne(texts.components.drop).setMany(texts.components.drop))
            .setAddFiles(UploadI18N.AddFiles().setOne(texts.components.upload).setMany(texts.components.upload))
            .setError(
                UploadI18N.Error().setTooManyFiles(texts.components.uploadRejected).setFileIsTooBig(
                    texts.components.uploadRejected,
                ).setIncorrectFileType(texts.components.uploadRejected),
            )
            .setFile(
                UploadI18N.File().setRetry(texts.components.retry).setStart(texts.components.upload).setRemove(
                    texts.components.removeFile,
                ),
            )
            .setUploading(
                UploadI18N.Uploading()
                    .setStatus(
                        UploadI18N.Uploading.Status().setConnecting(texts.components.preparing).setStalled(
                            texts.components.stalled,
                        ).setProcessing(texts.components.preparing).setHeld(texts.components.preparing),
                    )
                    .setRemainingTime(
                        UploadI18N.Uploading.RemainingTime().setPrefix("").setUnknown(
                            texts.components.downloading,
                        ),
                    )
                    .setError(
                        UploadI18N.Uploading.Error().setServerUnavailable(texts.components.failed).setUnexpectedServerError(
                            texts.components.failed,
                        ).setForbidden(texts.components.failed).setFileTooLarge(texts.components.uploadRejected),
                    ),
            )
            .setUnits(texts.components.byteUnits)
    }

    private val actions = Div().apply { addClassNames(CssClass.Hstack, CssClass.FiledropActions) }
    private val cancelAction: ActionHandle
    private val clearAction: ActionHandle

    init {
        val controls = ContentScope(actions, texts, Placement.Head)
        cancelAction = controls.action(texts.components.cancel) { onClick { this@FileDropDisplay.cancel() } }
        clearAction = controls.action(texts.lookup.clear) { onClick { this@FileDropDisplay.clear() } }
        upload.element.addEventListener(DomEvent.TransferRemove) { event ->
            val wasActive = engine.remove(event.eventData.get(DomEventData.DetailIdentity).asString())
            when {
                engine.fileCount() == 0 -> state.set(FileUploadState.Idle)
                wasActive -> state.set(FileUploadState.Cancelled)
            }
        }.addEventData(DomEventData.DetailIdentity)
        addClassName(CssClass.Filedrop)
        element.setRole(ElementRole.Group)
        element.setAttribute(HtmlAttribute.AriaLabel, label)
        val limitsHint = Span(texts.components.uploadLimits(limits.maxFiles, limits.maxFileBytes.toLong())).apply {
            addClassName(CssClass.Hint)
        }
        add(Span(label), upload, Div(limitsHint, status, actions).apply { addClassName(CssClass.FiledropMeta) })
        com.vaadin.flow.dom.ElementEffect.bind(element, state) { _, value ->
            cancelAction.isVisible = value is FileUploadState.Uploading || value is FileUploadState.Processing
            clearAction.isVisible = value != FileUploadState.Idle
        }
        cancelAction.isVisible = false
        clearAction.isVisible = false
        addAttachListener { event ->
            attachedUi = event.ui
            engine.allow(isAllowed)
            upload.element.setAttribute(HtmlAttribute.DataTsUploadGeneration, engine.generation().toString())
            upload.element.attachFileTransfers()
        }
        addDetachListener {
            upload.element.detachFileTransfers()
            val hadActive = engine.hasActive() || state.peek().isRunning()
            engine.cancel()
            engine.allow(false)
            attachedUi = null
            if (hadActive) state.set(FileUploadState.Cancelled)
        }
        status.element.bindText(
            state.map { value ->
                when (value) {
                    FileUploadState.Idle -> ""
                    is FileUploadState.Uploading -> texts.components.transferBytes(value.bytes)
                    is FileUploadState.Processing -> texts.components.preparing
                    is FileUploadState.Done -> texts.components.done
                    is FileUploadState.Error -> texts.components.failed
                    FileUploadState.Cancelled -> texts.components.cancelled
                }
            },
        )
    }

    fun allow(value: Boolean) {
        isAllowed = value
        upload.isEnabled = value
        clearAction.isEnabled = value
        engine.allow(value)
        if (!value) cancel()
    }

    fun cancel() {
        val previous = state.peek()
        val hadActive = engine.hasActive()
        engine.cancel()
        upload.element.setAttribute(HtmlAttribute.DataTsUploadGeneration, engine.generation().toString())
        upload.interruptUpload()
        if (hadActive || previous.isRunning()) {
            state.set(FileUploadState.Cancelled)
        }
    }

    fun clear() {
        engine.clear()
        upload.element.setAttribute(HtmlAttribute.DataTsUploadGeneration, engine.generation().toString())
        upload.clearFileList()
        state.set(FileUploadState.Idle)
    }
}

/** Transport identity is a generation plus a random UUID, never a filename. */
private object UploadIdentity {
    const val HEADER = "X-TestSys-Transfer"
    val PATTERN = Regex("^(\\d+):[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
}

internal class UploadRejectedException(message: String) : IOException(message)

internal class BoundedUploads(
    private val limits: UploadLimits,
    private val consume: (UploadedFile) -> Unit,
    private val publish: (FileUploadState, () -> Boolean) -> Unit,
) {
    private data class Active(
        val identity: String,
        val filename: String,
        val generation: Long,
        val cancelled: AtomicBoolean,
        val stream: InputStream,
    )
    private val lock = Any()
    private val epoch = AtomicLong()
    private val active = ConcurrentHashMap<Long, Active>()
    private val slots = mutableMapOf<String, Long>()
    private val sequence = AtomicLong()
    private val lastEvent = AtomicLong()
    private var memory = 0L

    @Volatile
    private var isEnabled = true

    var reservationGateOverride: (() -> Unit)? = null

    fun allow(value: Boolean) {
        isEnabled = value
    }

    fun generation(): Long = epoch.get()

    fun receive(transferId: String, filename: String, mime: String, declared: Long, stream: InputStream) {
        val id = sequence.incrementAndGet()
        lastEvent.updateAndGet { current -> maxOf(current, id) }
        val eventEpoch = UploadIdentity.PATTERN.matchEntire(transferId)?.groupValues?.get(1)?.toLongOrNull()
        var token: Active? = null
        var isReserved = false
        var isSuccessful = false
        var file: UploadedFile? = null
        var buffer: ByteArray? = null
        try {
            reservationGateOverride?.invoke()
            val current = synchronized(lock) {
                if (eventEpoch == null) throw UploadRejectedException("Invalid transfer identity '$transferId' of upload '$filename'")
                if (eventEpoch != epoch.get()) throw InterruptedIOException("Upload '$filename' was cancelled before reservation")
                if (transferId in slots) throw UploadRejectedException("Upload '$filename' repeats transfer identity '$transferId'")
                if (!isEnabled) throw UploadRejectedException("Upload '$filename' arrived while uploads are disabled")
                if (declared > limits.maxFileBytes) {
                    throw UploadRejectedException("Upload '$filename' declares $declared bytes, the limit is ${limits.maxFileBytes}")
                }
                if (!accepts(filename, mime)) {
                    throw UploadRejectedException(
                        "Upload '$filename' of type '$mime' is not accepted, expected extensions ${limits.extensions} " +
                            "or types ${limits.mimeTypes}",
                    )
                }
                if (slots.size >= limits.maxFiles) {
                    throw UploadRejectedException("Upload '$filename' exceeds the file count: ${slots.size} of ${limits.maxFiles} taken")
                }
                if (memory + limits.maxFileBytes > limits.maxMemoryBytes) {
                    throw UploadRejectedException(
                        "Upload '$filename' needs ${limits.maxFileBytes} bytes beyond the reserved $memory, " +
                            "the memory limit is ${limits.maxMemoryBytes}",
                    )
                }
                val created = Active(transferId, filename, epoch.get(), AtomicBoolean(false), stream)
                slots[transferId] = id
                memory += limits.maxFileBytes
                isReserved = true
                active[id] = created
                token = created
                created
            }
            val bytes = ByteArray(limits.maxFileBytes)
            buffer = bytes
            var count = 0
            var reported = 0
            var reportedAt = System.nanoTime()
            emit(id, current, FileUploadState.Uploading(filename, bytes = 0, total = declared.takeIf { size -> size >= 0 }))
            stream.use { input ->
                while (true) {
                    ensureActive(current)
                    val read = if (count == bytes.size) {
                        if (input.read() != -1) throw UploadRejectedException("Upload '$filename' exceeds ${limits.maxFileBytes} bytes")
                        -1
                    } else {
                        input.read(bytes, count, bytes.size - count)
                    }
                    if (read < 0) break
                    if (read == 0) continue
                    count += read
                    val now = System.nanoTime()
                    if (count - reported >= PROGRESS_BYTES_INTERVAL || now - reportedAt >= PROGRESS_NANOS_INTERVAL) {
                        emit(
                            id,
                            current,
                            FileUploadState.Uploading(
                                filename,
                                bytes = count.toLong(),
                                total = declared.takeIf { size -> size >= 0 },
                            ),
                        )
                        reported = count
                        reportedAt = now
                    }
                }
            }
            ensureActive(current)
            file = UploadedFile(filename, mime, count, bytes, current.cancelled)
            emit(id, current, FileUploadState.Uploading(filename, bytes = count.toLong(), total = declared.takeIf { size -> size >= 0 }))
            emit(id, current, FileUploadState.Processing(filename))
            consume(file)
            ensureActive(current)
            isSuccessful = true
            file.release()
            emit(id, current, FileUploadState.Done(filename, count.toLong()))
        } catch (failure: Exception) {
            // Application processing can throw checked exceptions unrelated to transport.
            publish(FileUploadState.Error(filename, failure)) {
                token?.cancelled?.get() != true && eventEpoch == epoch.get() && lastEvent.get() == id
            }
            throw failure
        } finally {
            file?.release()
            buffer?.fill(0)
            try {
                stream.close()
            } catch (_: IOException) {
                // The transfer failure already carries the outcome.
            }
            active.remove(id)
            if (isReserved) {
                synchronized(lock) {
                    memory -= limits.maxFileBytes
                    if (!isSuccessful) slots.remove(transferId, id)
                }
            }
        }
    }

    fun cancel() {
        val cancelled = synchronized(lock) {
            epoch.incrementAndGet()
            active.entries.map { (id, token) ->
                slots.remove(token.identity, id)
                token.cancelled.set(true)
                token
            }
        }
        cancelled.forEach { token ->
            try {
                token.stream.close()
            } catch (_: IOException) {
                // Cancellation remains cooperative.
            }
        }
    }

    fun clear() {
        cancel()
        synchronized(lock) { slots.clear() }
    }

    /** Removes the transfer [transferId] and returns whether it was still running. */
    fun remove(transferId: String): Boolean {
        val token = synchronized(lock) {
            slots.remove(transferId)?.let { id -> active[id] }?.also { current -> current.cancelled.set(true) }
        }
        if (token != null) {
            try {
                token.stream.close()
            } catch (_: IOException) {
                // Cancellation remains cooperative.
            }
        }
        return token != null
    }

    fun hasActive(): Boolean = active.isNotEmpty()

    fun fileCount(): Int = synchronized(lock) { slots.size }

    fun reservedBytes(): Long = synchronized(lock) {
        memory
    }

    private fun emit(id: Long, token: Active, value: FileUploadState) {
        publish(value) { !token.cancelled.get() && token.generation == epoch.get() && lastEvent.get() == id }
    }

    private fun ensureActive(token: Active) {
        if (token.cancelled.get() || token.generation != epoch.get()) {
            throw InterruptedIOException("Upload '${token.filename}' of generation ${token.generation} was cancelled")
        }
    }

    private fun accepts(filename: String, mime: String): Boolean {
        val isExtensionAllowed = limits.extensions.isEmpty() || limits.extensions.any { extension ->
            filename.endsWith(extension, ignoreCase = true)
        }
        val normalizedMime = mime.substringBefore(';').trim()
        val isMimeAllowed = limits.mimeTypes.isEmpty() || limits.mimeTypes.any { allowed ->
            when {
                allowed == "*/*" -> true
                allowed.endsWith("/*") -> normalizedMime.startsWith(allowed.dropLast(1), ignoreCase = true)
                else -> normalizedMime.equals(allowed, ignoreCase = true)
            }
        }
        return isExtensionAllowed && isMimeAllowed
    }
}

private fun Element.attachFileTransfers(): PendingJavaScriptResult = executeJs("window.testsysFileTransfers.attach(this)")

private fun Element.detachFileTransfers(): PendingJavaScriptResult = executeJs("window.testsysFileTransfers.detach(this)")

private fun FileUploadState.isRunning(): Boolean = this is FileUploadState.Uploading || this is FileUploadState.Processing
