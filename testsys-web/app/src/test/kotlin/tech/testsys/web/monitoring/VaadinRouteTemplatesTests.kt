package tech.testsys.web.monitoring

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class VaadinRouteTemplatesTests {
    private val templates = VaadinRouteTemplates(
        listOf(
            "",
            "login",
            "admin/:section?(communities|users)",
            "admin/communities/:communityId([0-9]+)",
            "admin/users/:userId([0-9]+)/:section?(developer|manager|judge)",
            "admin/observers/:observerId([0-9]+)",
            "developer/tasks/:taskId([0-9]+)",
            "developer/tasks/:taskId([0-9]+)/resources/:resourceId([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})",
            "files/:path*",
            "users/:userId",
        ),
    )

    @ParameterizedTest
    @CsvSource(
        "/, /",
        "/login, /login",
        "/developer/tasks/42, /developer/tasks/:taskId",
        "/developer/tasks/42/resources/0f8fad5b-d9cb-469f-a165-70867728950e, /developer/tasks/:taskId/resources/:resourceId",
        "/admin/observers/7, /admin/observers/:observerId",
        "/users/anything, /users/:userId",
    )
    fun `should name a matched path by its template without parameter values`(path: String, label: String) {
        assertEquals(label, templates.match(path))
    }

    @ParameterizedTest
    @CsvSource("/admin/users/5", "/admin/users/5/judge")
    fun `should name both variants of an optional parameter by one template`(path: String) {
        assertEquals("/admin/users/:userId/:section?", templates.match(path))
    }

    @ParameterizedTest
    @CsvSource("/admin", "/admin/users", "/admin/communities")
    fun `should name a page with an optional section by its template`(path: String) {
        assertEquals("/admin/:section?", templates.match(path))
    }

    @Test
    fun `should prefer the template with more literal segments`() {
        assertEquals("/admin/communities/:communityId", templates.match("/admin/communities/3"))
    }

    @ParameterizedTest
    @CsvSource("/files", "/files/a/b/c")
    fun `should match any number of segments with a wildcard parameter`(path: String) {
        assertEquals("/files/:path*", templates.match(path))
    }

    @ParameterizedTest
    @CsvSource("/developer/tasks/abc", "/admin/other", "/admin/users/5/unknown", "/no-such/987", "/login/extra")
    fun `should not match a path rejected by the value patterns or without a route`(path: String) {
        assertNull(templates.match(path))
    }
}
