package lesson09.dsl

import lesson09.HttpResponse
import lesson09.JsonDsl
import lesson09.dsl.scopes.BodyScope
import lesson09.dsl.scopes.HeaderScope
import lesson09.dsl.scopes.StatusScope

@JsonDsl
class ResponseScope (
    private val response: HttpResponse,
    private val collector: DivergenceCollector,
) {
    fun status(block: StatusScope.() -> Unit) {
        StatusScope(response.status, collector).block()
    }

    fun header(
        name: String,
        block: HeaderScope.() -> Unit
    ) {
        val matching = response.headers.filterKeys { it.equals(name, true) }
        val values: List<String>? =
            if (matching.isEmpty()) null else matching.values.flatten()
        HeaderScope(name, values, collector).block()
    }

    fun body(block: BodyScope.() -> Unit) {
        BodyScope(response.body, collector).block()
    }
}

fun validate(
    response: HttpResponse,
    block: ResponseScope.() -> Unit,
) : ValidationResult {
    val collector = DivergenceCollector()
    ResponseScope(response, collector).block()
    return collector.toResult()
}