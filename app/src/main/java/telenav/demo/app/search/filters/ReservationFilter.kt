package telenav.demo.app.search.filters

class ReservationFilter : Filter() {
    var isReserved = Reservation.DEFAULT
}

enum class Reservation {
    RESERVED,
    DEFAULT
}