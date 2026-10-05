package tech.testsys.infra.localization.codegen.mf2.validator.term

/** The definition of the term `RU_TASK_TERM`. */
internal val RU_TASK_DEFINITION: TermDefinition = TermDefinition(
    name = "task",
    cases = setOf("nom", "acc"),
    cells = setOf(
        "nom" to "one", "nom" to "few", "nom" to "many", "nom" to "other", "nom" to "sg", "nom" to "pl",
        "acc" to "one", "acc" to "few", "acc" to "many", "acc" to "other", "acc" to "sg", "acc" to "pl",
    ),
)
