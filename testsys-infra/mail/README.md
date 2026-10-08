# Модуль `testsys-infra:mail`

Модуль отправляет письма Пользователям по SMTP: реализует порт
[`UserMailSender`](../../testsys-domain/src/main/kotlin/tech/testsys/domain/contract/Mail.kt).
Документ описывает адаптер и его настройки для разработчиков и тех, кто собирает приложение.
Когда и какие письма отправляются, описано в фичах `testsys.user.registration` и `testsys.user.multi.changeMail`
в [features.md](../../docs/domain/features.md). Тексты писем — в бандле `user`
([user.properties](../localization/src/main/resources/localization/ru-RU/user.properties)), правила сообщений —
в [localization/README.md](../localization/README.md) и [add-localization.md](../../docs/guides/add-localization.md).

## Компоненты

| Компонент | Назначение |
|-----------|------------|
| `api/SmtpUserMailSender` | Реализация порта: собирает письмо и передаёт его в `JavaMailSender` |
| `api/MailConfiguration` | Бины модуля и настройки по умолчанию |
| `internal/MailSettings` | Адрес отправителя, регион и часовой пояс писем |

Внутренний API помечен `InternalMailApi`.

`SmtpUserMailSender` отправляет письмо одному получателю простым текстом в UTF-8.
Тему и текст письма адаптер берёт из бандла `user` модуля локализации в регионе из настроек;
своего текста он не добавляет. Каждый вызов создаёт свой экземпляр `Localization`.
Письма с кодом подтверждения при регистрации и при смене почты не содержат Псевдоним. Письмо с Кодом-доступа
и уведомление о смене почты содержат Псевдоним; уведомление не содержит новую почту.

Письмо собирается при вызове, а момент отправки зависит от синхронизации транзакций Spring
(`TransactionSynchronizationManager`):

- Если синхронизация активна, адаптер регистрирует `TransactionSynchronization` и отправляет письмо
  в `afterCommit`. При откате письмо не отправляется. `MailException` после фиксации адаптер не пробрасывает,
  а записывает в лог через Commons Logging (`spring-jcl`) с уровнем `ERROR` и адресом получателя.
  Зафиксированные изменения остаются в силе.
- Если синхронизации нет, адаптер отправляет письмо сразу. Исключения `JavaMailSender` выходят к вызывающему
  коду без преобразования.

## Настройки

Приложение подключает `MailConfiguration`. Значения по умолчанию загружаются из
[mail-defaults.properties](src/main/resources/mail-defaults.properties), приложение переопределяет их
через Spring Environment. Значения по умолчанию подходят только для локального SMTP-сервера без авторизации.

| Свойство | По умолчанию | Назначение |
|----------|--------------|------------|
| `testsys.mail.host` | `localhost` | Адрес SMTP-сервера |
| `testsys.mail.port` | `25` | Порт SMTP-сервера |
| `testsys.mail.username` | пусто | Имя пользователя; пустое значение отключает передачу учётных данных |
| `testsys.mail.password` | пусто | Пароль |
| `testsys.mail.smtp-auth` | `false` | Значение `mail.smtp.auth` |
| `testsys.mail.starttls` | `false` | Значение `mail.smtp.starttls.enable` |
| `testsys.mail.timeout` | `PT10S` | Предельный срок соединения, чтения и записи в формате ISO-8601 `Duration` |
| `testsys.mail.from` | `testsys@localhost` | Адрес отправителя; пустое значение отклоняется при создании бина |
| `testsys.mail.region` | `RU` | Регион сообщений, значение `SupportedRegion` |
| `testsys.mail.time-zone` | `Europe/Moscow` | Часовой пояс контекста локализации |
