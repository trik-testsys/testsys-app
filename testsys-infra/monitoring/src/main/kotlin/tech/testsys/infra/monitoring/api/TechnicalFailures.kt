package tech.testsys.infra.monitoring.api

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import java.util.Collections
import java.util.WeakHashMap

/**
 * Counts technical failures in [registry] where their handling ends; a counted exception, its cause or an exception
 * caused by a counted one is not counted again, while a shared deeper cause does not merge different exceptions.
 * The companion object provides [NONE], which counts nothing.
 *
 * @since %CURRENT_VERSION%
 */
class TechnicalFailures(private val registry: MeterRegistry) {
    // Throwable keeps identity equality, so the weak maps track instances without holding them after their handling.
    private val counted: MutableSet<Throwable> = Collections.newSetFromMap(WeakHashMap())

    private val chained: MutableSet<Throwable> = Collections.newSetFromMap(WeakHashMap())

    /**
     * Counts one failure at [stage] of [source]; [failure] is the exception, or `null` for a handled refusal without one.
     * Both names are lowercase words separated by underscores.
     *
     * @since %CURRENT_VERSION%
     */
    fun record(source: String, stage: String, failure: Throwable? = null) {
        require(LABEL.matches(source)) { "Technical failure source must be a lowercase label: $source" }
        require(LABEL.matches(stage)) { "Technical failure stage must be a lowercase label: $stage" }
        if (failure != null && !isNew(failure)) return

        registry.counter(
            TestSysMetrics.TECHNICAL_FAILURES,
            TestSysMetrics.SOURCE,
            source,
            TestSysMetrics.STAGE,
            stage,
            TestSysMetrics.EXCEPTION,
            failure?.let(::exceptionName) ?: TestSysMetrics.NO_EXCEPTION,
        ).increment()
    }

    private fun isNew(failure: Throwable): Boolean {
        val chain = causes(failure)
        synchronized(counted) {
            if (failure in chained || chain.any { throwable -> throwable in counted }) return false
            counted.add(failure)
            chained.addAll(chain)
        }
        return true
    }

    /**
     * Holds [NONE], backed by a registry without children, for components started without a metrics registry.
     *
     * @since %CURRENT_VERSION%
     */
    companion object {
        val NONE: TechnicalFailures = TechnicalFailures(CompositeMeterRegistry())

        private val LABEL = Regex("[a-z][a-z0-9_]*")

        private fun causes(failure: Throwable): List<Throwable> {
            val chain = mutableListOf<Throwable>()
            var current: Throwable? = failure
            while (current != null && chain.none { seen -> seen === current }) {
                chain.add(current)
                current = current.cause
            }
            return chain
        }

        private fun exceptionName(failure: Throwable): String =
            failure.javaClass.simpleName.ifEmpty { failure.javaClass.name.substringAfterLast('.') }
    }
}
