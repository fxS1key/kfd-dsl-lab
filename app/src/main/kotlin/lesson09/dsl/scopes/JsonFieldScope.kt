package lesson09.dsl.scopes

import lesson09.JsonDsl
import lesson09.dsl.DivergenceCollector

@JsonDsl
class JsonFieldScope(
    private val name: String,
    private val fields: Map<String, Any?>,
    private val collector: DivergenceCollector
) {
    private val path = "body.json.$name"

    fun checkEquals(expectedText : String, actual : Any) {
        if (fields[name] != expectedText) {
            collector.add(path, expectedText, actualText())
        }
    }

    fun present() {
        if (!fields.containsKey(name)) {
            collector.add(path, "present", "<missing>")
        }
    }

    fun equalTo(expected: String) = checkEquals("string \"$expected\"", expected)

    fun equalTo(expected: Long) = checkEquals("integer $expected", expected)

    fun equalTo(expected: Boolean) = checkEquals("boolean $expected", expected)

    // additional function for "actual"
    fun actualText(): String {
        if (!fields.containsKey(name)) return "<missing>"
        return when (val foo = fields[name]) {
            null -> "<null>"
            is String -> "string \"$foo\""
            is Long -> "integer \"$foo\""
            is Boolean -> "boolean \"$foo\""
            else -> foo.toString()
        }
    }
}