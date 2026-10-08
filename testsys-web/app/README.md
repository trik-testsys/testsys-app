# Основное веб-приложение

Документ описывает модуль `testsys-web:app`: запуск основного приложения Vaadin Flow, безопасность, страницы,
подключение операций и границы его ответственности. Компоненты и визуальные правила описаны
в [components/README.md](../components/README.md), общая структура и сборка —
в [structure.md](../../docs/project/structure.md), операции — в [testsys-operation/README.md](../../testsys-operation/README.md).

Приложение содержит точку запуска `TestSysApplication`, свою конфигурацию Spring, `AppShell`, настройку локали,
безопасность, страницы, обработчики отсутствующих маршрутов, отказа в доступе и ошибок операций и прокси-сервисы
операций.
Корень пакетов — `tech.testsys.web.app`. Зависимости модулей перечислены в structure.md; связи с dev-приложением нет.

| Пакет | Что лежит |
|-------|-----------|
| `config` | Spring-конфигурация: тексты, локаль, `InfraConfiguration`, `OperationsConfiguration`, `AfterTransactionExecutor`, `AfterCommitGrader` |
| `security` | `SecurityConfiguration`, `CabinetPrincipal` и программный вход `CabinetSignIn`, см. раздел [Безопасность](#безопасность) |
| `service` | `CurrentUser`, общие VO и `Page.map` прокси-сервисов |
| `service/<actor>` | Прокси-сервис `<Actor>Service` и его VO, см. раздел [Прокси-сервисы](#прокси-сервисы) |
| `view` | Страницы и шапки `CabinetHeaders`, см. раздел [Страницы](#страницы) |
| `error` | Обработчики отсутствующих маршрутов, отказа в доступе и ошибок операций |

`InfraConfiguration` подключает конфигурации инфраструктурных модулей `database`, `grpc`, `diagnostics` и `mail`
через `@Import`.

Общие тексты создаются фабрикой `buildUiTexts()` из components; контракт — в
[components/README.md](../components/README.md#тексты). Обработчик `NotFoundView` использует общий `NotFoundPage`;
контракт маршрутизации — в разделе [Отсутствующие маршруты](#отсутствующие-маршруты).

## Отсутствующие маршруты

Отсутствующие маршруты обрабатывает `NotFoundView` из пакета `error` через штатный
`HasErrorParameter<NotFoundException>` с кодом 404 во всех профилях. Визуальная часть общая — `NotFoundPage`
(см. [components/README.md](../components/README.md#приложения-и-общие-страницы)): бренд, русские заголовок,
пояснение и действие «Вернуться» по истории браузера; до определения истории и без предыдущей записи кнопка
недоступна. Ссылки на выдуманную главную, подробностей исключения и списка dev-маршрутов нет.
Витрины компонентов в приложении нет даже с профилем `dev`.

`vaadin.eagerServerLoad=true` в `application.yml` включает начальную серверную маршрутизацию до отправки HTML:
код 404 получает и первый HTTP-запрос, а не только последующая навигация. Это создаёт сессию и UI уже при первом
запросе страницы; без этой настройки Vaadin отдаёт HTML с кодом 200 и разрешает маршрут позднее.

## Безопасность

`SecurityConfiguration` подключает Spring Security через `VaadinSecurityConfigurer` и разрешает любой HTTP-запрос.
Доступ к странице проверяет `NavigationAccessControl` Vaadin при навигации, в том числе при первом HTTP-запросе
благодаря `vaadin.eagerServerLoad`. Он читает аннотации страницы: `@AnonymousAllowed` открывает страницу без входа,
`@RolesAllowed` — Пользователю указанного вида. Страница без аннотации закрыта для всех, поэтому экраны ошибок
тоже помечены `@AnonymousAllowed`. Гостя с закрытой страницы `NavigationAccessControl` отправляет на страницу
входа `AuthenticationView` и запоминает в сессии путь открытой страницы — адрес возврата. Вошедший Пользователь
другого вида получает экран `forbidden` с кодом 403 от `AccessDeniedView`.

`SecurityConfiguration` объявляет `TaskDecorator`, и Spring Boot применяет его к `applicationTaskExecutor`. Поэтому
работа страницы на этом исполнителе (`load`, `download*`) выполняется от имени поставившего её Пользователя,
и `CurrentUser` доступен в фоне.

Формы входа Spring Security нет: страницы входа и регистрации вызывают `CabinetSignIn.signIn`. Он меняет
идентификатор сессии (`changeSessionId`) против фиксации сессии и сохраняет в сессии `CabinetPrincipal` —
идентификатор и вид Пользователя без Кода-доступа и данных. Вид даёт единственное полномочие `ROLE_<вид>`:
`MULTIPLE_ROLE`, `PARTICIPANT`, `OBSERVER` или `SUPERVISOR`. `signIn` возвращает адрес возврата и удаляет его
из сессии. Выход — `AuthenticationContext.logout()` из меню Псевдонима: сессия закрывается, браузер переходит
на `/`.

## Страницы

| Страница | Маршрут | Доступ |
|----------|---------|--------|
| `MainView` | `""` | Без входа; вошедшего Пользователя перенаправляет на его стартовую страницу |
| `AuthenticationView` | `login` | Без входа |
| `RegistrationView` | `registration` | Без входа |
| `RestoreAccessView` | `restore-access` | Без входа |
| `MultiMainView` | `home` | `MULTIPLE_ROLE` |
| `ProfileView`, `DeveloperView`, `ManagerView`, `JudgeView`, `StudentView` | `profile`, `developer`, `manager`, `judge`, `student` | `MULTIPLE_ROLE` |
| `AdminView` | `admin`, `admin/communities`, `admin/users` | `MULTIPLE_ROLE` с Ролью Администратора |
| `AdminCommunityView` | `admin/communities/:communityId` | `MULTIPLE_ROLE`, создатель Сообщества |
| `AdminUserView` | `admin/users/:userId`, `admin/observers/:observerId` | `MULTIPLE_ROLE`, Администратор с доступом к Пользователю |
| `ParticipantView`, `ObserverView`, `SupervisorView` | `participant`, `observer`, `supervisor` | Соответствующий вид |

Требования к страницам — в разделе `testsys.web` в [features.md](../../docs/domain/features.md). Страницы Ролей,
кроме Кабинета Администратора, и профиль пока показывают только шапку и пустое состояние: их маршруты и доступ
окончательные, поэтому шапка уже ведёт на них. Стартовую страницу вида Пользователя возвращает `startPageOf`.

Страницы Кабинета Администратора вызывают прокси-сервис в `beforeEnter`, до построения таблиц. Поэтому отсутствие
Роли, Сообщества или доступа открывает экран ошибки из раздела [Ошибки операций](#ошибки-операций), а не ошибку
загрузки таблицы. `AdminView` без раздела переадресует на `admin/communities`. Наблюдатель открывается по своему
адресу `admin/observers/:observerId`: операция просмотра принимает идентификатор вместе с видом Пользователя.

Страницы Ролей принимают необязательный параметр маршрута `section` с разделом страницы, например
`developer/tasks`. Заголовок раздела Роли в «Меню» ведёт на страницу без параметра, а ссылки разделов — с ним,
поэтому шапка выделяет ровно одну ссылку по точному адресу. Допустимые значения параметра перечислены в маршруте
каждой страницы.

`AuthenticationView`, `RegistrationView` и `RestoreAccessView` — вкладки одного узкого блока, который строит
`guestForm`: выбор вкладки открывает её страницу, поэтому адрес возврата в сессии сохраняется.

`CabinetHeaders` строит шапку гостя и шапку Кабинета. Шапку Кабинета он собирает из Пользователя, которого
загружает `CurrentUser` при каждом построении страницы.

После регистрации `RegistrationView` выполняет вход по выданному Коду-доступа и показывает его на `MultiMainView`
вызовом `showAccessToken` у открытой страницы. Код-доступа не попадает ни в адрес, ни в сессию, поэтому повторное
открытие страницы его не показывает. Ошибки входа и регистрации страницы показывают тостом с причиной сами,
без `OperationErrorHandler`.

## Прокси-сервисы

Страницы вызывают операции только через прокси-сервисы `<Actor>Service` в пакете `service/<actor>`:
`AdministratorService`, `DeveloperService`, `JudgeService`, `ParticipantService`, `StudentService`, `StudyService`
и `UserService`. Методы сервиса соответствуют операциям один к одному и принимают те же входные данные без
Пользователя. Сервиса для `TaskValidationOperations` нет: её вызывает только диспетчер. У `ManagerOperations`,
`MultipleRoleUserOperations` и `ObserverOperations` сервисов пока нет: страницы их не вызывают.
`AdministratorService` не выставляет служебную `refreshCommunityInvite`.

Классы операций, у которых есть сервис, `TaskValidationOperations` и `TaskValidationDispatcher` создаются
`@Bean`-методами в `OperationsConfiguration`; модуль операций не сканируется.

- Сервис помечен `@Service` и `@Transactional`: каждый вызов выполняется в одной транзакции. Методы `view*`
  и `download*` выполняются с `readOnly = true`. Страницы вызывают операции только через сервисы; правило — в разделе
  «Транзакции» в [testsys-operation/README.md](../../testsys-operation/README.md#транзакции).
- Если транзакция сервиса столкнулась с параллельной транзакцией, advisor модуля `database` выполняет вызов сервиса
  заново в новой транзакции, см. раздел
  [«Транзакции и согласованность»](../../testsys-infra/database/README.md#транзакции-и-согласованность)
  в database/README.md.
- Пользователя, который выполняет операцию, сервис берёт из `CurrentUser`. Реализация `SecurityCurrentUser`
  при каждом вызове загружает Пользователя по `CabinetPrincipal` вошедшего Пользователя. Без входа, без
  сохранённого Пользователя и при другом виде Пользователя она бросает `IllegalStateException`.
  `UserService` выполняет операции до входа и `CurrentUser` не использует.
- Сервис возвращает VO. При ошибке операции `getOrThrow()` бросает `OperationException`, и `@Transactional`
  откатывает транзакцию, поэтому неудачная операция ничего не сохраняет. Обработку исключения описывает раздел
  [Ошибки операций](#ошибки-операций).
- Исключения из этих правил есть у `UserService`. `authenticate` возвращает `CabinetPrincipal` вместо VO, чтобы
  данные Пользователя не попадали в сессию. `confirmRegistration` возвращает исходный Код-доступа и выполняется
  с `noRollbackFor = OperationException`: потраченная попытка ввода кода сохраняется и при ошибке, а другие
  отказы этой операции ничего не меняют.
- VO не содержат сущностей и ленивых ссылок: связи заменены идентификаторами и списками идентификаторов,
  доменные value-типы сохраняются. VO содержит все поля данных сущности, её `id` и `createdAt`. Файлы Ресурсов
  представлены именем без содержимого.

`StudyService.sendSolution` передаёт сохранённую Посылку в `AfterCommitGrader`. `AfterCommitGrader` отдаёт
Посылку `BalancingGrader` только после фиксации транзакции, а при откате не отдаёт. `DeveloperOperations`
получает тот же `AfterCommitGrader`.

Диспетчер получает однопоточный исполнитель, обёрнутый в `AfterTransactionExecutor`. Запрос валидации,
поставленный в очередь внутри транзакции, передаётся исполнителю после её завершения с любым исходом. После отката
диспетчер всё равно снимает запрос с очереди, а `proceed` для несохранённого запроса ничего не делает.
`TaskValidationOperations` получает `BalancingGrader` напрямую: она работает вне транзакций сервисов.
Диспетчер запускает и останавливает бин `TaskValidationDispatcherLifecycle` (`SmartLifecycle`). При закрытии
контекста, до закрытия грейдера и базы данных, он отбрасывает задачи из очереди исполнителя и до 30 секунд ждёт
текущую задачу, не прерывая её. После перезапуска `start()` продолжает все активные запросы, в том числе
отброшенные. Задача, не завершившаяся за 30 секунд, может завершить свой запрос технической остановкой.

## Ошибки операций

`OperationException` из прокси-сервиса обрабатывают два обработчика из пакета `error`. Оба выбирают текст
по общему роду ошибки, см. раздел «Общие типы ошибок»
в [testsys-operation/README.md](../../testsys-operation/README.md#общие-типы-ошибок).

Исключение при навигации, в том числе из конструктора страницы, обрабатывает `OperationErrorView` через
`HasErrorParameter<OperationException>`. Он показывает общую визуальную часть `ErrorPage` из components:

| Род ошибки | Код | Тексты экрана |
|------------|-----|---------------|
| `EntityNotExistsError`, `ContestNotAddedToClassError` и `ContestNotAddedToCompetitionError` | 404 | `notFound`, как у отсутствующего маршрута |
| `AccessDeniedError`, `MissedRequiredRoleError`, `ResourceAccessError` и `ContestNotEnteredError` | 403 | `forbidden` |
| Остальные | 400 | `pageFailed` |

Ошибка операции — ожидаемый отказ, поэтому код 500 обработчик не возвращает: технические сбои приходят другими
исключениями и обрабатываются Vaadin. Тур вне Класса или Соревнования Пользователя считается отсутствующим.

Исключение в обработчике события получает `OperationErrorHandler`. Он становится `ErrorHandler` каждой новой
сессии и показывает тост `FeedbackKind.Error` с заголовком из группы `failures`. Свой заголовок есть
у `EntityNotExistsError`, `AccessDeniedError`, `MissedRequiredRoleError` и `ResourceAccessError`, общий —
у остальных ошибок. Обработчик ищет `OperationException` и среди причин исключения. Исключения других типов
он передаёт `DefaultErrorHandler` Vaadin без тоста.

Тексты экранов и тостов входят в `UiTexts`, см. [components/README.md](../components/README.md#тексты).

## Запуск и проверка

Команды запуска и сборки — в [structure.md](../../docs/project/structure.md#сборка-веб-приложений).

Для запуска нужны база данных PostgreSQL, заданная стандартными свойствами `spring.datasource.*`, и абсолютные
пути файлов в обязательных свойствах `testsys.file-storage.paths.<вид>` (виды перечислены в разделе
«Хранение файлов» в [database/README.md](../../testsys-infra/database/README.md#хранение-файлов)). Тесты используют
каталоги `build/test-file-storage/<вид>` модуля из `src/test/resources/config/application.properties` и PostgreSQL
в контейнере Testcontainers, поэтому для них нужен запущенный Docker. Каждый `@SpringBootTest` подключает
`PostgresTestConfiguration`: все Spring-контексты тестов JVM работают с одним контейнером и одной базой.

Очистка файлов запускается явно в режиме обслуживания; условия и настройки описаны в разделе
[«Очистка сирот»](../../testsys-infra/database/README.md#очистка-сирот).

`OperationsConfiguration` читает конфигурацию операций из обязательных свойств без значений по умолчанию:

| Свойство | Значение |
|----------|----------|
| `testsys.operation.community.public-community-id` | Идентификатор Публичного Сообщества; Сообщество должно существовать |
| `testsys.operation.email-confirmation.confirmation-code-lifetime` | Срок действия кода подтверждения в формате ISO-8601 `Duration` |
| `testsys.operation.email-confirmation.max-confirmation-attempts` | Число попыток ввода кода подтверждения |
| `testsys.operation.community-invite.ttl` | Срок действия Кода-приглашения в Сообщество в формате ISO-8601 `Duration` |
| `testsys.operation.community-invite.refresh-period` | Период замены Кодов-приглашений в Сообщество с истёкшим сроком в формате ISO-8601 `Duration`; пока не используется |

Настройки SMTP-сервера — в разделе «Настройки» в [mail/README.md](../../testsys-infra/mail/README.md#настройки).
Тесты страниц и `UserService` подключают `AppTestConfiguration`: она сохраняет Пользователей через порты хранения, подменяет
Публичное Сообщество Сообществом из этих данных и записывает письма вместо отправки. Тестовые Spring-контексты
не приостанавливаются (`src/test/resources/spring.properties`): `TaskValidationDispatcherLifecycle` останавливает
исполнитель диспетчера окончательно, и приостановленный контекст не запустился бы снова.

Cookie сессии называется `TESTSYS_APP_SESSION`. Отдельное имя позволяет открывать оба приложения
на одном хосте одновременно без перезаписи сессии другого приложения.
