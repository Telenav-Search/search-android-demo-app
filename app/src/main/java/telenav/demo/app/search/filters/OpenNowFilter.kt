package telenav.demo.app.search.filters

class OpenNowFilter : Filter() {
    var isOpened = OpenNow.DEFAULT
}

enum class OpenNow {
    OPEN,
    DEFAULT
}