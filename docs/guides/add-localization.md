# Добавление локализации

Гайд описывает, как добавить новое локализованное сообщение или новый регион. Он предназначен для разработчиков,
которым нужна строка, видимая пользователю.

Формат ключей, типы плейсхолдеров, устройство `glossary` и правила использования API — в
[localization/README.md](../../testsys-infra/localization/README.md); здесь только последовательность действий.

## Чек-лист

| # | Шаг                                   | Куда                                                                   |
|---|---------------------------------------|------------------------------------------------------------------------|
| 1 | Добавить ключ во все регионы          | `testsys-infra/localization/src/main/resources/localization/*.properties` |
| 2 | Сгенерировать API                     | `./gradlew :testsys-infra:localization:compileKotlin`                  |
| 3 | Вызвать сгенерированный метод         | Код, который показывает строку пользователю                            |
| 4 | Добавить регион (если нужен новый)    | `SupportedRegion`, `<REGION>.properties`                               |
| 5 | Проверить                             | Тесты кодгена                                                          |

## 1. Добавить ключ во все регионы

- Ключ добавляется в `RU.properties` и в **каждый** существующий `<REGION>.properties`: ключ, отсутствующий
  хотя бы в одном регионе, — ошибка кодгена.
- Формат ключа и паттерна — в разделах «Формат ключа» и «Поддерживаемые типы плейсхолдеров»
  в [localization/README.md](../../testsys-infra/localization/README.md).
- Если перевод ещё не готов, в регион кладётся заглушка, а не хардкод в Kotlin.
- Если в сообщение подставляется доменный термин, проверьте, есть ли он в `glossary`, и назовите плейсхолдер
  по правилу из раздела «Имя плейсхолдера для glossary-термина» в [localization/README.md](../../testsys-infra/localization/README.md).
  Образец такого сообщения — ключ `task.result`
  в [RU.properties](../../testsys-infra/localization/src/main/resources/localization/RU.properties).

## 2. Сгенерировать API

Генерация привязана к компиляции модуля, отдельно запускать её не нужно:

```bash
./gradlew :testsys-infra:localization:compileKotlin
```

Ошибки про недостающие ключи, конфликты типов и некорректные имена падают на этом шаге.

## 3. Вызвать сгенерированный метод

Сообщение вызывается методом на сгенерированном классе с именованными параметрами. Правила вызова — в разделе
«Использование в коде» в [localization/README.md](../../testsys-infra/localization/README.md).

## 4. Добавить регион

Шаг нужен, только если добавляется новый язык.

1. Добавьте значение в [SupportedRegion.kt](../../testsys-infra/localization/src/main/kotlin/tech/testsys/infra/localization/bundle/SupportedRegion.kt).
2. Создайте `<REGION>.properties` со **всеми** ключами из `RU.properties`.
3. Сгенерируйте API (шаг 2). Если какого-то ключа не хватает, сборка упадёт с его указанием.

## 5. Проверить

Тесты парсера и мерджера кодгена:

```bash
./gradlew :testsys-infra:localization:codegenTest
```

Общие команды сборки — в разделе «Сборка» в [structure.md](../project/structure.md).
