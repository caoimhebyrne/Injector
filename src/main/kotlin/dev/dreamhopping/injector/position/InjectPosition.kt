package dev.dreamhopping.injector.position

enum class InjectPosition {
    BEFORE_ALL,
    BEFORE_RETURN;

    companion object {
        fun fromString(string: String) = values().firstOrNull { it.name == string }
    }
}