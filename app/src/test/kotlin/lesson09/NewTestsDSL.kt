package lesson09

import lesson09.dsl.newCheck01
import lesson09.dsl.newCheck02
import lesson09.dsl.newCheck03
import lesson09.dsl.newCheck04
import lesson09.dsl.newCheck05
import lesson09.dsl.newCheck06
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NewChecksTest {

    @Test
    fun noContentAllowsAbsentOrEmptyBody() {
        assertTrue(newCheck01(HttpResponse(204, emptyMap(), null)).isSuccess)
        assertTrue(newCheck01(HttpResponse(204, emptyMap(), "")).isSuccess)
        assertFalse(newCheck01(HttpResponse(204, emptyMap(), " ")).isSuccess)
        assertFalse(newCheck01(HttpResponse(200, emptyMap(), null)).isSuccess)
    }

    @Test
    fun nonEmptyBodyDoesNotRequireValidJson() {
        val response = HttpResponse(
            200,
            mapOf("content-type" to listOf("application/json; charset=utf-8")),
            "invalid"
        )

        assertTrue(newCheck02(response).isSuccess)
        assertFalse(newCheck02(response.copy(headers = emptyMap())).isSuccess)
        assertFalse(newCheck02(response.copy(body = "")).isSuccess)
    }

    @Test
    fun twoHeaderConditionsCanUseDifferentValues() {
        val response = HttpResponse(
            200,
            linkedMapOf(
                "Accept" to listOf("application/json"),
                "accept" to listOf("text/html")
            ),
            null
        )

        assertTrue(newCheck03(response).isSuccess)
        assertFalse(
            newCheck03(
                response.copy(headers = mapOf("Accept" to listOf("application/json")))
            ).isSuccess
        )
        assertFalse(
            newCheck03(
                response.copy(headers = mapOf("Accept" to listOf("application/json,text/html")))
            ).isSuccess
        )
    }

    @Test
    fun presentNullIsDifferentFromMissingKey() {
        assertTrue(
            newCheck04(
                HttpResponse(200, emptyMap(), """{"id":null,"name":"Ivanov"}""")
            ).isSuccess
        )
        assertFalse(
            newCheck04(
                HttpResponse(200, emptyMap(), """{"name":"Ivanov"}""")
            ).isSuccess
        )
    }

    @Test
    fun numericAndBooleanValuesKeepTheirTypes() {
        val response = HttpResponse(
            200,
            emptyMap(),
            """{"id":42,"active":true}"""
        )

        assertTrue(newCheck05(response).isSuccess)
        assertFalse(
            newCheck05(response.copy(body = """{"id":"42","active":true}""")).isSuccess
        )
        assertFalse(
            newCheck05(response.copy(body = """{"id":42,"active":false}""")).isSuccess
        )
    }

    @Test
    fun errorResponseRequiresStatusHeaderAndJsonFields() {
        val response = HttpResponse(
            422,
            mapOf("Content-Type" to listOf("application/json")),
            """{"error":"invalid","retry":false}"""
        )

        assertTrue(newCheck06(response).isSuccess)
        assertFalse(newCheck06(response.copy(status = 200)).isSuccess)
        assertFalse(newCheck06(response.copy(headers = emptyMap())).isSuccess)
        assertFalse(newCheck06(response.copy(body = "invalid")).isSuccess)
        assertFalse(
            newCheck06(response.copy(body = """{"error":"invalid","retry":true}""")).isSuccess
        )
    }
}