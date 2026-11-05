package lesson09.dsl.scopes

import lesson09.JsonDsl
import lesson09.dsl.DivergenceCollector
import lesson09.dsl.ResponseScope

@JsonDsl
class JsonObjectScope(
    private val fields: Map<String, Any?>,
    private val collector: DivergenceCollector
) {
    fun field(name: String, block: JsonFieldScope.() -> Unit) {
        JsonFieldScope(name, fields, collector).block()
    }
}