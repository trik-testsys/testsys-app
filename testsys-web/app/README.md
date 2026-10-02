# Основное веб-приложение

Документ описывает модуль `testsys-web:app`: запуск основного приложения Vaadin Flow и границы его ответственности.
Компоненты и визуальные правила описаны в [components/README.md](../components/README.md), общая структура и сборка —
в [structure.md](../../docs/project/structure.md).

Приложение содержит точку запуска `TestSysApplication`, свою конфигурацию Spring, `AppShell`, настройку локали
и обработчик отсутствующих маршрутов. Корень пакетов — `tech.testsys.web.app`.
Зависимости модулей перечислены в structure.md; связи с dev-приложением нет. Витрины отсутствуют при любом профиле.
Предметные страницы Кабинетов, подключение операций и безопасность пока не реализованы.

Общие тексты создаются фабрикой `buildUiTexts` из components. Обработчик `NotFoundView` возвращает HTTP 404,
используя общий `NotFoundPage`. `vaadin.eagerServerLoad` обеспечивает этот статус первого ответа.

## Запуск и проверка

```bash
./gradlew :testsys-web:app:bootRun -Pdetekt.autoCorrect=false
```

По умолчанию используется порт 8080. Отсутствующие адреса, включая `/dev/showcase`, показывают страницу 404.

```bash
./gradlew :testsys-web:app:build -Pdetekt.autoCorrect=false
```

Frontend приложения находится в `src/main/frontend/`; generated и build-файлы принадлежат этому модулю.
Сборка production frontend и условия запуска описаны в structure.md.

Cookie сессии называется `TESTSYS_APP_SESSION`. Отдельное имя позволяет открывать оба приложения
на одном хосте одновременно без перезаписи сессии другого приложения.
