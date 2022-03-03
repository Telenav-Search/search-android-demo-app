package telenav.demo.app.search.filters

class FreeChargeFilter : Filter() {
    var isCharged = Reservation.DEFAULT
}

enum class FreeCharge {
    CHARGED,
    DEFAULT
}