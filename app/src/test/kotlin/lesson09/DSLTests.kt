package lesson09

import lesson09.dsl.Divergence
import lesson09.dsl.ValidationResult
import lesson09.dsl.describe
import lesson09.dsl.validate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun response(
    status: Int = 200,
    headers: Map<String, List<String>> = emptyMap(),
    body: String? = null,
) = HttpResponse(status, headers, body)

private fun isNotBlankValue(value: String): Boolean = value.isNotBlank()

class DslBasicsTest {

    @Test
    fun MainTestFromDocs() {
        val r = validate(HttpResponse(500, mapOf("Content-Type" to listOf("text/plain")), "{}")) {
            status { equalTo(200) }
            header("Content-Type") { startsWith("application/json") }
            body { json { field("id") { present() } } }
        }

        assertEquals("status: expected 200, actual 500\n" +
                "headers.Content-Type: expected prefix application/json, actual [text/plain]\n" +
                "body.json.id: expected present, actual <missing>", r.describe())
    }

    /*
    result
     */

    @Test
    fun `mismatch describe builds text from parts`() {
        val m = Divergence("status", "200", "500")
        assertEquals("status: expected 200, actual 500", m.describe())
    }

    @Test
    fun `result describe joins divergences with newline`() {
        val result = ValidationResult(
            listOf(Divergence("status", "200", "500"), Divergence("body", "non-empty", "<null>"))
        )
        assertEquals(
            "status: expected 200, actual 500\nbody: expected non-empty, actual <null>",
            result.describe(),
        )
    }

    /*
    status
     */

    @Test
    fun `status matches`() {
        val result = validate(response(status = 200)) { status { equalTo(200) } }
        assertTrue(result.isSuccess)
        assertTrue(result.divergences.isEmpty())
    }

    @Test
    fun `status mismatch gives one record without exception`() {
        val result = validate(response(status = 500)) { status { equalTo(200) } }
        assertEquals(listOf(Divergence("status", "200", "500")), result.divergences)
    }

    @Test
    fun `repeated status check is not deduplicated`() {
        val result = validate(response(status = 500)) {
            status { equalTo(200) }
            status { equalTo(200) }
        }
        assertEquals(2, result.divergences.size)
        assertEquals(result.divergences[0], result.divergences[1])
    }

    @Test
    fun `status failure does not stop header check`() {
        val result = validate(response(status = 500, headers = mapOf("Content-Type" to listOf("text/plain")))) {
            status { equalTo(200) }
            header("Content-Type") { startsWith("application/json") }
        }
        assertEquals(
            listOf(
                "status: expected 200, actual 500",
                "headers.Content-Type: expected prefix application/json, actual [text/plain]",
            ),
            result.divergences.map { it.describe() },
        )
    }

    /*
    Header: null / empty
     */

    @Test
    fun `missing header gives missing marker`() {
        val result = validate(response()) { header("X-Trace") { startsWith("a") } }
        assertEquals(listOf(Divergence("headers.X-Trace", "prefix a", "<missing>")), result.divergences)
    }

    @Test
    fun `header with empty list gives empty brackets`() {
        val result = validate(response(headers = mapOf("X-Trace" to emptyList()))) {
            header("X-Trace") { startsWith("a") }
        }
        assertEquals(listOf(Divergence("headers.X-Trace", "prefix a", "[]")), result.divergences)
    }

    @Test
    fun `empty string inside non-empty list is a real value`() {
        val h = mapOf("X-Trace" to listOf("", "abc"))
        val ok = validate(response(headers = h)) { header("X-Trace") { hasValue("") } }
        assertTrue(ok.isSuccess)

        val fail = validate(response(headers = mapOf("X-Trace" to listOf("")))) {
            header("X-Trace") { hasValue("abc") }
        }
        // listOf("").toString() is also "[]", so the text matches the empty-list case
        assertEquals(listOf(Divergence("headers.X-Trace", "value abc", "[]")), fail.divergences)
    }

    /*
    Header: name case, merging keys
     */

    @Test
    fun `header name is case insensitive`() {
        val result = validate(response(headers = mapOf("Content-Type" to listOf("application/json")))) {
            header("content-type") { startsWith("application/json") }
        }
        assertTrue(result.isSuccess)
    }

    @Test
    fun `keys differing only by case are merged in map order`() {
        val h = linkedMapOf(
            "Accept" to listOf("application/json"),
            "accept" to listOf("text/html"),
        )
        val ok = validate(response(headers = h)) {
            header("ACCEPT") {
                hasValue("application/json")
                hasValue("text/html")
            }
        }
        assertTrue(ok.isSuccess)

        val fail = validate(response(headers = h)) { header("accept") { startsWith("x") } }
        assertEquals("[application/json, text/html]", fail.divergences.single().actual)
    }

    /*
    Header: values
     */

    @Test
    fun `different checks may pass on different values`() {
        val h = mapOf("Accept" to listOf("application/json", "text/html"))
        val result = validate(response(headers = h)) {
            header("Accept") {
                hasValue("application/json")
                hasValue("text/html")
            }
        }
        assertTrue(result.isSuccess)
    }

    @Test
    fun `comma separated string is not split`() {
        val h = mapOf("Accept" to listOf("application/json,text/html"))
        val result = validate(response(headers = h)) {
            header("Accept") {
                hasValue("application/json")
                hasValue("text/html")
            }
        }
        assertEquals(2, result.divergences.size)
    }

    @Test
    fun `values are compared with case and outer spaces`() {
        val result = validate(response(headers = mapOf("X" to listOf("Application/JSON", " application/json")))) {
            header("X") { hasValue("application/json") }
        }
        assertEquals(1, result.divergences.size)
    }

    @Test
    fun `prefix follows String startsWith`() {
        val result = validate(response(headers = mapOf("Content-Type" to listOf("application/jsonp")))) {
            header("Content-Type") { startsWith("application/json") }
        }
        assertTrue(result.isSuccess)
    }

    @Test
    fun `extra headers do not affect result`() {
        val h = mapOf(
            "Content-Type" to listOf("application/json"),
            "X-Other" to listOf("anything"),
        )
        val result = validate(response(headers = h)) {
            header("Content-Type") { startsWith("application/json") }
        }
        assertTrue(result.isSuccess)
    }

    /*
    Header: satisfiesOurRequirements
     */

    @Test
    fun `predicate is not called for missing header or empty list`() {
        var calls = 0
        val predicate: (String) -> Boolean = { calls++; true }

        val missing = validate(response()) { header("X-Trace") { satisfiesOurRequirements("any", predicate) } }
        val empty = validate(response(headers = mapOf("X-Trace" to emptyList()))) {
            header("X-Trace") { satisfiesOurRequirements("any", predicate) }
        }

        assertEquals(0, calls)
        assertEquals(listOf(Divergence("headers.X-Trace", "any", "<missing>")), missing.divergences)
        assertEquals(listOf(Divergence("headers.X-Trace", "any", "[]")), empty.divergences)
    }

    @Test
    fun `one passing value is enough for predicate`() {
        val h = mapOf("X-Trace" to listOf("", "  ", "abc"))
        val result = validate(response(headers = h)) {
            header("X-Trace") { satisfiesOurRequirements("non-blank value") { it.isNotBlank() } }
        }
        assertTrue(result.isSuccess)
    }

    @Test
    fun `predicate forms give identical results`() {
        val variable: (String) -> Boolean = { it.isNotBlank() }
        val responses = listOf(
            response(headers = mapOf("X-Trace" to listOf("abc"))),
            response(headers = mapOf("X-Trace" to listOf(" ", ""))),
            response(headers = mapOf("X-Trace" to emptyList())),
            response(),
        )

        for (r in responses) {
            val literal = validate(r) {
                header("X-Trace") { satisfiesOurRequirements("non-blank value") { value -> value.isNotBlank() } }
            }
            val viaVariable = validate(r) { header("X-Trace") { satisfiesOurRequirements("non-blank value", variable) } }
            val viaReference = validate(r) { header("X-Trace") { satisfiesOurRequirements("non-blank value", ::isNotBlankValue) } }

            assertEquals(literal, viaVariable)
            assertEquals(literal, viaReference)
        }
    }

    /*
    Independence of runs, input immutability
     */

    @Test
    fun `input response is not changed`() {
        val values = mutableListOf("application/json")
        val headers = linkedMapOf("Accept" to values as List<String>)
        val r = response(status = 500, headers = headers, body = "x")

        validate(r) {
            status { equalTo(200) }
            header("accept") { hasValue("text/html") }
        }

        assertEquals(500, r.status)
        assertEquals(mapOf("Accept" to listOf("application/json")), r.headers)
        assertEquals("x", r.body)
    }

    @Test
    fun `reused block gives independent results`() {
        val rules: lesson09.dsl.ResponseScope.() -> Unit = {
            status { equalTo(200) }
            header("Content-Type") { startsWith("application/json") }
        }
        val good = response(headers = mapOf("Content-Type" to listOf("application/json")))
        val bad = response(status = 500, headers = mapOf("Content-Type" to listOf("text/plain")))

        val first = validate(good, rules)
        val second = validate(bad, rules)
        val third = validate(good, rules)

        assertTrue(first.isSuccess)
        assertEquals(2, second.divergences.size)
        assertTrue(third.isSuccess)
        // earlier results are untouched by later runs
        assertTrue(first.divergences.isEmpty())
        assertEquals(2, second.divergences.size)
    }
}