package tech.testsys.web.devapp.dev

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.router.Route
import org.springframework.core.env.Environment
import tech.testsys.web.components.RawVaadin
import tech.testsys.web.components.TestSysBrand
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.UiTexts
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.display.text
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.devapp.demo.DemoView

/** Illustrations of shared tokens and brand assets; visual rules belong to the UI documentation. */
@Route("dev/showcase/foundations")
internal class ShowcaseFoundationsView(texts: UiTexts, private val environment: Environment) : TestSysView(texts), BeforeEnterObserver {
    init {
        page(showcaseHeader()) {
            showcaseHead("Визуальные основы: примеры")
            foundations()
            footer {
                link("Демонстрация сценариев", DemoView::class.java)
            }
        }
    }
    override fun beforeEnter(event: BeforeEnterEvent) {
        if (!environment.matchesProfiles(DEV_PROFILE)) {
            event.rerouteToError(NotFoundException::class.java)
        }
    }
}

/** Reads the packaged CSS so these examples do not repeat token values. */
private fun tokenNames(file: String): List<String> {
    val text = checkNotNull(ShowcaseFoundationsView::class.java.getResource("/META-INF/resources/testsys-ui/tokens/$file.css")).readText()
    return Regex("(--[a-zA-Z0-9-]+)\\s*:").findAll(text).map {
        it.groupValues[1]
    }.distinct().toList()
}

@OptIn(RawVaadin::class)
private fun PageScope.foundations() {
    block("Палитра") {
        tokenNames("colors").forEach { name ->
            row {
                text(name, size = 8)
                horizontal {
                    custom(
                        Div().apply {
                            element.style.set("background", "var($name)")
                            element.style.set("width", "100%")
                            element.style.set("height", "var(--space-6)")
                            element.style.set("border", "1px solid var(--line)")
                            element.style.set("border-radius", "var(--radius-sm)")
                        },
                    )
                }
            }
        }
    }
    block("Типографика") {
        listOf("--font-sans", "--font-mono").forEach { name ->
            row {
                horizontal {
                    custom(
                        Div("$name · TestSys · 0123456789 · Кириллица").apply {
                            element.style.set("font-family", "var($name)")
                        },
                    )
                }
            }
        }
        tokenNames("typography").filter { name -> name.startsWith("--fs-") || name.startsWith("--fw-") }.forEach { name ->
            row {
                text(name, size = 8)
                horizontal {
                    custom(
                        Div("TestSys · Кириллица · 0123456789").apply {
                            element.style.set(
                                if (name.startsWith("--fs-")) "font-size" else "font-weight",
                                "var($name)",
                            )
                        },
                    )
                }
            }
        }
        row {
            text("Размеры и начертания берутся из общих токенов, а не определяются этой страницей.")
        }
    }
    block("Отступы, радиусы, тени и движение") {
        tokenNames("spacing").filter { name ->
            name.startsWith("--space-") || name.startsWith("--radius-") || name.startsWith("--shadow-")
        }.forEach { name ->
            row {
                text(name, size = 8)
                horizontal {
                    custom(
                        Div(name).apply {
                            element.style.set(
                                "padding",
                                if (name.startsWith("--space-")) {
                                    "var($name)"
                                } else {
                                    "var(--space-3)"
                                },
                            )
                            element.style.set(
                                "border-radius",
                                if (name.startsWith("--radius-")) {
                                    "var($name)"
                                } else {
                                    "var(--radius-md)"
                                },
                            )
                            element.style.set(
                                "box-shadow",
                                if (name.startsWith("--shadow-")) {
                                    "var($name)"
                                } else {
                                    "none"
                                },
                            )
                            element.style.set("background", "var(--surface-card)")
                        },
                    )
                }
            }
        }
        row {
            horizontal {
                action("Навести или сфокусировать")
            }
        }
        row {
            horizontal {
                custom(
                    Div().apply {
                        addClassNames("ts-skel", "ts-skel--text")
                    },
                )
            }
        }
    }
    block("Бренд") {
        listOf(
            TestSysBrand.HEADER,
            TestSysBrand.FOOTER,
            TestSysBrand.EMBLEM,
            TestSysBrand.WORDMARK,
            TestSysBrand.FAVICON,
        ).forEach { path ->
            row {
                horizontal {
                    custom(
                        Image(path, "Фирменный ресурс TestSys").apply {
                            element.style.set("height", "32px")
                        },
                    )
                }
            }
        }
    }
}
