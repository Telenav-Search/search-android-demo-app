package telenav.demo.app.search.filters

class PriceLevel : Filter() {
    var priceLevel = PriceLevelType.DEFAULT
}

enum class PriceLevelType(val priceLevel: Int) {
    ONE_DOLLAR(1),
    TWO_DOLLAR(2),
    THREE_DOLLAR(3),
    FOUR_DOLLAR(4),
    DEFAULT(-1),
}