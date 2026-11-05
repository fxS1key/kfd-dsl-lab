package lesson09.dsl

class DivergenceCollector {
    private val items = mutableListOf<Divergence>()

    fun add(path: String, expected: String, actual: String) {
        items.add(Divergence(path, expected, actual))
    }

    fun toResult(): ValidationResult = ValidationResult(items.toList())
}