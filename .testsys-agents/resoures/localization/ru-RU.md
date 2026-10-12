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

## «Футер», not «футтер»

- The borrowing from English "footer" is spelled with one «т»: Викисловарь has the entry «футер» (фу́тер), sense 2
  marked as computing jargon, «нижний колонтитул; подвал». The page does not mention «футтер». «Футер» is also a
  textile term (a knitted fabric), so a reader can resolve the jargon sense only from context.
- Usage evidence: a blog post on spark.ru says its editors did not know whether to write «футтер» or «футер»,
  so the double «т» is a common slip, not an accepted variant. No orthographic dictionary entry was opened.
- Applicability: `ui_page.footer.links` keeps «Ссылки футтера» verbatim because the 2026-10-11 migration requires the
  rendered output to stay unchanged. It is an accessible name read by screen readers. If the coordinator allows
  a wording change, «Ссылки футера» fixes the spelling. Non-jargon alternatives for a general audience, such as
  «Ссылки в подвале сайта» or «Нижняя навигация», are suggestions only and were not checked against a source.
- Sources: https://ru.wiktionary.org/wiki/футер (read 2026-10-11);
  https://spark.ru/startup/522832472692b/blog/1835/novij-futer (search snippet only, 2026-10-11).
- Independent review re-read the Викисловарь entry on 2026-10-11 with the same result. The project documentation
  (components README, ui-design.md) also writes «футтер», so a fix of the message may need the same docs change.
- Project decision (2026-10-12): the user approved «Ссылки футера»; `ui_page.footer.links` now uses one «т». The same
  change replaced «футтер» with «футер» in the project documentation (components README, ui-design.md), so the
  note above about the docs is historical.
- Independent review re-read the Викисловарь entry on 2026-10-12: «фу́тер», «Существительное, неодушевлённое,
  мужской род, 2-е склонение», genitive singular «фу́тера»; sense 2 «комп. жарг.», «в вёрстке»: «нижний колонтитул;
  подвал». No «футтер» on the page. This supports «Ссылки футера».

## Free text after a transitive verb: «Убрать {$value}»

- «Убрать» is transitive (Викисловарь: «Глагол, совершенный вид, переходный»), so its object takes the accusative.
  A nominative placeholder after it renders a wrong case for feminine nouns in -а/-я and animate masculine names
  («Убрать Маша», «Убрать Задача про лабиринт»); it reads correctly only for inanimate masculine or neuter values.
- Patterns that keep free nominative text grammatical, already used in the `ui_*` bundles: a colon
  («Меню пользователя: {$name}») or guillemets («Введите «{$name}», чтобы подтвердить»). This is the reviewer's
  inference, not a dictionary statement.
- Applicability: `ui_form.lookup.remove` keeps «Убрать {$value}» verbatim in the 2026-10-11 migration; a change such as
  «Убрать: {$value}» needs explicit approval.
- Source: https://ru.wiktionary.org/wiki/убрать (read 2026-10-11).
- Project decision (2026-10-12): the user approved guillemets, «Убрать «{$value}»» («Убрать «Маша»»).
- Quoted names and declension: «Мел» (08.09.2022, a popular outlet citing no handbook) states «если рядом есть
  родовое слово, то название в кавычках не склоняется»; without a generic word the name declines like an ordinary
  noun («работать в «Газпроме»» / «в компании «Газпром»»). Independent review (2026-10-12) reads the guillemets in
  `ui_form.lookup.remove` as a verbatim quote of arbitrary data, like «нажмите «Войти»», and does not call it a defect;
  the strictly normative form would add a generic word («Убрать значение «{$value}»»). A `$value` that already
  contains guillemets gives nested «». Rosental's own text was not found.
- Source: https://mel.fm/gramotnost/kak-pisat/8935620-gramotnost-na-mele-ili-gramotnost-na-mel-nuzhno-li-sklonyat-slova-v-kavychkakh
  (read 2026-10-12). The Microsoft Russian Localization Style Guide PDF returned no readable text on 2026-10-12.

## «Код-приглашение»: both parts decline

- The domain term (definitions.md, item 30) is a hyphenated compound of two independent nouns, the § 79 п. 1
  pattern, unlike «Код-доступа» (noun plus fixed genitive, only the first part declines: «по Коду-доступа»).
  Both parts of such compounds decline: Викисловарь gives «дом-музей», «дома-музея», «дому-музею», Вин. =
  Им. «дом-музей», «неодушевлённое, мужской род». By analogy: «Кода-приглашения», «Коду-приглашению», accusative
  equal to nominative; agreement follows the masculine first part («Код-приглашение недействителен»,
  «Код-приглашение, полученный …»). The analogy is an inference; no dictionary entry for the term itself exists.
- Applicability: `glossary.invite_code` holds nom/gen/dat/acc sg; page messages reference it.
- Source: https://ru.wiktionary.org/wiki/дом-музей (read 2026-10-12);
  https://www.azbyka.ru/otechnik/Spravochniki/pravila-russkoj-orfografii-i-punktuatsii/18 (§ 79, read 2026-10-08).
  A search for Розенталь's declension rule returned no readable primary text on 2026-10-12.
- Independent review (2026-10-12): Викисловарь «код» gives Им. «код», Р. «ко́да», Д. «ко́ду», В. «код»
  (inanimate masculine, type 1a), supporting «Коду-доступа», «Кода-приглашения», «Коду-приглашению». A closer
  analogue of a masculine plus neuter compound, «город-государство», declines both parts: Р. «го́рода-госуда́рства»,
  Д. «го́роду-госуда́рству», В. = Им. «го́род-госуда́рство». «Диван-кровать» is listed as masculine (Р. «дивана-кровати»), which supports
  masculine agreement after the first part; this remains an inference without a primary rule text.
- Sources: https://ru.wiktionary.org/wiki/код, https://ru.wiktionary.org/wiki/город-государство,
  https://ru.wiktionary.org/wiki/диван-кровать (all read 2026-10-12).
- Instrumental singular and nominative plural (independent review, 2026-10-12): Викисловарь gives Тв. ед.
  «до́мом-музе́ем» and Им. мн. «дома́-музе́и» (В. мн. = Им. мн.), and «го́родом-госуда́рством» /
  «города́-госуда́рства» for the masculine plus neuter analogue. By analogy `glossary.invite_code` `ins sg`
  «Кодом-приглашением» and `nom pl` «Коды-приглашения» (`page_administrator`); an inference, no entry for the term itself.
- Sources: https://ru.wiktionary.org/wiki/дом-музей, https://ru.wiktionary.org/wiki/город-государство (read
  2026-10-12).
- Lowercase «к сообществу» in `page_home.join_hint`, `page_home.join.title` and `page_home.joined` reproduces the
  page literals verbatim; definitions.md capitalizes terms («Сообщество»). Changing it needs user approval.

## Grouping separator of `:integer` in `ru`

- ICU4J 78.1 `:integer` renders `ru` digit groups with U+00A0 NO-BREAK SPACE («1 412»), the same as JDK 21
  `java.text.NumberFormat.getIntegerInstance(ru-RU)` (checked locally 2026-10-12 with Corretto 21.0.10). Golden tests
  pin it with the `NBSP` constant. Not a language finding; recorded for UI counters (`ui_data.pagination.counter`,
  `ui_form.lookup.more_chip`).

## Percent sign spacing: «45%» or «45 %»

- Russian references disagree: Шульмейстер (1967) and Гиленсон (1988) attach the sign to the number; ГОСТ 8.417
  requires a space before unit symbols; Мильчин и Чельцова (1998) require a space. Runet usage mostly attaches it.
- Applicability: `ui_transfer.percent` «45%» is an accepted traditional variant, not an error; keep one style.
- Source: https://dic.academic.ru/dic.nsf/ruwiki/57875 (Wikipedia mirror, read 2026-10-11); the handbooks
  themselves and the CLDR ru percent pattern were not checked.

## «Гайд»

- Викисловарь gives one sense, «справочник; справочное руководство», without a stylistic label, borrowed from
  English *guide*. TestSys uses «гайд» for an interactive step-by-step page tour (`ui_page.guide.*`), slightly broader
  than the dictionary sense; not a defect. No normative orthographic dictionary entry was opened.
- Source: https://ru.wiktionary.org/wiki/гайд (read 2026-10-11).

## «Посылка» as the domain term

- Викисловарь gives two homonyms: «посылка I» (the act of sending; a parcel sent by mail) and «посылка II»
  (филос., логич.: a premise). No computing sense is listed. In TestSys the word is the domain term (definitions.md,
  item 29: a sending of a Solution for checking a Task); the capitalized term form, the table/filter context and
  «ID Посылки» rule out the parcel and premise senses on the Judge pages. No undesirable connotation was found.
- Declension (Викисловарь): Им. «посылка» / «посылки», Р. «посылки» / «посылок», Д. «посылке» / «посылкам»,
  В. «посылку» / «посылки», П. «посылке» / «посылках». `glossary.submission` holds the forms used by `page_judge`.
- Source: https://ru.wiktionary.org/wiki/посылка (read 2026-10-12).

## Space after the number sign: «№ 7»

- Normacs experts state that the space between «№» and the first digit follows ordinary Russian rules, as in
  «номер 7», i.e. «№ 7»; their cited ГОСТ Р 2.105-2019 example is «Изм. № 1, 2». A Habr commenter reports that
  Мильчин's handbook separates «№» from the number in all editions since 1974; this is a secondary retelling, the
  handbook itself was not read. A no-break space keeps the sign and the number on one line (inference).
- Applicability: `page_judge.solution.title` renders «Посылка №{id}» without a space because the caller required
  the current text verbatim («Посылка №9223372036854775807»); «Посылка № 42» with a no-break space would be the
  normative variant and needs an explicit decision.
- Sources: https://www.normacs.info/answers/5654 (read 2026-10-12); https://habr.com/en/articles/870856/comments
  (search snippet only, 2026-10-12).

## «Вердикт» as the domain term

- Викисловарь: «вердикт» — inanimate masculine noun, Zaliznyak type 1a; Им. «вердикт» / «вердикты», Р. «вердикта» /
  «вердиктов». Meanings: 1. «юр. решение (обычно — вынесенное в судебном процессе присяжными заседателями),
  приговор»; 2. «перен. решение, суждение».
- Applicability: TestSys uses the figurative sense for the automatic checking result (definitions.md item 19) and,
  as «Судейский вердикт», for the Judge's result (item 26). No undesirable connotation found.
- Project decision, not a language source: on the Judge pages lowercase «вердикт» in «Выставить вердикт» and
  «История выставленных Судьями вердиктов.» is a shortened reference to «Судейский вердикт»; capitalized «Вердикт»
  means the automatic one. The surrounding «Судьями», «Судейские вердикты» and «Новый Судейский вердикт» disambiguate.
- Source: https://ru.wiktionary.org/wiki/вердикт (read 2026-10-12).

## «Полигон» as the domain term

- Викисловарь: inanimate masculine, type 1a; Им. «полигон» / «полигоны», Р. «полигона» / «полигонов», Д. «полигону» /
  «полигонам». Meanings: 1. воен. testing ground for weapons; 2. «место или сфера, область, где производится
  испытание, обработка, хранение или проверка чего-либо»; 3. матем., информ. «то же, что многоугольник»; 4. a
  polygon-shaped area.
- Applicability: the TestSys term (definitions.md item 15, a TRIK Studio world model used for checking) fits
  sense 2; scores and logs «по Полигонам» rule out the geometric sense. No undesirable connotation found.
- In «Скачать логи Полигона «{name}»» the generic word precedes the quoted name, so the name stays undeclined
  (see the «Quoted names and declension» note above).
- Source: https://ru.wiktionary.org/wiki/полигон (read 2026-10-12).

## Student and Participant Cabinets (page_student, page_participant)

### «Тур»: the round sense is inanimate

- Викисловарь separates «тур I» (inanimate, masculine, type 1a: a circle or turn; «этап поэтапного мероприятия»;
  a stage of a competition; a dance circle; a cairn) from «тур II» (animate: the aurochs). The TestSys term
  (definitions.md item 21, a set of Tasks with time limits) is «тур I», sense 2, so the accusative equals the
  nominative: «Начать Тур», «открыть Тур». The animal reading would need «Тура» and does not arise with the
  capitalized term in a cabinet. Forms used by `glossary.contest`: Им. «Тур» / «Туры», Р. «Тура» / «Туров», В. «Тур»,
  П. «Туре».
- Source: https://ru.wiktionary.org/wiki/тур (read 2026-10-12, first 100 000 characters of the page).

### «Класс»: the school group sense

- Викисловарь: inanimate masculine, type 1a; senses 3 and 4 are a school group of pupils («группа учеников,
  занимающихся вместе»), sense 6 the classroom, sense 8 a programming class. The TestSys term (definitions.md
  item 23: Students, Tours and an Organizer) fits sense 4; «присоединиться к Классу» excludes the room sense. Forms
  in `glossary.class`: Им./В. «Класс», Р. «Класса», Д. «Классу», Р. мн. «Классов».
- Source: https://ru.wiktionary.org/wiki/класс (read 2026-10-12).

### «Задача»: forms

- Викисловарь: inanimate feminine, type 4a; Тв. «задачей» / «задачею», the page uses the neutral «Задачей». Forms
  in `glossary.task`: Им. «Задача» / «Задачи», Р. «Задачи» / «Задач», Д. «Задаче» / «Задачам», Тв. «Задачей».
  Sense 2 (an exercise that requires logical methods or calculation) matches the domain term.
- Source: https://ru.wiktionary.org/wiki/задача (read 2026-10-12, first 100 000 characters of the page).

### «Решение»: decision versus solution

- Викисловарь: inanimate neuter, type 7a. Senses include the act of solving, an official ruling, an answer to a
  problem and a decision made. In TestSys it is a TRIK Studio program (definitions.md item 18); next to
  «отправьте», «Файл Решения» and «Язык Решения» the decision sense does not fit. Neuter agreement: «Решение
  отправлено»; the tag «Лучшее» and the badge «Проверено» agree with an implied «Решение» (inference).
- Source: https://ru.wiktionary.org/wiki/решение (read 2026-10-12).

### Subject of «показаны» takes the nominative

- In «…показаны его сведения и доступные Туры» and «Здесь показаны условия Тура, оставшееся время и доступные
  Задачи» the plural terms are subjects of the short passive participle «показаны», so they are nominative
  (`case=nom number=pl`), not accusative. For these inanimate plurals Им. мн. = В. мн. («Туры», «Задачи»), so the
  rendered text is the same; the case label matters for other languages and for future term forms. Verified by the
  independent localization review (2026-10-12) against https://ru.wiktionary.org/wiki/тур (тур I) and
  https://ru.wiktionary.org/wiki/задача.

### «Проверено» outside the Submission list

- The `submission_status` value «Проверено» (neuter) also appears in the Tour page column «Последняя Посылка»,
  where it does not agree with the feminine «Посылка» and reads as an impersonal status («проверено»). Inference
  from usage, no source.

### «условия Тура» versus the domain term «Условие»

- Викисловарь «условие» sense 4: «правило, существующее, установленное в той или иной области» («на условиях
  конкурса»); sense 7 is the given of a problem. The domain term «Условие» (definitions.md item 16) is a Resource,
  the Task statement. `page_student.contest.guide` and `page_participant.contest.guide` use lowercase plural «условия Тура» in sense 4 (the Tour's rules
  and time limits), while «Скачать Условие» on the Task page means the Resource. The capital letter and «Тура»
  separate them; a reader may still briefly connect the two (inference). A less ambiguous wording, such as
  «параметры Тура», would need an explicit change request.
- Source: https://ru.wiktionary.org/wiki/условие (read 2026-10-12).

### «Время на прохождение»

- Викисловарь «прохождение»: sense 1 «действие по значению гл. проходить/пройти»; senses 4–5 relate to finishing
  a game. There is no explicit sense for completing a course or a test; sense 1 covers «пройти Тур» (complete it),
  so the phrase reads as the time allowed to complete the Tour (inference). No undesirable connotation found.
- Source: https://ru.wiktionary.org/wiki/прохождение (read 2026-10-12).

### Glossary decisions for page_student and page_participant

- Project decision D2 (coordinator, 2026-10-12): declined or repeated domain terms become glossary terms.
  `glossary.contest`, `glossary.class`, `glossary.task` and `glossary.solution` were added; the existing
  `glossary.invite_code` and `glossary.submission` already had every form used.
- Role names («Организатор», «Ученик», «Участник») stay literal, following earlier bundles (`page_guest`,
  `page_registration`, `page_judge`). Single-occurrence terms «Соревнование», «Ресурсы», «Условие», «Упражнение»
  and «Кабинет» also stay literal. This is a project choice, not a language finding.
- Quoted names follow a generic word («Класс «{name}»», «Скачать Упражнение «{name}»»), so they stay undeclined
  (see «Quoted names and declension» above).
- Unverified: «Визуальный язык TRIK Studio» was kept verbatim; the TRIK Studio documentation name of its visual
  language was not checked.

### «Визуальный язык TRIK Studio»: no official name found

- The independent review fetched https://help.trikset.com/ (2026-10-12): it describes TRIK Studio only as
  «бесплатной средой программирования роботов» and names no visual language. Web searches found only SPbU theses and
  slides speaking of a «визуальный язык программирования», «графические программы» and «диаграммы» (search snippets
  only, not opened: https://dspace.spbu.ru/bitstream/11701/10976/1/Diploma_-_diploma.pdf,
  https://oops.math.spbu.ru/SE/diploma/2017/bmo/444-Shigarov-presentation.pdf/at_download/file). The wording stays
  verbatim until an official name is confirmed.
- Project observation (2026-10-12, no source): the role bundles name this language differently.
  `page_student.task.send.language_name` and `page_participant.task.send.language_name` use «Визуальный язык
  TRIK Studio»; `page_judge.solution.language`, `page_developer.language_name` and the `visual_language` variant of
  `page_developer.refusal.no_exercise` use «Визуальный язык». A confirmed official name affects all of them.

### Wording kept verbatim with possible misreadings

- «Отсчёт начнётся с началом Тура» (`page_student.remaining.not_started`, `page_participant.remaining.not_started`) is shown before the user's first entry. The
  same page shows the scheduled «Начало» and the «Начать Тур» button, while the countdown starts only when the user
  presses that button; «начало Тура» can be read as the scheduled start. A wording such as «Отсчёт начнётся, когда
  вы начнёте Тур» would need an explicit change request. Inference from page context by the independent review
  (2026-10-12), no language source.
- The Task page block that lists the user's sendings is titled «Решения» with the empty state «Решений пока нет»,
  while its guide and closed hint speak of «Посылки». definitions.md separates Решение (item 18) from Посылка
  (item 29); capitalization makes the mix visible. Not a grammar defect; «Посылки» / «Посылок пока нет» would need
  an explicit change request. Inference by the independent review (2026-10-12).
## Manager Cabinet (page_manager)

### Declension of the Manager domain terms

- Викисловарь tables (read 2026-10-12), used for the new `glossary.class`, `glossary.competition`,
  `glossary.contest`, `glossary.student`, `glossary.participant` and `glossary.community`:
  - «класс»: inanimate masculine, type 1a; Р. «класса» / «классов», Д. «классу», В. = Им. «класс» / «классы».
  - «соревнование»: inanimate neuter, type 7a; Р. «соревнования» / «соревнований», Д. «соревнованию»,
    В. = Им. «соревнование» / «соревнования», Тв. «соревнованием».
  - «тур» I: inanimate masculine, type 1a; Им. мн. «туры», Р. «тура» / «туров», В. = Им. «тур», Тв. мн. «турами».
  - «ученик»: animate masculine, type 3b; Им. мн. «ученики», Р. мн. «учеников», Д. мн. «ученикам», Тв. мн.
    «учениками».
  - «участник»: animate masculine, type 3a; Р. «участника» / «участников», В. = Р. «участника» / «участников»,
    Тв. «участником» / «участниками».
  - «сообщество»: inanimate neuter, type 1a; Д. мн. «сообществам».
- Animacy matters in the accusative: «Удалить Участника «…»?», «Участника с Посылками удалить нельзя», «Создайте
  Участников», «добавьте в него Участников» use the accusative equal to the genitive; inanimate «Класс», «Тур»,
  «Соревнование» keep the nominative form («Создать Класс», «Добавить Тур», «в Соревнование»).
- Sources: https://ru.wiktionary.org/wiki/класс, https://ru.wiktionary.org/wiki/соревнование,
  https://ru.wiktionary.org/wiki/тур, https://ru.wiktionary.org/wiki/ученик, https://ru.wiktionary.org/wiki/участник,
  https://ru.wiktionary.org/wiki/сообщество (all read 2026-10-12).

### Other meanings of «Тур», «Класс», «Ученик»

- «Тур» has a homonym «тур» II, an animate noun for the extinct aurochs (В. «тура»); «тур» I also means a tourist
  trip, a dance circuit or a summit cairn. The relevant sense is «тур» I, 2–3: a stage of a multi-stage event or
  competition. Capitalization, the Manager context and «Тур «{name}»» next to «Соревнование» rule out the other
  senses; the inanimate accusative «Добавить Тур» is the correct form for sense I.
- «Класс» also means a category, a social class, an OOP class (sense 8) and, as predicative slang, «очень хорошо»
  («Класс!»). The relevant sense is 4: «группа учеников, занимающихся совместно». In «Класс создан» or «Класс «7 Б»»
  the slang reading is not plausible (inference); no undesirable connotation found.
- «Ученик» also means an apprentice and a follower of a teaching; the relevant sense is 1, a school pupil, here the
  domain Role (definitions.md item 7). No undesirable connotation found.
- Sources: the Викисловарь pages above (read 2026-10-12).

### Plural and instrumental forms of existing terms

- «Коды-доступа»: Викисловарь «код» gives Им./В. мн. «ко́ды», Тв. мн. «ко́дами»; in «Код-доступа» only the first
  part declines (see «Код-приглашение»: both parts decline above), so the plural is «Коды-доступа» in both the
  nominative («Участники Соревнования, их Коды-доступа …») and the accusative («раздать им Коды-доступа»).
- «Посылками»: Викисловарь «посылка» I gives Тв. мн. «посы́лками» («Участника с Посылками удалить нельзя»).
- Sources: https://ru.wiktionary.org/wiki/код, https://ru.wiktionary.org/wiki/посылка (read 2026-10-12).

### Count labels in the genitive plural

- Column and field labels «Учеников», «Участников», «Туров» are elliptical counts («(число) учеников»); the number is
  in the cell, not in the message. They reproduce the existing interface text with the genitive plural term forms.
  This is a reading of the existing text, not a dictionary finding.

### Quoted user names after a generic word

- «Класс «{name}»», «Соревнование «{name}»», «Тур «{name}»», «Действия с Участником «{name}»» and «Удалить Участника
  «{name}»?» put the generic word before the quoted name, so the name stays undeclined (see «Quoted names and
  declension» above). A name that itself contains guillemets renders nested «ёлочки»: «Класс «7 «Б»»» (pinned by
  the golden test). Russian typography usually puts „лапки“ inside «ёлочки»; the output is kept verbatim, as on the
  other pages. Not verified against a typography handbook.

### Lowercase anaphoric «этот код»

- «Передайте этот код Ученикам …» keeps lowercase «код» because it refers back to the Код-приглашение shown in the
  block rather than naming the domain term; the caller required it verbatim. Project decision, not a language
  source.

### Ungrouped numbers in results and toasts

- `:integer` / `:number` with `useGrouping=never` keep the previous Kotlin `toString` output: «Всего: 1000.»,
  «1000 · 1000», «Движение · 9223372036854775807». A negative score renders with an ASCII hyphen-minus («-5 · 2»)
  in ICU4J 78.1 `ru`, the same as Kotlin; checked by the golden test on 2026-10-12. Not a language finding.

### «Участник»: general sense versus the domain Role

- Викисловарь «участник», sense 1: «тот, кто участвует в чём-либо, то есть занимается чем-либо совместно с другими».
  In this general sense the word also covers Ученики taking part in a Тур, while the domain term (definitions.md
  item 6) is a specific Role distinct from Ученик (item 7).
- Applicability: `page_manager.contest.results.empty` («Участников нет») is shown for a Тур in a Class and in a
  Competition, so it is a plain literal in the general sense and does not reference `glossary.participant`; a term
  reference would narrow it to the Role, and translations would follow that binding. The capitalized ru-RU output
  is the same either way because the word starts the sentence.
- Project source, not a language source: definitions.md items 6 and 7 give both Roles the right «принимать участие
  в доступных Турах», so Ученики are «участники» of a Тур in the general sense; the hint «… будут Ученики или
  Участники» resolves the reading on the page.
- Source: https://ru.wiktionary.org/wiki/участник (read 2026-10-12, re-read 2026-10-12 by an independent review).

### «Без учёта регистра» in the name filter hint

- Викисловарь «регистр», sense 6 (техн., комп.): «группа знаков на клавишах пишущей машинки или на клавиатуре
  компьютера»; no sense names прописные/строчные буквы explicitly, so «без учёта регистра» relies on this technical
  keyboard sense.
- Applicability: `page_manager.filter.name_hint` keeps «Часть названия, без учёта регистра» verbatim. It is
  established software-UI usage for Organizers and not an error; the e-mail wording «Большие и маленькие буквы
  различаются» (see «Case sensitivity» above) stays a choice for general-audience text. Changing the hint needs an
  explicit decision. No normative dictionary or style guide was opened.
- Source: https://ru.wiktionary.org/wiki/регистр (read 2026-10-12).
## Administrator Cabinet (page_administrator)

### Glossary terms added for the cabinet

- New terms (2026-10-12), each a self-contained block in `glossary.properties`: `administrator`, `cabinet`, `class`
  (entity `Class`; a term name does not become a Kotlin identifier, so the keyword is valid), `community`,
  `competition`, `contest` (Тур), `judge`, `nickname` (Псевдоним, definitions.md item 31; the code field is
  `UserData.name`), `observer`, `participant`, `role`, `solution`, `student`, `system`, `task`, `user`.
  `invite_code` gained `nom pl` «Коды-приглашения» and `ins sg` «Кодом-приглашением». Only referenced forms exist;
  the `* *` variant is the nominative singular. Other cabinets may need more forms of the same terms.
- Animate masculine nouns take the genitive form in the accusative: «Создать Наблюдателя», «пригласите
  Пользователей». Inanimate plurals keep the nominative form in the accusative: «Выберите Туры». Grammar facts, no
  source opened.
- Independent review (2026-10-12): «его Роли» in `page_administrator.user.guide` (a subject of «показаны» with «сведения»
  and «статистика») and «связанные Соревнования» in `page_administrator.user.assigned.guide` (an ungoverned list item) are
  nominative, not accusative; reference them with `case=nom`, so the glossary needs no unused `acc pl` forms.
  Grammar analysis, no source opened.

### «Псевдоним»

- Викисловарь gives one sense, a fictitious name an author uses, with no computing sense. «Ник» and «никнейм»
  appear only as synonyms. Genitive singular «псевдонима». In TestSys the word is the domain term for the name a
  User is shown under (definitions.md item 31). In the cabinet tables and filters the domain sense is clear; nothing
  undesirable was found.
- Source: https://ru.wiktionary.org/wiki/псевдоним (read 2026-10-12).

### «Неуспех» as a status badge

- Викисловарь: «неуспех» is a masculine noun with no stylistic label, meaning «отсутствие успеха; неудача»
  (Грибоедов: «по службе неуспех?»). It is a standard word, not a coinage, and it pairs naturally with «Успех» on
  badges. No undesirable connotation was found.
- Source: https://ru.wiktionary.org/wiki/неуспех (read 2026-10-12).

### «Техническая остановка»

- In general usage (production, transport, aviation) the phrase means stopping equipment or a vehicle for
  maintenance or a fault, for example a train or aircraft «техническая остановка». No dictionary entry was found;
  a web search returned only related terms such as «технический перерыв».
- Applicability (inference): `page_cabinet.validation_status` `stopped` is a badge in the Статус проверки column.
  The column context makes it read as «the check stopped for technical reasons», and the transport sense does not
  apply. It differs from `failure` «Неуспех», which is a check that ran and failed.
- Source: web search on 2026-10-12, no primary source read.

### Numerals with «символ» in `page_cabinet.name_too_long`

- The comparative «длиннее» governs the genitive, so the noun follows the genitive of the numeral: «не длиннее
  1 / 21 символа» (одного, двадцати одного символа); «3 / 5 / 255 символов». So `one` → «символа», and `few` and
  `many` → «символов». This is a grammar inference; a search found no handbook page for the exact phrase.
- Source: web search on 2026-10-12, secondary summaries only.
- Independent review (2026-10-12) confirms the forms by standard grammar (a compound numeral ending in «один» takes
  the singular noun: «двадцати одного символа»); under `:integer` in `ru` the `*` variant is unreachable and equals
  `many`. Still no primary handbook text: https://mel.fm/gramotnost/kak-govorit/6417539-numerals («Мел»,
  16.09.2018, read 2026-10-12) does not cover the rule; other search results were snippets only.

### Role names in apposition: «в Роли Разработчик»

- `page_administrator.user.roles.remove`, `remove_title` and `community.invites.actions` insert the nominative role name
  from `page_cabinet.role_name` after a declined «Роль»: «Исключить в Роли Разработчик», «для Роли Разработчик».
  Ordinary Russian puts a common noun in the genitive («в роли разработчика»). The nominative reads as a name of an
  item in a list, a label after a generic word. A generic word before a quoted name keeps the name undeclined (see
  «Quoted names and declension» above), but these names are not quoted. The coordinator kept the text verbatim. A
  normative-looking alternative would quote the name («в Роли «Разработчик»») or use a colon. Either change needs
  approval. A web search found no source specific to role labels.
- «Исключить» (Викисловарь, sense 1: «устранить, удалить откуда-либо, из состава чего-либо») fits removing
  membership. No stylistic label. The punitive colouring of «исключить из школы» comes from that context, not from
  the word. «Исключить в Роли X» without «из» is elliptical: the menu sits in a row of one Community, which
  supplies the missing «из Сообщества».
- Sources: https://ru.wiktionary.org/wiki/исключить (read 2026-10-12); the «Мел» article cited above.

### Other notes

- `page_administrator.users.nickname_hint` «без учёта регистра» uses «регистр». The note «Case sensitivity: …» above
  records a project preference for describing letter case without that word. The text is verbatim by coordinator
  decision; it is not a grammar error.
- `page_administrator.communities.users` «Пользователей» (genitive plural) is a numeric column header, read as an
  elliptical «(число) Пользователей». It is verbatim; «Пользователи» would be the usual header form.
- Independent review (2026-10-12): the Observer creation dialog labels the name field «Имя» / «Укажите имя»
  (`page_administrator.community.observer.name*`), while the same value is «Псевдоним» elsewhere on the pages. The meaning
  survives (definitions.md item 31 defines Псевдоним as a name); aligning the label needs user approval.

### «Роль» and «Соревнование»: declension and the user page guides

- Викисловарь: «роль» — inanimate feminine, 3rd declension (Zaliznyak 8e); Им. роль / роли, Р. роли / ролей,
  В. роль / роли, Тв. ролью / ролями, Пр. роли / ролях. Sense 5 (информ., a set of functions assigned to a server)
  is close to the domain term; theatre senses do not fit the capitalized term. «Соревнование» — inanimate neuter,
  type 7a; Им. мн. «соревнования», Р. мн. «соревнований». They confirm `glossary.role` and `glossary.competition`.
- In `page_administrator.user.guide` the singular locative «Роли» coincides with the plural nominative, so «о Пользователе,
  его Роли и …» could be read as «(о) его Роли»; the nominative «доступная вам статистика» makes the three-subject
  list the natural reading, and «показаны» agrees under either pattern (reviewer's inference; no primary
  agreement rule text read).
- «Туры, назначенные Пользователю, и связанные Соревнования»: Правила 1956 § 151 п. 1 sets off a postpositive
  participial phrase with commas; the comma before «и» closes it (inference for this exact combination).
  «Связанные» without a complement (Викисловарь «связанный», «находящийся во взаимосвязи с кем-либо, чем-либо»)
  is clear from «Туры» and the table.
- Sources: https://ru.wiktionary.org/wiki/роль, https://ru.wiktionary.org/wiki/соревнование,
  https://ru.wiktionary.org/wiki/связанный,
  https://www.azbyka.ru/otechnik/Spravochniki/pravila-russkoj-orfografii-i-punktuatsii/34 (all read 2026-10-12).
## Developer Cabinet (`page_developer`)

Notes collected while localizing the Developer Cabinet pages (stage 2f, 2026-10-12).

### «Тур» as the domain term

- Викисловарь has three entries: «тур I» (inanimate masculine; senses include 2. «этап поэтапного мероприятия» and
  3. спорт. «отдельный этап соревнования…»; sense 1 is a trip/tour), «тур II» (animate masculine, the extinct wild
  ox) and «тур III» (genitive plural of «тура»). Declension of «тур I»: Им. «тур» / «туры», Р. «тура» / «туров»,
  Д. «туру» / «турам», В. «тур» / «туры», Т. «туром» / «турами», П. «туре» / «турах».
- Applicability: the TestSys term (definitions.md item 21, a set of Задачи with time limits) fits senses 2–3. The
  capitalized term, the Developer Cabinet context (Задачи, начало, конец, длительность) and the inanimate
  accusative «Тур» rule out the trip and animal senses. No undesirable connotation found.
- Source: https://ru.wiktionary.org/wiki/тур (read 2026-10-12; the last ~11 % of the page was not read).

### «Условие» as the domain term

- Викисловарь lists seven senses (agreement, contract clause, circumstance, rule, setting, premise, and 7.
  «положение, сведение, лежащее в основе чего-либо, то, из чего надо исходить» with the gloss «при решении задачи»);
  it has no separate sense «текст задачи». Inanimate neuter, type 7a: Им. «условие» / «условия», Р. «условия» /
  «условий», В. = Им.
- Applicability: the TestSys term is a resource type, the problem statement document (definitions.md item 16).
  As a bare label («Тип: Условие», «Прикрепите Условие.») it could in principle be read as "condition"; the
  neighbouring Упражнение, Полигон, Авторское Решение and the capital letter resolve it. This is an inference, no
  usage source for «Условие» alone as a file type was read.
- Source: https://ru.wiktionary.org/wiki/условие (read 2026-10-12).

### «Диагностика» in the plural

- Викисловарь: senses 1. мед., техн. «исследование с целью определить причину болезни организма, неисправности
  механизма и т. п.»; 2. a branch of medicine. The article gives a full plural paradigm: Им. «диагностики»,
  Р. «диагностик», Д. «диагностикам», В. «диагностики». No note restricts the plural.
- Applicability: the TestSys term (definitions.md item 28) is one check of a Полигон XML, so the countable plural
  «Диагностики», «Диагностик пока нет» fits sense 1. No undesirable connotation found.
- Source: https://ru.wiktionary.org/wiki/диагностика (read 2026-10-12).

### «Откатить»: technical jargon with a bribery sense

- Викисловарь gives three senses: 1. «переместить дальше от чего-либо»; 2. техн. жарг. «вернуть систему в одно из
  предыдущих состояний»; 3. жарг. «дать откат, т. е. взятку должностному лицу…» (the entry is in the category of
  bribery verbs).
- Applicability: the Developer Cabinet keeps «Откатить» and «Откатить изменения Задачи?» verbatim (2026-10-12
  migration). With the object «изменения Задачи» and in the confirmation dialog sense 2 is the only plausible
  reading; the bare button label relies on that dialog context. The toast already uses the neutral «Изменения
  Задачи отменены»; «Отменить изменения» would be a jargon-free alternative and needs an explicit decision.
- Source: https://ru.wiktionary.org/wiki/откатить (read 2026-10-12).

### «МиБ» (mebibyte)

- Викисловарь defines «мебибайт» as «единица измерения количества информации, равная 2^20 (1024^2) байт»,
  masculine, genitive plural «мебибайтов» (also «мебибайт»); it gives no abbreviation. The abbreviation «МиБ» in
  `page_developer.task.upload.subtitle` is kept verbatim; its normative status (ГОСТ IEC 80000-13) was not checked.
- Source: https://ru.wiktionary.org/wiki/мебибайт (read 2026-10-12).

### «Сведения» as both a block title and a severity

- Project observation, not a language source: on the testing page «Сведения» is the details block title
  (`page_developer.details`) and the Info severity prefix of a Диагностика line («Сведения · …»). The line format
  «Сведения · место · замечание» keeps them apart; a different Info label would need an explicit decision.

### Date and time «12.10.2026 14:05» in `ru`

- Tooling fact, not a language finding: ICU4J 78.1 `:date fields=year-month-day length=short` renders `ru` as
  «12.10.2026» (day and month padded) and `:time precision=minute hour12=false` as «14:05» / «09:07»; two
  expressions separated by a space reproduce the former `dd.MM.yyyy HH:mm`; a single `:datetime` would use the
  locale's date-time glue instead (its exact `ru` output was not checked).
  Pinned by `PageDeveloperTests` (checked locally 2026-10-12).

### «Зафиксировать» as "commit"

- Викисловарь lists five senses of «зафиксировать», none stylistically labelled and none about computing; among
  them 1. «закрепить, запечатлеть с помощью рисунка, фотографии и т.п.» and 3. «умертвить, хлороформировать мелких
  животных, насекомых для целей научного исследования».
- Usage evidence: the Russian Pro Git uses the word for `git commit` («Простейший способ зафиксировать изменения —
  это набрать `git commit`», «фиксировать «снимки» состояния»). The page does not contain «зафиксированный»,
  «незафиксированные» or «фиксация».
- Applicability (independent review's inference): «Зафиксировать Задачу?», «Зафиксирована» and «незафиксированные
  изменения» follow established version-control usage; the objects Задача, версия, изменения rule out sense 3.
  No undesirable connotation found.
- Sources: https://ru.wiktionary.org/wiki/зафиксировать, https://git-scm.com/book/ru/v2/Основы-Git-Запись-изменений-в-репозиторий
  (both read 2026-10-12 by the independent review).

### «Провалить»

- Викисловарь gives one sense, «не справиться, потерпеть неудачу», without a stylistic label; perfective,
  transitive, synonym «завалить». «Провалено Посылок: 2 из 5» has no register problem.
- Not checked: whether a Посылка is a natural grammatical object of «провалить»; the page has no usage example.
- Source: https://ru.wiktionary.org/wiki/провалить (read 2026-10-12 by the independent review).

### «Замечание» as the fallback severity label

- Викисловарь: neuter, inanimate, type 7a; 1. «краткое суждение, высказанное устно или письменно по поводу
  чего-либо увиденного, услышанного, прочитанного» (no label); 3. a reprimand or admonition; 2 and 4 книжн., устар.
- Applicability (inference): the column «Замечания» and the «· место · замечание» line format select sense 1 for
  the `*` variant of `page_developer.diagnostic.line` / `line_at`; the reprimand reading is implausible there.
- Source: https://ru.wiktionary.org/wiki/замечание (read 2026-10-12 by the independent review).

### «До N файла/файлов» — unverified

- Standard grammar as reported by the independent review: «до» takes the genitive and every part of a compound
  numeral declines; a numeral ending in «один» keeps the noun in the singular («до (двадцати) одного файла»);
  2–4 and 5+ in oblique cases take the plural of the same case («до двух файлов», «до пяти файлов»). So the CLDR
  `one` category maps to «файла», `few`/`many` to «файлов», as in `page_developer.task.upload.subtitle`.
- Unverified: confirmed only through search-engine summaries of teaching materials. Rosental, chapter XXXVIII
  (https://evartist.narod.ru/text1/59.htm), does not state it; https://ru.wiktionary.org/wiki/двадцать_один has no
  declension table; https://mel.fm/amp/gramotnost/kak-govorit/6417539-numerals only says that all parts of
  compound numerals decline (all read 2026-10-12). rusgram.ru failed with a certificate error.

### English technical data inside Russian diagnostic lines

- Project observation: `$expected` of `page_developer.diagnostic.finding.invalid_attribute_value` (and the free
  text of `malformed_xml` and `summary.technical`) comes from the diagnostics module as English technical text
  («finite double», «long integer», «integer», `a|b`), so a line reads «атрибут «value» равен «abc», ожидается
  finite double». The message reproduces the former hardcoded text; a fully Russian line needs a separate
  decision, e.g. a selector over the expected kinds.
