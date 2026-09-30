---
name: testsys-design
description: Design and build TestSys web interfaces (Кабинеты) with the project design system - tokens, React components, page composition, copy and iconography rules. Use when the user asks to design, mock up, prototype or implement a TestSys screen, page, component or visual asset, either as a throwaway HTML artifact or as production code in testsys-web.
user-invocable: true
---

# TestSys design

The design system lives in `testsys-web/design-system/`. Its `README.md` owns every rule (page composition, copy,
visual foundations, iconography, usage) and indexes the files; this skill only points to it.

## Steps

1. Read `testsys-web/design-system/README.md`. For the components you are going to use, read their `.d.ts`
   (props) and `.prompt.md` (usage example) under `components/<group>/`.
2. Use the domain terms and Roles from `docs/domain/definitions.md`. Where the design system's components or sample
   screens use other vocabulary, `definitions.md` wins (the README says so).
3. If the user gave no concrete task, ask what they want to build and for whom (which Role and Кабинет), then
   decide with them between a throwaway HTML artifact and production code.
4. Build:
   - **Throwaway artifact:** a static HTML page that links or inlines `styles.css` and the tokens it needs; reuse
     the screens in `ui_kits/platform/` as a starting point.
   - **Production code:** follow `docs/project/structure.md` for where code goes; the design system is not wired
     into the `testsys-web` build yet, so ask before choosing a frontend stack or copying files into `src/`.
