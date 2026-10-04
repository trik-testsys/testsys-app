package tech.testsys.web.components.feedback

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.signals.Signal
import org.slf4j.LoggerFactory
import tech.testsys.web.components.Background
import tech.testsys.web.components.Bindable
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.UiTexts
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.layout.BlockScope
import tech.testsys.web.components.layout.LoadedBody
import java.util.concurrent.atomic.AtomicLong

private val logger = LoggerFactory.getLogger(LoadHandle::class.java)

private const val LOAD_OWNER = "a load"

/**
 * Handle of a block body loaded in the background: loads it again, and [isVisible] shows or hides the body with
 * the pagination of a loaded table, across reloads; the head and the own footer content of the block stay. A manual
 * [isVisible] while [bindVisible] is bound, and a second binding, throw `BindingActiveException`.
 *
 * @since %CURRENT_VERSION%
 */
class LoadHandle internal constructor(private val loader: BlockLoader<*>) : ElementHandle(loader.component) {
    private val visible = Bindable(loader.component.element, initial = true) { value -> loader.isShown = value }

    override var isVisible: Boolean
        get() = visible.value
        set(value) {
            visible.value = value
        }

    override fun bindVisible(signal: Signal<Boolean>): SignalBinding<Boolean> = visible.bind(signal)

    /**
     * Shows the placeholder and fetches the content again; only the result of the latest load is shown. May be called
     * from any thread: the load then starts in the UI thread. A closed page ignores the call; a block taken out of its
     * UI, e.g. by navigation to another route, fetches nothing until it is attached again and then loads once.
     *
     * @since %CURRENT_VERSION%
     */
    fun reload() {
        loader.reload()
    }
}

/**
 * Fills the whole block body with [content] built from what [fetch] returns in the background, showing [skeletonRows]
 * placeholder rows meanwhile. [content] fills the body only: rows, a table or an empty state, with the pagination
 * of a table in the block footer; the head, `actions { }`, `footer { }`, `editing` and tabs belong to the block itself.
 *
 * @param T the type of the fetched value.
 * @param fetch fetches the value off the UI thread; an exception from it, or from [content], shows the load failure
 * with a retry.
 * @param content builds the body from the fetched value in the UI thread, again on every load; content that adds
 * nothing hides the body, as a block without rows has none.
 * @throws IllegalArgumentException if [skeletonRows] is below one.
 * @throws IllegalStateException if the block already holds rows, a table, an empty state or a load, if this is
 * the content of a load, or if no UI builds the block.
 * @since %CURRENT_VERSION%
 */
fun <T> BlockScope.load(fetch: () -> T, skeletonRows: Int = 3, content: BlockScope.(T) -> Unit): LoadHandle {
    require(skeletonRows >= 1) { "Load must show at least 1 skeleton row, got $skeletonRows" }
    val ui = checkNotNull(UI.getCurrent()) { "Block with a load must be built in a UI thread" }
    val slot = placeLoad(LOAD_OWNER)
    val loader = BlockLoader(slot, texts, ui, skeletonRows, fetch, content)
    slot.onStart = loader::fetchNow
    return LoadHandle(loader)
}

/**
 * Loads a block body: fetches in the background executor and builds the content from the result in the UI thread.
 * Every load gets the next generation number, and a result of an earlier generation is dropped; so is a result for
 * a UI detached meanwhile. A body detached from its UI starts no load and drops a result; it loads again on attach.
 *
 * @param T the type of the fetched value.
 */
internal class BlockLoader<T>(
    private val slot: LoadedBody,
    private val texts: UiTexts,
    ui: UI,
    private val skeletonRows: Int,
    private val fetch: () -> T,
    private val content: BlockScope.(T) -> Unit,
) {
    private val generation = AtomicLong()

    // The UI that shows the block: the one that builds it, then the one it is attached to. Kept here because reload
    // may run in a thread without a current UI, where the component tree must not be read.
    @Volatile
    private var ui: UI = ui

    val component: Component = slot.component

    // Whether a reload or a result was skipped while the body was detached; used in the UI thread only.
    private var isMissed = false

    /** Whether the loaded body and the pagination of a loaded table are shown. */
    var isShown: Boolean
        get() = slot.isShown
        set(value) {
            slot.isShown = value
        }

    init {
        component.addAttachListener { event ->
            this.ui = event.ui
            if (isMissed) reloadNow()
        }
        showSkeleton()
    }

    /** Shows the placeholder and loads again in the UI thread of the block, or on its next attach if detached. */
    fun reload() {
        Background.inUi(ui) { if (component.isAttached) reloadNow() else isMissed = true }
    }

    /** Starts a fetch of the next generation in the background; called in the UI thread. */
    fun fetchNow() {
        val loadGeneration = generation.incrementAndGet()
        val target = ui
        Background.executor().execute {
            val outcome = fetchOutcome()
            Background.inUi(target) { if (generation.get() == loadGeneration) showIfAttached(outcome) }
        }
    }

    private fun reloadNow() {
        isMissed = false
        showSkeleton()
        fetchNow()
    }

    private fun showIfAttached(outcome: Outcome<T>) {
        if (component.isAttached) show(outcome) else isMissed = true
    }

    private fun showSkeleton() {
        slot.showWhole(buildSkeletonRows(skeletonRows), isFlush = true)
        slot.isBusy = true
    }

    @Suppress("TooGenericExceptionCaught")
    private fun fetchOutcome(): Outcome<T> = try {
        Outcome.Loaded(fetch())
    } catch (error: Exception) {
        // fetch is page code: its failure shows in the block, not on the whole page.
        logger.error("Block load failed", error)
        Outcome.Failed
    }

    @Suppress("TooGenericExceptionCaught")
    private fun show(outcome: Outcome<T>) {
        when (outcome) {
            is Outcome.Loaded -> try {
                slot.fill { content(outcome.value) }
                slot.isBusy = false
            } catch (error: Exception) {
                // content is page code: its failure shows in the block, not on the whole page.
                logger.error("Block load failed", error)
                showFailure()
            }
            Outcome.Failed -> showFailure()
        }
    }

    private fun showFailure() {
        val failure = EmptyContent(
            title = texts.load.failed,
            description = texts.load.failedHint,
            actions = { action(texts.load.retry) { onClick { reload() } } },
        )
        slot.showWhole(buildEmptyState(failure, texts, gridColumns = slot.columns, isError = true), isFlush = false)
        slot.isBusy = false
    }

    /** What a fetch ended with: the [Loaded.value] or a failure. */
    private sealed interface Outcome<out T> {
        class Loaded<T>(val value: T) : Outcome<T>

        data object Failed : Outcome<Nothing>
    }
}
