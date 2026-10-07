package tech.testsys.web.components.display

private const val INITIALS_LENGTH = 2
private val WORD_SEPARATOR = Regex("\\s+")

/** Derives two initials from [name] using the explicitly supplied [locale]. */
internal fun avatarInitials(name: String, locale: java.util.Locale): String = name.trim()
    .split(WORD_SEPARATOR).filter(String::isNotEmpty).take(INITIALS_LENGTH)
    .joinToString("") { word -> word.take(1) }.uppercase(locale)
