package telenav.demo.app.search.filters

class StarsFilter : Filter() {
    var stars= Stars.DEFAULT
}

enum class Stars(val starsNumber: Int) {
    ONE(1),
    TWO(2),
    TREE(3),
    FOUR(4),
    FIVE(5),
    DEFAULT(-1)
}