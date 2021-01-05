package telenav.demo.app.initialization

enum class SearchMode(val value: Int) {
    HYBRID(1), ONBOARD(0);

    companion object {
        fun fromInt(value: Int) = values().first { it.value == value }
    }
}