package lesson09.dsl

data class Divergence(
    val path: String,
    val expected: String,
    val actual: String,
)

data class ValidationResult(
    val divergences: List<Divergence>,
) {
    // empty = no errors
    val isSuccess: Boolean get() = divergences.isEmpty()
}

fun Divergence.describe(): String =
    "$path: expected $expected, actual $actual"

fun ValidationResult.describe(): String =
    divergences.joinToString("\n") { it.describe() }

