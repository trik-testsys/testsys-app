# deploy

Документ описывает, как устроены Docker-образ основного приложения `testsys-web:app`, Compose-файлы окружений
dev, e2e и production, файл Проверяющего узла и скрипт `deploy/testsys.sh`. Он предназначен для тех, кто меняет
эти файлы. Как запускать окружения и что задать перед запуском — в [deployment.md](../docs/project/deployment.md);
смысл свойств приложения — в документах модулей.

## Устройство

- **Образ.** `Dockerfile` собирает образ приложения без настроек окружений и секретов.
- **Окружение** — проект Compose `testsys-<окружение>` из двух файлов: общего `deploy/compose.yml` (приложение
  и PostgreSQL) и override окружения `deploy/compose.<окружение>.yml`. Dev и e2e добавляют Mailpit, dev — ещё
  загрузку начальных данных.
- **Проверяющий узел** описан отдельным файлом `deploy/compose.grading-node.yml`. Его подключают к окружению
  или запускают отдельным проектом, в том числе на другой машине.
- **Переменные Compose** — порты, адреса, секреты, настройки узла — лежат в файлах переменных `deploy/env/`.
- **Параметры приложения** лежат в конфигурационных файлах Spring `deploy/config/`, которые Compose монтирует
  в контейнер.
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
| `deploy/compose.dev.yml` | Override проекта `testsys-dev`: пароли, порты, Mailpit, начальные данные |
| `deploy/compose.e2e.yml` | Override проекта `testsys-e2e` для внешнего тестового процесса |
| `deploy/compose.prod.yml` | Override проекта `testsys-prod`: конфигурационный файл на сервере, секреты |
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

## Образ приложения

Сервис `app` собирается из корня репозитория: в `deploy/compose.yml` задано `build: ..`. Сборочный этап
на `eclipse-temurin:21-jdk-noble` выполняет `:testsys-web:app:bootJar` с `-Pdetekt.autoCorrect=false`. Кэш Gradle
(`~/.gradle`) и Node.js, который устанавливает плагин Vaadin (`~/.vaadin`), сохраняются между сборками в cache mount
BuildKit. Runtime-этап на `eclipse-temurin:21-jre-noble` запускает jar командой `java -jar /app/app.jar`
от пользователя `testsys` (UID и GID `10001`) и открывает порт `8080`.

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
каталог первого Compose-файла. Для dev и e2e команда выглядит так; файл `<окружение>.local.env` передаётся, только
если он есть, файл узла — только с `--node`:

```bash
docker compose -p testsys-<окружение> --env-file env/<окружение>.env [--env-file env/<окружение>.local.env] -f compose.yml -f compose.<окружение>.yml [-f compose.grading-node.yml] <команда>
```

Production получает один файл переменных `--env-file <путь>` вместо файлов из `env/`. Отдельный узел вызывается
как `docker compose -p testsys-grading-node --env-file <путь> -f compose.grading-node.yml <команда>`.

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
слияния, поэтому `:?` в `deploy/compose.yml` сработал бы во всех окружениях.

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

Переменными окружения Compose передаёт секреты, адреса, которые зависят от состава сервисов, и режим заголовков
HTTPS-прокси:

| Переменная приложения | Где задана | Свойство |
|-----------------------|------------|----------|
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` | `deploy/compose.yml`: база `testsys` сервиса `postgres`, пользователь `testsys` | `spring.datasource.url`, `username` |
| `SPRING_DATASOURCE_PASSWORD` | Override окружения | `spring.datasource.password` |
| `TESTSYS_MAIL_HOST`, `TESTSYS_MAIL_PORT` | `deploy/compose.dev.yml`, `deploy/compose.e2e.yml`: `mailpit`, `1025` | `testsys.mail.host`, `port` |
| `TESTSYS_MAIL_PASSWORD` | `deploy/compose.prod.yml` | `testsys.mail.password` |
| `TESTSYS_GRADING_NODES` | Override окружения из переменной Compose `TESTSYS_GRADING_NODES` | `testsys.grading.nodes`, формат — в разделе «Настройки» в [grpc/README.md](../testsys-infra/grpc/README.md#настройки) |
| `SERVER_FORWARDHEADERSSTRATEGY` | `deploy/compose.prod.yml` | `server.forward-headers-strategy` |

Свойства, которых нет ни в файлах, ни в переменных, сохраняют значения по умолчанию из документов модулей.

### Готовность приложения

Healthcheck сервиса `app` проверяет, что порт `8080` внутри контейнера принимает TCP-соединение:
`bash -c ': </dev/tcp/127.0.0.1/8080'`. Spring Boot открывает HTTP-порт после создания всех singleton-бинов,
то есть после миграций Liquibase и проверки Публичного Сообщества в `PublicCommunityBootstrap`. Поэтому статус
`healthy` означает, что миграции применены и приложение принимает запросы. Пока проверка ни разу не прошла, но
не дольше пяти минут (`start_period`), Docker проверяет порт каждые 2 секунды (`start_interval`) и не учитывает
неудачи. После первой успешной проверки или по истечении пяти минут он проверяет порт каждые 10 секунд и после
трёх неудач подряд помечает контейнер `unhealthy`.

## Начальные данные dev

Сервис `db-seed` в `deploy/compose.dev.yml` работает на образе `postgres:17-alpine` и запускает
`deploy/db-seed/entrypoint.sh` при каждом `up`. Он зависит от `app` с условием `service_healthy`, поэтому стартует
после миграций Liquibase и создания Публичного Сообщества. `psql` подключается к базе `testsys` от пользователя
`testsys`. Каталог данных образа postgres смонтирован в `tmpfs`, чтобы `db-seed` не оставлял анонимный том.

Скрипт из `TESTSYS_DB_SEED_SCRIPT` монтируется как `/opt/testsys/db-seed.sql`. `db-seed` действует так:

1. Без переменной Compose монтирует вместо скрипта `/dev/null`, и `db-seed` сообщает
   `TESTSYS_DB_SEED_SCRIPT is not set, nothing to apply`. Если по пути лежит не обычный файл, например каталог,
   который создал Docker Desktop, `db-seed` завершается с кодом `1`.
2. Если в базе есть таблица `testsys_dev_seed.applied`, скрипт уже применён. `db-seed` сравнивает записанный в ней
   SHA-256 с текущим скриптом. При совпадении он сообщает о пропуске, при расхождении выводит предупреждение.
3. Иначе `psql -X -v ON_ERROR_STOP=1 --single-transaction` создаёт схему `testsys_dev_seed` с таблицей `applied`,
   выполняет скрипт и записывает его SHA-256. При ошибке транзакция откатывается целиком, отметка не появляется,
   и `db-seed` завершается с ненулевым кодом.

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
`--rmi local`. Файл узла скрипт передаёт, только если с ним проходит `config --quiet`; иначе контейнер узла удаляет
`--remove-orphans`. Затем одноразовый контейнер `postgres:17-alpine`, в который смонтирован подкаталог
`${TESTSYS_GRADING_NODE_WORKSPACE}/testsys-<окружение>`, удаляет его содержимое командой
`find /workspace -mindepth 1 -delete`; сам подкаталог остаётся. Имя проекта скрипт задаёт сам и не берёт
из `COMPOSE_PROJECT_NAME`, чтобы очистка не затронула другой проект.

## Идентификаторы

Node id генератора идентификаторов нельзя задать переменной окружения, см. раздел
[«Идентификаторы»](../testsys-infra/database/README.md#идентификаторы) в database/README.md.
Compose-файлы запускают один экземпляр приложения со значением по умолчанию.
