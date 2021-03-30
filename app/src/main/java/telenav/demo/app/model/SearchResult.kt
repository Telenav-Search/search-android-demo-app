package telenav.demo.app.model

class SearchResult (
        val name: String,
        val categoryName: String,
        var address: String?,
        var phoneNo: String?,
        var ratingLevel: Double,
        var priceLevel: Int,
        var iconId: Int,
        var websitesList: List<String>,
        var email: String?,
        var hours: String?,
        var distance: Double,
        var latitude: Double,
        var longitude: Double
) {

    private constructor(builder: Builder) : this(
        builder.name, builder.categoryName, builder.address,
        builder.phoneNo, builder.ratingLevel, builder.priceLevel, builder.iconId,
        builder.websitesList, builder.email, builder.hours, builder.distance,
        builder.latitude, builder.longitude
    )

    companion object {
        inline fun build(name: String, categoryName: String, block: Builder.() -> Unit) = Builder(name, categoryName).apply(block).build()
    }

    class Builder(val name: String, val categoryName: String) {

        var address: String? = null
        var phoneNo: String? = null
        var ratingLevel: Double = 0.0
        var priceLevel: Int = 0
        var iconId: Int = 0
        var websitesList: List<String> = emptyList()
        var email: String? = null
        var hours: String? = null
        var distance: Double = 0.0
        var latitude: Double = 0.0
        var longitude: Double = 0.0

        fun build() = SearchResult(this)
    }
}
