# Локализация

Локализация в проекте — это типобезопасный API, сгенерированный из ICU
`MessageFormat`-паттернов. Рантайм-код **никогда не работает со строковыми
ключами**: каждое сообщение — это метод на сгенерированном классе с
типизированными параметрами. Опечатка в имени аргумента, забытая ветка
`select`, лишний/пропущенный плейсхолдер — должно быть ошибкой компиляции.

Модуль: `:testsys-infra:localization`. Публичная точка входа:
`tech.testsys.infra.localization.Localization.forRegion(region)`.

## Где что лежит

| Что                        | Где                                                                |
|----------------------------|--------------------------------------------------------------------|
| Ресурсы (`*.properties`)   | `testsys-infra/localization/src/main/resources/localization/`      |
| Рантайм                    | `testsys-infra/localization/src/main/kotlin/.../bundle`            |
| Кодген                     | `testsys-infra/localization/src/codegen/kotlin/...`                |
| Тесты кодгена              | `testsys-infra/localization/src/codegenTest/kotlin/...`            |
| Сгенерированный код        | `build/generated/source/localization/main/kotlin/` (не коммитим)   |

Имя файла ресурсов = идентификатор `SupportedRegion` в верхнем регистре
(`RU.properties`, `EN.properties`, …). Кодировка — UTF-8.

## Формат ключа

```
<bundle>.<segment>[.<segment>…]=<ICU pattern>
```

- Всё **до первой точки** — `bundle`, превращается в имя класса
  (`snake_case → PascalCase`).
- Всё **после первой точки** — имя метода (`dot`/`snake_case → camelCase`,
  сегменты сливаются).
- Ключ без точки или конфликт имён после нормализации — ошибка кодгена.

Примеры:

| Ключ                      | Класс         | Метод            |
|---------------------------|---------------|------------------|
| `task.deadline.in_days`   | `Task`        | `deadlineInDays` |
| `task.score.percent`      | `Task`        | `scorePercent`   |
| `participant.greeting`    | `Participant` | `greeting`       |
| `glossary.task`           | `Glossary`    | `task`           |

## Поддерживаемые типы плейсхолдеров

| ICU                           | Kotlin-параметр                              |
|-------------------------------|----------------------------------------------|
| `{name}`                      | `String`                                     |
| `{n, number}`, `{n, number, percent}` | `Number`                             |
| `{when, date}`, `{when, time}`        | `java.time.Instant`                  |
| `{n, plural, …}`, `{n, selectordinal, …}` | `Int`                            |
| `{n, spellout}`, `{n, ordinal}` | `Int`                                      |
| `{x, select, A {…} B {…} other {…}}` | сгенерированный `enum class` (объединение веток + `OTHER`) |

`select` всегда требует ветку `other` — это требование ICU, кодген
ничего не добавляет молча.

Конфликт типов одного плейсхолдера между регионами — ошибка кодгена с
указанием обоих ключей (`task.score.value: RU expects 'number', EN expects 'date'`).

## Объединение сигнатур между регионами

Сигнатура метода — **объединение** плейсхолдеров по всем регионам:

- плейсхолдер есть только в одном регионе → параметр всё равно в
  сигнатуре, в других регионах он молча игнорируется на рантайме;
- ветки `select` — объединение по всем регионам;
- если ключ отсутствует в каком-то регионе — ошибка кодгена
  (всё переводим).

## Glossary: словарь падежей и склонений

В `Glossary` лежат **только доменные термины** — слова, которые
повторяются в десятках сообщений и должны склоняться единообразно
(`задача`, `пользователь`, `контест`, …). Это нужно, чтобы собирать
осмысленные русские фразы вроде «Удалить 5 задач» вместо «Удалить 5 задача».

**Всё остальное форматируем in place**, прямо в сообщении-ключе:

- разовая фраза с числом или склонением, которая не переиспользуется,
  — пишется одним ICU-паттерном в своём ключе (см. `task.deadline.in_days`),
  а не разбивается на «общую» часть + `glossary`;
- статичные строки без параметров — просто текст в `.properties`;
- собирать предложение через `glossary` имеет смысл, только если
  вставляемое слово встречается ≥2-3 раз в разных контекстах. Иначе
  это лишний уровень косвенности и сложнее переводить на языки с
  другой структурой словосочетания.

Не добавляйте в `glossary` слова «на будущее» — заведём, когда
понадобится второе место.

Структура — `select` по падежу с вложенным `plural` по количеству.
**Только сам термин, без числа** — число подставляет вызывающая
сторона в основном сообщении:

```properties
glossary.task={case, select,\
    nom {{count, plural, one {Задача} few {Задачи} many {Задач} other {Задачи}}}\
    gen {{count, plural, one {Задачи} few {Задач}  many {Задач} other {Задачи}}}\
    …
    other {Задача}\
}
```

Падежи кодируем латинскими сокращениями: `nom`, `gen`, `dat`, `acc`,
`ins`, `loc`. Ветка `other` на уровне `select` обязательна по ICU —
кладём туда форму именительного как безопасный fallback на случай,
если в код просочится `TaskCase.OTHER`.

Внутри plural-веток **не используем `#`**: glossary возвращает слово,
а не «N слов». Число форматируется в внешнем сообщении (`{count, number}`
или просто `{count, plural, …}`).

Использование:

```kotlin
val l = Localization.forRegion(SupportedRegion.RU)
l.glossary.task(case = Glossary.TaskCase.GEN, count = 5)  // "Задач"
l.glossary.user(case = Glossary.UserCase.DAT, count = 1)  // "Пользователю"

// Собираем фразу с числом снаружи:
val n = 5
val glossaryTask = l.glossary.task(case = Glossary.TaskCase.GEN, count = n)
l.task.bulkDeleteConfirm(count = n, glossaryTask = glossaryTask)  // "Удалить 5 Задач?"
```

### Имя плейсхолдера для glossary-термина

Если плейсхолдер ICU-сообщения предназначен для подстановки термина из
`glossary.*`, его имя обязано начинаться с `glossary` и заканчиваться
именем термина в camelCase: `{glossaryTask}`, `{glossaryUser}`, и т. п.
Это правило для всех `.properties` файлов во всех регионах.

Зачем:

- сразу видно при чтении паттерна, что в слот не «любая строка», а
  результат вызова `l.glossary.<term>(…)`;
- глобальный grep по `{glossary` находит все места, где словарь
  реально подключён;
- параметр в Kotlin получает осмысленное имя
  (`glossaryTask: String`)

Плохо:

```properties
task.bulk.delete_confirm=Удалить {count, number} {term}?
task.assign.success={user} получил {task}.
```

Хорошо:

```properties
task.bulk.delete_confirm=Удалить {count, number} {glossaryTask}?
task.assign.success={glossaryUser} получил {glossaryTask}.
```


## Использование в коде

```kotlin
import tech.testsys.infra.localization.Localization
import tech.testsys.infra.localization.bundle.SupportedRegion

val l = Localization.forRegion(SupportedRegion.RU)

l.task.deadlineInDays(days = 3)            // "через 3 дня"
l.task.deadlinePassed()                    // "Дедлайн прошёл"
l.task.scorePercent(value = 0.87)          // "Результат: 87 %"
l.participant.greeting(name = "Алексей")   // "Здравствуйте, Алексей!"
```

Правила:

- **Никаких строковых ключей наружу.** Если хочется передать ключ в
  другой слой — передавайте лямбду `(Localization) -> String`.
- `Localization.forRegion(region)` дёшева, но можно кешировать инстанс
  на запрос/сессию.
- Параметры всегда именованные в вызове — это часть контракта,
  читаемость важнее одной строки.

## Как добавить новое сообщение

1. Добавляем ключ в `RU.properties` (и в каждый другой существующий
   `<REGION>.properties`).
2. Запускаем `./gradlew :testsys-infra:localization:generateLocalization`
   — или просто `compileKotlin`, генерация прицеплена к нему.
3. Вызываем сгенерированный метод из кода.

Если сообщение использует словарное слово в нужном падеже — собираем его
из `glossary.*` и подставляем в основное сообщение через bare-плейсхолдер
с обязательным префиксом `glossary…` (см. правило именования выше):

```properties
task.bulk.delete_confirm=Удалить {count, number} {glossaryTask}?
```

```kotlin
val glossaryTask = l.glossary.task(Glossary.TaskCase.ACC, count = n)
l.task.bulkDeleteConfirm(count = n, glossaryTask = glossaryTask)
```

## Как добавить новый регион

1. Добавить значение в `SupportedRegion`.
2. Создать `<REGION>.properties` со **всеми** ключами из `RU.properties`.
3. Прогнать кодген. Если чего-то не хватает — сборка упадёт с указанием
   ключей.

## Чего не делаем

- **Не конкатенируем переводы вручную.** «Удалить » + count + " задач" —
  плохо: ломается на других языках и числах. Всё через
  `MessageFormat`/glossary.
- **Не используем `String.format`/интерполяцию** для пользовательских
  строк.
- **Не правим сгенерированный код** в `build/`. Он перегенерируется при
  каждой сборке.
- **Не коммитим** `build/generated/` — он в `.gitignore`.
- **Не оставляем строки без локализации** в продовом коде. Если перевод
  ещё не готов — заводим ключ и оставляем заглушку в `RU.properties`,
  а не хардкод в Kotlin.

## Проверки

- `:testsys-infra:localization:codegenTest` — тесты парсера/мерджера
  кодгена.
- `:testsys-infra:localization:compileKotlin` — генерация + компиляция
  сгенерированного API. Сюда падают ошибки про недостающие ключи,
  конфликты типов и плохие имена.
