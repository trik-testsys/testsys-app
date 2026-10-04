# Documentation rules for agents

These instructions govern how agents write and review Russian documentation in TestSys. They address recurring
LLM writing habits and are loaded by the `coder` and `reviewer` role contracts. They are not a guide for human
authors or a source of project development requirements. Project facts, document structure and formatting remain
governed by [docs.md](../../docs/docs.md) and the relevant owner documents.

## Use and scope

- Read this file in each separate role context and reload it after context loss. Follow the writing rules when
  drafting or editing documentation; use the review pass when checking documentation changes.
- Follow the role's existing scope, permissions and report format. This file grants no additional tools, writes,
  delegation or permission to change unrelated documents.
- Preserve technical meaning, requirements, identifiers and domain terminology. A shorter sentence must retain
  every condition and exception. Verify a proposed correction against its owner document or implementation.
- Keep agent instructions here. Do not add this file to the project-document registry or link to it from project
  documentation. Do not copy these rules into role contracts, skills or client definitions.

## Writing rules

1. State the concrete fact, action or requirement first. Name the actor when the reader needs to know whether
   the system, a module or the user acts. Prefer a verb to a phrase built around an action noun.
2. Use natural Russian. Replace bureaucratic phrases, literal English translations and unnecessary jargon with
   familiar words. Keep established technical terms and exact project names; do not invent synonyms for them.
3. Give each sentence one main thought and each paragraph one topic. About 7–15 words is a useful guide for simple
   sentences; over 30 words is a reason to inspect and split, not an automatic violation. Keep related conditions
   together when splitting would obscure their relationship.
4. Break chains of genitives and stacked participial phrases. Check that an adverbial participle refers to the
   sentence's subject. Make pronoun references unambiguous; repeat a noun when needed.
5. In procedures, address the reader with an imperative: «добавьте», «проверьте». In descriptions, use a direct
   statement with an explicit actor: «модуль хранит», «процессор проверяет». Use passive voice when the actor is
   unknown or irrelevant, or when naming a status. Avoid switching between passive instructions, «вы» and «мы»
   within one passage; use «мы» only when speaking for the team is necessary.
6. Write a brief introduction about the document's purpose and scope. Do not repeat the table of contents or add
   a stock sentence about an obvious audience. Preserve the purpose information required by `docs.md`.
7. State rules directly. Replace slogans, aphorisms, metaphors and unsupported evaluations with the actual
   behaviour or constraint. Keep a comparison or «не X, а Y» only when it resolves a plausible misunderstanding.
8. Use bold only where `docs.md` permits it. Do not give every list item a bold slogan. Keep headings specific to
   their contents; do not add ornamental headings or filler sections.
9. Use lists for parallel items or sequences and tables for comparisons or mappings, following `docs.md`.
   Introduce a list clearly and keep its items grammatically consistent. Prefer connected prose for explanations
   that develop one thought. Do not force facts into triples or symmetrical pairs.
10. Use dashes according to Russian punctuation. Do not ban them or insert them for dramatic pauses. Move a
    second thought appended after a dash into its own sentence when that makes the relationship clearer.
11. State a rule once in its owner document and link to it elsewhere. Remove repeated explanations and closing
    summaries that only restate the preceding text. Keep checklist entries and necessary cross-references.
12. Match length to the task. Include the facts needed to use or review the change, without generic advice,
    repeated conclusions or decorative examples. Use verified examples and links to project code where appropriate.

### Common replacements

Choose by meaning; these are editing prompts, not unconditional search-and-replace operations.

| Phrase to inspect | Preferred wording |
|-------------------|-------------------|
| «выполнить удаление», «осуществляет проверку», «происходит проверка» | «удалить», «проверяет»; name who checks |
| «является», «представляет собой» | A direct definition with «это» or a dash, when appropriate |
| «имеет возможность» | «может» |
| «данный» | «этот», unless «данный» has a distinct technical meaning |
| «в случае, если» | «если» |
| «в рамках» | «в», «внутри», or omit it when redundant |
| «обладает Ролью» | «имеет Роль», «получает Роль», according to meaning |
| «не валиден» | «недействителен», «неверен», according to meaning |
| «на рантайме» | «во время выполнения» |
| «инстанс», «хардкодить», «мёрж» | «экземпляр», «зашивать в код», «слияние» |
| «продовый код», «блоб-сторадж», «роняет запуск» | «основной код», «хранилище файлов», «приложение не запустится» |

### Examples

These illustrate wording only; they do not establish project behaviour.

| Wording to revise | Direct wording |
|-------------------|----------------|
| «Пользователь имеет возможность зарегистрироваться.» | «Пользователь может зарегистрироваться.» |
| «Происходит проверка, привязана ли эта почта уже к какому-то Кабинету.» | «Система проверяет, не привязана ли почта к другому Кабинету.» |
| «Лучше отказать при сборке, чем вывести не то.» | «Сомнительные сообщения отклоняются при сборке.» |
| «Сгенерированный код и его вызывающие от него не зависят.» | «Ни сгенерированный код, ни код, который его вызывает, от этого API не зависят.» |

## Documentation review pass

1. Check facts before style: paths, class and function names, commands, links, anchors, conditions and exceptions.
   Use the actual owner documents and implementation; do not rely on an earlier report's line numbers or claims.
2. Read the changed passage with its surrounding text. Check it against the numbered writing rules above and
   the applicable rules and checklist of `docs.md`.
3. Inspect these recurring markers: bold slogans; ornamental comparisons; unnecessary «не X, а Y»; forced triples
   and mirror constructions; stock introductions; redundant conclusions; excessive dashes; bureaucratic phrases;
   chains of passive clauses; repeated ideas; metaphors or evaluations in place of concrete facts.
4. Treat a marker as a reason to inspect the passage. Keep justified terminology, contrast, punctuation and
   structure. Do not infer authorship, use AI detectors or report a defect from word counts or marker frequency alone.
5. For each finding, quote the passage and the violated rule, explain the concrete clarity or meaning problem,
   and propose a correction that preserves the original requirements. Deduplicate repeated instances of one issue.
   A wording improvement alone is `Info`; factual errors or ambiguous requirements use the role's existing severity
   criteria. Observations outside the review scope follow the role's existing reporting rules.
6. As `coder`, run this pass on the documentation you changed and correct issues within the approved scope.
   As `reviewer`, run it separately from the factual check and report verified findings without editing files.
   Apply the wording rules to your own prose while retaining the role's required report fields and structure.
