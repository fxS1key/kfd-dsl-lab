package lesson09

import lesson09.dsl.Divergence
import lesson09.dsl.ValidationResult
import lesson09.dsl.*
import lesson09.legacy.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private fun response(
    status: Int = 200,
    headers: Map<String, List<String>> = emptyMap(),
    body: String? = null,
) = HttpResponse(status, headers, body)

private val jsonHeaders = mapOf("Content-Type" to listOf("application/json"))
private val textHeaders = mapOf("Content-Type" to listOf("text/plain"))

private class Case(val response: HttpResponse, val expected: List<Divergence>)

private class Spec(
    val legacy: (HttpResponse) -> Boolean,
    val modern: (HttpResponse) -> ValidationResult,
    val status: Int,
    val headers: Map<String, List<String>>,
)

private fun verify(
    legacy: (HttpResponse) -> Boolean,
    modern: (HttpResponse) -> ValidationResult,
    cases: List<Case>,
) {
    for (case in cases) {
        val result = modern(case.response)
        assertEquals(case.expected, result.divergences, "divergences for ${case.response}")
        assertEquals(legacy(case.response), result.isSuccess, "legacy vs new for ${case.response}")
    }
}

class LegacyComparisonTest {

    @Test
    fun `check01 matches legacy`() {
        verify(
            ::check01, ::newCheck01,
            listOf(
                Case(response(204, body = null), emptyList()),
                Case(response(204, body = ""), emptyList()),
                Case(response(200, body = null), listOf(Divergence("status", "204", "200"))),
                Case(
                    response(204, body = " "),
                    listOf(Divergence("body", "null or empty body", "\" \"")),
                ),
                Case(
                    response(204, body = "x"),
                    listOf(Divergence("body", "null or empty body", "\"x\"")),
                ),
                Case(
                    response(500, body = "x"),
                    listOf(
                        Divergence("status", "204", "500"),
                        Divergence("body", "null or empty body", "\"x\""),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `check02 matches legacy`() {
        verify(
            ::check02, ::newCheck02,
            listOf(
                // invalid json is fine: check02 does not parse the body
                Case(response(200, jsonHeaders, "invalid"), emptyList()),
                Case(
                    response(200, mapOf("content-type" to listOf("application/json; charset=utf-8")), "x"),
                    emptyList(),
                ),
                Case(response(200, jsonHeaders, " "), emptyList()),
                Case(response(500, jsonHeaders, "x"), listOf(Divergence("status", "200", "500"))),
                Case(
                    response(200, textHeaders, "x"),
                    listOf(Divergence("headers.Content-Type", "prefix application/json", "[text/plain]")),
                ),
                Case(
                    response(200, emptyMap(), "x"),
                    listOf(Divergence("headers.Content-Type", "prefix application/json", "<missing>")),
                ),
                Case(response(200, jsonHeaders, ""), listOf(Divergence("body", "empty body", "\"\""))),
                Case(response(200, jsonHeaders, null), listOf(Divergence("body", "empty body", "<null>"))),
            ),
        )
    }

    @Test
    fun `check03 matches legacy`() {
        val both = mapOf("Accept" to listOf("application/json", "text/html"))
        verify(
            ::check03, ::newCheck03,
            listOf(
                Case(response(200, both), emptyList()),
                Case(
                    response(
                        200,
                        linkedMapOf("Accept" to listOf("application/json"), "accept" to listOf("text/html")),
                    ),
                    emptyList(),
                ),
                Case(response(500, both), listOf(Divergence("status", "200", "500"))),
                Case(
                    response(200, mapOf("Accept" to listOf("application/json"))),
                    listOf(Divergence("headers.Accept", "value text/html", "[application/json]")),
                ),
                Case(
                    response(200, mapOf("Accept" to listOf("application/json,text/html"))),
                    listOf(
                        Divergence("headers.Accept", "value application/json", "[application/json,text/html]"),
                        Divergence("headers.Accept", "value text/html", "[application/json,text/html]"),
                    ),
                ),
                Case(
                    response(200, emptyMap()),
                    listOf(
                        Divergence("headers.Accept", "value application/json", "<missing>"),
                        Divergence("headers.Accept", "value text/html", "<missing>"),
                    ),
                ),
                Case(
                    response(200, mapOf("Accept" to listOf("Application/JSON", "text/html"))),
                    listOf(Divergence("headers.Accept", "value application/json", "[Application/JSON, text/html]")),
                ),
            ),
        )
    }

    @Test
    fun `check04 matches legacy`() {
        verify(
            ::check04, ::newCheck04,
            listOf(
                Case(response(200, body = """{"id":null,"name":"Ivanov"}"""), emptyList()),
                Case(response(200, body = """{"id":1,"name":"Ivanov","extra":true}"""), emptyList()),
                Case(
                    response(500, body = """{"id":1,"name":"Ivanov"}"""),
                    listOf(Divergence("status", "200", "500")),
                ),
                Case(
                    response(200, body = """{"name":"Ivanov"}"""),
                    listOf(Divergence("body.json.id", "present", "<missing>")),
                ),
                Case(
                    response(200, body = """{"id":1,"name":"Petrov"}"""),
                    listOf(Divergence("body.json.name", "string \"Ivanov\"", "string \"Petrov\"")),
                ),
                Case(
                    response(200, body = """{"id":1}"""),
                    listOf(Divergence("body.json.name", "string \"Ivanov\"", "<missing>")),
                ),
            ),
        )
    }

    @Test
    fun `check05 matches legacy`() {
        verify(
            ::check05, ::newCheck05,
            listOf(
                Case(response(200, body = """{"id":42,"active":true}"""), emptyList()),
                Case(response(200, body = """{"id":42,"active":true,"x":null}"""), emptyList()),
                Case(
                    response(500, body = """{"id":42,"active":true}"""),
                    listOf(Divergence("status", "200", "500")),
                ),
                Case(
                    response(200, body = """{"id":"42","active":true}"""),
                    listOf(Divergence("body.json.id", "integer 42", "string \"42\"")),
                ),
                Case(
                    response(200, body = """{"id":43,"active":true}"""),
                    listOf(Divergence("body.json.id", "integer 42", "integer \"43\"")),
                ),
                Case(
                    response(200, body = """{"id":42,"active":false}"""),
                    listOf(Divergence("body.json.active", "boolean true", "boolean \"false\"")),
                ),
                Case(
                    response(200, body = """{"id":42}"""),
                    listOf(Divergence("body.json.active", "boolean true", "<missing>")),
                ),
            ),
        )
    }

    @Test
    fun `check06 matches legacy`() {
        val ok = """{"error":"invalid","retry":false}"""
        verify(
            ::check06, ::newCheck06,
            listOf(
                Case(response(422, jsonHeaders, ok), emptyList()),
                Case(response(200, jsonHeaders, ok), listOf(Divergence("status", "422", "200"))),
                Case(
                    response(422, textHeaders, ok),
                    listOf(Divergence("headers.Content-Type", "prefix application/json", "[text/plain]")),
                ),
                Case(
                    response(422, jsonHeaders, """{"error":"other","retry":false}"""),
                    listOf(Divergence("body.json.error", "string \"invalid\"", "string \"other\"")),
                ),
                Case(
                    response(422, jsonHeaders, """{"error":"invalid","retry":true}"""),
                    listOf(Divergence("body.json.retry", "boolean false", "boolean \"true\"")),
                ),
                Case(
                    response(500, textHeaders, """{"error":"other","retry":false}"""),
                    listOf(
                        Divergence("status", "422", "500"),
                        Divergence("headers.Content-Type", "prefix application/json", "[text/plain]"),
                        Divergence("body.json.error", "string \"invalid\"", "string \"other\""),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `unparsable or null body gives one body json record in json checks`() {
        val specs = listOf(
            Spec(::check04, ::newCheck04, 200, emptyMap()),
            Spec(::check05, ::newCheck05, 200, emptyMap()),
            Spec(::check06, ::newCheck06, 422, jsonHeaders),
        )
        val badBodies = listOf("invalid", null, "{", """{"id":1,}""")

        for (spec in specs) {
            for (bad in badBodies) {
                val r = response(spec.status, spec.headers, bad)
                val result = spec.modern(r)

                assertFalse(spec.legacy(r), "legacy for body=$bad")
                assertEquals(1, result.divergences.size, "count for body=$bad")
                assertEquals("body.json", result.divergences.single().path)
                assertEquals("valid JSON object", result.divergences.single().expected)
            }
        }
    }
}