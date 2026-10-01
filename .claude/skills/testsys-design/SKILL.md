---
name: testsys-design
description: Use when designing, prototyping or implementing a TestSys cabinet screen, page, UI component or visual asset with the project design system, including Vaadin/Kotlin-DSL interfaces and React/HTML mockups.
user-invocable: true
---

# TestSys design

Build TestSys interfaces with the existing design system. Project documentation owns the rules; this skill routes
requests to the relevant documents and maintained examples rather than redefining them. Paths below are relative
to this skill directory; source paths mentioned in the documents are relative to the repository root.

## Choose the deliverable

Use Vaadin Flow and the Kotlin-DSL in `testsys-web:components` by default, including requests that do not name a technology.
Use React/HTML when the user explicitly requests a mockup, prototype or standalone HTML artifact. Preserve an
explicit technology choice. Ask only for missing requirements that affect the screen: purpose, Role, data,
actions or states. Do not reopen a settled choice of format.

## Read the relevant sources

- For every interface, read [design-system/README.md](../../../testsys-web/components/design-system/README.md) for visual
  rules and [definitions.md](../../../docs/domain/definitions.md) for domain terms and Roles.
- For Kotlin code, read [components/README.md](../../../testsys-web/components/README.md) for page composition, scopes, component
  APIs, handles and examples; [structure.md](../../../docs/project/structure.md) for placement and build commands;
  and [code-style.md](../../../docs/project/code-style.md) for Kotlin and KDoc conventions.
- For a new or changed business scenario, consult [features.md](../../../docs/domain/features.md) and the
  applicable implementation guides listed in [docs.md](../../../docs/docs.md). Interface work alone does not
  authorize adding business operations or access rules.

## Build Kotlin-DSL interfaces

Use existing DSL components and inspect their source signatures before composing the page. Start with the page
examples linked from `components/README.md` and the relevant thematic views under `testsys-web/dev-app/src/main/kotlin/`.
Treat those views as UI examples; their demonstration data and profile restrictions are not production behavior.
Use the README sections for page headers, parameterized routes, grids, editing, binding and live updates as needed.

If the requested component is missing, follow `components/README.md`, section "Как добавить компонент", and inspect its
React reference under `testsys-web/components/design-system/components/<group>/` (`.jsx`, `.d.ts`, `.prompt.md`). Follow the
same section for shared CSS versus Vaadin-specific overrides. If no React reference exists, use the nearest
existing component as a starting point and resolve missing visual or interaction requirements with the user.
Do not introduce another frontend stack merely
to implement a page with components already available in the DSL.

For application text, use [localization/README.md](../../../testsys-infra/localization/README.md); for new messages,
follow [add-localization.md](../../../docs/guides/add-localization.md). Keep demo-only exceptions within their documented scope.

## Build explicitly requested mockups

Read the selected React components' `.d.ts` and `.prompt.md`; reuse the reference screens under
`testsys-web/components/design-system/ui_kits/platform/` and the full catalogue at `testsys-web/components/design-system/components/index.html`.
Use the canonical styles and tokens. Keep the artifact identifiable as a prototype and do not turn its sample
data or navigation into application behavior without a corresponding request.

## Verify and hand over

For Kotlin changes, follow [unit-tests.md](../../../docs/project/unit-tests.md) and the verification guidance in
`components/README.md`; build the affected modules using `structure.md`. Check the rendered interface and relevant
interactions against the reference. For mockups, check the artifact in a browser and run relevant existing checks.

Report what changed, what was verified and any remaining limitation. Keep commits and progression to another
roadmap item under the user's direction.
