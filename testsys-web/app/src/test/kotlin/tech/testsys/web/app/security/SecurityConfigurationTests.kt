package tech.testsys.web.app.security

import com.vaadin.flow.server.VaadinService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.service.CurrentUser
import java.util.concurrent.CompletableFuture

@SpringBootTest
class SecurityConfigurationTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var currentUser: CurrentUser

    @Test
    fun `should give background work of a page the signed-in user`() {
        val user = fixtures.userOf(UserKind.MULTIPLE_ROLE)
        signIn(user)

        val result = CompletableFuture.supplyAsync({ currentUser.user() }, VaadinService.getCurrent().executor).get()

        assertEquals(user.id, result.id)
    }
}
