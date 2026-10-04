---
name: testsys-design
description: Design, prototype or implement TestSys web screens and UI components using the project documentation and component APIs. Use for Vaadin/Kotlin DSL interfaces and explicitly requested standalone mockups.
user-invocable: true
---

# TestSys design

Project documentation owns the interface rules. This skill routes requests to those documents and the maintained
component APIs; it does not define a separate UI library or visual reference.

## Choose the deliverable

Use Vaadin Flow and the Kotlin DSL in `testsys-web:components` by default. Respect an explicitly requested
technology or standalone artifact. Ask only for missing requirements that affect the screen: purpose, Role,
data, actions or states. Do not reopen a settled format choice.

## Read the relevant sources

- Read [ui-design.md](../../../docs/project/ui-design.md) for visual and interface rules, and
  [definitions.md](../../../docs/domain/definitions.md) for domain terms and Roles.
- Read [components/README.md](../../../testsys-web/components/README.md) for composition, APIs, handles,
  client implementations, resources and component verification. Read
  [structure.md](../../../docs/project/structure.md) for code placement and build commands, and
  [code-style.md](../../../docs/project/code-style.md) for Kotlin and KDoc.
- Consult [dev-app/README.md](../../../testsys-web/dev-app/README.md) for demonstrations and usage examples.
  These examples help discover components; they are not an authority for interface rules or contracts.
- For a new business scenario, consult [features.md](../../../docs/domain/features.md) and the applicable guides
  listed in [docs.md](../../../docs/docs.md). UI work alone does not authorize business operations or access rules.

## Build interfaces

Inspect existing component signatures and relevant tests before composing a page. Follow the composition,
binding, editing, routing and live-update sections of `components/README.md` as needed. When a component is
missing, follow its "Как добавить компонент" section and use the nearest actual implementation as context.

For application text, use [localization/README.md](../../../testsys-infra/localization/README.md); for new messages,
follow [add-localization.md](../../../docs/guides/add-localization.md). Keep demonstration exceptions within their
documented scope.

For an explicitly requested standalone mockup, reuse the shared resources described in `components/README.md`
and follow `ui-design.md`. Keep the artifact identifiable as a mockup; do not create another maintained component
library or turn sample data into application behaviour without a corresponding request.

## Verify and hand over

Follow [unit-tests.md](../../../docs/project/unit-tests.md), the component verification guidance and the build
commands in `structure.md`. Check rendered behaviour against the documented requirements and component
contracts. For standalone artifacts, check the relevant interactions in a browser.

Report what changed, what was verified and any remaining limitation. Keep commits and progression to another
roadmap item under the user's direction.
