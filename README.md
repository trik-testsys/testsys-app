[![Build](https://github.com/trik-testsys/testsys-app/actions/workflows/build.yml/badge.svg)](https://github.com/trik-testsys/testsys-app/actions/workflows/build.yml)
[![Test](https://github.com/trik-testsys/testsys-app/actions/workflows/test.yml/badge.svg)](https://github.com/trik-testsys/testsys-app/actions/workflows/test.yml)
[![Lint](https://github.com/trik-testsys/testsys-app/actions/workflows/lint.yml/badge.svg)](https://github.com/trik-testsys/testsys-app/actions/workflows/lint.yml)

# TestSys Application

TestSys — система проверки решений для TRIK Studio: Задачи, Туры, Соревнования, отправка Решений и Вердикты.
Этот файл — точка входа в репозиторий для всех, кто начинает работать с проектом.

С чего начать:

- устройство репозитория, модули и сборка — [structure.md](docs/project/structure.md);
- термины предметной области — [definitions.md](docs/domain/definitions.md);
- что делает система — [features.md](docs/domain/features.md);
- перечень всей документации и правила её написания — [docs.md](docs/docs.md).

## Скилы и агенты Claude Code

В репозитории настроены скилы и агенты для [Claude Code](https://claude.com/claude-code). Скил вызывается командой
в сессии Claude Code, собирает у пользователя входные данные и решения и запускает агента, если он указан.
Ни один из них не делает коммитов и не меняет состояние git. Общие инструкции для Claude — в [CLAUDE.md](CLAUDE.md).

| Скил                                                       | Агенты                                                                                           | Назначение                                                                                        |
|------------------------------------------------------------|--------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| [/review-changes](.claude/skills/review-changes/SKILL.md)  | [reviewer](.claude/agents/reviewer.md), [review-verifier](.claude/agents/review-verifier.md)     | Строгое ревью изменений без правки файлов: незакоммиченные изменения, PR, диапазон коммитов, пути |
| [/fix-review](.claude/skills/fix-review/SKILL.md)          | [fixer](.claude/agents/fixer.md)                                                                 | Исправление выбранных замечаний из отчёта ревью, затем сборка и тесты                             |
| [/implement](.claude/skills/implement/SKILL.md)            | [coder](.claude/agents/coder.md)                                                                 | Реализация задачи по гайдам из `docs/guides` с самопроверкой по чек-листам, затем сборка и тесты  |
| [/testsys-design](.claude/skills/testsys-design/SKILL.md)  | —                                                                                                | Интерфейсы Кабинетов на Vaadin/Kotlin-DSL; React/HTML — для явно запрошенных макетов        |

Как устроен каждый скил и агент, описано в файлах по ссылкам. Ограничения агентов на запись и команды
обеспечивают хуки в `.claude/hooks/`.

## Скилы и агенты Codex

Общие инструкции для Codex — в [AGENTS.md](AGENTS.md). Скилы находятся в `.agents/skills/`, определения агентов —
в `.codex/agents/`. Ограничения каждого агента указаны в его определении; локальных хуков Codex в репозитории нет.

| Скил | Агенты | Назначение |
|------|--------|------------|
| [/review-changes](.agents/skills/review-changes/SKILL.md) | [reviewer](.codex/agents/reviewer.toml), [review-verifier](.codex/agents/review-verifier.toml) | Ревью изменений без правки файлов и независимая проверка замечаний |
| [/fix-review](.agents/skills/fix-review/SKILL.md) | [fixer](.codex/agents/fixer.toml) | Исправление выбранных замечаний ревью, сборка и тесты |
| [/implement](.agents/skills/implement/SKILL.md) | [coder](.codex/agents/coder.toml) | Реализация задачи по гайдам проекта с самопроверкой, сборкой и тестами |
| [/testsys-design](.agents/skills/testsys-design/SKILL.md) | — | Интерфейсы Кабинетов на Vaadin/Kotlin-DSL; React/HTML — для явно запрошенных макетов |

Агенты не делают коммитов и не меняют состояние git. Формат работы каждого скила и агента описан по ссылкам.
