# deploy

Документ описывает, как устроены Docker-образ основного приложения `testsys-web:app`, Compose-файлы окружений
dev, e2e и production, мониторинг на Prometheus и Grafana, файл Проверяющего узла и скрипт `deploy/testsys.sh`.
Он предназначен для тех, кто меняет эти файлы. Как запускать окружения, открывать Grafana и что задать перед
запуском — в [deployment.md](../docs/project/deployment.md); смысл свойств приложения — в документах модулей; имена
и метки метрик TestSys — в [monitoring/README.md](../testsys-infra/monitoring/README.md).

## Устройство

- **Образ.** `Dockerfile` собирает образ приложения без настроек окружений и секретов.
- **Окружение** — проект Compose `testsys-<окружение>`. Его основа — общий `deploy/compose.yml` (приложение
  и PostgreSQL), поверх которого подключается override окружения `deploy/compose.<окружение>.yml`. Dev и e2e
  добавляют Mailpit, dev — ещё загрузку начальных данных.
- **Мониторинг** — Prometheus и Grafana из `deploy/compose.monitoring.yml`. Этот файл входит в проекты dev
  и production между общим файлом и override; в e2e мониторинга нет.
- **Проверяющий узел** описан отдельным файлом `deploy/compose.grading-node.yml`. Его подключают к окружению
  или запускают отдельным проектом, в том числе на другой машине.
- **Переменные Compose** — порты, адреса, секреты, настройки узла — лежат в файлах переменных `deploy/env/`.
- **Конфигурационные файлы** приложения, Prometheus и Grafana лежат в `deploy/config/`, Compose монтирует их
  в контейнеры.
- **Скрипт** `deploy/testsys.sh` собирает из этих файлов команду Compose.

## Файлы

Образ:

| Файл | Назначение |
|------|------------|
| `Dockerfile` | Сборка образа приложения, см. [Образ приложения](#образ-приложения) |
| `.dockerignore` | Исключения из контекста сборки образа |
| `.gitattributes` | Окончания строк LF для скриптов `deploy/**/*.sh` и файлов `deploy/env/*` в любом checkout |

Compose-файлы:

| Файл | Назначение |
|------|------------|
| `deploy/compose.yml` | Общие сервисы всех окружений: приложение и PostgreSQL; отдельно не запускается |
| `deploy/compose.monitoring.yml` | Prometheus и Grafana проектов dev и production, см. [Мониторинг](#мониторинг); отдельно не запускается |
| `deploy/compose.dev.yml` | Override проекта `testsys-dev`: пароли, порты, адрес Grafana, Mailpit, начальные данные |
| `deploy/compose.e2e.yml` | Override проекта `testsys-e2e` для внешнего тестового процесса |
| `deploy/compose.prod.yml` | Override проекта `testsys-prod`: конфигурационный файл на сервере, секреты, пароль и адрес Grafana, политика перезапуска |
| `deploy/compose.grading-node.yml` | Проверяющий узел, см. [Проверяющий узел](#проверяющий-узел) |

Скрипты:

| Файл | Назначение |
|------|------------|
| `deploy/testsys.sh` | Команды окружений и узла, см. [Скрипт](#скрипт) |
| `deploy/grading-node/entrypoint.sh` | Стартовые проверки Проверяющего узла, см. [Проверяющий узел](#проверяющий-узел) |
| `deploy/db-seed/entrypoint.sh` | Применение скрипта начальных данных dev, см. [Начальные данные dev](#начальные-данные-dev) |

Файлы переменных, см. [Файлы переменных](#файлы-переменных):

| Файл | Назначение |
|------|------------|
| `deploy/env/dev.env`, `deploy/env/e2e.env` | Переменные dev и e2e, хранятся в Git |
| `deploy/env/dev.local.env.example`, `deploy/env/e2e.local.env.example` | Шаблоны значений машины для dev и e2e |
| `deploy/env/prod.env.example` | Шаблон файла переменных production |
| `deploy/env/grading-node.env.example` | Шаблон файла переменных отдельного Проверяющего узла |

Конфигурация приложения, см. [Конфигурация приложения](#конфигурация-приложения):

| Файл | Назначение |
|------|------------|
| `deploy/config/application.yml` | Настройки приложения, общие для всех окружений |
| `deploy/config/application-dev.yml`, `deploy/config/application-e2e.yml` | Параметры приложения в dev и e2e |
| `deploy/config/application-prod.example.yml` | Шаблон параметров приложения в production |

Конфигурация мониторинга, см. [Мониторинг](#мониторинг):

| Файл | Назначение |
|------|------------|
| `deploy/config/prometheus.yml` | Интервал сбора, протокол опроса и адрес сервера метрик TestSys |
| `deploy/config/grafana/provisioning/datasources/prometheus.yaml` | Источник данных Prometheus с `uid` `testsys-prometheus` |
| `deploy/config/grafana/provisioning/dashboards/testsys.yaml` | Загрузка дашбордов из `/etc/grafana/dashboards` в папку «TestSys» |
| `deploy/config/grafana/dashboards/*.json` | Дашборды, см. [Дашборды](#дашборды) |

## Образ приложения

Сервис `app` собирается из корня репозитория: в `deploy/compose.yml` задано `build: ..`. Сборочный этап
на `eclipse-temurin:21-jdk-noble` выполняет `:testsys-web:app:bootJar` с `-Pdetekt.autoCorrect=false`. Кэш Gradle
(`~/.gradle`) и Node.js, который устанавливает плагин Vaadin (`~/.vaadin`), сохраняются между сборками в cache mount
BuildKit. Runtime-этап на `eclipse-temurin:21-jre-noble` запускает jar командой `java -jar /app/app.jar`
от пользователя `testsys` (UID и GID `10001`). Один процесс обслуживает три HTTP-сервера, и образ открывает их
порты: `8080` — основной интерфейс, `8081` — служебный интерфейс, `8082` — сервер метрик. Как приложение запускает
эти серверы — в разделе «Служебный контекст» в [app/README.md](../testsys-web/app/README.md#служебный-контекст).

Каталог `/var/lib/testsys/files` принадлежит пользователю `testsys`; Compose монтирует в него том `files`.
В production у сервиса `app` есть ещё `image: ${TESTSYS_APP_IMAGE:-testsys-app:latest}` из `deploy/compose.prod.yml`,
поэтому собранный образ получает это имя. В dev и e2e образ называется `testsys-<окружение>-app`.

`.dockerignore` исключает из контекста историю Git, локальные результаты сборки и кэши, `.env`, файлы ключей,
рабочие планы, документацию, каталог `deploy/` и настройки инструментов разработки. Файлы, нужные Gradle, Vaadin
и сборке frontend, остаются. В checkout на Windows `gradlew` может иметь окончания строк CRLF, поэтому сборочный
этап перед запуском заменяет их в этом файле на LF.

## Скрипт

Скрипт переходит в каталог `deploy/` и вызывает Compose с именем проекта `testsys-dev`, `testsys-e2e`, `testsys-prod`
или `testsys-grading-node`, файлами переменных и Compose-файлами окружения. Каталог проекта Compose — `deploy/`,
каталог первого Compose-файла. Для dev и e2e команда выглядит так. Файл `<окружение>.local.env` передаётся, только
если он есть; файл мониторинга — только для dev, но в каждой команде; файл узла — только с `--node`:

```bash
docker compose -p testsys-<окружение> --env-file env/<окружение>.env [--env-file env/<окружение>.local.env] -f compose.yml [-f compose.monitoring.yml] -f compose.<окружение>.yml [-f compose.grading-node.yml] <команда>
```

Production получает один файл переменных `--env-file <путь>` вместо файлов из `env/` и, как dev, файл мониторинга
в каждой команде. Отдельный узел вызывается как
`docker compose -p testsys-grading-node --env-file <путь> -f compose.grading-node.yml <команда>`.

Override идёт после файла мониторинга, потому что дополняет его сервисы: задаёт адрес Grafana, а в production ещё
пароль администратора Grafana и политику перезапуска. Без `-f compose.monitoring.yml` Compose не принимает модель
dev и production: сервис `grafana` в override не имеет образа. Поэтому прямой вызов Compose для этих окружений тоже
передаёт файл мониторинга. Так `up` и `down` с `--remove-orphans` не удаляют Prometheus и Grafana, а `clean`
удаляет и их тома.

Команда `up` в каждом окружении:

| Окружение | Команда Compose | Когда завершается |
|-----------|-----------------|-------------------|
| `dev` | `up -d --build --remove-orphans` | После завершения `db-seed`; код берётся из `ps -a`, журнал последнего запуска — из `docker logs --since` |
| `e2e`, `prod` | `up -d --build --remove-orphans --wait` | Когда сервисы станут `healthy` или, если у сервиса нет healthcheck, запущенными |
| `node` | `up -d --remove-orphans` | Сразу после запуска контейнера |

`up` и `down` получают `--remove-orphans`, поэтому команда без `--node` удаляет контейнер узла, запущенный
ранее с `--node`. С `--no-build` скрипт проверяет `docker image inspect` для образа приложения из
`config --images app` и выполняет `up` с `--no-build` вместо `--build`. Флаг `--pull never` не используется,
чтобы Compose мог скачать отсутствующие образы других сервисов.

Неизвестные команды и опции завершают скрипт с кодом `2`: произвольные команды Compose он не передаёт.

## Файлы переменных

Dev и e2e читают сначала `<окружение>.env`, затем `<окружение>.local.env`. Значение из локального файла перекрывает
значение из отслеживаемого, а переменная оболочки — оба. Относительные пути в значениях переменных Compose
отсчитывает от `deploy/`. Файл production и файл отдельного узла скрипт получает из `--env-file <путь>` или
`TESTSYS_ENV_FILE` и превращает относительный путь в абсолютный от каталога, из которого запущен скрипт.

`:?` в Compose стоит только в override-файлах и в файле узла: Compose подставляет переменные в каждый файл до
слияния. Поэтому `:?` в `deploy/compose.yml` сработал бы во всех окружениях, а в `deploy/compose.monitoring.yml` —
и в dev, и в production.

Git Bash на Windows при запуске `docker` преобразует значения переменных оболочки, похожие на пути POSIX,
в пути Windows: `/run/desktop/mnt/host/c/<путь>` превращается в `C:/Program Files/Git/run/desktop/mnt/host/c/<путь>`.
Скрипт отключает это преобразование для переменных `TESTSYS_*` переменной `MSYS2_ENV_CONV_EXCL=TESTSYS_`. Значения
из файлов переменных читает сам Compose, они не преобразуются.

## Конфигурация приложения

`deploy/compose.yml` задаёт приложению переменную `SPRING_CONFIG_IMPORT`. Spring Boot импортирует два файла,
смонтированных в контейнер только для чтения:

| Файл в контейнере | Источник | Содержимое |
|-------------------|----------|------------|
| `/etc/testsys/application.yml` | `deploy/config/application.yml`, монтирует `deploy/compose.yml` | Шесть путей `testsys.file-storage.paths.<вид>` |
| `/etc/testsys/application-environment.yml` | Файл окружения, монтирует override | Параметры операций `testsys.operation.*`; в production также настройки почты |

Пути файлов указывают на подкаталоги `/var/lib/testsys/files/<вид>` тома `files`. Виды и смысл путей описаны
в разделе «Хранение файлов» в [database/README.md](../testsys-infra/database/README.md#хранение-файлов),
параметры операций — в разделе «Запуск и проверка» в
[app/README.md](../testsys-web/app/README.md#запуск-и-проверка), настройки почты — в разделе «Настройки»
в [mail/README.md](../testsys-infra/mail/README.md#настройки). Ключи двух файлов не пересекаются.

Импорт не помечен `optional:`, поэтому без любого из файлов приложение не запустится. Переменные окружения
перекрывают значения из импортированных файлов, а импортированные файлы перекрывают значения по умолчанию модулей.

Переменными окружения Compose передаёт секреты, адреса, которые зависят от состава сервисов, адреса и порты
HTTP-серверов внутри контейнера, флаг импорта Проверяющих узлов и режим заголовков HTTPS-прокси:

| Переменная приложения | Где задана | Свойство |
|-----------------------|------------|----------|
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` | `deploy/compose.yml`: база `testsys` сервиса `postgres`, пользователь `testsys` | `spring.datasource.url`, `username` |
| `SPRING_DATASOURCE_PASSWORD` | Override окружения | `spring.datasource.password` |
| `TESTSYS_SERVICE_ADDRESS`, `TESTSYS_SERVICE_PORT` | `deploy/compose.yml`: `0.0.0.0`, `8081` | `testsys.service.address`, `port` |
| `TESTSYS_METRICS_PORT` | `deploy/compose.yml`: `8082`; этот порт — цель Prometheus, см. [Сбор метрик](#сбор-метрик) | `testsys.metrics.port` |
| `TESTSYS_SERVICE_SECRET` | `deploy/compose.dev.yml`, `deploy/compose.e2e.yml`: фиксированное значение; `deploy/compose.prod.yml` из переменной Compose `TESTSYS_SERVICE_SECRET` | `testsys.service.secret` |
| `TESTSYS_SERVICE_GRAFANAURL` | Override окружения из переменной Compose `TESTSYS_GRAFANA_URL`, см. [Grafana](#grafana) | `testsys.service.grafana-url` |
| `TESTSYS_MAIL_HOST`, `TESTSYS_MAIL_PORT` | `deploy/compose.dev.yml`, `deploy/compose.e2e.yml`: `mailpit`, `1025` | `testsys.mail.host`, `port` |
| `TESTSYS_MAIL_PASSWORD` | `deploy/compose.prod.yml` | `testsys.mail.password` |
| `TESTSYS_OPERATION_SUPERVISOR_ACCESS_TOKEN` | `deploy/compose.prod.yml` из переменной Compose `TESTSYS_SUPERVISOR_ACCESS_TOKEN` | `testsys.operation.supervisor.access-token` |
| `TESTSYS_GRADING_NODES` | Override окружения из переменной Compose `TESTSYS_GRADING_NODES`; в production она необязательна | `testsys.grading.nodes`, формат — в разделе «Настройки» в [grpc/README.md](../testsys-infra/grpc/README.md#настройки) |
| `TESTSYS_GRADING_IMPORT_NODES` | `deploy/compose.dev.yml`, `deploy/compose.e2e.yml`: `true`; `deploy/compose.prod.yml` из переменной Compose `TESTSYS_GRADING_IMPORT_NODES`, по умолчанию `false` | `testsys.grading.import-nodes`, действие — в разделе «Реестр узлов» в [grpc/README.md](../testsys-infra/grpc/README.md#реестр-узлов) |
| `SERVER_FORWARDHEADERSSTRATEGY` | `deploy/compose.prod.yml` | `server.forward-headers-strategy`; действует только на основной интерфейс, служебный контекст и сервер метрик задают `none` |

Свойства, которых нет ни в файлах, ни в переменных, сохраняют значения по умолчанию из документов модулей.

Адрес и порт служебного сервера и порт сервера метрик уже заданы в `application.yml` внутри jar.
`deploy/compose.yml` задаёт порты переменными окружения, которые перекрывают значения из файлов конфигурации.
Поэтому опубликованные порты и healthcheck не зависят от значений по умолчанию jar.

Служебный сервер по умолчанию слушает `127.0.0.1`. В контейнере это собственный loopback контейнера, и соединения
с опубликованного порта до сервера не доходят, поэтому `deploy/compose.yml` задаёт адрес `0.0.0.0`. Сервер метрик
по умолчанию слушает `0.0.0.0`, его адрес Compose не задаёт.

### Публикация портов

`deploy/compose.yml` публикует три порта приложения только на `127.0.0.1` хоста. Порты хоста задают переменные
Compose; их значения в dev и e2e — в `dev.env` и `e2e.env`.

| Порт в контейнере | Сервер | Переменная Compose | По умолчанию |
|-------------------|--------|--------------------|--------------|
| `8080` | Основной интерфейс | `TESTSYS_HTTP_PORT` | `8080` |
| `8081` | Служебный интерфейс | `TESTSYS_SERVICE_HTTP_PORT` | `8081` |
| `8082` | Сервер метрик | `TESTSYS_METRICS_HTTP_PORT` | `8082` |

В production HTTPS-прокси обслуживает только основной интерфейс. К служебному серверу обращаются напрямую: он
игнорирует forwarded-заголовки и выдаёт cookie без `Secure`, см. раздел «Конфигурация запуска»
в [app/README.md](../testsys-web/app/README.md#конфигурация-запуска).

### Готовность приложения

Healthcheck сервиса `app` проверяет, что порты `8080`, `8081` и `8082` внутри контейнера принимают TCP-соединения:
`bash -c ': </dev/tcp/127.0.0.1/8080 && : </dev/tcp/127.0.0.1/8081 && : </dev/tcp/127.0.0.1/8082'`.

`TestSysRuntime` сначала запускает общий контекст инфраструктуры. В нём применяются миграции Liquibase,
`PublicCommunityBootstrap` проверяет Публичное Сообщество, а `SupervisorBootstrap` — Супервайзера. Затем
`TestSysRuntime` по очереди запускает основной, служебный и метрический контексты, и каждый открывает свой порт;
порт метрик открывается последним. Если любой контекст не запустился, `TestSysRuntime` закрывает уже запущенные,
и процесс завершается. Поэтому статус `healthy` означает, что миграции применены и все три сервера принимают
запросы.

Пока проверка ни разу не прошла, но не дольше пяти минут (`start_period`), Docker проверяет порты каждые 2 секунды
(`start_interval`) и не учитывает неудачи. После первой успешной проверки или по истечении пяти минут он проверяет
порты каждые 10 секунд и после трёх неудач подряд помечает контейнер `unhealthy`.

## Начальные данные dev

Сервис `db-seed` в `deploy/compose.dev.yml` работает на образе `postgres:17-alpine` и запускает
`deploy/db-seed/entrypoint.sh` при каждом `up`. Он зависит от `app` с условием `service_healthy`, поэтому стартует
после миграций Liquibase, создания Публичного Сообщества и проверки Супервайзера в общем контексте инфраструктуры,
см. [Готовность приложения](#готовность-приложения). `psql` подключается к базе
`testsys` от пользователя `testsys`. Каталог данных образа postgres смонтирован в `tmpfs`, чтобы `db-seed`
не оставлял анонимный том.

Скрипт из `TESTSYS_DB_SEED_SCRIPT` монтируется как `/opt/testsys/db-seed.sql`. `db-seed` действует так:

1. Без переменной Compose монтирует вместо скрипта `/dev/null`, и `db-seed` сообщает
   `TESTSYS_DB_SEED_SCRIPT is not set, nothing to apply`. Если по пути лежит не обычный файл, например каталог,
   который создал Docker Desktop, `db-seed` завершается с кодом `1`.
2. Если в базе есть таблица `testsys_dev_seed.applied`, скрипт уже применён. `db-seed` сравнивает записанный в ней
   SHA-256 с текущим скриптом. При совпадении он сообщает о пропуске, при расхождении выводит предупреждение.
3. Иначе `psql -X -v ON_ERROR_STOP=1 --single-transaction` создаёт схему `testsys_dev_seed` с таблицей `applied`,
   выполняет скрипт и записывает его SHA-256. При ошибке транзакция откатывается целиком, отметка не появляется,
   и `db-seed` завершается с ненулевым кодом.

## Мониторинг

Сервисы `prometheus` и `grafana` описаны в `deploy/compose.monitoring.yml` и работают в проектах `testsys-dev`
и `testsys-prod` рядом с приложением. Образы закреплены в этом файле: `prom/prometheus` и `grafana/grafana`.
Адреса, учётные записи Grafana и проверка сбора — в разделе «Метрики и мониторинг»
в [deployment.md](../docs/project/deployment.md#метрики-и-мониторинг), устройство сервера метрик — в разделе
«Сервер метрик» в [app/README.md](../testsys-web/app/README.md#сервер-метрик).

### Сбор метрик

Prometheus раз в 30 секунд запрашивает `GET /actuator/prometheus` у сервиса `app` своего проекта по адресу
`app:8082` внутри сети проекта. Порт `8082` — значение `TESTSYS_METRICS_PORT` из `deploy/compose.yml`; порт хоста
`TESTSYS_METRICS_HTTP_PORT` на сбор не влияет. Prometheus не подставляет переменные в `deploy/config/prometheus.yml`,
поэтому при смене `TESTSYS_METRICS_PORT` цель меняется в этом файле. Сервер метрик отдаёт метрики без сессии
и служебного входа, поэтому Prometheus не нужны учётные данные TestSys.

Prometheus хранит метрики 14 дней в томе `prometheus-data`. У Prometheus нет зависимости от `app`: пока приложение
не готово, цель `testsys` остаётся в состоянии `DOWN`.

Опрос использует OpenMetrics 1.0.0. Приложение передаёт время регистрации счётчиков и HTTP-гистограмм в рядах
`_created`; Prometheus с флагом `created-timestamp-zero-ingestion` добавляет в это время нулевую точку.
Так первый замер со значением 1 даёт прирост от 0. Протокол закреплён в `deploy/config/prometheus.yml`:
Protobuf-экспортер используемого client_java 1.7.0 передаёт это время у счётчиков, но пропускает у гистограмм.

Изменённые образ или команду запуска Prometheus применяет `up`: Compose пересоздаёт контейнер и сохраняет том.
Изменённый `deploy/config/prometheus.yml` Prometheus читает только при запуске, поэтому после правки перезапустите
контейнер, например в dev:

```bash
docker restart testsys-dev-prometheus-1
```

### Grafana

При запуске Grafana загружает из `deploy/config/grafana/provisioning/` источник данных Prometheus по адресу
`http://prometheus:9090` и дашборды из `deploy/config/grafana/dashboards/`. Пользователей, пароли и настройки
Grafana хранит в томе `grafana-data`.

| Переменная Grafana | Где задана | Значение |
|--------------------|------------|----------|
| `GF_SECURITY_ADMIN_USER` | `deploy/compose.monitoring.yml` | `admin` |
| `GF_USERS_ALLOW_SIGN_UP`, `GF_AUTH_ANONYMOUS_ENABLED` | `deploy/compose.monitoring.yml` | `false`: самостоятельная регистрация и анонимный доступ выключены |
| `GF_ANALYTICS_REPORTING_ENABLED`, `GF_ANALYTICS_CHECK_FOR_UPDATES`, `GF_ANALYTICS_CHECK_FOR_PLUGIN_UPDATES` | `deploy/compose.monitoring.yml` | `false` |
| `GF_SERVER_ROOT_URL` | `deploy/compose.dev.yml`, `deploy/compose.prod.yml` из переменной Compose `TESTSYS_GRAFANA_URL` | Адрес Grafana в браузере; его путь — префикс Grafana |
| `GF_SERVER_SERVE_FROM_SUB_PATH` | `deploy/compose.monitoring.yml` | `true`: Grafana отвечает по префиксу из `GF_SERVER_ROOT_URL` |
| `GF_SECURITY_ADMIN_PASSWORD` | `deploy/compose.prod.yml` из переменной Compose `TESTSYS_GRAFANA_ADMIN_PASSWORD` | Пароль `admin`; Grafana применяет его, только когда создаёт свою базу в пустом томе |

Без `GF_SECURITY_ADMIN_PASSWORD` в dev администратор получает стандартный пароль Grafana. Тот же
`TESTSYS_GRAFANA_URL` override передаёт приложению в `TESTSYS_SERVICE_GRAFANAURL`, поэтому кнопка «Grafana»
служебного кабинета открывает Grafana своего проекта. Compose не сверяет порт в этом адресе с опубликованным портом
Grafana; их согласует тот, кто задаёт переменные. Префикс в файлах переменных по умолчанию — `/grafana/`; другой
путь в адресе Grafana принимает без изменений Compose-файлов. В e2e Grafana нет, а `TESTSYS_GRAFANA_URL` нужен только
приложению.

Порты Prometheus и Grafana публикуются только на `127.0.0.1` хоста:

| Порт в контейнере | Сервис | Переменная Compose | По умолчанию |
|-------------------|--------|--------------------|--------------|
| `9090` | `prometheus` | `TESTSYS_PROMETHEUS_PORT` | `9090` |
| `3000` | `grafana` | `TESTSYS_GRAFANA_PORT` | `3000` |

В production `deploy/compose.prod.yml` задаёт обоим сервисам `restart: unless-stopped`; в dev политика перезапуска
не задана. Healthcheck у сервисов нет, поэтому `up --wait` в production ждёт только их запуска.

### Дашборды

Grafana загружает дашборды из `deploy/config/grafana/dashboards/` в папку «TestSys». Дашборды обновляются раз
в 30 секунд, период по умолчанию — последние 3 часа. Сохранить изменения дашборда в Grafana нельзя: правьте
JSON-файл в репозитории, Grafana перечитывает его сама. В production Grafana читает файл из checkout на сервере.

| Дашборд | Что показывает |
|---------|----------------|
| «TestSys — HTTP», `testsys-http.json` | Число запросов, группы кодов и отдельные коды ответа, среднее время и 95-й перцентиль; таблица точек входа за период. Фильтр «Интерфейс» выбирает основной или служебный интерфейс |
| «TestSys — ошибки операций и технические сбои», `testsys-failures.json` | Ошибки операций по родам и операциям, технические сбои по источникам, этапам и исключениям, повторы транзакций — отдельными панелями |
| «TestSys — проверка Посылок», `testsys-grading.json` | Результат опроса TestSys; очередь проверки по четырём этапам и общее число незавершённых проверок; узлы по участию и результату опроса; сообщённые узлами очередь и ёмкость; завершённые проверки по исходу |

На каждом дашборде переключатель «Сглаживание» выбирает окно расчёта: «Авто», «1 мин», «5 мин», «10 мин»
или «30 мин». Он влияет на графики частоты HTTP-запросов, среднего времени и 95-го перцентиля ответа,
ошибок операций, технических сбоев, повторов транзакций и завершённых проверок. Очередь, узлы, статус опроса,
итоги и таблицы за выбранный период от этого переключателя не зависят.

По умолчанию выбран режим «Авто»: Grafana использует `$__rate_interval`. При текущем интервале сбора 30 секунд
он равен `max($__interval + 30s, 120s)`. Значение `$__interval` зависит от выбранного периода и ширины графика:
при расширении периода окно обычно растёт, при приближении остаётся не меньше 2 минут.

Фиксированные окна не меняются при выборе периода. Чем больше окно, тем плавнее график и тем слабее видны
короткие пики. Окно «1 мин» при сборе раз в 30 секунд чувствительно к пропускам замеров: для расчёта нужны
хотя бы две точки, поэтому на графике возможны разрывы.

Таблица точек входа показывает для каждой точки входа число запросов, число и долю ответов 4xx и 5xx и 95-й
перцентиль времени ответа за выбранный период. Столбцы сортируются щелчком по заголовку; по умолчанию таблица
отсортирована по доле 5xx.

Коды HTTP-ответов, ошибки операций и технические сбои — разные срезы одних событий, поэтому дашборды их
не складывают. Требование — в [testsys.dev.metric.failures](../docs/domain/features.md#testsysdevmetricfailures-implemented).

Числа за период — округлённый результат `increase`, частоты — результат `rate`. Это оценки по соседним замерам,
а не журнал событий. Ряды счётчиков создаются при первом событии; до него панели итогов показывают 0.
Первый прирост становится виден после успешного опроса, если нулевая точка `_created` и ненулевой замер попадают
в окно расчёта. Для короткого окна, пропущенных опросов и событий у границы периода возможны отличия от точного числа.

При перезапуске TestSys новые счётчики и гистограммы получают новое время регистрации. Prometheus может добавить
новый ноль даже при одинаковых значениях до и после перезапуска. Ноль должен предшествовать замеру и не нарушать
порядок уже сохранённых точек; слишком старое время, рассинхронизация часов или более поздняя точка прежнего ряда
могут помешать его добавлению. События процесса, завершившегося до успешного опроса, восстановить нельзя.
Перезапуск Prometheus с сохранённым томом не создаёт новое событие.

Панель «Опрос TestSys» показывает, отвечает ли сервер метрик: если он недоступен, остальные панели не получают
новых данных.

Автоматические оповещения не настроены.

### Проверка конфигурации

Первое событие четырёх собственных счётчиков, HTTP-ответы 4xx и 5xx и перезапуски проверяет
`PrometheusIngestionTests` в `testsys-web:app`. Тест берёт образ и команду Prometheus из
`deploy/compose.monitoring.yml`, конфигурацию из `deploy/config/prometheus.yml`, в которой заменяет цель `app:8082`
сервером метрик теста, и выражения дашбордов из `deploy/config/grafana/dashboards/`. Он запускает отдельные
контейнеры и не затрагивает проекты и тома окружений:

```bash
./gradlew :testsys-web:app:test --tests 'tech.testsys.bootstrap.PrometheusIngestionTests' -Pdetekt.autoCorrect=false
```

## Проверяющий узел

Файл `deploy/compose.grading-node.yml` не задаёт имя проекта: при двух `name:` побеждает последний файл, и файл узла
переименовал бы проект окружения. Поэтому скрипт использует один файл двумя способами: добавляет его последним `-f`
к командам окружения или запускает отдельным проектом `testsys-grading-node`. Узел слушает gRPC на порту `8080`
только по HTTP/2, порт задан в коде узла. Порт публикуется длинным синтаксисом с `host_ip` и `published`
из переменных; пустой `published` выбирает свободный порт хоста.

Узел запускает каждую проверку в отдельном контейнере `run-<guid>` на демоне Docker своей машины. Для этого в узел
монтируются сокет `/var/run/docker.sock` и рабочий каталог. Образ проверки приходит в каждом запросе: приложение
передаёт строку из столбца `tag` таблицы `ts_trik_studio_version`, а узел подставляет её в `docker run` как имя
образа без изменений.

### Рабочий каталог

Рабочий каталог узла — подкаталог `${TESTSYS_GRADING_NODE_WORKSPACE}/${COMPOSE_PROJECT_NAME}`, поэтому узлы разных
проектов на одной машине пишут в разные подкаталоги. Подкаталог монтируется в узел как `/grading-node-workspace`
с `create_host_path: false`, и то же выражение передаётся в `HOST_DIRECTORY`. Там узел пишет журнал `log<дата>.txt`
и создаёт для каждой проверки подкаталог `tmp-<guid>`, который демон Docker монтирует в контейнер проверки по пути
`HOST_DIRECTORY/tmp-<guid>`. Узел вставляет этот путь в команду `docker run` без кавычек, поэтому в нём не должно
быть пробельных символов.

Перед `up --node` и `node up` скрипт читает `TESTSYS_GRADING_NODE_WORKSPACE` через `config --environment` и проверяет,
что путь абсолютный, без запятых и пробельных символов. Затем он создаёт подкаталог проекта: путь указан
в пространстве имён демона Docker, поэтому `mkdir -p` выполняется в одноразовом контейнере `postgres:17-alpine`,
в который смонтирован этот каталог, а не в оболочке скрипта. `--mount` не создаёт отсутствующий каталог, поэтому
без корня `TESTSYS_GRADING_NODE_WORKSPACE` скрипт завершается с ошибкой до запуска сервисов.

### Переменные и стартовые проверки узла

`deploy/compose.grading-node.yml` передаёт узлу переменные `MOUNTED_DIRECTORY`, `HOST_DIRECTORY`, `WORKERS_COUNT`,
`INNER_TIMEOUT_SECONDS` и `NODE_ID` из переменных Compose `TESTSYS_GRADING_NODE_*` и отключает стандартный
HTTP-порт ASP.NET Core пустым `ASPNETCORE_HTTP_PORTS`. Сам узел читает свои переменные только при первом
gRPC-вызове. Поэтому файл монтирует `deploy/grading-node/entrypoint.sh` в контейнер узла как
`/opt/testsys/grading-node-entrypoint.sh` и запускает его вместо стандартной точки входа образа. Скрипт проверяет,
что `HOST_DIRECTORY` — абсолютный путь без пробельных символов, а `WORKERS_COUNT`, `INNER_TIMEOUT_SECONDS`
и `NODE_ID` — целые неотрицательные числа. При ошибке он выводит имя переменной и завершается с кодом `1`, иначе
запускает узел командой `dotnet TestSys.Trik.GradingNode.Runner.dll`. Образ узла не меняется.

## Очистка

`clean` выполняет `down --volumes --remove-orphans` для проекта `testsys-dev` или `testsys-e2e`, с `--rmi` — ещё
`--rmi local`. В dev команда получает файл мониторинга и поэтому удаляет и тома `prometheus-data`
и `grafana-data`. Файл узла скрипт передаёт, только если с ним проходит `config --quiet`; иначе контейнер узла удаляет
`--remove-orphans`. Затем одноразовый контейнер `postgres:17-alpine`, в который смонтирован подкаталог
`${TESTSYS_GRADING_NODE_WORKSPACE}/testsys-<окружение>`, удаляет его содержимое командой
`find /workspace -mindepth 1 -delete`; сам подкаталог остаётся. Имя проекта скрипт задаёт сам и не берёт
из `COMPOSE_PROJECT_NAME`, чтобы очистка не затронула другой проект.

## Идентификаторы

Node id генератора идентификаторов нельзя задать переменной окружения, см. раздел
[«Идентификаторы»](../testsys-infra/database/README.md#идентификаторы) в database/README.md.
Compose-файлы запускают один экземпляр приложения со значением по умолчанию.
