package tech.testsys.infra.localization.codegen.mf2.function

import tech.testsys.infra.localization.codegen.mf2.model.Operand

/** `:string`: text; as a selector it becomes a nested enum of its keys. */
internal data object StringFunction : Mf2Function(name = "string", options = emptyList()) {
    override val selection: Selection = Selection.TEXT

    override fun operandType(options: Map<String, Operand>): ArgumentType = ArgumentType.STRING
}
