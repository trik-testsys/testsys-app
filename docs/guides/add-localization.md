# Добавление локализации

Гайд описывает, как добавить локализованное сообщение или новый регион: как выбрать ключ, написать MF2-сообщение,
сослаться на термин и вызвать сгенерированный метод. Он предназначен для разработчиков, которым нужна строка,
видимая пользователю.

Язык сообщений — Unicode MessageFormat 2 (MF2), и гайд его не пересказывает: синтаксис, встроенные функции и их
опции описаны в спецификации [LDML 48, часть 9: MessageFormat](https://www.unicode.org/reports/tr35/tr35-messageFormat.html)
и [справочнике встроенных MF2-функций](https://messageformat.unicode.org/docs/reference/functions/),
реализация — в [руководстве ICU по MessageFormat 2](https://unicode-org.github.io/icu/userguide/format_parse/messages/mf2.html).
Спецификация описывает функции MF2 в целом; проект разрешает белый список опций, применяемых ICU4J 78.1,
и добавляет собственные функции с контрактами ниже. Здесь — как ключ превращается в метод, какие типы получают параметры
и какие ограничения проверяет сборка. Что такое MF2 и ICU, зачем нужен модуль и как он устроен — в
[localization/README.md](../../testsys-infra/localization/README.md).

Пример каждой поддерживаемой функции и конструкции лежит в отдельном наборе примеров
[src/test/examples/localization](../../testsys-infra/localization/src/test/examples/localization) модуля. Он
проходит тот же кодген, что и сообщения продукта, и отрисовывается тестом
[ExampleLocalizationTests.kt](../../testsys-infra/localization/src/test/kotlin/tech/testsys/infra/localization/examples/ExampleLocalizationTests.kt);
ниже примеры приводятся по ключам этого набора.

## Чек-лист

Сообщения лежат в `testsys-infra/localization/src/main/resources/localization/`: у каждого региона папка с именем
его языкового тега, в ней по файлу на бандл — `<языковой тег>/<bundle>.properties`.

| # | Шаг                                    | Куда                                                        |
|---|----------------------------------------|-------------------------------------------------------------|
| 1 | Выбрать ключ и файл бандла             | `<языковой тег>/<bundle>.properties`, например `ru-RU/task.properties` |
| 2 | Написать сообщение                     | Там же                                                      |
| 3 | Сослаться на термин, если он нужен     | `<языковой тег>/glossary.properties`                        |
| 4 | Добавить сообщение во все регионы      | Файл того же бандла в папке каждого региона                 |
| 5 | Сгенерировать API                      | `./gradlew :testsys-infra:localization:compileKotlin`       |
| 6 | Вызвать сгенерированный метод          | Код, который показывает строку пользователю                 |
| 7 | Добавить регион, если нужен новый язык | `regions.properties`, папка `<языковой тег>/`               |
| 8 | Проверить                              | Golden-тест модуля локализации                              |

## 1. Выбрать ключ и файл бандла

Ключ имеет вид `<bundle>.<segment>[.<segment>…]`, каждый сегмент — `[a-z][a-z0-9_]*`. Всё до первой точки
становится классом бандла, остальное — методом:

| Ключ                    | Класс  | Метод            | Файл в регионе `ru-RU`   |
|-------------------------|--------|------------------|--------------------------|
| `task.deadline.in_days` | `Task` | `deadlineInDays` | `ru-RU/task.properties`  |
| `user.rating.delta`     | `User` | `ratingDelta`    | `ru-RU/user.properties`  |

- **Ключ пишется полностью и лежит в файле своего бандла.** Ключ другого бандла в файле — ошибка сборки. Новый
  бандл — новый файл `<bundle>.properties`.
- Имя бандла — имя доменной сущности в коде (раздел «Именование» в [code-style.md](../project/code-style.md)):
  сообщения о Туре лежат в бандле `contest`, о Соревновании — в `competition`.
- Префикс `glossary` зарезервирован для терминов (шаг 3).
- Ошибка сборки: ключ без точки; два ключа или два бандла, дающие одно имя; класс с именем генерируемого или
  рантайм-типа (`Localization`, `SupportedRegion`, `String`, `Instant`, …); метод `toString`, `hashCode`
  или `equals`. Имена классов и вложенных `enum` сравниваются без учёта регистра: на нечувствительной к регистру
  файловой системе `Testcase.kt` и `TestCase.kt` — один файл.

## 2. Написать сообщение

Значение ключа — MF2-сообщение. Поддерживается весь синтаксис спецификации, кроме конструкций из раздела
[«Запрещённые конструкции»](#запрещённые-конструкции).

- Имя переменной — `[a-z][A-Za-z0-9]*` и не ключевое слово Kotlin: входные переменные становятся параметрами
  метода.
- Объявленный `.input`, который сообщение не использует, — ошибка сборки.

### Параметры и их типы

Тип параметра выводится из функции, которая форматирует переменную:

| MF2                           | Kotlin-параметр                                                                  | Пример                 |
|-------------------------------|----------------------------------------------------------------------------------|------------------------|
| `{$x}` без функции, `:string` | `String`; селектор `:string` — вложенный `enum`                                  | `solution.status`      |
| `:integer`, `:ordinal`        | `Int`                                                                            | `task.deadline.in_days` |
| `:number`, `:percent`, `:spellout`, `:unit` | `Number`                                                           | `task.score.points`    |
| `:offset`                     | тип операнда                                                                     | `competition.participants` |
| `:currency`                   | `Number` с `currency=<ISO 4217>`; `CurrencyAmount` без `currency`                | `competition.fee`, `competition.prize` |
| `:date`, `:time`, `:datetime` | `Instant`; `ZonedDateTime` с `timeZone=input`                                    | `contest.start`        |
| `:term`                       | операнд `Int` или без операнда                                                   | `task.result`          |

- Опция-переменная добавляет параметр: опция цифр (`maximumFractionDigits=$p`, `task.score.average`) — `Int`,
  `currency=$c` (`competition.fee.foreign`) и `unit=$u` (`solution.limit`) — `String`, `timeZone=$z`
  (`contest.start.local`) — `ZoneId`.
- Параметры идут в порядке первого появления переменной. `Number` и `Int` одной переменной сливаются в `Int`;
  другие несовместимые типы — ошибка сборки.
- Вложенный `enum` селектора — объединение ключей всех регионов и `OTHER` для варианта `*`.

### Опции

- **Разрешены только опции, которые ICU4J 78.1 действительно применяет.** Опция, которую ICU молча игнорирует
  (например, `minimumIntegerDigits` у `:number`), — ошибка `is not honoured by ICU4J 78.1`. Так же отклоняются
  сочетания, из которых ICU применяет не всё:
  - несколько опций точности у `:number`;
  - `compactDisplay` без `notation=compact`;
  - `fields` и `length` друг без друга (у `:datetime` — `dateFields` и `dateLength`);
  - `hour12` без `precision` (у `:datetime` — без `timePrecision`);
  - `timeZoneStyle` без `precision`; у `:datetime` — без `timePrecision` и без пары `dateFields` и `dateLength`;
  - `icu:skeleton` вместе с опциями формата: у `:number` и `:integer` — с любой опцией, кроме `select`,
    у `:date`, `:time` и `:datetime` — с любой, кроме `calendar` и `timeZone`.
- Значения литералов проверяются при сборке: перечисления, коды ISO 4217, зоны, календари, единицы, наборы
  правил RBNF локали.
- Значение-переменная допустимо только у опций из раздела «Параметры и их типы», и это аргумент сообщения,
  а не объявленная в нём переменная. Опции цифр — это `minimumIntegerDigits`, `minimumFractionDigits`,
  `maximumFractionDigits`, `minimumSignificantDigits`, `maximumSignificantDigits` и `fractionDigits` из тех, что
  функция применяет. `select` и остальные опции — только литералы.
- `select` — только `ordinal` или `exact`: `plural` — значение по умолчанию и явно не пишется.
- `icu:skeleton` — запасной выход для формата, которого нет среди стандартных опций (`competition.period`). Скелет
  `:date`, `:time` и `:datetime`, который даёт стандартная комбинация опций, — ошибка с указанием этой комбинации.

### Выбор варианта

Образец выбора по числу — ключ `task.deadline.in_days` в
[task.properties](../../testsys-infra/localization/src/test/examples/localization/ru-RU/task.properties) набора
примеров.

- Селектор объявляется с функцией через `.input` или `.local`. Выбирать можно только по `:string`, `:integer`,
  `:number`, `:percent` и `:offset`.
- **Каждая достижимая категория перечисляется явно.** Если для категории был бы выбран `*`, это ошибка сборки:
  у `:integer` в `ru` достижимы `one`, `few`, `many`, у `:number` и `:percent` — ещё и `other`.
- Ключи `:string` — `[a-z][a-z0-9_]*`, ключ `other` запрещён: его занимает константа `OTHER`.
- Точный числовой ключ — целое (`0`, `1`). При `select=exact` допустимы только точные ключи (`contest.attempts`),
  а у селектора с опциями цифр или `icu:skeleton` точные ключи запрещены.
- ICU4J 78.1 никогда не выбирает ключ `other` числового селектора и берёт вместо него `*`. Поэтому варианты
  `other` и `*` должны давать одинаковый текст (`task.score.points`).
- Выбор идёт по **отформатированному** значению: `1` с одной цифрой дробной части («1,0») — категория `other`;
  `:percent` выбирает по значению, умноженному на 100 (`task.score.share`).

### Даты и часовые пояса

- По умолчанию дата показывается в зоне контекста `Localization.forRegion(region, timeZone)` (`contest.start`).
- Сообщение может задать зону: литералом `timeZone=|Europe/Moscow|` (`contest.start.moscow`) или `timeZone=UTC`
  (`contest.start.utc`), параметром `timeZone=$zone` (`contest.start.local`) или `timeZone=input` — тогда параметр
  `ZonedDateTime` показывается в своей зоне (`contest.start.venue`).
- Литерал `timeZone` — `UTC` или идентификатор IANA. Смещение вида `|+03:00|` — ошибка сборки: ICU4J 78.1 не
  распознаёт такую зону и молча показывает время по Гринвичу. Зона-смещение в контексте или в аргументе
  (`ZoneOffset.ofHours(3)`, `UTC+03:00`) допустима: рантайм передаёт её ICU в понятном ему виде.
- Один аргумент-дата в разных зонах внутри одного сообщения — ошибка сборки.
- `:date` без опций включает день недели («чт, 9 окт. 2025 г.», `contest.date`).

### Пользовательские функции

Проект добавляет к функциям спецификации четыре своих:

| Функция     | Что делает                                                        | Пример                                   |
|-------------|-------------------------------------------------------------------|------------------------------------------|
| `:term`     | Форма доменного термина (шаг 3)                                   | `task.result`                            |
| `:spellout` | Число словами по набору правил RBNF `rules` (обязателен)          | `rules=spellout-cardinal-feminine` в `task.count.words` |
| `:ordinal`  | Порядковое число по набору правил RBNF `rules` (обязателен)       | `rules=digits-ordinal-masculine` в `contest.number` |
| `:unit`     | Число с единицей измерения ICU `unit` (обязателен) и опциями `:unit` спецификации | `unit=megabyte` в `solution.memory` |

Каждая функция возвращает текст внутри сообщения; эти четыре функции не могут быть селекторами `.match`.

#### `:term`

Подставляет форму термина из `glossary.properties` региона. Обязательные литеральные опции — `name` (имя термина)
и `case` (ключ падежа). Допустимая опция `number=sg|pl` обязательна без операнда и запрещена с операндом.
С числом операнд — переменная `Int`; если она объявлена с функцией, это `:integer` или его `:offset`.
Категория CLDR после смещения выбирает форму. Литеральный операнд не допускается. Сборка проверяет каждую нужную форму
термина, правила описаны в [шаге 3](#3-сослаться-на-термин).

Примеры `task.result` (с числом) и `task.list.title` (без числа) — в
[task.properties](../../testsys-infra/localization/src/test/examples/localization/ru-RU/task.properties).

#### `:spellout` и `:ordinal`

`:spellout` записывает число словами, `:ordinal` — порядковое число по правилам RBNF региона. Единственная
собственная опция — обязательный литерал `rules`, имя набора правил без `%`; сборка проверяет, что набор существует
для функции в локали региона. Операнд `:spellout` — числовой литерал или переменная `Number`, в том числе значение
`:number`, `:integer` или их `:offset`. Операнд `:ordinal` — целочисленный литерал или переменная `Int`, в том числе
значение `:integer` или его `:offset`. Совместное использование переменной `:spellout` как `:integer` или `:term`
также сужает параметр до `Int`.

На рантайме NaN и бесконечности отклоняются с `IllegalArgumentException`. После точного применения offset
целое должно лежать в диапазоне `Long.MIN_VALUE..Long.MAX_VALUE`; обрезания и переполнения нет. Дробь у `:spellout`
форматируется через `Double`: конечное значение, которое превращается в бесконечность при преобразовании,
отклоняется. Диагностика называет функцию, фактически форматируемое значение и причину. Точность дробного RBNF
ограничена этим преобразованием.

Примеры — `task.count.words` в
[task.properties](../../testsys-infra/localization/src/test/examples/localization/ru-RU/task.properties)
и `contest.number` в
[contest.properties](../../testsys-infra/localization/src/test/examples/localization/ru-RU/contest.properties).

#### `:unit`

Форматирует число с единицей измерения через числовой скелет ICU. Операнд — числовой литерал или переменная
`Number`, в том числе значение `:number`, `:integer` или их `:offset`. Обязательная опция `unit` — литерал или
аргумент `String` с одним идентификатором единицы ICU (`kilogram`, `kilometer-per-hour`). Литерал проверяется
при сборке; недопустимый аргумент, например `meter scale/1000`, вызывает `IllegalArgumentException` при форматировании.

Допустимы `usage`, `unitDisplay`, `notation`, `compactDisplay`, `numberingSystem`, `signDisplay`, `useGrouping`,
`minimumIntegerDigits`, `minimumFractionDigits`, `maximumFractionDigits`, `minimumSignificantDigits`,
`maximumSignificantDigits`, `roundingPriority`, `roundingIncrement`, `roundingMode`, `trailingZeroDisplay`.
Значения-переменные допустимы только для `unit` и перечисленных выше опций цифр; остальные значения — литералы.

- `usage` требует литерального `unit` (`contest.venue.distance`).
- `compactDisplay` требует `notation=compact`.
- `roundingPriority=morePrecision|lessPrecision` требует границы и дробных, и значащих цифр.
- `roundingIncrement`, отличный от `1`, требует `maximumFractionDigits`, отсутствия опций значащих цифр
  и `roundingPriority=auto`.
- `roundingMode` не принимает `halfCeil` и `halfFloor`: их нет в скелетах чисел ICU4J 78.1.

NaN и бесконечности сохраняют форматирование ICU, включая использование после offset. Ограничения RBNF
на диапазон `Long` к `:unit` не применяются. Примеры `solution.memory` и `solution.limit` — в
[solution.properties](../../testsys-infra/localization/src/test/examples/localization/ru-RU/solution.properties).
Вывод всех примеров закреплён в
[ExampleLocalizationTests.kt](../../testsys-infra/localization/src/test/kotlin/tech/testsys/infra/localization/examples/ExampleLocalizationTests.kt).

### Запрещённые конструкции

Всё ниже — ошибки сборки.

| Конструкция                                                            | Почему                                                        |
|------------------------------------------------------------------------|---------------------------------------------------------------|
| Разметка `{#b}…{/b}`                                                   | API возвращает простой `String`, ICU молча отбрасывает разметку |
| Опции `u:`                                                             | Без эффекта при выводе `String`                               |
| `:icu:gender`, функции с пространством имён и неизвестные              | Выбор по полу — через `:string`                               |
| Аргумент с именем опции числа (`$notation`, …) рядом с числовой функцией, если он не значение этой же опции в каждом числовом выражении сообщения | ICU4J 78.1 передаёт аргументы сообщения числовым функциям как опции |
| Встроенная функция поверх `:offset` и `:offset` поверх `:offset`       | ICU4J 78.1 теряет сдвиг                                       |
| Встроенная функция, кроме `:offset`, поверх значения, объявленного с другой функцией | ICU4J 78.1 смешал бы её с опциями объявления    |
| `:offset` поверх `:number` с `notation`, `compactDisplay`, `minimumFractionDigits`, `maximumFractionDigits` или `minimumSignificantDigits` | ICU4J 78.1 применяет эти опции только к `:number` и теряет их при сдвиге |
| Одна переменная как значение опции в нескольких объявлениях            | Парсер ICU4J 78.1 считает значение опции в объявлении объявлением переменной и сообщает `syntax error: Variable '…' already declared` |

### Запись в .properties

Кодировка — UTF-8 без BOM. Файл читается строже, чем `java.util.Properties`: повтор ключа, пустое значение
(`key=`) и пробел в конце значения — ошибки сборки.

Во всех localization `.properties`, включая `regions.properties`, комментарии начинаются только с `#`.
Отдельная строка, начинающаяся с `!` после ведущих пробелов, табуляции или form feed, отклоняется с ошибкой
`Comments must start with #`. Она не продолжается обратной косой чертой в конце строки; `!` внутри значения,
в том числе в начале строки-продолжения, остаётся текстом. Комментарий `#` также не продолжается обратной косой
чертой в конце строки. В `regions.properties` последовательный блок `#` прямо над регионом становится KDoc
его константы, пустая или отклонённая строка разрывает блок.

- **Каждая строка-продолжение заканчивается на ` \`** (пробел и обратная косая черта): `Properties` отбрасывает
  ведущие пробелы следующей строки, а MF2 нужен пробел между частями `.match`.
- Строку внутри `{{…}}` не переносят; перевод строки в тексте — `\n`.
- Экранирование MF2 пишется удвоенным: `\\{`, `\\}` (`resource.name.template`), `\\|`, `\\\\`
  (`resource.path.hint`).
- Текст, начинающийся с `.`, оформляется паттерном в кавычках: `{{.qrs-файл …}}` (`resource.qrs.missing`).

## 3. Сослаться на термин

Термин — доменное слово, которое встречается в нескольких сообщениях и должно склоняться одинаково (Задача,
Пользователь). Разовая фраза с числом склоняется в своём ключе (`task.deadline.in_days`); слова «на будущее» не
заводятся, неиспользуемый термин — ошибка сборки.

Термин — ключ `glossary.<name>` в файле `<языковой тег>/glossary.properties`, образец — `glossary.task`:

- объявляет ровно `.input {$case :string}` и `.input {$form :string}` и выбирает `.match $case $form`;
- варианты — только текст, без плейсхолдеров;
- ключи падежа свои у каждого региона, в `ru` — `nom`, `gen`, `dat`, `acc`, `ins`, `loc`;
- ключи формы — категории множественного числа, которые принимает целое число (для ссылок с числом; в `ru` —
  `one`, `few`, `many`), и `sg`/`pl` (без числа).

Ссылки:

| Вид        | Запись                                          | Нужен вариант термина                     | Пример         |
|------------|-------------------------------------------------|-------------------------------------------|----------------|
| С числом   | `{$n :term name=task case=acc}`, `$n` — `:integer` или его `:offset` | `<case> <категория>` для каждой достижимой категории | `task.result`, над `:offset` — `competition.rivals` |
| Без числа  | `{:term name=task case=gen number=pl}`          | `<case> sg` или `<case> pl`               | `task.list.title` |

Если ссылке нужен вариант, которого в термине ещё нет, он добавляется в `glossary.<name>` того же региона.

## 4. Добавить сообщение во все регионы

Ключ добавляется в файл своего бандла в папке **каждого** региона: ключ, отсутствующий хотя бы в одном регионе, —
ошибка сборки. Если перевод ещё не готов, в регион кладётся заглушка, а не хардкод в Kotlin.

## 5. Сгенерировать API

Генерация привязана к компиляции модуля, отдельно запускать её не нужно:

```bash
./gradlew :testsys-infra:localization:compileKotlin
```

Все ошибки печатаются вместе: `<REGION> / <key>: <problem>` для сообщений, `<file>:<line>: <problem>` для
файлов. Текст ошибки называет правило и способ исправления.

## 6. Вызвать сгенерированный метод

Метод вызывается на экземпляре `Localization` с именованными параметрами; образец — вызов `deadlineInDays` для
ключа `task.deadline.in_days` в
[ExampleLocalizationTests.kt](../../testsys-infra/localization/src/test/kotlin/tech/testsys/infra/localization/examples/ExampleLocalizationTests.kt).
Правила вызова — в разделе «Использование в коде» в
[localization/README.md](../../testsys-infra/localization/README.md).

## 7. Добавить регион

Шаг нужен, только если добавляется новый язык.

1. Добавьте в
   [regions.properties](../../testsys-infra/localization/src/main/resources/localization/regions.properties)
   строку `<REGION>=<языковой тег BCP 47>`, где `<REGION>` — `[A-Z]{2}`, и комментарий `#` над ней: он станет
   KDoc константы `SupportedRegion`.
2. Создайте папку `<языковой тег>/` с файлами **всех** бандлов из `ru-RU/`, включая `glossary.properties`, если он
   есть, с ключами форм по категориям новой локали.
3. Сгенерируйте API (шаг 5). Сборка перечислит недостающие файлы бандлов (по одной ошибке на файл), ключи,
   категории и варианты терминов.

## 8. Проверить

Новое сообщение добавляется в golden-тест
[LocalizationTests.kt](../../testsys-infra/localization/src/test/kotlin/tech/testsys/infra/localization/LocalizationTests.kt).
Поддержка новой функции или конструкции добавляет её пример в набор примеров
[src/test/examples/localization](../../testsys-infra/localization/src/test/examples/localization) и его проверку в
[ExampleLocalizationTests.kt](../../testsys-infra/localization/src/test/kotlin/tech/testsys/infra/localization/examples/ExampleLocalizationTests.kt).
Тесты модуля запускаются командой:

```bash
./gradlew :testsys-infra:localization:check
```

Общие команды сборки — в разделе «Сборка» в [structure.md](../project/structure.md).
