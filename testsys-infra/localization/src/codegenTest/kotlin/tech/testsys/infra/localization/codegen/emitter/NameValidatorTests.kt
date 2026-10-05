package tech.testsys.infra.localization.codegen.emitter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.ruErrors
import tech.testsys.infra.localization.codegen.signature.MergedKey

class NameValidatorTests {

    @Test
    fun `should reject a key without a bundle segment`() {
        assertEquals(
            listOf("RU / task: a key must be '<bundle>.<segment>…' with segments matching [a-z][a-z0-9_]*"),
            ruErrors("task=x"),
        )
    }

    @Test
    fun `should reject a bundle that produces a generated type name`() {
        assertEquals(
            listOf(
                "RU / localization.title: the bundle 'localization' produces the class 'Localization', which clashes with a " +
                    "generated or runtime type",
            ),
            ruErrors("localization.title=x"),
        )
    }

    @Test
    fun `should reject a bundle that produces the name of the companion object`() {
        assertEquals(
            listOf(
                "RU / companion.title: the bundle 'companion' produces the class 'Companion', which clashes with a " +
                    "generated or runtime type",
            ),
            ruErrors("companion.title=x"),
        )
    }

    @Test
    fun `should reject bundles whose classes differ only in case`() {
        assertEquals(
            listOf(
                "RU / test_case.b: the bundles 'test_case', 'testcase' produce the classes 'TestCase', 'Testcase', which " +
                    "differ only in case and share one file on a case-insensitive file system",
            ),
            ruErrors("testcase.a=x", "test_case.b=y"),
        )
    }

    @Test
    fun `should reject bundles that produce the same class`() {
        assertEquals(
            listOf("RU / my__task.b: the bundles 'my__task', 'my_task' all produce the class 'MyTask'"),
            ruErrors("my_task.a=x", "my__task.b=y"),
        )
    }

    @Test
    fun `should reject keys that produce the same method`() {
        assertEquals(
            listOf("RU / task.in__days: the keys 'task.in__days', 'task.in_days' all produce the method 'Task.inDays'"),
            ruErrors("task.in_days=x", "task.in__days=y"),
        )
    }

    @Test
    fun `should reject a method named like a member of Any`() {
        assertEquals(
            listOf("RU / task.to_string: the method name 'toString' clashes with a member of Any"),
            ruErrors("task.to_string=x"),
        )
    }

    @Test
    fun `should reject nested enums with the same name`() {
        assertEquals(
            listOf("RU / user.a: the selectors of 'user.a', 'user.a_b' all produce the nested enum 'User.ABC'"),
            ruErrors(
                "user.a_b=.input {\$c :string} .match \$c x {{x}} * {{y}}",
                "user.a=.input {\$bC :string} .match \$bC x {{x}} * {{y}}",
            ),
        )
    }

    @Test
    fun `should reject nested enums whose names differ only in case`() {
        assertEquals(
            listOf(
                "RU / user.a: the selectors of 'user.a', 'user.ab' produce the nested enums 'User.ABc', 'User.AbC', which " +
                    "differ only in case and share one class file on a case-insensitive file system",
            ),
            ruErrors(
                "user.ab=.input {\$c :string} .match \$c x {{x}} * {{y}}",
                "user.a=.input {\$bc :string} .match \$bc x {{x}} * {{y}}",
            ),
        )
    }

    @Test
    fun `should reject a nested enum named like its bundle class`() {
        assertEquals(
            listOf("RU / task_list.task: the nested enum 'TaskList' has the name of its bundle class"),
            ruErrors("task_list.task=.input {\$list :string} .match \$list x {{x}} * {{y}}"),
        )
    }

    @Test
    fun `should accept keys that produce distinct classes and methods`() {
        val keys = listOf(
            MergedKey("task.in_days", emptyMap()),
            MergedKey("task.title", emptyMap()),
            MergedKey("user.name", emptyMap()),
        )

        val problems = NameValidator.findProblems(keys) { "RU" }

        assertEquals(emptyList<String>(), problems)
    }
}
