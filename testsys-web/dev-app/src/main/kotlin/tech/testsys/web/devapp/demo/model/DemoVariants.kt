package tech.testsys.web.devapp.demo.model

/** Role of a demonstration user, one of the Roles of the domain. */
internal enum class DemoRole { Student, Organizer, Participant, Developer, Judge, Administrator, Observer, Supervisor }

/** Grading status of a demonstration Solution. */
internal enum class DemoSolutionStatus { Queue, Checking, Checked, Error, Timeout }

/** Format of a demonstration Solution. */
internal enum class DemoSolutionKind { VisualLanguage, Python, JavaScript }

/** Revision state of a demonstration Task. */
internal enum class DemoTaskState { New, Uncommitted, Committed }
