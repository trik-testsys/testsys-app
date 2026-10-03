package tech.testsys.infra.localization.examples

import com.ibm.icu.util.Currency
import com.ibm.icu.util.CurrencyAmount
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.infra.localization.examples.bundle.SupportedRegion
import java.time.Instant
import java.time.ZoneId

// The golden test of the example set: one message per supported function and construct, rendered through the
// generated API. The JVM default zone is UTC in this test task, so every date below without a zone of its own proves
// that the context zone applies.
class ExampleLocalizationTests {

    private val l = Localization.forRegion(SupportedRegion.RU, ZoneId.of("Europe/Moscow"))
    private val instant = Instant.parse("2025-10-09T08:53:20Z")

    @Nested
    inner class TaskTests {

        @ParameterizedTest
        @CsvSource(
            "0, Вы решили 0 Задач",
            "1, Вы решили 1 Задачу",
            "2, Вы решили 2 Задачи",
            "5, Вы решили 5 Задач",
            "21, Вы решили 21 Задачу",
        )
        fun `should decline the counted term in result`(count: Int, expected: String) {
            assertEquals(expected, l.task.result(count = count))
        }

        @Test
        fun `should prefer the exact key 0 in progress`() {
            assertEquals("Вы ещё не решили ни одной Задачи", l.task.progress(count = 0))
        }

        @ParameterizedTest
        @CsvSource("1, Решена 1 Задача", "2, Решены 2 Задачи", "5, Решено 5 Задач", "21, Решена 21 Задача")
        fun `should decline the term in progress`(count: Int, expected: String) {
            assertEquals(expected, l.task.progress(count = count))
        }

        @Test
        fun `should render the singular bare term in created`() {
            assertEquals("Задача создана", l.task.created())
        }

        @Test
        fun `should render the plural genitive bare term in list title`() {
            assertEquals("Список Задач", l.task.listTitle())
        }

        @ParameterizedTest
        @CsvSource("1, одна Задача", "2, две Задачи", "5, пять Задач", "21, двадцать одна Задача")
        fun `should spell out the count in the feminine and decline the term in count words`(count: Int, expected: String) {
            assertEquals(expected, l.task.countWords(count = count))
        }

        @ParameterizedTest
        @CsvSource("1, через 1 день", "3, через 3 дня", "11, через 11 дней")
        fun `should select the plural category in deadline in days`(days: Int, expected: String) {
            assertEquals(expected, l.task.deadlineInDays(days = days))
        }

        @Test
        fun `should render the static deadline passed`() {
            assertEquals("Дедлайн прошёл", l.task.deadlinePassed())
        }

        @Test
        fun `should keep the ICU default fraction digits in score percent`() {
            assertEquals("Результат: 87,34$NBSP%", l.task.scorePercent(value = 0.8734))
        }

        @ParameterizedTest
        @CsvSource(
            "0.01, Набран 1 % от максимума",
            "0.02, Набрано 2 % от максимума",
            "0.05, Набрано 5 % от максимума",
            "0.015, 'Набрано 1,5 % от максимума'",
        )
        fun `should select on the value multiplied by 100 in score share`(share: Double, expected: String) {
            assertEquals(expected, l.task.scoreShare(share = share))
        }

        @ParameterizedTest
        @CsvSource("1, 1 балл", "1.5, '1,5 балла'", "3, 3 балла", "21, 21 балл")
        fun `should select on the formatted number in score points`(points: Double, expected: String) {
            assertEquals(expected, l.task.scorePoints(points = points))
        }

        @ParameterizedTest
        @CsvSource("1, 'Средний балл: 4,3'", "2, 'Средний балл: 4,26'")
        fun `should take the fraction digits from a variable in score average`(precision: Int, expected: String) {
            assertEquals(expected, l.task.scoreAverage(average = 4.2567, precision = precision))
        }

        @Test
        fun `should use compact notation in solutions total`() {
            assertEquals("Всего решений: 1,2${NBSP}млн", l.task.solutionsTotal(total = 1_234_567))
        }
    }

    @Nested
    inner class UserTests {

        @Test
        fun `should render the plural nominative bare term in deleted`() {
            assertEquals("Пользователи удалены", l.user.deleted())
        }

        @Test
        fun `should select the feminine variant in solved`() {
            assertEquals("Анна решила 3 Задачи", l.user.solved(gender = User.SolvedGender.FEMALE, count = 3, name = "Анна"))
        }

        @Test
        fun `should select the masculine variant in solved`() {
            assertEquals("Иван решил 1 Задачу", l.user.solved(gender = User.SolvedGender.MALE, count = 1, name = "Иван"))
        }

        @Test
        fun `should select the fallback variant in solved if the gender is other`() {
            assertEquals("Пользователь alex решил 5 Задач", l.user.solved(gender = User.SolvedGender.OTHER, count = 5, name = "alex"))
        }

        @ParameterizedTest
        @CsvSource("5, Изменение рейтинга: +5", "-3, Изменение рейтинга: -3")
        fun `should always show the sign in rating delta`(delta: Int, expected: String) {
            assertEquals(expected, l.user.ratingDelta(delta = delta))
        }
    }

    @Nested
    inner class SolutionTests {

        @ParameterizedTest
        @CsvSource("ACCEPTED, Решение принято", "REJECTED, Решение отклонено", "OTHER, Решение проверяется")
        fun `should select the status variant`(status: Solution.StatusStatus, expected: String) {
            assertEquals(expected, l.solution.status(status = status))
        }

        @Test
        fun `should show seconds in the context zone in sent`() {
            assertEquals("Решение отправлено в 11:53:20", l.solution.sent(sentAt = instant))
        }

        @Test
        fun `should round to significant digits in duration`() {
            assertEquals("Время выполнения: 1,23 с", l.solution.duration(seconds = 1.23456))
        }

        @Test
        fun `should format the unit with a minimum fraction digit in memory`() {
            assertEquals("Использовано памяти: 12,0 МБ", l.solution.memory(memory = 12))
        }

        @Test
        fun `should take the unit from the argument in limit`() {
            assertEquals("Ограничение ресурса: 5 кг", l.solution.limit(value = 5, unit = "kilogram"))
        }
    }

    @Nested
    inner class ContestTests {

        @ParameterizedTest
        @CsvSource("1, 1-й тур", "3, 3-й тур")
        fun `should render the masculine ordinal in number`(number: Int, expected: String) {
            assertEquals(expected, l.contest.number(number = number))
        }

        @Test
        fun `should use the default datetime format in the context zone in start`() {
            assertEquals("Тур начнётся чт, 9 окт. 2025${NARROW_NBSP}г., 11:53", l.contest.start(start = instant))
        }

        @Test
        fun `should use the literal zone and zone style in start moscow`() {
            val tokyo = Localization.forRegion(SupportedRegion.RU, ZoneId.of("Asia/Tokyo"))

            assertEquals("Начало тура: 9 октября 2025${NARROW_NBSP}г. в 11:53 GMT+3", tokyo.contest.startMoscow(start = instant))
        }

        @Test
        fun `should use the UTC zone of the message in start utc`() {
            assertEquals("Начало тура по UTC: 08:53", l.contest.startUtc(start = instant))
        }

        @Test
        fun `should use the zone argument in start local`() {
            assertEquals(
                "Начало тура по местному времени: чт, 9 окт. 2025${NARROW_NBSP}г., 18:53",
                l.contest.startLocal(start = instant, zone = ZoneId.of("Asia/Vladivostok")),
            )
        }

        @Test
        fun `should keep the zone of the zoned operand in start venue`() {
            assertEquals(
                "Начало тура по времени площадки: чт, 9 окт. 2025${NARROW_NBSP}г., 13:53",
                l.contest.startVenue(start = instant.atZone(ZoneId.of("Asia/Yekaterinburg"))),
            )
        }

        @Test
        fun `should render the long year month day date in deadline`() {
            assertEquals("Приём решений до 9 октября 2025${NARROW_NBSP}г.", l.contest.deadline(deadline = instant))
        }

        @Test
        fun `should render the long month day date in day`() {
            assertEquals("День тура: 9 октября", l.contest.day(day = instant))
        }

        @Test
        fun `should include the weekday in a date without options in date`() {
            assertEquals("Дата тура: чт, 9 окт. 2025${NARROW_NBSP}г.", l.contest.date(date = instant))
        }

        @Test
        fun `should convert to the unit of the road usage in venue distance`() {
            assertEquals("До площадки: 1,5 км", l.contest.venueDistance(distance = 1_500))
        }

        @ParameterizedTest
        @CsvSource("1, Осталась последняя попытка", "3, Осталось попыток: 3", "21, Осталось попыток: 21")
        fun `should match only exact values in attempts`(left: Int, expected: String) {
            assertEquals(expected, l.contest.attempts(left = left))
        }

        @ParameterizedTest
        @CsvSource("START, 1, До начала тура остался 1 день", "END, 3, До конца тура осталось 3 дня", "OTHER, 5, Осталось 5 дней")
        fun `should select on two selectors in countdown`(phase: Contest.CountdownPhase, days: Int, expected: String) {
            assertEquals(expected, l.contest.countdown(phase = phase, days = days))
        }
    }

    @Nested
    inner class CompetitionTests {

        @ParameterizedTest
        @CsvSource("1, Вы заняли 1-е место", "2, Вы заняли 2-е место")
        fun `should render the neuter ordinal in place`(place: Int, expected: String) {
            assertEquals(expected, l.competition.place(place = place))
        }

        @ParameterizedTest
        @CsvSource("1, Только вы", "2, Вы и ещё 1 участник", "4, Вы и ещё 3 участника", "22, Вы и ещё 21 участник")
        fun `should select on the offset in participants`(count: Int, expected: String) {
            assertEquals(expected, l.competition.participants(count = count))
        }

        @ParameterizedTest
        @CsvSource("2, Кроме вас: 1 Пользователь", "3, Кроме вас: 2 Пользователя", "6, Кроме вас: 5 Пользователей")
        fun `should decline the term by the shifted number in rivals`(count: Int, expected: String) {
            assertEquals(expected, l.competition.rivals(count = count))
        }

        @Test
        fun `should not group digits in season`() {
            assertEquals("Сезон 2026", l.competition.season(year = 2026))
        }

        @Test
        fun `should use the year month skeleton in period`() {
            assertEquals("Период проведения: октябрь 2025${NARROW_NBSP}г.", l.competition.period(month = instant))
        }

        @Test
        fun `should use the literal currency in fee`() {
            assertEquals("Взнос за участие: 1${NBSP}500,00$NBSP₽", l.competition.fee(fee = 1500))
        }

        @Test
        fun `should use the currency argument in fee foreign`() {
            assertEquals("Взнос за участие: 1${NBSP}500,00$NBSP€", l.competition.feeForeign(fee = 1500, currency = "EUR"))
        }

        @Test
        fun `should use the currency of the amount in prize`() {
            assertEquals(
                "Призовой фонд: 50${NBSP}000,00$NBSP₽",
                l.competition.prize(prize = CurrencyAmount(50_000, Currency.getInstance("RUB"))),
            )
        }
    }

    @Nested
    inner class ResourceTests {

        @Test
        fun `should format the literal number in size limit`() {
            assertEquals("Размер файла не должен превышать 10${NBSP}000 КБ", l.resource.sizeLimit())
        }

        @Test
        fun `should render the local literal in download`() {
            assertEquals("Скачайте TRIK Studio, чтобы открыть решение", l.resource.download())
        }

        @Test
        fun `should render the quoted pattern starting with a dot in qrs missing`() {
            assertEquals(".qrs-файл решения не найден", l.resource.qrsMissing())
        }

        @Test
        fun `should unescape braces in name template`() {
            assertEquals("Имя файла: {логин}_{задача}.qrs", l.resource.nameTemplate())
        }

        @Test
        fun `should unescape the backslash and the pipe in path hint`() {
            assertEquals("Папки в пути разделяются знаком \\, варианты имени — знаком |", l.resource.pathHint())
        }
    }

    private companion object {
        // ICU separates digit groups and units with a no-break space and the Russian year abbreviation with a
        // narrow one; code points keep the expectations visible in the source.
        val NBSP = Char(0x00A0).toString()
        val NARROW_NBSP = Char(0x202F).toString()
    }
}
