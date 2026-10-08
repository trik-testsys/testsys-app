package tech.testsys.infra.localization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.bundle.SupportedRegion
import java.time.ZoneId

// The golden test of the product messages; the functions and constructs are shown by ExampleLocalizationTests.
class LocalizationTests {

    private val l = Localization.forRegion(SupportedRegion.RU, ZoneId.of("Europe/Moscow"))

    @Test
    fun `should point to the example set in todo example`() {
        assertEquals("View examples in testsys-infra/localization/src/test/examples/localization", l.todo.example())
    }

    @Nested
    inner class UserTests {

        @Test
        fun `should name the confirmation code in registration confirmation code subject`() {
            assertEquals("Код подтверждения для регистрации в TestSys", l.user.registrationConfirmationCodeSubject())
        }

        @Test
        fun `should greet without nickname and show the code on its own line in registration confirmation code body`() {
            assertEquals(
                "Здравствуйте!\n\n" +
                    "Вы начали регистрацию в TestSys. Чтобы подтвердить адрес электронной почты, введите этот код " +
                    "на странице регистрации:\n\n" +
                    "01234567\n\n" +
                    "Код действует ограниченное время. Если код больше не действует, начните регистрацию заново, " +
                    "и Вам придёт новый код.\n\n" +
                    "Если Вам пришло несколько таких писем, введите код из последнего.\n\n" +
                    "Если Вы не начинали регистрацию в TestSys, ничего делать не нужно. Просто удалите это письмо.",
                l.user.registrationConfirmationCodeBody(confirmationCode = "01234567"),
            )
        }

        @Test
        fun `should render the access code term in registration access code subject`() {
            assertEquals("Ваш Код-доступа к TestSys", l.user.registrationAccessCodeSubject())
        }

        @Test
        fun `should show the case-sensitive access code verbatim in registration access code body`() {
            assertEquals(
                "Здравствуйте, Маша!\n\n" +
                    "Регистрация в TestSys завершена. Ваш Код-доступа:\n\n" +
                    "aB3d-x9Yz-0kLm-P7qR\n\n" +
                    "Чтобы войти в TestSys, введите этот код на странице входа. Больше ничего вводить не нужно.\n\n" +
                    "Вводите код точно так, как он написан здесь, вместе с дефисами. Большие и маленькие буквы " +
                    "различаются.\n\n" +
                    "Сохраните это письмо или запишите код. Никому его не сообщайте: тот, кто знает код, может войти " +
                    "в TestSys под Вашим именем.",
                l.user.registrationAccessCodeBody(name = "Маша", accessCode = "aB3d-x9Yz-0kLm-P7qR"),
            )
        }

        @Test
        fun `should name the confirmation code in email change confirmation code subject`() {
            assertEquals(
                "Код подтверждения для смены адреса электронной почты в TestSys",
                l.user.emailChangeConfirmationCodeSubject(),
            )
        }

        @Test
        fun `should greet without nickname and show the code on its own line in email change confirmation code body`() {
            assertEquals(
                "Здравствуйте!\n\n" +
                    "Вы запросили смену адреса электронной почты в TestSys на этот адрес. Чтобы подтвердить новый " +
                    "адрес, введите в TestSys этот код:\n\n" +
                    "01234567\n\n" +
                    "Код действует ограниченное время. Если код больше не действует, запросите смену адреса заново, " +
                    "и Вам придёт новый код.\n\n" +
                    "Если Вам пришло несколько таких писем, введите код из последнего.\n\n" +
                    "Если Вы не запрашивали смену адреса электронной почты в TestSys, ничего делать не нужно. " +
                    "Просто удалите это письмо.",
                l.user.emailChangeConfirmationCodeBody(confirmationCode = "01234567"),
            )
        }

        @Test
        fun `should state the change in email change notice subject`() {
            assertEquals("Адрес электронной почты в TestSys изменён", l.user.emailChangeNoticeSubject())
        }

        @Test
        fun `should greet by nickname and render the access code term in accusative in email change notice body`() {
            assertEquals(
                "Здравствуйте, Маша!\n\n" +
                    "Ваш адрес электронной почты в TestSys изменён. Письма от TestSys теперь будут приходить " +
                    "на новый адрес, а не на этот.\n\n" +
                    "Изменить адрес может только тот, кто знает Ваш Код-доступа.",
                l.user.emailChangeNoticeBody(name = "Маша"),
            )
        }
    }
}
