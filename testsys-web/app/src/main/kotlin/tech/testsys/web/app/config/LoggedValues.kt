package tech.testsys.web.app.config

import org.springframework.core.DefaultParameterNameDiscoverer
import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.group.InviteCodeHash
import tech.testsys.domain.model.task.FileContent
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.user.AccessTokenHash
import tech.testsys.domain.model.user.PasswordHash
import java.io.InputStream
import java.lang.reflect.Method

/**
 * Renders call arguments and results for the call log: masks passwords, tokens, secrets and codes, writes only the
 * name and size of inline files and the name and storage kind of stored ones and shortens long strings and collections.
 */
internal object LoggedValues {
    private const val MASK = "***"
    private const val MAX_ITEMS = 10
    private const val MAX_LENGTH = 500
    private const val SENSITIVE = "password|token|secret|code"

    private val sensitiveName = Regex(SENSITIVE, RegexOption.IGNORE_CASE)

    // `name=value` or `name=Type(...)` with one level of nested parentheses, as printed by data classes.
    private val sensitiveField = Regex("""(\w*(?i:$SENSITIVE)\w*)=(\w*\((?:[^()]|\([^()]*\))*\)|[^,)\]}]*)""")
    private val parameterNames = DefaultParameterNameDiscoverer()

    /** Renders [args] of [method] as `name=value`; without parameter names every string argument is masked. */
    fun arguments(method: Method, args: Array<out Any?>): String {
        val names = parameterNames.getParameterNames(method)
        return args.withIndex().joinToString(", ") { (index, arg) ->
            val name = names?.getOrNull(index)
            when {
                name == null -> if (arg is CharSequence) MASK else value(arg)
                sensitiveName.containsMatchIn(name) -> "$name=$MASK"
                else -> "$name=${value(arg)}"
            }
        }
    }

    /** Renders a call [result]; a string result is masked, because some calls return a secret, such as a password hash. */
    fun result(result: Any?): String = if (result is CharSequence) MASK else value(result)

    /** Renders [item] by the masking and shortening rules. */
    fun value(item: Any?): String = when (item) {
        null -> "null"
        is CharSequence -> shortened(item.toString())
        is AccessTokenHash, is InviteCodeHash, is PasswordHash -> MASK
        is FileData -> file(item)
        is ByteArray -> "byte[${item.size}]"
        is InputStream -> "InputStream"
        is DomainEntity<*> -> "${item.javaClass.simpleName}(id=${item.id.value})"
        is Collection<*> -> items(item.take(MAX_ITEMS).map { element -> value(element) }, item.size)
        is Map<*, *> -> items(item.entries.take(MAX_ITEMS).map { (key, element) -> "${value(key)}=${value(element)}" }, item.size)
        is Array<*> -> items(item.take(MAX_ITEMS).map { element -> value(element) }, item.size)
        else -> shortened(item.toString().replace(sensitiveField) { field -> "${field.groupValues[1]}=$MASK" })
    }

    private fun file(file: FileData): String = when (val content = file.content) {
        is FileContent.Inline -> "FileData(name=${file.uploadedFilename}, size=${content.bytes.size})"
        is FileContent.Stored -> "FileData(name=${file.uploadedFilename}, stored=${content.kind})"
    }

    private fun items(shown: List<String>, size: Int): String {
        val rest = if (size > MAX_ITEMS) listOf("… ($size total)") else emptyList()
        return (shown + rest).joinToString(prefix = "[", postfix = "]")
    }

    private fun shortened(text: String): String = if (text.length <= MAX_LENGTH) text else text.take(MAX_LENGTH) + "…"
}
