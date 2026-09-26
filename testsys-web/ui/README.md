# Kotlin-DSL дизайн-системы

Документ описывает модуль `:testsys-web:ui`: как на его DSL строится страница Кабинета, какие правила сетки
и оформления DSL проверяет сам и как добавить в него компонент. Он предназначен для тех, кто пишет страницы
`testsys-web` или компоненты дизайн-системы на Kotlin. Визуальные правила, токены и эталонные React-компоненты —
в [design-system/README.md](../design-system/README.md), место модуля в проекте — в
[structure.md](../../docs/project/structure.md), оформление тестов — в [unit-tests.md](../../docs/project/unit-tests.md).

## Устройство

Модуль зависит только от Vaadin Flow (`vaadin-core`). От Spring, домена и операций он не зависит.

- Статические компоненты (сетка, блоки, шапка, теги, бейджи, алерты) рендерят ту же разметку
  с классами `.ts-*`, что и React-эталон.
- Интерактивные (кнопки, поля, тосты, диалоги) — компоненты Vaadin под темой Lumo, связанной с токенами.
- Таблица — разметка `.ts-table` эталона; её страница, сортировка и выбор строк хранятся на сервере.
- CSS дизайн-системы не копируется в репозиторий: задача `processResources` кладёт `design-system/styles.css`
  и `tokens/` в jar (`META-INF/resources/design-system/`). Собственный CSS модуля — один файл
  `src/main/resources/META-INF/resources/testsys-vaadin.css`: сопоставление Lumo с токенами и правила для DOM,
  которым Vaadin отличается от эталона.
- jar модуля сохраняет даты изменения файлов (`isPreserveFileTimestamps = true`): Vaadin отдаёт стили с
  `Last-Modified` из jar, и с одинаковой датой воспроизводимой сборки браузер держал бы старый CSS после пересборки.
- Приложение подключает стили через `@StyleSheet` на `AppShellConfigurator` в порядке: `Lumo.STYLESHEET`,
  `TestSysTheme.DESIGN_SYSTEM`, `TestSysTheme.VAADIN_OVERRIDES`.

## Страница

Страница — класс с `@Route`, унаследованный от `TestSysView`. Метод `page(header) { … }` собирает шапку
`CabinetHeader` и тело.

```kotlin
@Route("profile")
class ProfileView(texts: UiTexts) : TestSysView(texts) {
    init {
        page(CabinetHeader(items = sections, active = "profile", user = HeaderUser(name))) {
            block(title = "Профиль") {
                editing(onSave = { binder.writeBeanIfValid(user) }, onCancel = { binder.readBean(user) })
                row {
                    textInput("Логин", labelSize = 4, size = 20) {
                        binder.forField(this).bind({ it.login }, { target, login -> target.login = login })
                    }
                }
            }
            row {
                slot(size = 16) { row { block(title = "Решения") { … } } }
                slot(size = 8) {
                    row { highlightBlock(title = "Идёт тур") { … } }
                    row { block(title = "Туры") { … } }
                }
            }
        }
    }
}
```

## Сетка

| Уровень | Что внутри | Правило |
|---------|------------|---------|
| `page` | `row`, `block`, `highlightBlock` | Блок прямо в странице занимает всю ширину |
| `row` страницы | `slot(size)` | `size` от 1 до 24, сумма слотов ряда — не больше 24 |
| `slot` | `row` | Ряды слота стоят друг под другом на колонках слота |
| `row` слота | `block(size)`, `highlightBlock(size)`, `statCard(…)` | `size` от 1 до ширины слота, по умолчанию весь слот; сумма — не больше ширины слота |
| `row` блока | поля, `field`, `text`, `tag`, `badge`, `counter`, `icon`, `alert`, `statCard`, `horizontal`, `vertical` | Поле занимает `labelSize + size`, остальные — `size`; без `size` элемент занимает остаток строки, после него строка закрыта; сумма — не больше размера блока |

- Неверный размер — `IllegalArgumentException`, переполнение ряда и элемент после занявшего остаток строки —
  `IllegalStateException` с перечнем размеров. Ошибка возникает при построении страницы, поэтому на каждую
  страницу пишется тест, который её строит.
- Подсетка идёт насквозь от ряда страницы до поля, поэтому подписи полей соседних блоков стоят на одних вертикалях
  страницы.
- `highlightBlock` — не больше одного на ряд страницы, включая блоки в его слотах.
- Блоки одного ряда всегда одной высоты, промежутки фиксированы.
- Строка блока без элементов не выводится; блок без непустых строк выводится без тела.
- Скрытый элемент строки блока (`isVisible = false`) освобождает свои колонки: следующие элементы сдвигаются влево.
- Размеры передаются именованным аргументом (`slot(size = 16)`, `block(size = 8)`, `labelSize = 4`): Detekt
  `MagicNumber` не проверяет именованные аргументы.

## Скоупы

Каждый уровень — класс-скоуп с `@TestSysDsl` (`@DslMarker`) и `internal`-конструктором: страница не создаёт
скоуп сама, не видит Vaadin-контейнер и не может вызвать функцию внешнего уровня. `TestSysView` маркером
не помечен, поэтому внутри `page { … }` доступны члены самой страницы (сервисы, методы). `page(...)` вызывается один
раз, повторный вызов — `IllegalStateException`. К `content` страница напрямую не обращается: компоненты Vaadin
ставятся только через `custom()` (см. «Запасной выход»).

| Скоуп | Доступно |
|-------|----------|
| `PageScope` | `row`, `block`, `highlightBlock` |
| `PageRowScope` | `slot` |
| `SlotScope` | `row` |
| `SlotRowScope` | `block`, `highlightBlock`, `statCard` |
| `BlockScope` | `row` (строка тела) или `table` (всё тело), `actions { }` (правая часть шапки), `footer { }`, `editing(onSave, onCancel)` |
| `BlockRowScope` | поля, `field`, элементы отображения с `size`, `horizontal(size)`, `vertical(size)` |
| `ContentScope` | `text`, действия, `icon`, `tag`, `badge`, `counter`, `alert`, `horizontal`, `vertical`, `custom` (без полей) |
| `TableScope<T>` | колонки таблицы, `empty`, `onRowClick` |
| `DialogScope` | `row` (строка на 12 колонках), `footer { dialog -> }` |

Содержимое тела блока кладётся только в строки или в таблицу: элемент прямо в `BlockScope` не компилируется.
`ContentScope` — поток без размеров: шапка, подвал, группы `horizontal`/`vertical` строки блока и ячейки `column`
таблицы.

Функции содержимого — расширения скоупов в пакетах `actions`, `forms`, `display`, `feedback`, `data`, `core`;
`toast`, `confirm` и `dialog` — функции верхнего уровня в пакетах `feedback` и `overlay`. Функции
отображения (`text`, `icon`, `tag`, `badge`, `counter`, `alert`) объявлены и для `ContentScope`, и для
`BlockRowScope` (с необязательным `size`), поля — только для `BlockRowScope`, действия — только для `ContentScope`.
`statCard` в ряду слота создаёт свой блок, в строке блока — только разметку `.ts-stat`.

## Поля

Поле — разметка `.ts-field` из двух ячеек: подпись слева на `labelSize` колонок и значение справа на `size`
колонок; всё поле занимает `labelSize + size` колонок строки блока. Функции полей — `textInput`, `codeInput`,
`textArea`, `select`, `checkbox`, `dateInput`, `timeInput`, `dateTimeInput`, `dateRangeInput`, `integerInput`,
`decimalInput`, `lookup` (см. [Лукап](#лукап)) — объявлены на `BlockRowScope` и возвращают `ValueInput<T>`.

- `labelSize` и `size` обязательны, каждый не меньше 1, иначе — `IllegalArgumentException`.
- Длинная подпись переносится, её первая строка стоит на уровне середины контрола. Подсказка (`hint`) и ошибка
  проверки — под контролом, в ячейке значения. У `checkbox` в ячейке значения только флажок, у `dateRangeInput` —
  два пикера.
- `textArea` растёт вместе с текстом от `minLines` до `maxLines` строк, после `maxLines` включается прокрутка.
  Равные значения фиксируют высоту. Без `minLines` поле начинается с двух строк, без `maxLines` растёт без
  ограничения. Значение меньше 1 или `minLines` больше `maxLines` — `IllegalArgumentException`.
- Подпись рисует DSL, у контрола Vaadin своей подписи нет. Доступное имя контрола — `aria-label` с текстом подписи.
  Клик по подписи обрабатывается в браузере, без запроса к серверу: ставит фокус в контрол, а флажок переключает,
  как нативный `<label>`.
- Звёздочка у подписи повторяет `requiredIndicatorVisible` контрола, который выставляет `Binder.asRequired`.
- Выключенное поле (`isEnabled = false`) получает класс `ts-field--disabled` с приглушённой подписью.
- `isVisible` скрывает всё поле: подпись и значение.

`field(label, labelSize, size) { }` — поле с нетиповым значением: та же подпись, а значение — поток элементов
`ContentScope` (теги, бейдж, ссылка). Подпись такого поля — не `<label>`, клик по ней ничего не делает; функция
возвращает `ElementHandle`.

## Таблица

`table(key, pageSize = 20, selectable = false, fetch) { … }` в `BlockScope` — таблица строк, которые `fetch` отдаёт
постранично; `key` определяет строку для выбора. Завершающая лямбда (`TableScope<T>`) объявляет колонки и настройки
таблицы. Образец — таблицы витрины в
`testsys-web/src/main/kotlin/tech/testsys/web/dev/ShowcaseDataSections.kt`.

```kotlin
block(title = "Посылки") {
    table(key = { it.id }, selectable = true, fetch = { request -> submissions.find(filter, request) }) {
        codeColumn("ID") { "#${it.id}" }
        numberColumn("Баллы", sortKey = "score") { it.score }
        column("Вердикт") { row -> badge(row.verdict, row.tone) }
        empty("Посылок пока нет")
        onRowClick { row -> openSubmission(row) }
    }
}
```

- Таблица занимает всё тело блока: тело без отступов и без сетки, в нём ничего, кроме таблицы. Блок с таблицей
  не принимает `row { }`, блок со строками — `table`; второй `table` в блоке — тоже `IllegalStateException`.
  Шапка, `actions { }` и `footer { }` блока остаются.
- `fetch` получает `PageRequest(offset, limit, sort)` и возвращает `Page(rows, total)`; `sort` — `Sort(key,
  isDescending)` или `null`. Таблица вызывает `fetch` при построении (первая страница без сортировки), при смене
  страницы и сортировки и при `refresh`. Исключение из `fetch` не перехватывается.
- Если `total` уменьшился и текущая страница опустела, таблица переходит на последнюю непустую страницу.
- `pageSize` меньше 1 или таблица без колонок — `IllegalArgumentException`.

### Колонки

| Функция | Значение | Вид |
|---------|----------|-----|
| `textColumn` | `String?` | Обычный текст |
| `codeColumn` | `String?` | Моноширинный (`.ts-num`): идентификаторы, коды |
| `numberColumn` | `Number?` | Вправо, моноширинный, разряды по `UiTexts.locale` |
| `dateColumn` | `LocalDate?` | Формат `UiTexts.calendar.dateFormat` |
| `dateTimeColumn` | `LocalDateTime?` | Дата по тому же формату и время `HH:mm` |
| `column` | `ContentScope.(T) -> Unit` | Любое содержимое `ContentScope`: бейдж, теги, действия |

- У всех колонок параметры `title` и `sortKey = null`; порядок объявления — порядок колонок. `null` в ячейке типовой
  колонки выводится как «—».
- `sortKey` делает заголовок сортируемым: первый клик — по убыванию, повторный — смена направления, клик по другой
  колонке — по убыванию по ней; смена сортировки возвращает на первую страницу. Активный заголовок — `.ts-sorted`
  со стрелкой «↓» или «↑». Сортируемый заголовок получает фокус с клавиатуры, Enter и пробел сортируют как клик,
  направление сообщается атрибутом `aria-sort`.
- `empty(text)` — текст пустого состояния, по умолчанию `UiTexts.table.empty`. Пустая таблица выводит одну строку
  с ячейкой во всю ширину и разметкой `.ts-empty` эталона.
- `onRowClick { row -> }` — клик по строке, строка получает курсор-указатель. Клик по элементу управления в строке
  (флажку, кнопке, ссылке, полю) строку не «кликает».

### Выбор строк

`selectable = true` добавляет первую колонку с флажками и флажок «все на странице» в заголовке: он отмечен, если
выбраны все строки страницы, в промежуточном состоянии — если часть, и выключен на пустой странице. Доступные имена
флажков — `UiTexts.table.selectAll` и `UiTexts.table.selectRow`. Выбор хранится по `key(row)` и сохраняется при смене
страницы, сортировки и `refresh`, в том числе ключи строк, которые после нового запроса не пришли. Выбранная строка —
`.ts-row-selected`.

### Пагинация

Пагинация стоит в подвале блока после его собственного содержимого (`.ts-table-pager`): слева диапазон
«1–20 из 1 412» (`UiTexts.table.range`), справа компактный пейджер эталона «‹ 3 / 71 ›» (`.ts-pager`) с кнопками
`UiTexts.table.previous` и `UiTexts.table.next`, выключенными на первой и последней странице. Если все строки
помещаются на одну страницу, пагинация не выводится, а подвал без собственного содержимого скрывается вместе с ней.

## Диалоги

Диалог — `vaadin-dialog` (модальность, затемнение, ловушка фокуса, закрытие по Esc и клику по фону) с разметкой
`.ts-dialog` эталона внутри; части оверлея Vaadin обнуляются в `testsys-vaadin.css`, как у тоста. Роль диалога —
`dialog` (у опасного подтверждения — `alertdialog`), доступное имя — заголовок. Крестик закрытия в шапке — `iconAction`
с подписью `UiTexts.dialog.close`; у опасного подтверждения шапки нет.

`confirm` и `dialog` — функции верхнего уровня пакета `overlay`. Они берут `UiTexts` из текущего `UI`, к которому
их привязывает `page(...)` страницы; вызов без построенной на этом `UI` страницы — `IllegalStateException`.

### Подтверждение

```kotlin
confirm(
    title = "Удалить тур «Весенний кубок»?", text = "Вместе с туром удалятся все посылки.", action = "Удалить",
    isDanger = true, typeToConfirm = "Весенний кубок",
) { deleteContest() }
```

- `confirm(title, text = null, action, isDanger = false, typeToConfirm = null, onConfirm)` вызывается
  из обработчиков, как `toast`, и открывает диалог шириной 440 px сразу.
- Подвал: «Отмена» (`UiTexts.dialog.cancel`) и кнопка действия. У обычного подтверждения она главная (`mainAction`).
  `isDanger` включает вид `.ts-dialog--alert` (предупреждающая иконка вместо шапки), а кнопка действия получает
  внутреннюю роль `danger` (см. [Оформление](#оформление)).
- `typeToConfirm` ставит между текстом и подвалом поле с подписью `UiTexts.dialog.typeToConfirm(name)` и фокусом
  после открытия. Кнопка действия включена, только когда значение поля без пробелов по краям в точности, с учётом
  регистра, совпадает с `typeToConfirm`.
- Кнопка действия вызывает `onConfirm` и закрывает диалог; если `onConfirm` бросил исключение, диалог остаётся
  открытым. «Отмена», крестик, Esc и клик по фону закрывают диалог без вызова.

### Диалог с формой

```kotlin
val newTour = dialog(title = "Новый тур", subtitle = "Название обязательно") {
    row { textInput("Название", labelSize = 4, size = 8) { binder.forField(this).asRequired("Заполните").bind(…) } }
    footer { dialog ->
        action("Отмена") { onClick { dialog.close() } }
        mainAction("Создать") { onClick { if (binder.writeBeanIfValid(tour)) dialog.close() } }
    }
}
```

- `dialog(title, subtitle = null) { … }` строит диалог шириной 520 px (`.ts-dialog--md`) один раз и возвращает
  `DialogHandle`; открывается он `open()` сколько угодно раз, значения полей между открытиями сохраняются.
- `row { }` — `BlockRowScope` на 12 колонках (`.ts-dialog__grid`): все поля и элементы строки блока, пустые строки
  не выводятся.
- `footer { dialog -> }` — `ContentScope` подвала. Лямбда получает ручку диалога параметром: маркер DSL не пускает
  обработчики кнопок к членам внешнего `DialogScope`, а переменная с ручкой ещё не присвоена, пока диалог строится.
- У диалога своё состояние редактируемости, по умолчанию редактируемо; переключается через `DialogHandle.isEditable`.

## Лукап

`lookup(label, labelSize, size, fetch, display, columns, hint = null, pageSize = 10) { }` в `BlockRowScope` — поле
одной сущности, выбранной в диалоге с поиском и таблицей; возвращает `ValueInput<T?>`. Работает везде, где есть
строка: в блоке и в диалоге с формой. Образец — лукапы тура в `ShowcaseDataSections.kt` витрины.

```kotlin
lookup(
    "Тур", labelSize = 4, size = 8,
    fetch = { query, request -> contests.search(query, request) },
    display = { it.name },
    columns = {
        textColumn("Название") { it.name }
        dateColumn("Начало") { it.startsOn }
    },
) { binder.forField(this).asRequired("Выберите тур").bind(…) }
```

- В ячейке значения — рамка как у поля ввода с нативной кнопкой, на которой текст `display(value)`, иконкой выбора
  (подпись `UiTexts.lookup.open`, вне порядка табуляции) и крестиком очистки (`UiTexts.lookup.clear`) при непустом
  значении. Кнопка значения — единственный элемент поля в порядке табуляции: клик по подписи ставит в неё фокус,
  Enter и пробел открывают диалог, у пустого значения её имя — `UiTexts.lookup.open`. Клик по рамке тоже открывает
  диалог. После очистки крестиком фокус переходит на кнопку значения.
- Выключенное и нередактируемое поле диалог не открывает и иконок не показывает. Кнопка значения в этих состояниях
  помечена `aria-disabled`, без `aria-haspopup`, а у пустого значения её имя — подпись поля; у нередактируемого поля
  она остаётся в порядке табуляции, и значение можно прочитать. Вид — как у остальных полей в этих состояниях.
  `Binder`, звёздочка, ошибки и `isVisible` — как у всех полей.
- Диалог выбора — диалог шириной 520 px с заголовком — подписью поля; пока он открыт, второй не открывается:
  - строка поиска (плейсхолдер и имя `UiTexts.lookup.search`) с фокусом после открытия; запрос без пробелов
    по краям уходит в `fetch` через 300 мс после последнего ввода, таблица возвращается на первую страницу;
  - таблица из `columns` — тот же `TableScope`; пагинация — в подвале диалога, по умолчанию 10 строк
    на странице; пустой результат — `UiTexts.lookup.empty`;
  - текущее значение подсвечено как выбранная строка (сравнение через `equals`);
  - клик по строке выбирает значение и закрывает диалог; выбор и очистка крестиком — изменения значения
    пользователем (`isFromClient`);
  - Esc, крестик и клик по фону закрывают диалог без изменений.
- `pageSize` меньше 1, `columns` без колонок или с `empty`/`onRowClick` (их задаёт сам лукап) —
  `IllegalArgumentException`.

## Оформление

Страница не задаёт стили: вид выбирает DSL по смыслу элемента и месту.

| Что | Как выбирается вид |
|-----|--------------------|
| Кнопки | Роль: `mainAction` — primary, `action` — secondary, `destructiveAction` — danger-soft, `linkAction` — link, `iconAction` — secondary без подписи (подпись уходит в `aria-label` и подсказку). Внутренняя роль `danger` (заливка `--danger`, как `.ts-btn--danger`) — только у кнопки действия опасного `confirm`, страницам она недоступна. Размер: в `actions { }` — sm, в группах строк блока и в `footer { }` — md |
| Блок | `block` — обычный, `highlightBlock` — тёмный. Тело без отступов включает содержимое, которому они мешают: `table` |
| Бейдж, тег, счётчик, алерт, тост (`toast(kind, title)` — функция верхнего уровня, вызывается из обработчиков) | Перечисления смысла: `Tone`, `TagKind`, `CounterKind`, `FeedbackKind`; текст бейджа передаёт страница |
| Таблица, диалог | Вид ячейки — по функции колонки (см. [Колонки](#колонки)); ширина диалога — по функции: `confirm` — 440 px, `dialog` и диалог лукапа — 520 px; `isDanger` — вид `.ts-dialog--alert` |
| Раскладка в блоке | Строки `row { }`; в строке — группы `horizontal(size) { }` и `vertical(size) { }` на своих колонках, в шапке, подвале и группах — `horizontal { }` и `vertical { }` без размера |

## Ручки и служебные свойства

Функции возвращают ручку. Действия и поля ввода принимают завершающую лямбду `configure` с ней же; элементы
отображения (`text`, `tag`, `badge`, `counter`, `statCard`, …) и `field { }` возвращают ручку без `configure`:

```kotlin
mainAction("Отправить решение", icon = IconName.Upload) {
    isEnabled = canSubmit
    onClick { isLoading = true; submit() }
}
```

| Ручка | Свойства |
|-------|----------|
| `ElementHandle` | `isVisible` |
| `TextHandle` | `isVisible`, `text` (значение `statCard`, `counter`, `text`) |
| `ActionHandle` | `isVisible`, `isEnabled`, `isLoading`, `onClick` |
| `BlockHandle` | `isVisible`, `isEditable` (возвращают `block` и `highlightBlock`) |
| `TableHandle<T>` | `isVisible` (скрывает таблицу с пагинацией, блок остаётся), `refresh(toFirstPage = false)`, `selected` (ключи выбранных строк), `clearSelection()`, `onSelectionChange { keys -> }` |
| `DialogHandle` | `open()`, `close()`, `isOpen`, `isEditable`, `onClose { }` (при любом закрытии: кнопкой, крестиком, Esc, кликом по фону) |
| `ValueInput<T>` | `isVisible`, `isEnabled`, `isEditable` и всё из `HasValue`, `HasValidation`, `HasValidator` |

`ValueInput` привязывается к `Binder` как обычное поле: `binder.forField(input)`. Ручка не является компонентом
Vaadin, поэтому `Binder` не пропускает скрытое поле сам: чтобы не проверять его, пока оно скрыто, вызовите
у привязки `setIsAppliedPredicate { input.isVisible }`. Ограничения полей (`min`, `max`,
`step`, диапазон дат) проверяет валидатор по умолчанию, сообщения об ошибках берутся из `UiTexts`.

### Редактируемость

Поле редактируемо, только если редактируемы и оно само (`ValueInput.isEditable`), и его блок
(`BlockHandle.isEditable`); по умолчанию оба `true`. Нередактируемое поле — `readOnly` в Vaadin: тип и формат
значения сохраняются, значение можно выделить и скопировать, из кода оно читается и записывается; вид — пунктирная
рамка без заливки. Выключенное поле (`isEnabled = false`) остаётся серым, даже если оно ещё и нередактируемо.
`Binder.setReadOnly` работает через `isEditable`.

`editing(onSave, onCancel)` в `BlockScope` — стандартный переключатель режима блока, в том числе у `highlightBlock`:

- блок открывается в режиме просмотра (`isEditable = false`);
- DSL ставит кнопки в конец шапки, после содержимого `actions { }`, и выводит шапку даже у блока без заголовка:
  в просмотре — «Изменить» (`action` с иконкой карандаша), в редактировании — «Отмена» (`action`) и «Сохранить»
  (`mainAction`);
- «Изменить» включает редактирование; «Сохранить» вызывает `onSave` и возвращает просмотр, если тот вернул `true`,
  иначе блок остаётся в редактировании с ошибками у полей; «Отмена» вызывает `onCancel` и возвращает просмотр;
- `BlockHandle.isEditable`, изменённый из кода, переключает и поля, и кнопки;
- повторный вызов `editing` в том же блоке — `IllegalStateException`.

Блок без `editing` редактируем, пока страница не выключит его через `BlockHandle.isEditable`.

## Тексты

Модуль не зависит от локализации. Все строки, которые компоненты показывают сами (бренд, «Войти», кнопки режима
редактирования, календарь, ошибки полей, подписи таблицы, диалогов и лукапа), приходят в `UiTexts`. Строки таблицы,
диалогов и лукапа собраны в группы `table`, `dialog` и `lookup` с ключами `ui.table.*`, `ui.dialog.*`
и `ui.lookup.*`. Приложение собирает `UiTexts` функцией `buildUiTexts` в `testsys-web` из ключей `ui.*` модуля
локализации и данных ICU. Строки страницы передаются в функции DSL как `String`; правила локализации —
в [localization/README.md](../../testsys-infra/localization/README.md).

Функции верхнего уровня (`confirm`, `dialog`) не видят скоупов DSL, поэтому `page(...)` привязывает `UiTexts`
страницы к текущему `UI`, а диалоги берут их оттуда (см. [Диалоги](#диалоги)).

## Запасной выход

`custom(component)` в `ContentScope` ставит на страницу любой компонент Vaadin; в строке блока он кладётся в группу
`horizontal`/`vertical`. Функция помечена
`@RequiresOptIn` аннотацией `RawVaadin`: странице нужен `@OptIn(RawVaadin::class)`, и это видно на ревью.
Если компонент нужен больше чем на одной странице, его добавляют в DSL.

## Как добавить компонент

1. Найдите эталон в `design-system/components/<группа>/` и решите, чем он станет: разметкой `.ts-*` (статический
   компонент) или компонентом Vaadin (ввод, фокус, клавиатура, оверлеи). Сложный интерактивный компонент без аналога
   в Vaadin подключается как React через `ReactAdapterElement`.
2. Добавьте функцию-расширение нужного скоупа в пакет группы: элемент отображения — для `ContentScope` и для
   `BlockRowScope` (с `size`), поле — для `BlockRowScope`. Параметры описывают смысл, а не вид; вид выбирается
   внутри по смыслу и `ContentScope.placement`. Функция возвращает ручку и принимает `configure`. Пакеты групп
   зависят от `layout`, где лежат скоупы; обратная зависимость одна и намеренная: `layout.EditingSwitch` строит
   стандартные кнопки переключателя `editing` функциями пакета `actions`.
3. CSS: общие для React и Vaadin классы — в `design-system/tokens/components.css`, правила только для DOM
   Vaadin — в `testsys-vaadin.css`.
4. Встроенные строки компонента добавьте в `UiTexts` и в `buildUiTexts`, а ключи — по
   [add-localization.md](../../docs/guides/add-localization.md).
5. Тесты — Karibu-Testing (`MockVaadinTests`, `buildTestPage` в тестовых исходниках модуля): разметка совпадает
   с эталоном, смысл превращается в нужный класс или атрибут, ручка меняет состояние.
6. Покажите компонент на витрине `/dev/showcase` (`testsys-web`, профиль `dev`) и сверьте с эталоном.
