package lesson09.dsl.scopes

import lesson09.dsl.DivergenceCollector
import lesson09.dsl.JsonDsl

@JsonDsl
class StatusScope (
    private val actual: Int,
    private val collector: DivergenceCollector,
) {
    fun equalTo(expected: Int) {
        if (expected != actual) {
            collector.add("status", expected.toString(), actual.toString())
        }
    }
}