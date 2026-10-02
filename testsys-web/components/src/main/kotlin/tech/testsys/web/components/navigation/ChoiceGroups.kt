package tech.testsys.web.components.navigation

import com.vaadin.flow.component.Text
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.Signal
import tech.testsys.web.components.Bindable
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.TestSysDsl
import tech.testsys.web.components.display.CounterKind
import tech.testsys.web.components.display.buildCounter
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.BlockScope
import tech.testsys.web.components.layout.ContentScope

private const val MIN_OPTIONS = 2

/** Option of a tab or pill group; [countKind] is `null` for a pill, which has no counter. */
internal class ChoiceOption<V>(val value: V, val label: String, val count: Int?, val countKind: CounterKind?)

/**
 * Scope of block tabs: the tabs in order.
 *
 * @param V the type of the tab values.
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class TabsScope<V> internal constructor() {
    internal val options = mutableListOf<ChoiceOption<V>>()

    /**
     * Adds a tab of [value] named [label]; a [count] above zero shows a counter of [countKind] next to the label.
     *
     * @throws IllegalArgumentException if [count] is negative.
     * @since %CURRENT_VERSION%
     */
    fun tab(value: V, label: String, count: Int? = null, countKind: CounterKind = CounterKind.Neutral) {
        require(count == null || count >= 0) { "Tab '$label' count must not be negative, got $count" }
        options += ChoiceOption(value, label, count, countKind)
    }
}

/**
 * Scope of pills: the pills in order.
 *
 * @param V the type of the pill values.
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class PillsScope<V> internal constructor() {
    internal val options = mutableListOf<ChoiceOption<V>>()

    /**
     * Adds a pill of [value] named [label].
     *
     * @since %CURRENT_VERSION%
     */
    fun pill(value: V, label: String) {
        options += ChoiceOption(value, label, count = null, countKind = null)
    }
}

/**
 * Handle of a group of tabs or pills: the chosen value and the listener of choices.
 *
 * @param V the type of the values.
 * @property value the chosen value; setting it does not run the listener.
 * @since %CURRENT_VERSION%
 */
open class ChoiceHandle<V> internal constructor(private val group: ChoiceGroup<V>) : ElementHandle(group.root) {
    var value: V
        get() = group.value
        set(value) {
            group.select(value)
        }

    /**
     * Runs [listener] with the value a user chooses; a click on the chosen button does not run it. Replaces a
     * listener set by an earlier call.
     *
     * @since %CURRENT_VERSION%
     */
    fun onChange(listener: (V) -> Unit) {
        group.onChange = listener
    }
}

/**
 * Handle of block tabs: the chosen value, the listener of choices and the counters.
 *
 * @param V the type of the tab values.
 * @since %CURRENT_VERSION%
 */
class TabsHandle<V> internal constructor(private val tabs: ChoiceGroup<V>) : ChoiceHandle<V>(tabs) {
    private val counts = mutableMapOf<V, Bindable<Int?>>()

    /**
     * Shows [count] next to the tab of [value]; `null` or zero hides the counter.
     *
     * @throws IllegalArgumentException if no tab has [value] or [count] is negative.
     * @throws BindingActiveException if the counter of [value] is bound by [bindCount].
     * @since %CURRENT_VERSION%
     */
    fun setCount(value: V, count: Int?) {
        counter(value).value = count
    }

    /**
     * Binds the counter of the tab of [value] to [signal]: every count it produces is shown at once, `null` or zero
     * hides the counter. A manual [setCount] of that tab while bound, and a second binding of it, throw
     * [BindingActiveException]; the other tabs keep their own counters.
     *
     * @throws IllegalArgumentException if no tab has [value].
     * @since %CURRENT_VERSION%
     */
    fun bindCount(value: V, signal: Signal<Int?>): SignalBinding<Int?> = counter(value).bind(signal)

    private fun counter(value: V): Bindable<Int?> = counts.getOrPut(value) {
        Bindable(tabs.root.element, initial = tabs.count(value)) { count -> tabs.setCount(value, count) }
    }
}

/**
 * Adds tabs to the block head that switch what the block shows; without a block title they stand in its place, with
 * a title they take a second line of the head. The page reacts to a choice through [TabsHandle.onChange].
 *
 * @param V the type of the tab values.
 * @throws IllegalArgumentException if there are fewer than two tabs, their values repeat or [initial] is not among them.
 * @throws IllegalStateException if the block already has tabs.
 * @since %CURRENT_VERSION%
 */
fun <V> BlockScope.tabs(initial: V, content: TabsScope<V>.() -> Unit): TabsHandle<V> {
    val root = Div().apply { addClassNames("ts-tabs", "ts-tabs--bare", "ts-tabs--lg") }
    title?.let { name -> root.element.setAttribute("aria-label", name) }
    val group = ChoiceGroup(
        options = TabsScope<V>().apply(content).options,
        initial = initial,
        root = root,
        buttonClass = "ts-tab",
        activeClass = "ts-tab--active",
    )
    placeTabs(root)
    return TabsHandle(group)
}

/**
 * Adds pills that switch a category of what the block shows, e.g. in the block head.
 *
 * @param V the type of the pill values.
 * @throws IllegalArgumentException if there are fewer than two pills, their values repeat or [initial] is not among them.
 * @since %CURRENT_VERSION%
 */
fun <V> ContentScope.pills(initial: V, content: PillsScope<V>.() -> Unit): ChoiceHandle<V> {
    val group = pillGroup(initial, content)
    add(group.root)
    return ChoiceHandle(group)
}

/**
 * Adds pills on [size] columns of the row, or on the rest of it.
 *
 * @param V the type of the pill values.
 * @throws IllegalArgumentException if there are fewer than two pills, their values repeat or [initial] is not among them.
 * @since %CURRENT_VERSION%
 */
fun <V> BlockRowScope.pills(initial: V, size: Int? = null, content: PillsScope<V>.() -> Unit): ChoiceHandle<V> {
    val group = pillGroup(initial, content)
    place(size, group.root)
    return ChoiceHandle(group)
}

private fun <V> pillGroup(initial: V, content: PillsScope<V>.() -> Unit): ChoiceGroup<V> = ChoiceGroup(
    options = PillsScope<V>().apply(content).options,
    initial = initial,
    root = Div().apply { addClassName("ts-pills") },
    buttonClass = "ts-pill",
    activeClass = "ts-pill--active",
)

/**
 * Buttons of [options] in [root] with [initial] chosen; the chosen button carries [activeClass] and `aria-pressed`.
 */
internal class ChoiceGroup<V>(
    options: List<ChoiceOption<V>>,
    initial: V,
    val root: Div,
    buttonClass: String,
    private val activeClass: String,
) {
    private val counters = mutableMapOf<V, Span>()
    private val buttons: Map<V, NativeButton>

    var onChange: (V) -> Unit = {}

    var value: V = initial
        private set

    init {
        require(options.size >= MIN_OPTIONS) { "A choice group needs at least $MIN_OPTIONS options, got ${options.size}" }
        require(options.map { option -> option.value }.toSet().size == options.size) { "Choice group values must be unique" }
        require(options.any { option -> option.value == initial }) { "Initial value $initial is not among the options" }
        root.element.setAttribute("role", "group")
        buttons = options.associate { option -> option.value to button(option, buttonClass) }
        buttons.values.forEach { button -> root.add(button) }
        update()
    }

    fun select(target: V) {
        require(target in buttons) { "Value $target is not among the options" }
        value = target
        update()
    }

    fun setCount(target: V, count: Int?) {
        require(count == null || count >= 0) { "Count must not be negative, got $count" }
        showCount(counter(target), count)
    }

    /** The count shown next to [target], or `null` if its counter is empty. */
    fun count(target: V): Int? = counter(target).text.toIntOrNull()

    private fun counter(target: V): Span = requireNotNull(counters[target]) { "Value $target is not among the tabs" }

    private fun button(option: ChoiceOption<V>, buttonClass: String): NativeButton = NativeButton().apply {
        addClassName(buttonClass)
        element.setAttribute("type", "button")
        add(Text(option.label))
        option.countKind?.let { kind ->
            val counter = buildCounter(option.count ?: 0, kind)
            showCount(counter, option.count)
            counters[option.value] = counter
            add(counter)
        }
        addClickListener { choose(option.value) }
    }

    private fun choose(target: V) {
        if (target == value) return
        value = target
        update()
        onChange(target)
    }

    private fun update() {
        buttons.forEach { (buttonValue, button) ->
            val isActive = buttonValue == value
            button.setClassName(activeClass, isActive)
            button.element.setAttribute("aria-pressed", isActive.toString())
        }
    }

    private fun showCount(counter: Span, count: Int?) {
        counter.text = count?.toString().orEmpty()
        counter.isVisible = count != null && count > 0
    }
}
