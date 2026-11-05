package lesson09.dsl

import lesson09.HttpResponse
import lesson09.dsl.scopes.*


fun newCheck01(response: HttpResponse): ValidationResult =
    validate(response) {
        status {
            equalTo(204)
        }

        body {
            nullOrEmpty()
        }
    }

fun newCheck02(response: HttpResponse): ValidationResult =
    validate(response) {
        status {
            equalTo(200)
        }

        header("Content-Type") {
            startsWith("application/json")
        }

        body {
            nonEmpty()
        }
    }

fun newCheck03(response: HttpResponse): ValidationResult =
    validate(response) {
        status {
            equalTo(200)
        }

        header("Accept") {
            hasValue("application/json")
            hasValue("text/html")
        }
    }

fun newCheck04(response: HttpResponse): ValidationResult =
    validate(response) {
        status {
            equalTo(200)
        }

        body {
            json {
                field("id") {
                    present()
                }

                field("name") {
                    equalTo("Ivanov")
                }
            }
        }
    }

fun newCheck05(response: HttpResponse): ValidationResult =
    validate(response) {
        status {
            equalTo(200)
        }

        body {
            json {
                field("id") {
                    equalTo(42L)
                }

                field("active") {
                    equalTo(true)
                }
            }
        }
    }

fun newCheck06(response: HttpResponse): ValidationResult =
    validate(response) {
        status {
            equalTo(422)
        }

        header("Content-Type") {
            startsWith("application/json")
        }

        body {
            json {
                field("error") {
                    equalTo("invalid")
                }

                field("retry") {
                    equalTo(false)
                }
            }
        }
    }