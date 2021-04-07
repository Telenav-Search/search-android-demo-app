package telenav.demo.app.model

import com.telenav.sdk.entity.model.base.Parking

class SearchResult(
    val id: String?,
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
    var longitude: Double,
    var permanentlyClosed: Boolean?,
    var parking: Parking?
) {

    private constructor(builder: Builder) : this(builder.id,
        builder.name, builder.categoryName, builder.address,
        builder.phoneNo, builder.ratingLevel, builder.priceLevel, builder.iconId,
        builder.websitesList, builder.email, builder.hours, builder.distance,
        builder.latitude, builder.longitude, builder.permanentlyClosed, builder.parking
    )

    companion object {
        inline fun build(name: String, categoryName: String, block: Builder.() -> Unit) = Builder(name, categoryName).apply(block).build()
    }

    class Builder(val name: String, val categoryName: String) {
        var id: String? = null
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
        var permanentlyClosed: Boolean? = null
        var parking: Parking? = null

        fun build() = SearchResult(this)
    }
}
