package tech.testsys.web.components.core

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.vaadin.flow.component.UI
import com.vaadin.flow.server.VaadinService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import java.util.concurrent.Executor
import kotlin.concurrent.thread

class BackgroundTests : MockVaadinTests() {
    @AfterEach
    fun resetExecutor() {
        Background.executorOverride = null
    }

    @Nested
    inner class ExecutorTests {
        @Test
        fun `should return the executor of the Vaadin service`() {
            assertSame(VaadinService.getCurrent().executor, Background.executor())
        }

        @Test
        fun `should return the override if it is set`() {
            val override = Executor { command -> command.run() }
            Background.executorOverride = override

            assertSame(override, Background.executor())
        }

        @Test
        fun `should throw outside a UI thread without an override`() {
            var failure: Throwable? = null

            thread { failure = runCatching { Background.executor() }.exceptionOrNull() }.join()

            assertInstanceOf(IllegalStateException::class.java, failure)
        }
    }

    @Nested
    inner class InUiTests {
        @Test
        fun `should run the action at once in the UI thread`() {
            val calls = mutableListOf<String>()

            Background.inUi(UI.getCurrent()) { calls += "run" }

            assertEquals(listOf("run"), calls)
        }

        @Test
        fun `should not run the action from another thread before the UI runs queued work`() {
            val ui = UI.getCurrent()
            val calls = mutableListOf<String>()

            thread { Background.inUi(ui) { calls += "run" } }.join()

            assertTrue(calls.isEmpty())
        }

        @Test
        fun `should run the action from another thread in the UI thread with the UI current`() {
            val ui = UI.getCurrent()
            val runs = mutableListOf<Pair<Thread, UI?>>()
            thread { Background.inUi(ui) { runs += Thread.currentThread() to UI.getCurrent() } }.join()

            MockVaadin.clientRoundtrip()

            assertEquals(listOf(Thread.currentThread() to ui), runs)
        }

        @Test
        fun `should skip the action if the UI is detached`() {
            val ui = UI.getCurrent()
            ui.session.removeUI(ui)
            val calls = mutableListOf<String>()

            Background.inUi(ui) { calls += "run" }

            assertTrue(calls.isEmpty())
        }
    }
}
