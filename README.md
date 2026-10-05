[![Build](https://github.com/trik-testsys/testsys-app/actions/workflows/build.yml/badge.svg)](https://github.com/trik-testsys/testsys-app/actions/workflows/build.yml)
[![Test](https://github.com/trik-testsys/testsys-app/actions/workflows/test.yml/badge.svg)](https://github.com/trik-testsys/testsys-app/actions/workflows/test.yml)
[![Lint](https://github.com/trik-testsys/testsys-app/actions/workflows/lint.yml/badge.svg)](https://github.com/trik-testsys/testsys-app/actions/workflows/lint.yml)

# TestSys Application

TestSys — система для проведения онлайн соревнований по робототехнике.

`testsys-app` — основной репозиторий проекта. Здесь разрабатывается приложение,
предоставляющее пользователям веб-интерфейс и API для работы с TestSys.

С чего начать:

- устройство репозитория, модули и сборка — [structure.md](docs/project/structure.md);
- термины предметной области — [definitions.md](docs/domain/definitions.md);
- что делает система — [features.md](docs/domain/features.md);
- перечень всей документации и правила её написания — [docs.md](docs/docs.md).

Если вы используете LLM в разработке, ознакомьтесь с [инструкцией](README_LLM_USAGE.md).

## Скилы и роли

Общие инструкции — в [AGENTS.md](AGENTS.md). Подключение клиента описано в
[README_LLM_USAGE.md](README_LLM_USAGE.md).

| Скилл  |  Роли  |  Назначение |
| ------- | ------ | ------------ |
| [implement](.testsys-agents/skills/implement/SKILL.md)  |  [coder](.testsys-agents/roles/coder.md)  |  Планирование задачи по гайдам, согласование решений, реализация и самопроверка со сборкой и тестами |
| [review-changes](.testsys-agents/skills/review-changes/SKILL.md)  |  [reviewer](.testsys-agents/roles/reviewer.md), [review-verifier](.testsys-agents/roles/review-verifier.md)  |  Строгое ревью незакоммиченных изменений, PR, коммитов или путей без правок исходников |
| [fix-review](.testsys-agents/skills/fix-review/SKILL.md)  |  [fixer](.testsys-agents/roles/fixer.md)  |  Перепроверка и исправление выбранных замечаний отчёта, сборка и тесты |
| [generate-localization](.testsys-agents/skills/generate-localization/SKILL.md)  |  [localization-generator](.testsys-agents/roles/localization-generator.md), [localization-reviewer](.testsys-agents/roles/localization-reviewer.md)  |  Добавление региона или заполнение недостающих переводов с `ru-RU`; существующие переводы меняются только по явному запросу |
| [add-localization](.testsys-agents/skills/add-localization/SKILL.md)  |  Те же роли локализации  |  Создание и явное изменение исходных сообщений `ru-RU`, golden-проверки и независимое языковое ревью |
| [testsys-design](.testsys-agents/skills/testsys-design/SKILL.md) | — | Интерфейсы на Vaadin/Kotlin DSL и явно запрошенные самостоятельные макеты |
