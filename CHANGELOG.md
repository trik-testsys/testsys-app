# История изменений

Реестр изменений проекта по версиям. Он предназначен для разработчиков TestSys и для тех, кто разворачивает
Систему: по нему видно, что вошло в версию и какой номер получит следующая. История изменений для Пользователей
ведётся отдельно, см. раздел «Страницы» в [app/README.md](testsys-web/app/README.md#страницы).

## Формат записи

Версии идут от новой к старой. Изменения, которые ещё не вошли в релиз, записываются под заголовком
`## %CURRENT_VERSION%`. При релизе `release.yml` заменяет этот заголовок на номер версии и дату релиза
(см. раздел «CI» в [structure.md](docs/project/structure.md)). Первый PR после релиза добавляет заголовок заново
над предыдущей версией.

Каждый PR в `dev` добавляет в конец подходящего раздела текущей версии хотя бы одну запись:
`- Что изменилось (%PR_LINK%).` Плейсхолдер `%PR_LINK%` пишется буквально: когда PR открыт, `link-changelog.yml`
добавляет в его ветку коммит, который заменяет плейсхолдер ссылкой на PR. Перед следующим пушем заберите этот
коммит из ветки. Запись описывает результат для читателя и не повторяет заголовок коммита. Раздел выбирается
по правилам
[семантического версионирования](https://semver.org/lang/ru/):

| Раздел     | Что в него попадает                                                                                   |
|------------|-------------------------------------------------------------------------------------------------------|
| `### Major` | Обновление требует ручных действий или ломает совместимость: изменён или удалён параметр приложения, изменён контракт gRPC с Проверяющим узлом, удалена фича |
| `### Minor` | Новая функциональность, которая не ломает совместимость                                              |
| `### Patch` | Исправления ошибок, рефакторинг, изменения сборки, CI и документации                                 |

Запись об изменении параметра приложения называет его свойство и переменную окружения и говорит, что сделать
при обновлении. Перечень параметров — в разделе «Параметры приложения»
в [deployment.md](docs/project/deployment.md#параметры-приложения).

Разделы идут в порядке Major, Minor, Patch. Раздел добавляется вместе с первой записью, пустые разделы
не заводятся. Номер следующей версии определяет старший раздел, в котором есть записи.

**Записи выпущенных версий не меняются**, кроме исправления опечаток и ссылок.

## %CURRENT_VERSION%

### Major

- Супервайзер стал Нефиксированной Ролью, которую выдают в служебном интерфейсе. Удалены свойство
  `testsys.operation.supervisor.name` и переменная `TESTSYS_SUPERVISOR_ACCESS_TOKEN` (свойство
  `testsys.operation.supervisor.access-token`): уберите их из файла параметров и файла переменных. Миграция
  удаляет прежнего Супервайзера вместе с его входами, после обновления выдайте Роль Супервайзера нужному
  Пользователю ([#36](https://github.com/trik-testsys/testsys-app/pull/36)).
- Добавлена обязательная переменная `TESTSYS_APP_URL` (свойство `testsys.service.app-url`) — адрес основного
  интерфейса в браузере. Задайте её в файле переменных, без неё приложение не запустится
  ([#39](https://github.com/trik-testsys/testsys-app/pull/39)).

### Minor

- Добавлена страница истории изменений для Пользователей
  ([#37](https://github.com/trik-testsys/testsys-app/pull/37)).
- На странице входа показываются объявления ([#38](https://github.com/trik-testsys/testsys-app/pull/38)).
- Обновлён служебный интерфейс ([#39](https://github.com/trik-testsys/testsys-app/pull/39)).

### Patch

- Релиз отправляет `master`, `dev` и тег одним атомарным пушем
  ([#34](https://github.com/trik-testsys/testsys-app/pull/34)).
- Переработаны компоненты веб-интерфейса ([#35](https://github.com/trik-testsys/testsys-app/pull/35)).
- Добавлена история изменений проекта и проверка её пополнения в PR (%PR_LINK%).

## 1.0.0-beta.1 — 2026-10-29

Первая бета-версия.

### Minor

- Доменная модель, билдеры и их DSL
  ([#4](https://github.com/trik-testsys/testsys-app/pull/4), [#5](https://github.com/trik-testsys/testsys-app/pull/5),
  [#10](https://github.com/trik-testsys/testsys-app/pull/10)).
- Хранение доменных сущностей в PostgreSQL, учёт загруженных Ресурсов Задач, ускорение запросов
  ([#6](https://github.com/trik-testsys/testsys-app/pull/6),
  [#12](https://github.com/trik-testsys/testsys-app/pull/12),
  [#26](https://github.com/trik-testsys/testsys-app/pull/26)).
- Локализация сообщений в формате MessageFormat 2
  ([#7](https://github.com/trik-testsys/testsys-app/pull/7),
  [#11](https://github.com/trik-testsys/testsys-app/pull/11)).
- Хеширование Кодов-доступа ([#14](https://github.com/trik-testsys/testsys-app/pull/14)).
- Операции Разработчика, в том числе диагностика Задач
  ([#13](https://github.com/trik-testsys/testsys-app/pull/13),
  [#15](https://github.com/trik-testsys/testsys-app/pull/15),
  [#19](https://github.com/trik-testsys/testsys-app/pull/19)).
- gRPC-грейдер для проверки Решений на Проверяющих узлах ([#16](https://github.com/trik-testsys/testsys-app/pull/16)).
- Операции Судьи, Участника и Ученика, Организатора, Администратора, Наблюдателя и Супервайзера, Коды-приглашения
  ([#17](https://github.com/trik-testsys/testsys-app/pull/17),
  [#20](https://github.com/trik-testsys/testsys-app/pull/20),
  [#21](https://github.com/trik-testsys/testsys-app/pull/21),
  [#22](https://github.com/trik-testsys/testsys-app/pull/22),
  [#23](https://github.com/trik-testsys/testsys-app/pull/23),
  [#24](https://github.com/trik-testsys/testsys-app/pull/24),
  [#30](https://github.com/trik-testsys/testsys-app/pull/30)).
- Аудит операций и новая структура тестов ([#25](https://github.com/trik-testsys/testsys-app/pull/25)).
- Веб-интерфейс: дизайн-система компонентов и страницы Кабинетов
  ([#9](https://github.com/trik-testsys/testsys-app/pull/9),
  [#27](https://github.com/trik-testsys/testsys-app/pull/27),
  [#28](https://github.com/trik-testsys/testsys-app/pull/28)).
- Служебный интерфейс ([#29](https://github.com/trik-testsys/testsys-app/pull/29)).
- Развёртывание через Docker Compose и workflow релиза ([#30](https://github.com/trik-testsys/testsys-app/pull/30)).
- Журналирование ([#31](https://github.com/trik-testsys/testsys-app/pull/31)).
- Новая реализация входа в Систему ([#32](https://github.com/trik-testsys/testsys-app/pull/32)).

### Patch

- Параметр `createdAt` передаётся всем наследникам доменных сущностей
  ([#1](https://github.com/trik-testsys/testsys-app/pull/1)).
- Добавлена проверка исходной ветки PR в `dev` ([#2](https://github.com/trik-testsys/testsys-app/pull/2)).
- Исправлено версионирование сущностей ([#8](https://github.com/trik-testsys/testsys-app/pull/8)).
- Образ релиза публикуется в GitHub Packages вместо Docker Hub
  ([#33](https://github.com/trik-testsys/testsys-app/pull/33)).
