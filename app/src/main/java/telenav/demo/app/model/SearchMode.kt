package telenav.demo.app.model

enum class SearchMode(val value: Int) {
    HYBRID(1), ONBOARD(0);

    companion object {
        fun fromInt(value: Int) = values().first { it.value == value }
    }
}