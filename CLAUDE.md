# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

TestSys — a Kotlin/JVM system for grading TRIK Studio solutions (tasks, contests, competitions, submissions, verdicts).
Gradle multi-module build, Kotlin 2.2, JDK 21, Spring Boot 3.5 / Hibernate, PostgreSQL + Liquibase, KSP, Detekt.

Domain vocabulary (Задача, Тур, Соревнование, Полигон, Вердикт, roles, …) is defined in `docs/definitions.md`;
`docs/features.md` is the feature registry with `testsys.*` codifiers (use them in commit messages and KDoc).

## Language conventions

- KDoc, in-code comments and messages: **English**.
- Markdown docs (`README.md`, `docs/*.md`): **Russian**. Keep identifiers, code snippets and gradle commands untouched.
- Every public declaration carries KDoc with `@since %CURRENT_VERSION%` — keep the literal placeholder, do not substitute a version.

## Commands

```bash
./gradlew build                       # compile + test + detektMain for all modules
./gradlew detektMain                  # lint (autoCorrect=true: it rewrites files, re-check git diff afterwards)
```

Build facts that bite:
- `allWarningsAsErrors = true` — any Kotlin warning fails compilation.
- `check` depends on `detektMain` (the plain `detekt` task is disabled); CI runs `./gradlew build -x detekt` and `./gradlew detekt` separately.
- Detekt config is `detekt.yml` at root with `buildUponDefaultConfig = false`; max line length 140. Generated sources under `build/generated/` are excluded.
- Dependency versions live only in `gradle/libs.versions.toml`; shared config is the `testsys.conventions` plugin in `buildSrc`.
- PRs into `dev` are accepted only from branches prefixed `sh1sh4k1n9/`, `ch3zych3z/`, `KarasssDev/`.

## Modules

| Module                   | Role                                                                                                |
|--------------------------|-----------------------------------------------------------------------------------------------------|
| `testsys-domain`         | Pure domain: models, ports (contracts), builder DSL. No Spring, no JPA, no any extrenal dependency. |
| `testsys-infra/database` | JPA/Hibernate implementation of the domain persistence ports.                                       |
| `testsys-infra/grpc`     | gRPC module                                                                                         |
| `testsys-infra/service`  | Implementation of services, used in operations                                                      |
| `testsys-operation`      | Implementation of all operations defined in documentation                                           |                                                                             
| `testsys-web`            | Web application implemetation                                                                       |                                                                             

## Architecture

Hexagonal: the domain module defines ports under `tech.testsys.domain.contract`, infra modules implement them.

### Domain model (`tech.testsys.domain.model`)

- Every entity extends `DomainEntity<Id>`; ids are `@JvmInline value class`es implementing `DomainId` (`TaskId`, `ContestId`, …). Equality is class + id only.
  Exception: `SingleRoleUserId` is shared by `Participant`, `Observer`, `Supervisor`.
- Each entity is split into an immutable `X(id, createdAt, data)` and an `XData` payload. Updates produce a new instance via `X.withData { … }`.
- Entities never hold references to other entities. Relations are `LazyEntity<Id, E>` / `LazyEntityList<Id, E>` (ids only) resolved through `EntityLoader.load(...)`.
  Create them with `id.lazify()` / `ids.lazify()`.
- Variant state is modelled with sealed hierarchies, e.g. `TaskContent` (`New` / `Uncommited` / `Committed`, wip vs. last committed content),
  `SubmissionStatus`, `SubmissionKind`, `GradingResult`, `TrikSupportedLanguage`.
- Users: `MultipleRoleUser` owns a set of `CompatibleUserRole`s (Developer, Student, Administrator, Judge, Manager);
  `SingleRoleUser` is one of Participant / Observer / Supervisor.
- Ports: `EntityRepository<Data, Id, Entity>` = `EntityFinder + EntityLoader + EntitySaver + EntityRemover`; per-entity interfaces in
  `contract/persistence/repository/*Repositories.kt`. Also `FileBlobStorage`, `Grader`, `Pagination`/`Page`, `DomainException`.

### Builder DSL (`tech.testsys.domain.builder`)

`builder/api/{Task,Group,User}Api.kt` expose the entry points: `task { … }`, `taskData { … }`, `submission { … }`, `X.withData { … }`, plus
`Chooser`s for sealed types (`LanguageChooser`, `SubmissionStatusChooser`, …). Builders validate required fields and throw
`IllegalArgumentException` on `build()`. Infra mappings construct domain objects only through this DSL.

Tests for builders extend `DomainEntityBuilderTests<Entity, Data, DataBuilder>` and only override `buildDataWithAllFields()`.

### Database module (`tech.testsys.infra.database`)

Layers, each with `group/`, `task/`, `user/` sub-packages mirroring the domain:

- `internal/jpa/entity` — JPA entities. `SequenceJpaEntity` (sequence-generated `Long? id`) for real entities,
  `CompositeJpaEntity<T : CompositeId>` with an `@Embeddable data class` key for join tables. Both inherit `createdAt`/`updatedAt`/`@Version`
  from `JpaEntity`. Entities store foreign keys as plain `Long` columns (`ownerId`, `wipContentId`) — no JPA associations.
- `internal/jpa/repository` — Spring Data interfaces extending `SequenceJpaEntityRepository` / `CompositeJpaEntityRepository`
  (both include `JpaSpecificationExecutor`).
- `internal/mapping` — `object XMapping : EntityMapping<Domain, Jpa>` with `toDomain(...)` and `toJpaEntity(...)` overloads
  (one for new `Data`, one for updating an existing row that copies `createdAt` and `version` from the current row).
  `XMappingTest : EntityMappingTest<XMapping>` enforces the method/return-type contract via reflection.
- `api/persistence/adapter` — `@Component XPersistenceAdapter : AbstractPersistenceAdapter<Data, Id, Entity, JpaEntity>, XRepository`.
  The base class implements find/load/remove and the list overloads; subclasses implement `save(data)`, `update(entity)`, `assemble(jpaEntity)`
  and, for tables shared by several domain kinds, `supports(jpaEntity)`. If `save`/`update` need non-default `@Transactional`
  settings, override every overload (Spring AOP self-invocation caveat).
- Join tables are reconciled with `syncJoinTable(...)` from `internal/utils/PersistenceUtils.kt`; helpers `requireId()`,
  `findByIdOrError()`, `populateFields(jpaEntity)` live in `internal/utils`.
- `api/persistence/FileDataStorage` — files are append-only: metadata row (`FileDataJpaEntity`) + blob in `FileBlobStorage`;
  versions of one logical file share a `versionBucket` UUID; `storeIfChanged` dedups by filename + content hash.
- Schema: Liquibase changelogs in `src/main/resources/db/changelog/changes/x.y.z/` (master lists them in order);
  Hibernate runs with `ddl-auto=validate`, so every entity change needs a matching changeset. Table/column names come from
  `TestsysPhysicalNamingStrategy`. `SchemaValidationTest` (H2 in PostgreSQL mode) is currently commented out.
