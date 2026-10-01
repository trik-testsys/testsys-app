package tech.testsys.web.components.core

/**
 * Lucide icon of the design system subset (ISC license, https://lucide.dev): 24×24 grid, 2px round stroke.
 *
 * @since %CURRENT_VERSION%
 */
enum class IconName(internal val paths: String) {
    Search("""<circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>"""),
    ChevronDown("""<path d="m6 9 6 6 6-6"/>"""),
    ChevronUp("""<path d="m18 15-6-6-6 6"/>"""),
    ChevronLeft("""<path d="m15 18-6-6 6-6"/>"""),
    ChevronRight("""<path d="m9 18 6-6-6-6"/>"""),
    Bell("""<path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/>"""),
    Plus("""<path d="M5 12h14"/><path d="M12 5v14"/>"""),
    Minus("""<path d="M5 12h14"/>"""),
    Check("""<path d="M20 6 9 17l-5-5"/>"""),
    X("""<path d="M18 6 6 18"/><path d="m6 6 12 12"/>"""),
    Upload("""<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><path d="m17 8-5-5-5 5"/><path d="M12 3v12"/>"""),
    Download("""<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><path d="m7 10 5 5 5-5"/><path d="M12 15V3"/>"""),
    Clock("""<circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>"""),
    File("""<path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"/><path d="M14 2v4a2 2 0 0 0 2 2h4"/>"""),
    Info("""<circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/>"""),
    TriangleAlert(
        """<path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3"/><path d="M12 9v4"/>""" +
            """<path d="M12 17h.01"/>""",
    ),
    CircleX("""<circle cx="12" cy="12" r="10"/><path d="m15 9-6 6"/><path d="m9 9 6 6"/>"""),
    CircleCheck("""<circle cx="12" cy="12" r="10"/><path d="m9 12 2 2 4-4"/>"""),
    Ellipsis("""<circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/><circle cx="5" cy="12" r="1"/>"""),
    ListFilter("""<path d="M3 6h18"/><path d="M7 12h10"/><path d="M10 18h4"/>"""),
    Calendar("""<rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4"/><path d="M8 2v4"/><path d="M3 10h18"/>"""),
    Flag("""<path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/><path d="M4 22v-7"/>"""),
    GripVertical(
        """<circle cx="9" cy="12" r="1"/><circle cx="9" cy="5" r="1"/><circle cx="9" cy="19" r="1"/><circle cx="15" cy="12" r="1"/>""" +
            """<circle cx="15" cy="5" r="1"/><circle cx="15" cy="19" r="1"/>""",
    ),
    HelpCircle("""<circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><path d="M12 17h.01"/>"""),
    RefreshCw(
        """<path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/><path d="M21 3v5h-5"/>""" +
            """<path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"/><path d="M8 16H3v5"/>""",
    ),
    ExternalLink("""<path d="M15 3h6v6"/><path d="M10 14 21 3"/><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/>"""),
    Trash("""<path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/>"""),
    Pencil(
        """<path d="M21.17 6.81a1 1 0 0 0-3.99-3.99L3.84 16.17a2 2 0 0 0-.5.83l-1.32 4.35a.5.5 0 0 0 .62.62l4.35-1.32a2 2 0 0 0 """ +
            """.83-.5z"/>""",
    ),
    User("""<path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>"""),
    Users(
        """<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/>""" +
            """<path d="M16 3.13a4 4 0 0 1 0 7.75"/>""",
    ),
    Trophy(
        """<path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6"/><path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18"/><path d="M4 22h16"/>""" +
            """<path d="M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22"/>""" +
            """<path d="M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22"/><path d="M18 2H6v7a6 6 0 0 0 12 0V2Z"/>""",
    ),
    Code("""<path d="m16 18 6-6-6-6"/><path d="m8 6-6 6 6 6"/>"""),
    Lock("""<rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>"""),
    Megaphone("""<path d="m3 11 18-5v12L3 14v-3z"/><path d="M11.6 16.8a3 3 0 1 1-5.8-1.6"/>"""),
    Snowflake(
        """<path d="M2 12h20"/><path d="M12 2v20"/><path d="m20 16-4-4 4-4"/><path d="m4 8 4 4-4 4"/><path d="m16 4-4 4-4-4"/>""" +
            """<path d="m8 20 4-4 4 4"/>""",
    ),
    LogOut("""<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="m16 17 5-5-5-5"/><path d="M21 12H9"/>"""),
    Settings(
        """<path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 """ +
            """0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 """ +
            """0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 """ +
            """0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 """ +
            """.73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"/>""" +
            """<circle cx="12" cy="12" r="3"/>""",
    ),
}
