# Модуль `testsys-infra:crypto`

Модуль хеширует и проверяет Пароли Пользователей с Нефиксированными Ролями: реализует порт
[`PasswordHasher`](../../testsys-domain/src/main/kotlin/tech/testsys/domain/contract/Password.kt). Документ
предназначен для тех, кто меняет хранение Паролей или собирает приложение. Требования к Паролю — в фичах
`testsys.user.authentication`, `testsys.user.registration`, `testsys.user.multi.changePassword`
и `testsys.user.multi.restoreAccess` в [features.md](../../docs/domain/features.md), проверку длины Пароля выполняет
`isValidPassword` из [testsys-operation/README.md](../../testsys-operation/README.md#регистрация).

## Компоненты

| Компонент | Назначение |
|-----------|------------|
| `api/SpringPasswordHasher` | Реализация порта на `DelegatingPasswordEncoder` из Spring Security Crypto |
| `api/CryptoConfiguration` | Сканирует пакет `api`; приложение подключает её через `@Import` в `InfraConfiguration` |

Модуль зависит от `testsys-domain`, Spring Context и `spring-security-crypto`; версии задаёт BOM Spring Boot.

## Хеширование

`SpringPasswordHasher` получает кодировщик из `PasswordEncoderFactories.createDelegatingPasswordEncoder()`.
Он хеширует Пароль bcrypt со стоимостью 10 и новой солью на каждый вызов и хранит хеш с префиксом алгоритма:
`{bcrypt}$2a$10$…`. По префиксу кодировщик проверяет и хеши, созданные другим алгоритмом из своего набора, поэтому
алгоритм можно сменить без переноса сохранённых хешей.

bcrypt учитывает только первые 72 байта Пароля. Адаптер не хеширует Пароль длиннее 72 байт в UTF-8: `hash` бросает
`IllegalArgumentException`, а `matches` возвращает `false`. Операции отклоняют такой Пароль раньше.

## Выравнивание времени

При создании бина адаптер один раз считает хеш фиктивного Пароля. `matches` без сохранённого хеша (`null`) и для
Пароля длиннее 72 байт сравнивает с ним фиктивный Пароль и возвращает `false`. Поэтому ответ для непривязанной почты
занимает примерно столько же времени, сколько проверка настоящего Пароля.

## Тесты

[SpringPasswordHasherTests](src/test/kotlin/tech/testsys/infra/crypto/api/SpringPasswordHasherTests.kt) проверяет
формат хеша, разные хеши одного Пароля, точное сравнение и отказы для `null` и Пароля длиннее 72 байт. Docker
тестам модуля не нужен.
