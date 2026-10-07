package tech.testsys.web.devapp.showcase

import tech.testsys.web.components.actions.action
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.icon
import tech.testsys.web.components.display.CounterKind
import tech.testsys.web.components.display.TagKind
import tech.testsys.web.components.display.Tone
import tech.testsys.web.components.display.Trend
import tech.testsys.web.components.display.badge
import tech.testsys.web.components.display.counter
import tech.testsys.web.components.display.statCard
import tech.testsys.web.components.display.tag
import tech.testsys.web.components.display.text
import tech.testsys.web.components.display.verdict
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.alert
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.layout.PageScope

private const val DEMO_VERDICT_SCORE = 87.0

private const val ICONS_PER_ROW = 4

internal const val LONG_TEXT = "Дан ориентированный граф из n вершин и m рёбер. " +
    "Найдите кратчайший путь от вершины 1 до вершины n. " +
    "Если пути нет, выведите −1. " +
    "Веса рёбер — целые числа от 1 до 10⁹. " +
    "Граф может содержать кратные рёбра и петли. " +
    "Ограничение времени — одна секунда, памяти — 256 МБ."

internal fun PageScope.displaySection() {
    row {
        slot(size = 12) {
            row {
                block(title = "Отображение") {
                    row { text("Обычный текст абзаца на всю ширину блока") }
                    row {
                        horizontal {
                            verdict(0.0)
                            verdict(DEMO_VERDICT_SCORE, label = "баллов")
                        }
                    }
                    row {
                        horizontal {
                            icon(IconName.Trophy)
                            text("Иконка рядом с текстом")
                        }
                    }
                    row { horizontal { TagKind.entries.forEach { kind -> tag(kind.name, kind) } } }
                    row { horizontal { Tone.entries.forEach { tone -> badge(tone.name, tone) } } }
                    row { horizontal { CounterKind.entries.forEach { kind -> counter(value = 12, kind = kind) } } }
                }
            }
        }
        slot(size = 12) {
            row {
                Trend.entries.forEach { trend ->
                    statCard(label = "Тренд ${trend.name}", value = "1 842", size = 6, delta = "+38", trend = trend)
                }
            }
            row { statCard(label = "Без изменения", value = "42") }
            row {
                block(title = "Метрики в строке блока") {
                    row {
                        statCard(label = "Решено", value = "42", size = 6, delta = "+3", trend = Trend.Up)
                        statCard(label = "Попытки", value = "118", size = 6, delta = "−2", trend = Trend.Down)
                    }
                }
            }
        }
    }
}

internal fun PageScope.feedbackSection() {
    block(title = "Обратная связь") {
        FeedbackKind.entries.forEach { kind -> row { alert(kind, "Алерт ${kind.name}", text = "Описание под заголовком") } }
        row {
            horizontal {
                FeedbackKind.entries.forEach { kind ->
                    action("Показать тост ${kind.name}") {
                        onClick { toast(kind = kind, title = "Тост ${kind.name}", description = "Описание тоста") }
                    }
                }
            }
        }
    }
}

internal fun PageScope.iconGallery() {
    block(title = "Icon: поддерживаемые имена") {
        IconName.entries.chunked(ICONS_PER_ROW).forEach { names ->
            row {
                names.forEach { name ->
                    horizontal(size = 6) {
                        icon(name)
                        text(name.name)
                    }
                }
            }
        }
    }
}
