package tech.testsys.infra.database.internal.settings

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import tech.testsys.infra.database.internal.InternalDatabaseApi

@InternalDatabaseApi
internal object SettingsPayloadCodec {
    private val mapper = ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)

    fun encode(overrides: Map<String, String>): String = mapper.writeValueAsString(overrides)

    fun decode(payload: String): Map<String, String> = try {
        val root = mapper.readTree(payload)
        check(root != null && root.isObject) { "Invalid runtime settings payload: expected a JSON object" }

        root.properties().associate { (property, value) ->
            check(value.isTextual) { "Invalid runtime settings payload for $property: expected a non-null JSON string" }
            property to value.textValue()
        }
    } catch (error: JsonProcessingException) {
        throw IllegalStateException("Invalid runtime settings payload", error)
    }
}
