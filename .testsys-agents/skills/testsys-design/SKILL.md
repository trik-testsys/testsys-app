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
- For a page, follow [implement-web-page.md](../../../docs/guides/implement-web-page.md). Read the public composition,
  handles, bindings and component API sections it selects in
  [components/README.md](../../../testsys-web/components/README.md). Read
  [structure.md](../../../docs/project/structure.md) for code placement and build commands, and
  [code-style.md](../../../docs/project/code-style.md) for Kotlin and KDoc.
- Consult [dev-app/README.md](../../../testsys-web/dev-app/README.md) for demonstrations and usage examples.
  These examples help discover components; they are not an authority for interface rules or contracts.
- For a new business scenario, consult [features.md](../../../docs/domain/features.md) and the applicable guides
  listed in [docs.md](../../../docs/docs.md). UI work alone does not authorize business operations or access rules.

## Build interfaces

Inspect the public signatures and relevant tests of the components the page uses. Follow the page guide and its
selected API sections. If a component is missing, follow
[add-web-component.md](../../../docs/guides/add-web-component.md) and inspect the nearest actual implementation.
Read core internals, client implementations and resource details when adding or changing a component;
ordinary page composition uses the public DSL.

For application text, use [localization/README.md](../../../testsys-infra/localization/README.md); for new messages,
follow [add-localization.md](../../../docs/guides/add-localization.md). Keep demonstration exceptions within their
documented scope.

For an explicitly requested standalone mockup, reuse the shared resources described in `components/README.md`
and follow `ui-design.md`. Keep the artifact identifiable as a mockup; do not create another maintained component
library or turn sample data into application behaviour without a corresponding request.

## Verify and hand over

Follow the page or component guide, [unit-tests.md](../../../docs/project/unit-tests.md) and the build
commands in `structure.md`. Check rendered behaviour against the documented requirements and component
contracts. For standalone artifacts, check the relevant interactions in a browser.

Report what changed, what was verified and any remaining limitation. Keep commits and progression to another
roadmap item under the user's direction.
