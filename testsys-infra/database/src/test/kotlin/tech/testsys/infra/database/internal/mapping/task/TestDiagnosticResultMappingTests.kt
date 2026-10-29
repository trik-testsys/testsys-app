package tech.testsys.infra.database.internal.mapping.task

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import tech.testsys.domain.builder.api.*
import tech.testsys.domain.model.task.DiagnosticReport
import tech.testsys.domain.model.task.DiagnosticSeverity
import tech.testsys.infra.database.internal.InternalDatabaseApi

@OptIn(InternalDatabaseApi::class)
class TestDiagnosticResultMappingTests {
    @ParameterizedTest
    @MethodSource("reports")
    fun `should round trip every typed reason with Unicode and indexed attribute locations`(report: DiagnosticReport) {
        val row = TestDiagnosticResultMapping.toJpaEntity(requestId = 1, testId = 2, position = 0, report = report)

        val restored = TestDiagnosticResultMapping.toDomain(row)

        assertEquals(report, restored)
    }

    companion object {
        @JvmStatic
        fun reports(): List<DiagnosticReport> = listOf(
            diagnosticReport {
                severity = DiagnosticSeverity.Info
                data.unknownElement {
                    tag = "Текст./| @🙂"
                }
                location = diagnosticLocation {
                    attribute = "id"
                    path += diagnosticPathSegment {
                        tag = "root"
                        index = 1
                    }
                    path += diagnosticPathSegment {
                        tag = "event"
                        index = 2
                    }
                }
            },
            diagnosticReport {
                severity = DiagnosticSeverity.Warning
                data.malformedXml {
                    details = "Текст./| @🙂"
                }
                location = diagnosticLocation {
                    attribute = "id"
                    path += diagnosticPathSegment {
                        tag = "root"
                        index = 1
                    }
                    path += diagnosticPathSegment {
                        tag = "event"
                        index = 2
                    }
                }
            },
            diagnosticReport {
                severity = DiagnosticSeverity.Error
                data.missingChild {
                    tag = "Текст./| @🙂"
                }
                location = diagnosticLocation {
                    attribute = "id"
                    path += diagnosticPathSegment {
                        tag = "root"
                        index = 1
                    }
                    path += diagnosticPathSegment {
                        tag = "event"
                        index = 2
                    }
                }
            },
            diagnosticReport {
                severity = DiagnosticSeverity.Info
                data.missingAttribute {
                    attribute = "Текст./| @🙂"
                }
                location = diagnosticLocation {
                    attribute = "id"
                    path += diagnosticPathSegment {
                        tag = "root"
                        index = 1
                    }
                    path += diagnosticPathSegment {
                        tag = "event"
                        index = 2
                    }
                }
            },
            diagnosticReport {
                severity = DiagnosticSeverity.Warning
                data.invalidAttributeValue {
                    attribute = "Текст./| @🙂"
                    expected = "Текст./| @🙂"
                    actual = "Текст./| @🙂"
                }
                location = diagnosticLocation {
                    attribute = "id"
                    path += diagnosticPathSegment {
                        tag = "root"
                        index = 1
                    }
                    path += diagnosticPathSegment {
                        tag = "event"
                        index = 2
                    }
                }
            },
            diagnosticReport {
                severity = DiagnosticSeverity.Error
                data.invalidChildCount {
                    expected = 42
                    actual = 42
                }
                location = diagnosticLocation {
                    attribute = "id"
                    path += diagnosticPathSegment {
                        tag = "root"
                        index = 1
                    }
                    path += diagnosticPathSegment {
                        tag = "event"
                        index = 2
                    }
                }
            },
            diagnosticReport {
                severity = DiagnosticSeverity.Info
                data.missingTimeLimit()
                location = diagnosticLocation {
                    attribute = "id"
                    path += diagnosticPathSegment {
                        tag = "root"
                        index = 1
                    }
                    path += diagnosticPathSegment {
                        tag = "event"
                        index = 2
                    }
                }
            },
            diagnosticReport {
                severity = DiagnosticSeverity.Warning
                data.multipleTimeLimits {
                    count = 42
                }
                location = diagnosticLocation {
                    attribute = "id"
                    path += diagnosticPathSegment {
                        tag = "root"
                        index = 1
                    }
                    path += diagnosticPathSegment {
                        tag = "event"
                        index = 2
                    }
                }
            },
            diagnosticReport {
                severity = DiagnosticSeverity.Error
                data.negativeTimeLimit {
                    value = 42
                }
                location = diagnosticLocation {
                    attribute = "id"
                    path += diagnosticPathSegment {
                        tag = "root"
                        index = 1
                    }
                    path += diagnosticPathSegment {
                        tag = "event"
                        index = 2
                    }
                }
            },
            diagnosticReport {
                severity = DiagnosticSeverity.Info
                data.excessiveTimeLimit {
                    value = 42
                    maximum = 42
                }
                location = diagnosticLocation {
                    attribute = "id"
                    path += diagnosticPathSegment {
                        tag = "root"
                        index = 1
                    }
                    path += diagnosticPathSegment {
                        tag = "event"
                        index = 2
                    }
                }
            },
            diagnosticReport {
                severity = DiagnosticSeverity.Warning
                data.invalidEventId {
                    id = "Текст./| @🙂"
                }
                location = diagnosticLocation {
                    attribute = "id"
                    path += diagnosticPathSegment {
                        tag = "root"
                        index = 1
                    }
                    path += diagnosticPathSegment {
                        tag = "event"
                        index = 2
                    }
                }
            },
            diagnosticReport {
                severity = DiagnosticSeverity.Error
                data.missingScoreOutput()
                location = diagnosticLocation {
                    attribute = "id"
                    path += diagnosticPathSegment {
                        tag = "root"
                        index = 1
                    }
                    path += diagnosticPathSegment {
                        tag = "event"
                        index = 2
                    }
                }
            },
            diagnosticReport {
                severity = DiagnosticSeverity.Warning
                data.missingScoreOutput()
            },
        )
    }
}
