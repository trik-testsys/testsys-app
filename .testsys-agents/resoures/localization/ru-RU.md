# ru-RU language research notes

Research notes for Russian (`ru-RU`) source messages. They are data for the localization roles; the project
glossary and owner documents stay authoritative.

## Polite address: «Вы» / «вы»

- Capital «Вы», «Ваш» marks politeness towards one specific addressee in letters and official documents.
  For official messages to one person the capital form is expected; in personal letters it is the writer's choice.
  Lowercase «вы» is used in mass texts: newspaper publications, advertising and messages to users on web pages.
- Applicability to TestSys: a transactional e-mail sent to one recipient's mailbox (registration letters, whether
  or not they greet by nickname) fits the "one specific addressee" case, so «Вы» is appropriate. Interface text on web pages addressed to any
  visitor follows the lowercase mass-text usage unless the project decides otherwise. Lowercase «вы» is not an error
  and is not perceived as rude.
- Uncertainty: the dictionary wording (Лопатин, Нечаева, Чельцова, «Прописная или строчная?», 2011: «Вы, Ваш как
  форма выражения вежливости при обращении к одному конкретному лицу в письмах, официальных документах») was seen
  only as a secondary quotation, not in the dictionary itself.
- Sources: https://rg.ru/2009/06/25/vy.html (read 2026-10-08); https://mel.fm/gramotnost/3856279-you (search
  snippet only, 2026-10-08). gramota.ru returned HTTP 403 on 2026-10-08.

## «Код-доступа» (domain term) versus «код доступа»

- The domain term in `docs/domain/definitions.md` is spelled «Код-доступа». In general Russian the phrase is
  «код доступа»: a noun with a dependent noun in the genitive, written separately, like «пароль доступа».
- The hyphen rule for compound nouns (Правила русской орфографии и пунктуации, § 79, п. 1) covers words with the
  meaning of one word made of two independently used nouns without a connecting vowel (жар-птица,
  изба-читальня); a noun plus a genitive noun is not such a compound. Conclusion that the hyphenated spelling is
  non-standard is an inference from this rule; no dictionary entry for «код доступа» was found.
- Applicability: registration e-mails use the domain spelling through `glossary.access_code`; future interface
  messages, such as the sign-in page, should reference the same term. A change of spelling needs one edit in the
  glossary term.
- Source: https://www.azbyka.ru/otechnik/Spravochniki/pravila-russkoj-orfografii-i-punktuatsii/18 (read
  2026-10-08). The academic dictionary search at orfo.ruslang.ru returned no usable result on 2026-10-08.
- Independent review (2026-10-08) re-read § 79: point 14, note 3 also excludes a hyphen when a general concept is
  followed by a specific one («птица зяблик»); nothing in § 79 supports «Код-доступа». Still an inference from the
  rules, not a dictionary entry.
- Usage evidence (reported by the independent review, 2026-10-08; usage, not the norm, Russian-language text from
  Kazakhstan): https://guide.kaspi.kz/client/ru/bank/security/q921 writes two lowercase words — «Код доступа может
  состоять из 4 или 6 цифр.», «Никому не сообщайте код доступа…». It also shows «Никому не сообщайте код» as an
  established phrase in security warnings.
- Unavailable on 2026-10-08: gramota.ru (HTTP 403), dict.leo.org (HTTP 403), ru.wiktionary.org/wiki/код_доступа
  (HTTP 404, no entry).
- Project decision (2026-10-08): user-facing text keeps the domain spelling «Код-доступа».

## Case sensitivity: «Большие и маленькие буквы различаются» instead of «регистр»

- Case sensitivity is explained as «Большие и маленькие буквы различаются» rather than with the jargon word
  «регистр», which also has unrelated everyday meanings (official register, voice or organ register). This is a
  project wording choice, not a dictionary finding.

## Greeting without a name: «Здравствуйте!»

- «Здравствуйте!» on its own is a neutral, polite opening for a letter whose addressee's name is not used.
  Контур.Толк lists «Здравствуйте» and «добрый день» as equally appropriate in messengers and in official letters
  to partners or state bodies, and calls «Доброго времени суток» a cliché to avoid. «Мел» calls «добрый день» or
  «здравствуйте» the universal greeting when the reader may be in another time zone; the time-zone claim comes
  from «Мел» only.
- Applicability to TestSys: the registration confirmation-code e-mail goes to a not-yet-confirmed address and must
  not contain user-controlled text such as the nickname, so it opens with «Здравствуйте!». Avoiding «Добрый день!» is
  a project wording choice (the letter may be read at any time), not a source requirement: «Мел» also treats it as
  universal. A greeting without a name is not an error and is not perceived as cold in a transactional letter.
- Sources: https://kontur.ru/talk/spravka/47867-pochemu_ne_stoit_pisat_dobrogo_vremeni_sutok (Контур.Толк,
  21.02.2024, read 2026-10-08); https://mel.fm/pravopisaniye/8350471-hello («Мел», 28.12.2016, read 2026-10-08).
  These are style guides, not dictionaries; no normative source specific to name-less greetings was found.

## Comma before «и» after an «если» clause

- Rule: «Запятая перед союзами и, да (в значении „и“), или, либо не ставится, если соединяемые ими предложения
  имеют общий второстепенный член или общее придаточное предложение» (Правила русской орфографии и пунктуации,
  1956, note to § 137).
- Applicability (reviewer's inference): in «Если код больше не действует, начните регистрацию заново, и Вам придёт
  новый код» the second part is the result of restarting registration, not a second consequence of the
  condition, so the comma is defensible.
- Source: https://www.azbyka.ru/otechnik/Spravochniki/pravila-russkoj-orfografii-i-punktuatsii/29 (read
  2026-10-08). Unverified: gramota.ru snippets cite Lopatin (2006) § 112 (same exception) and § 114 (a dash for
  result or consequence); the pages returned HTTP 403 on 2026-10-08.

## «Код больше не действует» covers expiry and exhausted attempts

- «Действовать» means «Быть в исправности, работать» and «Применяться, иметь действие» (Ушаков); «Быть в
  исправности, исправно работать; функционировать» (МАС). «Больше не действует» is therefore not limited to an
  expired lifetime.
- Applicability (inference): the confirmation-code e-mail uses «Если код больше не действует…» for both an expired
  code and exhausted attempts; both make the request inactive, and the next request gets a new code.
- Project decision (2026-10-08): the confirmation-code e-mail does not mention the limit on input attempts; do not
  reintroduce attempt wording.
- Source: https://kartaslov.ru/значение-слова/действовать (Ушаков and МАС as reproduced there, read 2026-10-08).
  No usage example of the exact phrase was found.
- Applicability to e-mail change (2026-10-08): the confirmation-code letter for an e-mail change reuses «Если код
  больше не действует…» with the same meaning; the change request also becomes inactive on expiry or exhausted
  attempts (features.md, testsys.user.multi.changeMail).

## «Адрес электронной почты» instead of bare «почта»

- In the dictionaries reproduced on kartaslov.ru, the main sense of «почта» is the postal service: «Учреждение
  связи, ведающее пересылкой писем, периодических изданий, посылок, денег и т. п.» (МАС, sense 1); Ушаков has no
  e-mail sense. Викисловарь sense 4 (comp.) defines e-mail as a means of communication («средства электронной
  коммуникации, позволяющие получать и отправлять цифровые послания»), not as an address. No dictionary on that
  page defines «почта» as an e-mail address; that use is colloquial metonymy.
- Applicability: the project documents say «почта» for the address, but user-facing e-mails use the full «адрес
  электронной почты», as the registration confirmation letter already does. The e-mail change letters follow it;
  a repeated mention in the same paragraph may shorten to «адрес» when the referent is unambiguous («запросите
  смену адреса заново»).
- Source: https://kartaslov.ru/значение-слова/почта (МАС, Ушаков, Викисловарь as reproduced there, read
  2026-10-08). A web search for a Russian UX style guide on «почта» versus «адрес электронной почты» found no
  normative source; Yandex Forms help uses the full form, but that is one product's usage
  (https://yandex.ru/support/forms/ru/blocks-ref/email, search result only, not opened).

## Accusative of «Код-доступа»

- «Код» is an inanimate masculine noun, so its accusative singular equals the nominative: «знает Ваш
  Код-доступа». The glossary term has an explicit `acc sg` variant with the same text as `nom sg` because the
  codegen requires every referenced case explicitly.
- Source: https://ru.wiktionary.org/wiki/код (read 2026-10-08 by the independent reviewer): «код» is
  «неодушевлённое, мужской род, 2-е склонение (тип 1a по Зализняку)», Им. ед. = Вин. ед. «код».

## «Смена адреса … на этот адрес»

- МАС, as reproduced on kartaslov.ru, defines «смена» as «действие по значению гл. менять; изменение или
  замещение», which supports «смена X на Y» for one address replacing another. «Смена»/«сменить» in the
  confirmation-code letter and «изменён»/«изменить» in the notice are both standard and not a terminology conflict.
- Source: https://kartaslov.ru/значение-слова/смена (read 2026-10-08 by the independent reviewer).
- Applicability: in the e-mail change, the code also stops working when a later request to another address
  overwrites the request; «запросите смену адреса заново» stays correct (features.md, testsys.user.multi.changeMail,
  step 4 of the request). This is an inference from the feature, not a language source. A search for the exact
  wording «Вы запросили смену адреса электронной почты … Если вы не запрашивали» found no usage evidence either way.

## E-mail change letters: content limits

- Project constraints (from the coordinator, 2026-10-08): the notice to the previous address must not show the
  new address and must not suggest actions TestSys does not offer (contacting support, restoring access,
  cancelling the change). It states the fact, that further letters go to the new address, and that only someone
  who knows the Код-доступа can change the address. This is a project decision, not a language finding.
- The confirmation-code letter to the new address uses «Вы запросили … Если Вы не запрашивали …», mirroring the
  registration letter's «Вы начали … Если Вы не начинали …»: the recipient may not be the account owner, and the
  closing paragraph covers that case. It names no page («введите в TestSys этот код»), because the page that
  accepts the code is not implemented yet.
