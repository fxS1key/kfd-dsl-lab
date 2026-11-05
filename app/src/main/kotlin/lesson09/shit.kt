package lesson09

import lesson09.dsl.describe
import lesson09.dsl.validate

@DslMarker
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class JsonDsl

fun main() {
    val shittyResponse = HttpResponse(500, mapOf("Content-Type" to listOf("text/plain")), "{\"id\": 42 }")

    val r = validate(shittyResponse){
        this@validate.header("Content-Type") {
            startsWith("application/json")
        }
        this@validate.body {
            this@validate.status {
                equalTo(200)
            }
            json {
                field("id") {
                    present()
                    equalTo(42)
                }
            }
        }
    }

    println(r.describe())
}