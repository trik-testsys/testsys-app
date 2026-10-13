# Веб-приложения

Документ описывает модуль `testsys-web:app`: запуск основного и служебного приложений Vaadin Flow, безопасность, страницы,
подключение операций и границы его ответственности. Компоненты и визуальные правила описаны
в [components/README.md](../components/README.md), общая структура и сборка —
в [structure.md](../../docs/project/structure.md), операции — в [testsys-operation/README.md](../../testsys-operation/README.md).

Точка входа в `TestSysApplication.kt` запускает [TestSysRuntime](src/main/kotlin/tech/testsys/bootstrap/TestSysRuntime.kt).
Один `bootJar` содержит общий невебовый Spring-контекст и три соседних servlet-контекста с отдельными Tomcat:
основной интерфейс, служебный интерфейс и сервер метрик.
Родитель создаёт datasource, JPA, Liquibase, операции, грейдер, диспетчер валидации и реестр метрик один раз.
Дочерние контексты получают эти зависимости из родителя.

`SharedInfrastructureConfiguration` явно подключает инфраструктурные автоконфигурации и автоконфигурации метрик;
`WebAutoConfiguration` подключает servlet, Vaadin, security и наблюдение HTTP-запросов отдельно в основном
и служебном контекстах. Сервер метрик описан в разделе [Метрики](#метрики).
Конфигурации основного и служебного интерфейсов помечены `@SpringBootApplication` для обнаружения приложений
инструментами разработки.
Стандартный поиск автоконфигураций отключён через `spring.boot.enableautoconfiguration=false`; явные импорты продолжают работать.
Основной дочерний контекст включает транзакционные прокси сервисов через
`@EnableTransactionManagement(proxyTargetClass = true)`: прокси наследуют класс сервиса, поэтому страницы получают
сервис по его классу и тогда, когда сервис реализует интерфейсы.
`MainWebConfiguration` создаёт локальный retry-advisor с политикой родителя:
инфраструктурный auto-proxy creator Spring рассматривает только локальные определения advisor.
Transaction manager остаётся общим.
Основной и служебный контексты создают свой `applicationTaskExecutor` с декоратором своего security context.

Пакет `tech.testsys.bootstrap` владеет запуском, окружениями и сервером метрик. `tech.testsys.web.service` содержит
служебные страницы, security и proxy-конфигурацию. `tech.testsys.web.monitoring` содержит общую для обоих интерфейсов
разметку HTTP-запросов, учёт сбоев страниц и настройку реестра метрик родителя; ни один интерфейс этот пакет
не сканирует. Общий `AppShell` находится в `tech.testsys.web.shell`: каждый Vaadin-контекст создаёт
свою оболочку и свой реестр маршрутов. `@EnableVaadin` ограничивает сканирование пакетами своего интерфейса
и оболочки. Основной интерфейс остаётся в `tech.testsys.web.app`; таблица ниже описывает его подпакеты.
Зависимости модулей перечислены в structure.md.

| Пакет | Что лежит |
|-------|-----------|
| `config` | Spring-конфигурация: локализация `UI` (`UiLocaleInitializer`), `InfraConfiguration`, `OperationsConfiguration`, `MainWebConfiguration` с метриками основного интерфейса, `AfterTransactionExecutor`, `AfterCommitGrader`; лог вызовов и контекст запроса `CallLoggingInterceptor`, `LoggedValues` и `RequestLogContext`, см. раздел [Логирование](#логирование) |
| `security` | `SecurityConfiguration`, `CabinetPrincipal`, программный вход `CabinetSignIn` и `RateLimitConfiguration`, см. раздел [Безопасность](#безопасность) |
| `service` | `CurrentUser`, общие VO и `Page.map` прокси-сервисов |
| `service/<actor>` | Прокси-сервис `<Actor>Service` и его VO, см. раздел [Прокси-сервисы](#прокси-сервисы) |
| `view` | Страницы и шапки `CabinetHeaders`, см. раздел [Страницы](#страницы) |
| `error` | Обработчики отсутствующих маршрутов, отказа в доступе, ошибок операций и непредвиденных исключений навигации |

`InfraConfiguration` подключает конфигурации инфраструктурных модулей `database`, `grpc`, `diagnostics`, `mail`,
`settings` и `monitoring` через `@Import`.

`UiLocaleInitializer` при создании каждого `UI` выбирает регион Пользователя и вызывает `initializeUiLocale`
из components; устройство локализации `UI` и часового пояса — в разделе «Тексты»
[components/README.md](../components/README.md#тексты). Регион берётся из cookie `TESTSYS_REGION`, значение
которого — имя `SupportedRegion`, затем из лучшего поддерживаемого языка заголовка `Accept-Language`, затем из
`testsys.web.localization.default-region`. Неизвестное значение cookie и некорректный заголовок пропускаются.
Даты показываются в поясе браузера; до его получения — в `testsys.web.localization.default-time-zone`.
`AppShell` строится до создания `UI`, поэтому имя бренда для заголовка вкладки он берёт из региона
по умолчанию. Страницы показывают и читают даты в поясе `currentTimeZone()` через `toUserDateTime`,
`toUserInstant`, `startOf` и `endOf`.

Обработчик `NotFoundView` использует общий `NotFoundPage`; контракт маршрутизации — в разделе
[Отсутствующие маршруты](#отсутствующие-маршруты).

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
и `CurrentUser` доступен в фоне. Декоратор также передаёт задаче MDC поставившего её запроса и очищает MDC потока
после задачи, см. раздел [Логирование](#логирование).

Формы входа Spring Security нет: страницы входа и регистрации вызывают `CabinetSignIn.signIn`. Он меняет
идентификатор сессии (`changeSessionId`) против фиксации сессии и сохраняет в сессии `CabinetPrincipal` —
идентификатор и вид Пользователя без Кода-доступа и данных. Вид даёт единственное полномочие `ROLE_<вид>`:
`MULTIPLE_ROLE`, `PARTICIPANT`, `OBSERVER` или `SUPERVISOR`. `signIn` возвращает адрес возврата и удаляет его
из сессии. Выход — `AuthenticationContext.logout()` из меню Псевдонима: сессия закрывается, браузер переходит
в корень основного интерфейса: на `/` или на context path из `server.servlet.context-path` со слешем в конце.

### Ограничение частоты запросов

`RateLimitConfiguration` подключает к основному интерфейсу штатный фильтр Tomcat `RateLimitFilter`
с `ExactRateLimiter`. Фильтр считает HTTP-запросы каждого адреса клиента в окне фиксированной длины. Окна
отсчитываются от начала эпохи Unix, а не от первого запроса клиента, поэтому на стыке двух окон клиент успевает
отправить почти вдвое больше запросов. На запрос сверх лимита фильтр отвечает кодом 429 до Spring Security
и сервлета Vaadin. Запросы под `/VAADIN/` внутри context path не считаются: там лежат сборка клиента, шрифты
и канал push, а браузер не повторяет такие запросы после 429.

Значения по умолчанию заданы в `application.yml`:

| Свойство | По умолчанию | Значение |
|----------|--------------|----------|
| `testsys.web.rate-limit.requests` | `1200` | Положительное число запросов одного адреса за окно |
| `testsys.web.rate-limit.duration` | `PT1M` | Длина окна в формате ISO-8601 `Duration`, целое число секунд |
| `testsys.web.rate-limit.enforce` | `true` | `false` пропускает запросы сверх лимита и только пишет их в лог |

С неверным значением основной интерфейс не запустится.

Фильтр берёт адрес клиента из `remoteAddr` запроса. С `server.forward-headers-strategy=native` Tomcat подставляет
в него адрес из `X-Forwarded-For`, если запрос пришёл от доверенного прокси. Без заголовка все клиенты за прокси
делят счётчик адреса прокси, см. раздел «HTTPS-прокси» в [deployment.md](../../docs/project/deployment.md#https-прокси). Каждый
адрес IPv6 считается отдельно, поэтому клиент, который меняет адреса внутри своей подсети, получает лимит на каждый
адрес.

Клиент Vaadin считает ответ 429 на запросы UIDL и heartbeat восстановимой ошибкой и повторяет запрос.
Каждый запрос сверх лимита фильтр пишет в лог на уровне WARN логгером `org.apache.catalina.filters.RateLimitFilter`
с адресом клиента, поэтому при наплыве запросов таких строк много. Фильтр стоит внутри фильтра наблюдения,
и отклонённые запросы видны в `http.server.requests` с меткой `status="429"`, см. раздел [HTTP-запросы](#http-запросы).

Счётчики хранятся в памяти: каждый экземпляр приложения считает свои запросы, перезапуск обнуляет счётчики.
Служебный интерфейс и сервер метрик фильтр не подключают. Попытки служебного входа ограничивает
`ServiceLoginAttemptLimiter`, см. раздел [Служебный контекст](#служебный-контекст); доступ к метрикам — в разделе
[Сервер метрик](#сервер-метрик).

`RateLimitConfigurationHttpTests` проверяет ответ 429 сверх лимита и подсчёт по адресу из `X-Forwarded-For`.
`RateLimitConfigurationTests` проверяет значения свойств, режим `enforce: false` и пропуск запросов под `/VAADIN/`.

## Страницы

| Страница | Маршрут | Доступ |
|----------|---------|--------|
| `MainView` | `""` | Без входа; вошедшего Пользователя перенаправляет на его стартовую страницу |
| `AuthenticationView` | `login` | Без входа |
| `RegistrationView` | `registration` | Без входа |
| `RestoreAccessView` | `restore-access` | Без входа |
| `MultiMainView` | `home` | `MULTIPLE_ROLE` |
| `ProfileView` | `profile` | `MULTIPLE_ROLE` |
| `AdminView` | `admin`, `admin/communities`, `admin/users` | `MULTIPLE_ROLE` с Ролью Администратора |
| `AdminCommunityView` | `admin/communities/:communityId` | `MULTIPLE_ROLE`, создатель Сообщества |
| `AdminUserView` | `admin/users/:userId`, `admin/users/:userId/developer`, `admin/users/:userId/manager`, `admin/users/:userId/judge`, `admin/observers/:observerId` | `MULTIPLE_ROLE`, Администратор с доступом к Пользователю |
| `SupervisorView` | `supervisor`, `supervisor/users`, `supervisor/communities` | `SUPERVISOR` |
| `SupervisorCommunityView` | `supervisor/communities/:communityId` | `SUPERVISOR` |
| `SupervisorUserView` | `supervisor/users/:userId`, а также разделы `developer`, `manager`, `judge` | `SUPERVISOR`, любой Пользователь, кроме Супервайзеров |
| `DeveloperView` | `developer`, `developer/tasks`, `developer/contests` | `MULTIPLE_ROLE` с Ролью Разработчика |
| `DeveloperTaskView` | `developer/tasks/:taskId` | `MULTIPLE_ROLE`, владелец Задачи |
| `DeveloperTestingView` | `developer/tasks/:taskId/testing/:testingId` | `MULTIPLE_ROLE`, владелец Задачи; запрос принадлежит Задаче |
| `DeveloperResourceView` | `developer/tasks/:taskId/resources/:resourceId` | `MULTIPLE_ROLE`, владелец Задачи |
| `DeveloperContestView` | `developer/contests/:contestId` | `MULTIPLE_ROLE`, владелец Тура или Разработчик Сообщества с доступом к нему |
| `ManagerView` | `manager`, `manager/classes`, `manager/competitions` | `MULTIPLE_ROLE` с Ролью Организатора |
| `ManagerClassView` | `manager/classes/:classId` | `MULTIPLE_ROLE`, создатель Класса |
| `ManagerCompetitionView` | `manager/competitions/:competitionId` | `MULTIPLE_ROLE`, создатель Соревнования |
| `ManagerContestView` | `manager/classes/:classId/contests/:contestId`, `manager/competitions/:competitionId/contests/:contestId` | `MULTIPLE_ROLE`, создатель Класса или Соревнования, в которое добавлен Тур |
| `StudentView` | `student`, `student/classes` | `MULTIPLE_ROLE` с Ролью Ученика |
| `StudentClassView` | `student/classes/:classId` | `MULTIPLE_ROLE`, Ученик Класса |
| `StudentContestView`, `StudentTaskView` | `student/classes/:classId/contests/:contestId`, `student/classes/:classId/contests/:contestId/tasks/:taskId` | `MULTIPLE_ROLE`, Ученик Класса |
| `ParticipantView`, `ObserverView` | `participant`, `observer` | Соответствующий вид |
| `ParticipantContestView`, `ParticipantTaskView` | `participant/contests/:contestId`, `participant/contests/:contestId/tasks/:taskId` | `PARTICIPANT` |
| `JudgeView` | `judge`, `judge/submissions` | `MULTIPLE_ROLE` с Ролью Судьи |
| `JudgeSolutionView` | `judge/submissions/:submissionId` | `MULTIPLE_ROLE` с Ролью Судьи |

Требования к страницам — в разделе `testsys.web` в [features.md](../../docs/domain/features.md). Страницы всех Ролей
реализованы. Стартовую страницу вида Пользователя возвращает `startPageOf`.

`ParticipantView` загружает Туры через `ParticipantService.viewContests` в `beforeEnter`.
`StudyContestTaskVo` передаёт результаты таблицы Задач, `JudgeResultVo` — расширенные строки таблицы Судьи.
`DeveloperTaskView` показывает последнюю фиксацию в диалоге; скачивание использует идентификаторы версий
из этой фиксации через `DeveloperService.downloadResourceVersion`.

История тестирований в `DeveloperTaskView` открывает выбранный запрос в `DeveloperTestingView`.
При открытии страница загружает запрос через историю его Задачи и показывает названия версий Ресурсов из снимка
запроса. Ссылка в хлебных крошках возвращает к исходной Задаче.

Страницы Кабинетов вызывают прокси-сервис в `beforeEnter`, до построения таблиц. Поэтому отсутствие Роли, сущности
или доступа открывает экран ошибки из раздела [Ошибки операций](#ошибки-операций), а не ошибку загрузки таблицы.
`AdminView` без раздела переадресует на `admin/communities`, `DeveloperView` — на `developer/tasks`, `ManagerView` —
на `manager/classes`, `JudgeView` — на `judge/submissions`. Наблюдатель открывается по своему адресу
`admin/observers/:observerId`: операция просмотра принимает идентификатор вместе с видом Пользователя.

`ManagerContestView` и `ManagerCompetitionView` скачивают CSV в UTF-8 с BOM. Столбцы разделены `;`, значения
заключаются в кавычки по RFC 4180, строки завершаются `\r\n`. В сводной таблице Тура на каждую Задачу
приходится два столбца: лучший результат и количество Посылок. Файл Участников содержит столбцы «ID»,
«Псевдоним», «Код-доступа». Данные страницы запрашивают заново при скачивании. Скачивание готовит файл в фоновом
потоке, поэтому заголовки CSV из сообщений страница форматирует под блокировкой сессии своего `UI` через `inUiOf`.

`DeveloperContestView` показывает отказы прикрепления Задачи тостом с причиной; диалог остаётся открытым.

Страницы Ученика и страницы Тура и Задачи Участника тоже вызывают прокси-сервис в `beforeEnter`. Страницы Тура и Задачи
общие для Участника и Ученика: у каждого вида Пользователя свой класс страницы со своим маршрутом, а общая часть —
в абстрактных `StudyContestView` и `StudyTaskView`.

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
открытие страницы его не показывает. `RestoreAccessView` показывает новый Код-доступа на своей странице и вход
не выполняет; Код-доступа тоже хранится только в построенной странице. Ошибки входа, регистрации и подтверждения
восстановления доступа страницы показывают тостом с причиной сами, без `OperationErrorHandler`.
Так же `MultiMainView` показывает отказы присоединения к Сообществу; после присоединения страница перестраивается
вместе с шапкой, чтобы новая Роль появилась и в «Меню».

Все текущие страницы настраивают вводное пояснение и гайды своих блоков, включая гостевые формы
и обе стадии регистрации.
Описания находятся рядом с содержимым блоков; общие секции используют те же описания при повторном построении.
Экраны ошибок получают гайды из components. Контракт DSL — в разделе
[Гайды страницы и блока](../components/README.md#гайды-страницы-и-блока).

Тексты страниц из таблицы ниже — сообщения модуля локализации в бандлах `page_*`; страница получает их через
`currentLocalization()` при каждом построении и в обработчиках. Как добавить сообщение — в
[add-localization.md](../../docs/guides/add-localization.md).

| Бандл | Тексты |
|-------|--------|
| `page_guest` | `MainView`, вкладки `guestForm`, `AuthenticationView`, `RestoreAccessView` |
| `page_registration` | `RegistrationView` |
| `page_profile` | `ProfileView` |
| `page_confirmation` | Подтверждение почты кодом из письма, общее для `RegistrationView` и `ProfileView` |
| `page_home` | `MultiMainView` |
| `page_cabinet` | Шапка Кабинета, названия Ролей и разделы «Меню»; общие для Кабинетов статус проверки Задачи, ограничение длины названия и запись длительности |
| `page_administrator` | `AdminView`, `AdminCommunityView`, `AdminUserView` |
| `page_developer` | `DeveloperView`, `DeveloperContestView`, `DeveloperTaskView`, `DeveloperResourceView`, `DeveloperTestingView` |
| `page_manager` | `ManagerView`, `ManagerClassView`, `ManagerCompetitionView`, `ManagerContestView` и заголовки их CSV |
| `page_judge` | `JudgeView`, `JudgeSolutionView` |
| `page_student` | `StudentView`, `StudentClassView`, `StudentContestView`, `StudentTaskView` |
| `page_participant` | `ParticipantView`, `ParticipantContestView`, `ParticipantTaskView` |
| `page_observer` | `ObserverView` и заголовки его CSV |
| `page_supervisor` | `SupervisorView`, `SupervisorUserView`, `SupervisorCommunityView` |

Как выбрать бандл для текста страницы Роли — в шаге 1
[add-localization.md](../../docs/guides/add-localization.md#1-выбрать-ключ-и-файл-бандла). Общие страницы Тура
и Задачи `StudyContestView` и `StudyTaskView` получают тексты через `StudyTexts`: страницы Ученика читают их
из `page_student`, страницы Участника — из `page_participant`.

Страницы Супервайзера используют общие блоки страниц Администратора: `CommunityDetailsView`, `adminUsersTable`
и блоки разделов Ролей из `UserRoleDetails.kt`. Тексты этих блоков остаются в `page_administrator`, поэтому
`SupervisorCommunityView` получает из `page_supervisor` только название Кабинета для хлебных крошек.

Страницы из таблицы, у которых есть свой заголовок вкладки, задают его через `HasDynamicTitle` из сообщения.

## Прокси-сервисы

Страницы вызывают операции только через прокси-сервисы `<Actor>Service` в пакете `service/<actor>`:
`AdministratorService`, `DeveloperService`, `JudgeService`, `ManagerService`, `MultipleRoleUserService`,
`ObserverService`, `ParticipantService`, `StudentService`, `StudyService` и `UserService`. Методы сервиса соответствуют
операциям один к одному и принимают те же входные данные без Пользователя. Сервиса для `TaskValidationOperations`
нет: её вызывает только диспетчер. `ObserverService` возвращает странице пары Соревнование–назначенный Тур и
результаты всех доступных Наблюдателю Соревнований. `ObserverView` формирует один CSV-файл независимо
от фильтров, сортировки и текущей страницы таблицы. Заголовки этого CSV страница форматирует через `inUiOf`,
как и страницы Организатора.
`MultipleRoleUserService.viewProfile` добавляет к Ролям Псевдоним и почту текущего Пользователя и возвращает их
в `ProfileVo`.
`AdministratorService` не выставляет служебную `refreshCommunityInvite`, `ManagerService` — служебную
`refreshClassInvite`.

Классы операций, у которых есть сервис, `TaskValidationOperations` и `TaskValidationDispatcher` создаются
`@Bean`-методами в `OperationsConfiguration`; модуль операций не сканируется.

- Сервис помечен `@Service` и `@Transactional`: каждый вызов выполняется в одной транзакции. Методы `view*`
  и `download*` выполняются с `readOnly = true`. Страницы вызывают операции только через сервисы; правило — в разделе
  «Транзакции» в [testsys-operation/README.md](../../testsys-operation/README.md#транзакции).
- Если транзакция сервиса столкнулась с параллельной транзакцией, advisor модуля `database` выполняет вызов сервиса
  заново в новой транзакции, см. раздел
  [«Транзакции и согласованность»](../../testsys-infra/database/README.md#транзакции-и-согласованность)
  в database/README.md.
- Вызовы сервиса пишутся в лог на уровне DEBUG, см. раздел [Логирование](#логирование).
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
  отказы этой операции ничего не меняют. Так же выполняются `MultipleRoleUserService.confirmEmailChange`
  и `UserService.confirmAccessRestoration`, который возвращает новый Код-доступа. Операции восстановления доступа
  `UserService` вызывает у `MultipleRoleUserOperations`.
- VO не содержат сущностей и ленивых ссылок: связи заменены идентификаторами и списками идентификаторов,
  доменные value-типы сохраняются. VO содержит все поля данных сущности, её `id` и `createdAt`. Файлы Ресурсов
  представлены именем без содержимого.
- Связанные сущности, которые возвращает операция, сервис отдаёт парами и тройками VO. Так `DeveloperService`
  возвращает страницу Туров и Задачу с Сообществами доступа, Тур — с его Задачами и Сообществами доступа, а версии
  Ресурса — с Решением Авторского Решения. `downloadResourceVersion` возвращает файл версии как `FileData`
  с содержимым.

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
сессии и показывает тост `FeedbackKind.Error` с заголовком из сообщений `ui_feedback.error.failure.*`. Свой заголовок есть
у `EntityNotExistsError`, `AccessDeniedError`, `MissedRequiredRoleError` и `ResourceAccessError`, общий —
у остальных ошибок. Обработчик ищет `OperationException` и среди причин исключения. Исключения других типов
он учитывает как технический сбой и передаёт `DefaultErrorHandler` Vaadin без тоста. Непредвиденное исключение
при навигации показывает штатный экран Vaadin через `InternalErrorView` — наследника `InternalServerError`, который
сначала учитывает сбой. Правила учёта — в разделе [Метрики](#метрики).

Тексты экранов и тостов — сообщения `ui_feedback.error.*`, см. [components/README.md](../components/README.md#тексты).

## Запуск и проверка

Команды запуска и сборки — в [structure.md](../../docs/project/structure.md#сборка-веб-приложений).
Запуск в Docker Compose для разработки, e2e и production — в [deployment.md](../../docs/project/deployment.md).

Для запуска нужны база данных PostgreSQL, заданная стандартными свойствами `spring.datasource.*`, и абсолютные
пути файлов в обязательных свойствах `testsys.file-storage.paths.<вид>` (виды перечислены в разделе
«Хранение файлов» в [database/README.md](../../testsys-infra/database/README.md#хранение-файлов)). Тесты используют
каталоги `build/test-file-storage/<вид>` модуля из `src/test/resources/config/application.properties` и PostgreSQL
в контейнере Testcontainers, поэтому для них нужен запущенный Docker. Тесты основного интерфейса с `@SpringBootTest`
подключают `PostgresTestConfiguration`: все проверки с БД в одной JVM используют общий контейнер. Конфигурация явно подключает
`ServiceConnectionAutoConfiguration` и общую инфраструктуру для существующих тестов основного интерфейса.
Тесты модуля запускаются с поясом JVM `UTC`, отличным от `testsys.web.localization.default-time-zone`, поэтому
чтение дат в поясе JVM вместо пояса Пользователя заметно в тестах.

Очистка файлов запускается явно в режиме обслуживания; условия и настройки описаны в разделе
[«Очистка сирот»](../../testsys-infra/database/README.md#очистка-сирот).

После миграций Liquibase приложение проверяет Сообщество с настроенным ID. Если ID свободен, оно в одной
транзакции создаёт технического владельца, Сообщество и оба Кода-приглашения. При повторном запуске приложение
проверяет владельца, его Роль и членство, а также приглашения. При неполных или противоречивых данных запуск
завершается ошибкой. Конфликт при одновременном запуске двух экземпляров повторно проверяется в новой транзакции.
Почта технического владельца — `public-community-owner@testsys.invalid`.
По Коду-доступа технического владельца войти в приложение нельзя.

После создания всех singleton-бинов общего контекста инфраструктуры и до запуска веб-контекстов бин
`supervisorBootstrap` вызывает `SupervisorBootstrap`. Он создаёт единственного Супервайзера или устанавливает существующему Псевдоним
и Код-доступа из конфигурации; требования — в фиче
[testsys.entity.supervisor](../../docs/domain/features.md#testsysentitysupervisor-implemented). Псевдоним и формат
Кода-доступа проверяются до транзакции. Проверка Кода-доступа у Пользователей других видов, поиск, создание
и обновление Супервайзера выполняются в одной транзакции. Код-доступа не попадает в сообщения об ошибках.
Единственность Супервайзера обеспечивает приложение, а не схема БД. Два экземпляра с разными Кодами-доступа при
одновременном первом запуске могут создать двух Супервайзеров; тогда следующий запуск завершится ошибкой.

`OperationsConfiguration` разрешает статические параметры при запуске:

| Свойство | Значение |
|----------|----------|
| `testsys.operation.community.public-community-id` | Обязательный точный ID Публичного Сообщества; приложение создаёт запись с этим ID или проверяет существующую |
| `testsys.operation.community.public-community-name` | Название Публичного Сообщества, обязательное при первом создании |
| `testsys.operation.supervisor.name` | Псевдоним Супервайзера; пробелы по краям удаляются |
| `testsys.operation.supervisor.access-token` | Код-доступа Супервайзера в формате из фичи [testsys.entity.supervisor](../../docs/domain/features.md#testsysentitysupervisor-implemented) |
| `testsys.operation.community-invite.refresh-period` | Период замены Кодов-приглашений в Сообщество с истёкшим сроком в формате ISO-8601 `Duration`; пока не используется |
| `testsys.operation.class-invite.refresh-period` | Период замены Кодов-приглашений в Класс с истёкшим сроком в формате ISO-8601 `Duration`; пока не используется |

Пять динамических параметров `OperationsConfiguration` привязывает из общего снимка классами с `@SettingsProperties`,
по одному на конфигурацию операций; устройство описано в разделе
[«Типизированные значения»](../../testsys-infra/settings/README.md#типизированные-значения).
У них нет значений по умолчанию в каталоге; сохранённого значения в БД достаточно,
даже если свойство отсутствует в Environment. Порядок источников —
в [settings/README.md](../../testsys-infra/settings/README.md#значения-и-источники).

| Свойство | Значение |
|----------|----------|
| `testsys.operation.email-confirmation.confirmation-code-lifetime` | Срок действия кода подтверждения в формате ISO-8601 `Duration` |
| `testsys.operation.email-confirmation.max-confirmation-attempts` | Число попыток ввода кода подтверждения |
| `testsys.operation.community-invite.ttl` | Срок действия Кода-приглашения в Сообщество в формате ISO-8601 `Duration` |
| `testsys.operation.competition.max-participants` | Предельное общее количество Участников в Соревновании |
| `testsys.operation.class-invite.ttl` | Срок действия Кода-приглашения в Класс в формате ISO-8601 `Duration` |

Локализацию интерфейса задают свойства со значениями по умолчанию в `application.yml`:

| Свойство | По умолчанию | Значение |
|----------|--------------|----------|
| `testsys.web.localization.default-region` | `RU` | Регион, если cookie региона и языки браузера не дают поддерживаемого; имя `SupportedRegion` |
| `testsys.web.localization.default-time-zone` | `Europe/Moscow` | Часовой пояс дат, пока браузер не сообщил свой |

Настройки SMTP-сервера — в разделе «Настройки» в [mail/README.md](../../testsys-infra/mail/README.md#настройки).
Реестр Проверяющих узлов хранится в БД; его восстановление и импорт адресов конфигурации описаны в разделе
[«Реестр узлов»](../../testsys-infra/grpc/README.md#реестр-узлов), настройки грейдера — в разделе
[«Настройки»](../../testsys-infra/grpc/README.md#настройки) grpc/README.md.

Тесты страниц и `UserService` подключают `AppTestConfiguration`: она сохраняет Пользователей, Сообщества, Классы,
Соревнования и Туры через порты хранения, подменяет Публичное Сообщество Сообществом из этих данных и записывает
письма вместо отправки. Для Супервайзера `userOf` возвращает единственного Супервайзера, созданного при запуске
контекста, с Псевдонимом и Кодом-доступа из тестовой конфигурации. Классы, Туры, Задачи, Посылки и записи входа
для страниц Ученика и Участника сохраняет импортированный ею `StudyFixtures`. Тестовые Spring-контексты не приостанавливаются
(`src/test/resources/spring.properties`): `TaskValidationDispatcherLifecycle` останавливает исполнитель диспетчера
окончательно, и приостановленный контекст не запустился бы снова.

Cookie основного интерфейса называется `TESTSYS_APP_SESSION`.

## Служебный контекст

[ServiceWebConfiguration](src/main/kotlin/tech/testsys/web/service/ServiceWebConfiguration.kt) подключает только
служебные страницы: `ServiceLoginView` по адресу `login`, `ServiceHomeView` в корне своего сервера,
`ServiceSettingsView` по адресу `settings`, `ServiceGradingView` по адресу `grading`, `ServiceUsersView` по адресу
`users`, `ServiceUserView` по адресу `users/:userId` и `ServiceTrikStudioVersionsView` по адресу
`trik-studio-versions`.
Он не подключает `CurrentUser`, `CabinetHeaders` и прокси-сервисы доменных операций.
`ServicePrincipal` хранит только техническую идентичность с полномочием `ROLE_SERVICE`.

Кнопка «Grafana» на `ServiceHomeView` открывает в той же вкладке адрес из `testsys.service.grafana-url`.
Кабинет не передаёт Grafana учётных данных: в Grafana входят её собственными учётными записями. Grafana окружений
Docker Compose и её адрес описаны в разделе «Метрики и мониторинг»
в [deployment.md](../../docs/project/deployment.md#метрики-и-мониторинг).

`ServiceSecurityConfiguration` сохраняет штатные правила Vaadin для ресурсов, init, UIDL, heartbeat, push и CSRF.
Навигация проверяет аннотации страниц; остальные HTTP-точки закрыты через `denyAll`.
Статические ресурсы компонентов доступны по `/testsys-ui/**`.
После входа `ServiceSignIn` меняет ID HTTP-сессии и сохраняет security context в служебную сессию.
Выход через `AuthenticationContext.logout()` закрывает эту сессию и возвращает `login`.
Cookie `TESTSYS_SERVICE_SESSION` имеет `HttpOnly`, `SameSite=Lax`, путь `/` и не имеет `Domain`. Флага `Secure`
нет: сервер доступен по HTTP, поэтому порт кабинета открывают только в закрытой сети.
Сессия передаётся только cookie. Cookies различаются именами: браузер может отправлять обе на оба порта.
Vaadin использует штатный heartbeat и штатное закрытие сессии; отдельного таймера активности человека нет.

`ServiceAuthenticator` сравнивает SHA-256 дайджесты через `MessageDigest.isEqual`.
`ServiceLoginAttemptLimiter` атомарно учитывает неудачи по проверенному контейнером адресу клиента.
История и блокировки живут в памяти; детали алгоритма и временных границ проверяет
[ServiceLoginAttemptLimiterTests](src/test/kotlin/tech/testsys/web/service/security/ServiceLoginAttemptLimiterTests.kt).

### Конфигурация запуска

[ServiceWebSettings](src/main/kotlin/tech/testsys/bootstrap/ServiceWebSettings.kt) проверяется до создания инфраструктуры.

| Свойство | Назначение |
|----------|------------|
| `testsys.service.secret` | Обязательный код длиной не меньше 64 символов; пустой код запрещён |
| `testsys.service.address` | Обязательный адрес служебного HTTP-сервера |
| `testsys.service.port` | Обязательный порт служебного HTTP-сервера; `0` выбирает свободный порт в тестах |
| `testsys.service.grafana-url` | Обязательный абсолютный адрес Grafana со схемой `http` или `https`, без имени пользователя и пароля; значения по умолчанию нет |

При развёртывании передавайте случайно сгенерированный код через `TESTSYS_SERVICE_SECRET` или защищённый файл
конфигурации. Не добавляйте его в командную строку, исходники или журналы. Страницы не передают код через URL,
а после обработки попытки очищают поле. Объект конфигурации хранит дайджест и не выводит секрет через `toString`.

У каждого из четырёх контекстов отдельный изменяемый `Environment`. Spring Boot загружает файлы, переменные окружения
и аргументы командной строки обычным порядком. `server.*` задаёт основной HTTP-сервер.
`server.servlet.context-path` задаёт префикс пути основного интерфейса, например `/testsys`. Страницы, ресурсы, push
и cookie сессии получают этот префикс. Служебный интерфейс и сервер метрик всегда работают в корне своего сервера.
В служебном дочернем контексте
[WebContextEnvironment](src/main/kotlin/tech/testsys/bootstrap/WebContextEnvironment.kt) после загрузки свойств задаёт
служебные адрес, порт, корневой context path и параметры cookie. Настройки основного HTTP-сервера их не заменяют.
Так же `WebContextEnvironment` задаёт свойства сервера метрик, см. раздел [Метрики](#метрики).
После загрузки конфигурации `TestSysRuntime` закрепляет во всех дочерних контекстах только внутренний инвариант
`spring.boot.enableautoconfiguration=false`, в том числе при замене конфигурационного файла через `spring.config.location`.

К служебному HTTP-серверу обращаются напрямую, без reverse proxy. Он игнорирует forwarded-заголовки: адрес клиента
для ограничения попыток входа берётся из соединения. Внутренние порты закрывают от внешнего доступа средствами
развёртывания.

При ошибке запуска любого дочернего контекста `TestSysRuntime` закрывает уже созданные контексты.
Единственный shutdown hook закрывает сервер метрик, служебный, затем основной интерфейс и последним общий родитель.

### Проверки двух интерфейсов

`ServicePagesTests` автоматически находит и строит служебные маршруты с собственным principal, без доменного
Пользователя. `ServiceUserView` строится для Пользователя каждого вида, которого находит подменённый
`ServiceUserSearch` из `ServiceTestConfiguration`; как задать наборы параметров маршрута, описано в разделе
[«Построение страниц»](../../docs/project/unit-tests.md#построение-страниц) unit-tests.md. Тесты основного
интерфейса сохраняют свои fixtures и проверяют прежние страницы.
`ServiceHomeViewTests` проверяет, что кнопка «Grafana» открывает в той же вкладке только адрес из конфигурации.
`WebContextsTests` запускает настоящую иерархию на случайных портах и проверяет общую инфраструктуру,
в том числе выполнение служебного поиска Пользователей в PostgreSQL, разные реестры маршрутов, исполнители
и cookies, а после закрытия — освобождение портов трёх серверов, включая сервер метрик.
Второй тест `WebContextsTests` запускает иерархию с `server.servlet.context-path=/testsys`. Он проверяет страницу,
ресурсы компонентов и путь cookie основного интерфейса под префиксом, а также служебный интерфейс и сервер метрик
в корне. `TestSysRuntimeTests` проверяет отказ до запуска инфраструктуры и освобождение основного порта
при ошибке второго HTTP-сервера, а также закрытие обоих интерфейсов и родителя при занятом порте метрик.
`ServiceForwardedHeadersHttpTests` проверяет, что forwarded-заголовки клиента игнорируются. Проверки метрик перечислены
в разделе [Метрики](#метрики).

Оба интерфейса используют frontend одного артефакта и ресурсы jar компонентов.
HTTP-тестовая оболочка не проверяет клиентский код: для его проверки запускают собранный `bootJar`
и `bootRun`, открывают оба интерфейса в браузере и проверяют вход, выход, ресурсы и `/VAADIN/push`.
Устройство сборки frontend — в [structure.md](../../docs/project/structure.md#сборка-веб-приложений).

## Служебные настройки

Родительский контекст восстанавливает общий `RuntimeSettings` до запуска грейдера и диспетчера.
Основной и служебный интерфейсы используют один holder и store. Протокол, источники и границы применения
принадлежат [settings](../../testsys-infra/settings/README.md).

Служебная главная открывает маршрут `settings`. Он требует `ROLE_SERVICE`, читает обзор при навигации.
Форма отправляет только изменённые поля с ожидаемой ревизией. Устаревшая форма
не перезаписывает чужие изменения; отмена восстанавливает её исходные значения.
Под каждым полем указан путь свойства. Подсказка «i» отдельными абзацами объясняет назначение, формат ввода
и момент применения. Для целых значений она указывает тип Kotlin `Int` или `Long` и допустимость нуля.
Поле размера сообщения показывает сохранённое значение. До перезапуска уведомление показывает сохранённый
и действующий пределы; действующий предел также указан в подсказке.
Публичное Сообщество доступно только для просмотра; настройки отправки почты не показываются.
При неопределённом исходе записи оба интерфейса продолжают работать с прежним действующим снимком,
а редактирование блокируется до перезапуска. Страница сообщает неопределённость сохранённого состояния.

## Состояние проверки

Служебная главная открывает маршрут `grading` действием «Проверка». Страница требует `ROLE_SERVICE` и показывает
снимок грейдера этого процесса из `GradingQueueMonitor` и `GradingNodeRegistry`; их устройство описано в разделах
[«Реестр узлов»](../../testsys-infra/grpc/README.md#реестр-узлов) и
[«Состояние очереди»](../../testsys-infra/grpc/README.md#состояние-очереди) grpc/README.md.

Блок «Очередь» показывает число незавершённых проверок на каждом этапе и их сумму. Блок «Проверяющие узлы»
перечисляет узлы реестра по адресу. В строке узла — последнее известное состояние узла, кнопка участия, «Применить»
и «Удалить» с подтверждением. Кнопка участия показывает неприменённое значение участия, а без него — сохранённое:
кнопка «Вкл» оформлена как основное действие, «Выкл» — как разрушительное. Нажатие меняет значение только
на странице. Значение сохраняет кнопка «Применить»; она доступна, пока значение отличается от сохранённого.
Состояние узла показывает бейдж: зелёный «Доступен», красный «Недоступен» или оранжевый «Неизвестно». Рядом
с «Недоступен» выводится код gRPC-статуса; его формирование описано в разделе
[«Компоненты»](../../testsys-infra/grpc/README.md#компоненты) grpc/README.md. У доступного узла второй бейдж
«В очереди: N/M» сообщает очередь и ёмкость узла. Он зелёный при пустой очереди, красный при заполненной
и оранжевый в остальных случаях. «Добавить» открывает диалог адреса и участия.
«Импортировать из конфигурации» доступно, если в конфигурации есть адреса вне реестра. Диалог импорта
не проверяет доступность узлов и предлагает участие для каждого нового адреса; после применения он показывает исход
по каждому адресу.

Пока страница открыта, `UI` опрашивает сервер каждые 5 секунд. Этот интервал не связан с `testsys.grading.poll-interval`.
Обновление меняет на месте счётчики и коды причин недоступности узлов. Страница перестраивается, если изменились состав
реестра, участие, состояние или очередь узлов, неопределённость или наличие новых адресов конфигурации. Изменённое,
но не применённое участие при перестройке сохраняется, пока сохранённое состояние реестра определено. При уходе
со страницы опрос `UI` отключается. При неопределённом исходе записи реестра страница сообщает об этом
и выключает изменяющие действия до перезапуска.

## Пользователи в служебном кабинете

Служебная главная открывает маршрут `users` действием «Пользователи». Страницы `users` и `users/:userId` требуют
`ROLE_SERVICE` и читают Пользователей через `ServiceUserSearch` общего родителя; его устройство описано в разделе
[«Служебный поиск Пользователей»](../../testsys-infra/database/README.md#служебный-поиск-пользователей)
database/README.md. Требования к поиску и карточке — в
[testsys.dev.service.userSearch](../../docs/domain/features.md#testsysdevserviceusersearch-implemented).

Таблица `ServiceUsersView` показывает ID, Псевдоним, почту и Роли: каждая Роль — отдельный тег. Сортировать можно
по ID и Псевдониму. Поля «ID», «Сообщество (ID)», «Класс (ID)» и «Соревнование (ID)» принимают число после удаления
пробелов по краям; иначе поле получает ошибку «Введите число», и таблица не меняется. Псевдоним и почта передаются
без изменений, пустое поле не задаёт условия. Клик по строке открывает карточку `ServiceUserView`.
Отсутствующий или нечисловой идентификатор в адресе карточки открывает экран отсутствующей страницы.

Карточка показывает сведения Пользователя в нередактируемых полях. Поле Кода-доступа заполняется из Пользователя,
прочитанного при открытии карточки, и скрывает значение по разделу
[«Скрытые значения»](../components/README.md#скрытые-значения) components/README.md. «Копировать» записывает
в буфер значение этого поля и не читает Пользователя заново. Код-доступа не передаётся в адресе и не записывается
страницами в журнал.

## Версии TRIK Studio в служебном кабинете

Служебная главная открывает маршрут `trik-studio-versions` действием «Версии TRIK Studio». Страница требует
`ROLE_SERVICE` и работает с версиями через `ServiceTrikStudioVersions` общего родителя; его устройство описано
в разделе [«Служебная регистрация версий TRIK Studio»](../../testsys-infra/database/README.md#служебная-регистрация-версий-trik-studio)
database/README.md. Требования — в
[testsys.dev.service.trikStudioVersions](../../docs/domain/features.md#testsysdevservicetrikstudioversions-implemented).

Таблица `ServiceTrikStudioVersionsView` показывает номер версии и тег образа моноширинным шрифтом в порядке тегов.
«Добавить» в шапке блока открывает диалог с полями «Версия» и «Тег образа»; каждое открытие начинается с пустых полей.
После регистрации диалог закрывается, страница показывает уведомление и перечитывает таблицу. Отказ оставляет диалог
открытым и показывает причину ошибкой на поле, к которому она относится.

## Логирование

Требования описаны в фичах `testsys.dev.logging.calls` и `testsys.dev.logging.navigation`
в [features.md](../../docs/domain/features.md#testsysdevloggingcalls-implemented). Правила записи в лог из кода —
в разделе «Логирование» [code-style.md](../../docs/project/code-style.md#логирование).

### Вызовы

Два инфраструктурных advisor-а пишут вызовы на уровне DEBUG через `CallLoggingInterceptor`:

| Advisor | Где объявлен | Что оборачивает | Порядок |
|---------|--------------|-----------------|---------|
| `callLoggingAdvisor` | `MainWebConfiguration` | Те же методы прокси-сервисов, что и `operationMetricsAdvisor` | `HIGHEST_PRECEDENCE + 1`: внутри метрик, снаружи повтора транзакций |
| `infrastructureCallLoggingAdvisor` | `InfraConfiguration` в общем родителе | Бины классов из пакетов `tech.testsys.infra.*`, которые реализуют порт из `tech.testsys.domain.contract` или `SettingsService`, `SettingsStore`, `GradingNodeStore`, `GradingNodeRegistry`, `ServiceUserSearch`, `ServiceTrikStudioVersions`; оборачиваются только методы с именами из этих интерфейсов | `HIGHEST_PRECEDENCE`: снаружи транзакции и её повторов |

Вызов, повторённый после конфликта транзакций, записывается один раз. Вложенный вызов другого обёрнутого бина
записывается отдельно.

Каждый вызов даёт две строки в логгере класса цели. Первая — `call <Класс>.<метод>(<аргументы>)`, вторая —
`done <Класс>.<метод> in N ms: <результат>` или `fail <Класс>.<метод> in N ms: <ошибка>`. Суффикс Kotlin-имени
метода с value-классами отбрасывается. При `OperationException` записывается ошибка операции, при другом исключении —
только полное имя его класса, без сообщения. Исключение пробрасывается без изменений. Если DEBUG выключен,
`CallLoggingInterceptor` сразу передаёт вызов дальше и строк не строит.

Advisor-ы создают прокси наследованием класса. Бин, класс которого закрыт для наследования (например, класс без
аннотации Spring, объявленный `@Bean`-методом), объявляется с `@Proxyable(ProxyType.INTERFACES)`, и advisor оборачивает
его через интерфейс. Так объявлены бины `SettingsCoordinator` и `GradingNodeRegistryCoordinator`.

### Маскирование

Аргументы, результат и ошибку записывает `LoggedValues`:

| Значение | Запись |
|----------|--------|
| Аргумент, имя параметра которого содержит `password`, `token`, `secret` или `code` без учёта регистра | `<имя>=***` |
| Строковый аргумент, если имена параметров недоступны | `***` |
| Строковый результат | `***`: `UserService.confirmRegistration` и `UserService.confirmAccessRestoration` возвращают Код-доступа |
| `AccessTokenHash`, `InviteCodeHash` | `***`: при `HashAlgorithm.Identity` в них хранится исходный код |
| `FileData` | Имя и размер файла; для хранимого файла — имя и вид хранилища |
| `ByteArray`, `InputStream` | `byte[N]`, `InputStream` |
| Доменная сущность | `<Класс>(id=…)` |
| Коллекция, `Map`, массив | Первые 10 элементов по тем же правилам и `… (N total)` |
| Остальное | `toString()`, в котором значение поля с теми же словами в имени заменено на `***`, в том числе `Тип(…)` с одним уровнем вложенных скобок; строка длиннее 500 символов обрезается |

Маска может скрыть и безопасное значение, например `statusCode`. Почта и Псевдоним не маскируются.

### Контекст запроса и переходы

`RequestLogContext` основного интерфейса регистрируется как `VaadinRequestInterceptor`. В начале каждого запроса
Vaadin он кладёт в MDC ключ `user` с ID вошедшего Пользователя из `CabinetPrincipal` и ключ `route` с адресом
страницы, открытой в `UI` запроса. У гостя ключа `user` нет. В конце запроса оба ключа удаляются. В начале запроса
сессия Vaadin ещё не найдена, поэтому адрес страницы хранится в атрибуте HTTP-сессии `testsys.route.<ID UI>`.
ID `UI` берётся из параметра `v-uiId`, а у запросов скачивания и загрузки файла — из пути
`VAADIN/dynamic/resource/<ID UI>/…`. Атрибут удаляется при отсоединении `UI`.

Перед входом на страницу `RequestLogContext` заново берёт ID вошедшего Пользователя для `user` и кладёт в `route`
адрес открываемой страницы без параметров запроса. Поэтому вызовы при построении страницы и её фоновая работа
пишутся с новым адресом, а после входа и перехода в одном запросе — с ID Пользователя. После перехода
`RequestLogContext` пишет на уровне DEBUG строку `Opened <адрес> (<класс страницы>)` и сохраняет адрес в атрибуте
HTTP-сессии. Действия на странице попадают в лог через вызовы
прокси-сервисов: Spring AOP не перехватывает лямбды DSL страниц. Push работает в режиме по умолчанию
`WEBSOCKET_XHR`, поэтому запросы клиента идут через XHR и проходят `RequestLogContext`. Фоновая работа страницы
получает MDC через `TaskDecorator`, см. раздел [Безопасность](#безопасность).

`logging.pattern.correlation` в `application.yml` добавляет в каждую строку лога `[<user> <route>]`; вне запроса
основного интерфейса выводится `[- -]`. Служебный интерфейс MDC не заполняет.

### Включение

DEBUG по умолчанию выключен. Конфигурация лога `src/main/resources/logback.xml` выводит строки в консоль в формате
Spring Boot на уровне INFO и добавляет логгеры из необязательного файла `/etc/testsys/logging/loggers.xml`.
Если задано свойство `logging.file.name`, она пишет те же строки в файл с ротацией Spring Boot
`logging.logback.rollingpolicy.*`. Условие проверяет системное свойство JVM `LOG_FILE`, которое Spring Boot
выставляет из `logging.file.name`.
Logback проверяет этот файл раз в 10 секунд и после изменения перечитывает всю конфигурацию, поэтому уровень
меняется без перезапуска. Файл содержит элементы `<logger>` внутри `<included>`. В окружениях Compose это
`deploy/config/logging/loggers.xml`, см. раздел «Логирование» в [deploy/README.md](../../deploy/README.md#логирование).

При перечитывании Logback сбрасывает уровни, заданные свойствами `logging.level.*`, поэтому в окружениях Compose
уровни задаются только в `loggers.xml`. Расширения Spring Boot вроде `<springProfile>` в конфигурации
не работают. Формат строк и `logging.pattern.correlation` Spring Boot передаёт через системные свойства JVM,
поэтому перечитывание их не меняет. Если файл после правки некорректен, Logback продолжает работать
с последней корректной конфигурацией и не пишет об ошибке в лог. Исправленный файл снова применяется.

Без файла `loggers.xml`, например при локальном запуске, уровень задаётся без пересборки свойством
`logging.level.<логгер>` в конфигурационном файле. Переменной окружения, например
`LOGGING_LEVEL_TECH_TESTSYS_WEB_APP=DEBUG`, задаётся только уровень пакета: Spring Boot приводит её имя к нижнему
регистру, поэтому логгер класса задаётся в конфигурационном файле.

| Логгер | Что включает |
|--------|--------------|
| `tech.testsys.web.app.service` | Вызовы прокси-сервисов |
| `tech.testsys.web.app.config.RequestLogContext` | Переходы между страницами |
| `tech.testsys.web.app` | Вызовы прокси-сервисов и переходы |
| `tech.testsys.infra` | Вызовы инфраструктуры; уровень можно задать и для одного модуля, например `tech.testsys.infra.grpc` |

`LoggedValuesTests`, `CallLoggingInterceptorTests` и `RequestLogContextTests` проверяют маскирование, строки вызовов
и контекст запроса. `ServicesContextTests` проверяет порядок advisor-ов у прокси-сервисов и инфраструктурных бинов,
`SecurityConfigurationTests` — передачу MDC фоновой работе и адрес перехода после выхода.

## Метрики

Требования к метрикам — в разделе `testsys.dev.metric` в [features.md](../../docs/domain/features.md#testsysdevmetricexport-implemented).
Собственные метрики TestSys, их метки и правила учёта технических сбоев описаны
в [monitoring/README.md](../../testsys-infra/monitoring/README.md). Этот раздел описывает реестр, HTTP-метрики, учёт
в интерфейсах и сервер экспорта. Prometheus, Grafana и дашборды описаны в разделе «Мониторинг»
в [deploy/README.md](../../deploy/README.md#мониторинг), доступ к ним — в разделе «Метрики и мониторинг»
в [deployment.md](../../docs/project/deployment.md#метрики-и-мониторинг).

### Реестр

`SharedInfrastructureConfiguration` подключает в родителе `MetricsAutoConfiguration`,
`CompositeMeterRegistryAutoConfiguration`, `PrometheusMetricsExportAutoConfiguration` и `ObservationAutoConfiguration`.
Реестр Prometheus и `ObservationRegistry` создаются один раз, дочерние контексты используют их из родителя.
[MetricsConfiguration](src/main/kotlin/tech/testsys/web/monitoring/MetricsConfiguration.kt) настраивает реестр:

- `http.server.requests` публикует корзины 10, 25, 50, 100, 250 и 500 мс, 1, 2,5, 5, 10 и 30 с; клиентские
  перцентили не вычисляются, 95-й перцентиль строится по корзинам;
- после 100 разных значений `uri` новые ряды `http.server.requests` отбрасываются;
- long task timer наблюдений не создаются;
- наблюдения `spring.security.*` отключены, запрос наблюдается один раз на уровне веба.

Метрики JVM, Tomcat и HikariCP не подключены.

`management.prometheus.metrics.export.properties` в `application.yml` включает свойство client_java
`io.prometheus.exporter.include_created_timestamps`: текстовый экспорт счётчиков и гистограмм содержит время
их регистрации в рядах `_created`. Использование этого времени при подсчёте первого события описано в разделе
[«Сбор метрик»](../../deploy/README.md#сбор-метрик) deploy/README.md.

### HTTP-запросы

`WebAutoConfiguration` подключает `WebMvcObservationAutoConfiguration` в основном и служебном контекстах. Фильтр
наблюдения охватывает все запросы контекста, включая фильтры безопасности и сервлет Vaadin. Метрика
`http.server.requests` получает метки `method`, `status`, `outcome` (группа кода), `exception` (простое имя класса
исключения, вышедшего в контейнер, или `none`), `uri` и `interface`. Конвенция
[WebRequestObservationConvention](src/main/kotlin/tech/testsys/web/monitoring/WebRequestObservationConvention.kt)
добавляет `interface` — `main` из `MainWebConfiguration` или `service` из `ServiceMetricsConfiguration` — и заменяет
`uri`. Пути в таблице указаны без context path основного интерфейса.

| Запрос | `uri` |
|--------|-------|
| Путь `/VAADIN/push` и пути под ним | `/VAADIN/push` |
| Параметр `v-r` в строке запроса: `uidl`, `heartbeat`, `init`, `push` | `/?v-r=<вид>` |
| Другое значение `v-r` | `/?v-r=other` |
| `/VAADIN/dynamic/**` — скачивание файлов | `/VAADIN/dynamic/**` |
| Остальные пути `/VAADIN/**` | `/VAADIN/**` |
| `/testsys-ui/**` — ресурсы компонентов | `/testsys-ui/**` |
| Путь страницы своего интерфейса | Шаблон маршрута без шаблонов значений: `/developer/tasks/:taskId`, `/admin/users/:userId/:section?` |
| Прочее с кодом 404 | `NOT_FOUND` |
| Прочее с кодом 3xx | `REDIRECTION` |
| Прочее | `OTHER` |

Шаблоны конвенция берёт из реестра маршрутов Vaadin своего servlet-контекста вместе с алиасами. Значение параметра
в пути должно подходить под шаблон значения из маршрута. Если путь подходит к нескольким маршрутам, выбирается
маршрут с большим числом литеральных сегментов. Строку запроса конвенция читает только для `v-r` и не разбирает тело
запроса. Первый запрос страницы учитывается с её шаблоном и кодом ответа, который Vaadin определяет до отправки HTML
благодаря `vaadin.eagerServerLoad`. Переходы без перезагрузки и действия на странице идут запросами `/?v-r=uidl`
с кодом 200.

Дашборды в `deploy/config/grafana/dashboards/` используют метки `interface`, `uri`, `status` и `outcome`.
Если эти метки или их значения меняются, обновите и дашборды.

### Ошибки операций и технические сбои

`MainWebConfiguration` регистрирует инфраструктурный advisor на `@Service`-классах пакета `service` с порядком
`HIGHEST_PRECEDENCE`, снаружи advisor повтора транзакций. Его `OperationMetricsInterceptor` учитывает только самый
внешний вызов прокси-сервиса и только после всех повторов. `OperationException` увеличивает
`testsys.operation.errors` с меткой `operation` вида `<Сервис>.<метод>`; суффикс Kotlin-имени метода с value-классами
отбрасывается. Любое другое исключение учитывается как технический сбой `main`/`service`. Исключение в обоих случаях
пробрасывается без изменений; успешные вызовы не считаются. Если страница сама обрабатывает `OperationException`,
отказ всё равно учтён один раз на границе сервиса.

[WebFailures](src/main/kotlin/tech/testsys/web/monitoring/WebFailures.kt) учитывает сбои страниц интерфейса до показа
уведомления:

| Где | Этап |
|-----|------|
| `OperationErrorHandler` основного и `ServiceErrorHandler` служебного интерфейса: обработчики событий и задачи `UI.access` | `event` |
| `InternalErrorView` основного и `ServiceInternalErrorView` служебного интерфейса: открытие страницы | `navigation` |

`WebFailures` не учитывает исключение, в цепочке причин которого есть `OperationException`, и обрыв соединения клиентом
из списка `DefaultErrorHandler.SOCKET_EXCEPTIONS`. Исключение, уже учтённое на границе сервиса, обработчик повторно
не считает. Не учитываются исключения, которые компоненты перехватывают сами: при фоновой загрузке блока страницы
и в производителях файлов для скачивания, если исключение не прошло через прокси-сервис. Исключение, вышедшее
в servlet-контейнер вне Vaadin, видно только как HTTP-ответ 5xx с меткой `exception`.

`OperationsConfiguration` передаёт диспетчеру валидации обработчик, который учитывает технический сбой
`validation`/`processing`.

### Сервер метрик

Третий дочерний контекст [MetricsWebConfiguration](src/main/kotlin/tech/testsys/bootstrap/MetricsWebConfiguration.kt)
запускает Tomcat со Spring MVC и Actuator без Vaadin, Spring Security и сессий. Он отдаёт только
`GET /actuator/prometheus` в формате, согласованном по заголовку `Accept`: без него — текстовый Prometheus,
с `application/openmetrics-text;version=1.0.0` — OpenMetrics 1.0.0. Индекс `/actuator`, статические ресурсы и отдельный
management-сервер выключены; `management.server.port` в этом контексте сбрасывается. Не задавайте
`management.server.address` и `management.server.ssl.enabled=true`: с ними сервер метрик не запустится.
Запросы к серверу метрик не наблюдаются. Основной и служебный интерфейсы Actuator не подключают.

[MetricsWebSettings](src/main/kotlin/tech/testsys/bootstrap/MetricsWebSettings.kt) проверяется до создания
инфраструктуры:

| Свойство | Назначение |
|----------|------------|
| `testsys.metrics.address` | Необязательный адрес сервера метрик; по умолчанию `0.0.0.0` — все интерфейсы |
| `testsys.metrics.port` | Обязательный порт сервера метрик: `8082` в `application.yml`, `0` в тестах |

Приложение не ограничивает доступ к метрикам. Изоляцию порта метрик от внешнего доступа обеспечивает развёртывание,
например внутренняя сеть Docker, из которой Prometheus читает метрики. Если сервер метрик не запустился, например
порт занят, `TestSysRuntime` закрывает оба интерфейса и родителя, и процесс не запускается.

### Проверки метрик

- `MetricsHttpTests` запускает настоящую иерархию и по HTTP проверяет метки `uri` и `interface` страниц, служебных
  запросов Vaadin, ресурсов и отсутствующих адресов, корзины длительности запроса, отсутствие значений параметров
  в выдаче, чтение метрик без cookie и недоступность `/actuator/prometheus` на основном и служебном портах.
- `MetricsConfigurationTests` проверяет фильтры реестра: корзины длительности, предел значений `uri`, запрет long task
  timer и пропуск наблюдений Spring Security.
- `WebRequestObservationConventionTests` и `VaadinRouteTemplatesTests` проверяют нормализацию путей.
- `OperationMetricsInterceptorTests`, `OperationErrorHandlerTests`, `InternalErrorViewTests`, `ServiceErrorHandlerTests`
  и `ServiceInternalErrorViewTests` проверяют учёт ошибок операций и сбоев, в том числе однократный учёт исключения,
  которое прошло границу сервиса и дошло до обработчика.
- `OperationsConfigurationTests` проверяет учёт сбоя обработки запроса валидации с источником `validation`
  и этапом `processing`.
- `MetricsPrivacyTests` основного и служебного интерфейсов проверяют, что Код-доступа и код служебного входа
  не попадают в выдачу Prometheus.

Служебным тестам без родителя реестр и `TechnicalFailures` подставляет `ServiceMetricsTestConfiguration`.
