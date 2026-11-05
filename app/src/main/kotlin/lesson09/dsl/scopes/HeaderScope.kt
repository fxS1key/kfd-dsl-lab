package lesson09.dsl.scopes

import lesson09.JsonDsl
import lesson09.dsl.DivergenceCollector

@JsonDsl
class HeaderScope(
    name: String,
    private val values: List<String>?,
    private val collector: DivergenceCollector
) {
    private val path = "headers.$name"

    private fun check(
        expected: String,
        predicate: (String) -> Boolean,
    ) {
        if (values?.any(predicate) != true) {
            collector.add(path, expected, values?.toString() ?: "<missing>")
        }
    }

    fun hasValue(expected: String) = check("value $expected") { it == expected }

    fun startsWith(prefix: String) = check("prefix $prefix") { it.startsWith(prefix) }

    fun satisfiesOurRequirements(description: String, predicate: (String) -> Boolean) =
        check(description, predicate)
}