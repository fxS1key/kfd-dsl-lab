package lesson09.dsl.scopes

import lesson09.dsl.DivergenceCollector
import lesson09.dsl.JsonDsl
import lesson09.support.json.JsonParseResult
import lesson09.support.json.parseJsonObject

@JsonDsl
class BodyScope(
    private val body: String?,
    private val collector: DivergenceCollector
) {
    fun nullOrEmpty() {
        if (!body.isNullOrEmpty()) {
            collector.add("body", "null or empty body", describe(body))
        }
    }

    fun nonEmpty() {
        if (body.isNullOrEmpty()) {
            collector.add("body", "empty body", describe(body))
        }
    }

    fun describe(body : String?) : String =
        if (body == null) "<null>" else "\"$body\""

    fun json(block: JsonObjectScope.() -> Unit) {
        val source = body
        if (source == null) {
            collector.add("body.json", "valid JSON object", "<null>")
            return
        }
        when (val parsed = parseJsonObject(source)) {
            is JsonParseResult.Success -> JsonObjectScope(parsed.fields, collector).block()
            is JsonParseResult.Failure ->
                collector.add("body.json", "valid JSON object", parsed.reason)
        }
    }
}