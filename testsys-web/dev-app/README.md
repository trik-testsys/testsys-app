# Dev-приложение веб-компонентов

Документ описывает самостоятельный модуль `testsys-web:dev-app`: витрину компонентов и демонстрационные сценарии.
Рабочие страницы Кабинетов сюда не входят. API компонентов описан в [components/README.md](../components/README.md),
общая структура и сборка — в [structure.md](../../docs/project/structure.md).

Точка запуска — `DevApplication`, корень пакетов — `tech.testsys.web.devapp`. У приложения свои `AppShell`,
Spring-конфигурация, локаль, frontend и обработчик HTTP 404. Оно использует библиотеку components и локализацию,
а не исходники или артефакт основного приложения. Большая фабрика текстов и визуальная реализация 404 общие.

## Запуск и проверка

```bash
./gradlew :testsys-web:dev-app:bootRun -Pdetekt.autoCorrect=false --args='--spring.profiles.active=dev'
```

По умолчанию используется порт 8081. Главная витрина — `/dev/showcase`; тематические страницы — `states`,
`forms`, `overlays`, `display` и `header` под этим адресом. Профиль `dev` открывает витрину; без него ее адреса
возвращают 404. Ошибки загрузки и фиктивные данные в демонстрациях не являются рабочими сценариями Системы.

```bash
./gradlew :testsys-web:dev-app:build -Pdetekt.autoCorrect=false
```

Frontend находится в `src/main/frontend/`; generated и build-файлы принадлежат этому модулю.
Каталог React-компонентов и макеты сохранены в [design-system](../components/design-system/README.md)
и могут просматриваться отдельно, как описано в этом документе.

Cookie сессии называется `TESTSYS_DEV_SESSION`. Отдельное имя позволяет открывать оба приложения
на одном хосте одновременно без перезаписи сессии другого приложения.
