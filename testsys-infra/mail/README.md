# Модуль `testsys-infra:mail`

Модуль отправляет письма Пользователям по SMTP: реализует порт
[`UserMailSender`](../../testsys-domain/src/main/kotlin/tech/testsys/domain/contract/Mail.kt).
Документ описывает адаптер и его настройки для разработчиков и тех, кто собирает приложение.
Когда и какие письма отправляются, описано в фичах `testsys.user.registration`, `testsys.user.multi.changeMail`
и `testsys.user.multi.restoreAccess` в [features.md](../../docs/domain/features.md). Тексты писем — в бандле `user`
([user.properties](../localization/src/main/resources/localization/ru-RU/user.properties)), правила сообщений —
в [localization/README.md](../localization/README.md) и [add-localization.md](../../docs/guides/add-localization.md).

## Компоненты

| Компонент | Назначение |
|-----------|------------|
| `api/SmtpUserMailSender` | Реализация порта: собирает письмо и передаёт его в `JavaMailSender` |
| `api/MailConfiguration` | Бины модуля и настройки по умолчанию |
| `internal/MailSettings` | Адрес отправителя, регион и часовой пояс писем |
| `internal/MailProperties` | Параметры `testsys.mail.*` без пароля, которые `MailConfiguration` привязывает через `Binder` |

Внутренний API помечен `InternalMailApi`.

`SmtpUserMailSender` отправляет письмо одному получателю простым текстом в UTF-8.
Тему и текст письма адаптер берёт из бандла `user` модуля локализации в регионе из настроек;
своего текста он не добавляет. Каждый вызов создаёт свой экземпляр `Localization`.
Письма с кодом подтверждения при регистрации, смене почты и восстановлении доступа не содержат Псевдоним.
Уведомление о смене почты содержит Псевдоним и не содержит новую почту. Писем с Кодом-доступа или Паролем
модуль не отправляет.

Письмо собирается при вызове, а момент отправки зависит от синхронизации транзакций Spring
(`TransactionSynchronizationManager`):

- Если синхронизация активна, адаптер регистрирует `TransactionSynchronization` и отправляет письмо
  в `afterCommit`. При откате письмо не отправляется. `MailException` после фиксации адаптер не пробрасывает,
  а записывает в лог через SLF4J с уровнем `ERROR` и адресом получателя.
  Зафиксированные изменения остаются в силе. Такой сбой адаптер также учитывает как технический сбой
  `mail`/`after_commit`, см. [monitoring/README.md](../monitoring/README.md#учёт-технических-сбоев).
- Если синхронизации нет, адаптер отправляет письмо сразу. Исключения `JavaMailSender` выходят к вызывающему
  коду без преобразования.

## Настройки

Приложение подключает `MailConfiguration`. Значения по умолчанию загружаются из
[mail-defaults.properties](src/main/resources/mail-defaults.properties), приложение переопределяет их
через Spring Environment. Значения по умолчанию подходят только для локального SMTP-сервера без авторизации;
сами значения и место их задания в окружениях — в таблице раздела «Параметры приложения»
в [deployment.md](../../docs/project/deployment.md#параметры-приложения).

`MailConfiguration` один раз привязывает свойства через `Binder` к `MailProperties`. Пароль привязывается
отдельно, поэтому отчёт о неудачной привязке его не показывает. С отсутствующим или неверным значением
приложение не запустится.

| Свойство | Назначение |
|----------|------------|
| `testsys.mail.host` | Адрес SMTP-сервера |
| `testsys.mail.port` | Порт SMTP-сервера |
| `testsys.mail.username` | Имя пользователя; пустое значение отключает передачу учётных данных |
| `testsys.mail.password` | Пароль |
| `testsys.mail.smtp-auth` | Значение `mail.smtp.auth`; при `true` пустые имя пользователя или пароль отклоняются при создании бина |
| `testsys.mail.ssl` | TLS с момента подключения (implicit TLS), значение `mail.smtp.ssl.enable` |
| `testsys.mail.starttls` | Значение `mail.smtp.starttls.enable` |
| `testsys.mail.timeout` | Предельный срок соединения, чтения и записи; ISO-8601 `Duration` или единицы Spring, например `PT10S` или `10s` |
| `testsys.mail.from` | Адрес отправителя; пустое значение отклоняется при создании бина |
| `testsys.mail.region` | Регион сообщений, значение `SupportedRegion` |
| `testsys.mail.time-zone` | Часовой пояс контекста локализации |

Для SMTP-сервера с implicit TLS на порту 1127 задайте `testsys.mail.ssl=true`,
`testsys.mail.starttls=false` и `testsys.mail.port=1127`.
