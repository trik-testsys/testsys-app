package tech.testsys.infra.localization.runtime

import com.ibm.icu.util.ULocale
import tech.testsys.infra.localization.InternalLocalizationApi
import java.time.Instant
import java.time.ZoneId

/** The context zone of the runtime tests. */
internal val MOSCOW: ZoneId = ZoneId.of("Europe/Moscow")

/** A moment formatted by the runtime tests: 11:53:20 in Moscow. */
internal val INSTANT: Instant = Instant.parse("2025-10-09T08:53:20Z")

private const val USER_TERM = ".input {\$case :string} .input {\$form :string} .match \$case \$form " +
    "nom one {{Пользователь}} nom few {{Пользователя}} nom many {{Пользователей}} nom other {{Пользователя}} " +
    "nom sg {{Пользователь}} nom pl {{Пользователи}} * * {{Пользователь}}"
private const val TASK_TERM = ".input {\$case :string} .input {\$form :string} .match \$case \$form " +
    "gen few {{Задач}} gen pl {{Задач}} * * {{Задача}}"

/** A runtime of the region RU with the terms `user` and `task` and the messages [patterns] (key → MF2 message). */
@InternalLocalizationApi
internal fun ruRuntime(vararg patterns: Pair<String, String>): MessageRuntime = MessageRuntime(
    regionId = "RU",
    locale = ULocale.forLanguageTag("ru-RU"),
    patterns = patterns.toMap(),
    terms = mapOf("user" to USER_TERM, "task" to TASK_TERM),
    dateZones = emptyMap(),
)
