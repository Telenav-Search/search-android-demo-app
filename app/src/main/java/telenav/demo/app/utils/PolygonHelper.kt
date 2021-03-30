package telenav.demo.app.utils

object PolygonHelper {

    @JvmStatic
    fun getClosedPolygonPoints(points: List<MathUtil.Point>): List<MathUtil.Point> {
        val filteredPolygon = reduceIdenticalSections(
            points
        ).toMutableList()
        //closes the polygon
        filteredPolygon.add(filteredPolygon.first())
        return getPolygonWithoutIntersections(filteredPolygon)
    }

    @JvmStatic
    private fun getPolygonWithoutIntersections(points: List<MathUtil.Point>): List<MathUtil.Point> {
        var lastSize = points.size
        var noIntersection = resolveIntersection(points)

        //remove all intersections
        while (noIntersection.size < lastSize) {
            lastSize = noIntersection.size
            noIntersection = resolveIntersection(noIntersection)
        }
        return noIntersection
    }

    @JvmStatic
    private fun resolveIntersection(points: List<MathUtil.Point>): List<MathUtil.Point> {
        val listSize = points.size

        for (i in 0..listSize - 4) {
            val currentLine =
                MathUtil.Line(points[i], points[i + 1])
            val firstIntLineStartIndex =
                getIntersectedLineStartPointIndex(
                    currentLine,
                    points,
                    i + 2,
                    listSize - 2
                )
                    ?: continue

            // if the last line that closes the polygon intersects with other lines
            // then the last point needs to be found, where adding the close line doesn't intersect with others
            if (isLastLineStartIndex(
                    firstIntLineStartIndex,
                    listSize
                )
            ) {
                val closeLine = MathUtil.Line(
                    points[firstIntLineStartIndex],
                    points[firstIntLineStartIndex + 1]
                )
                val closeLineStartPointIndex =
                    getIntersectedLineStartPointIndex(
                        closeLine, points, 0, firstIntLineStartIndex - 2
                    )

                //closeLineStartPointIndex should never be null
                return getClosedPolygonPointsWithoutIntersections(
                    points,
                    closeLineStartPointIndex!!
                )
            }
            return getClosedPolygonPointsWithoutIntersections(points, firstIntLineStartIndex)
        }
        return points
    }

    @JvmStatic
    private fun getClosedPolygonPointsWithoutIntersections(
        points: List<MathUtil.Point>,
        lastIndex: Int
    ): List<MathUtil.Point> {
        return points.subList(0, lastIndex + 1).toMutableList()
            .apply { add(points.first()) }
    }

    @JvmStatic
    private fun isLastLineStartIndex(index: Int, listSize: Int) = index == listSize - 2

    @JvmStatic
    fun <T> reduceIdenticalSections(list: List<T>): List<T> {
        val filteredList = arrayListOf<T>()
        filteredList.add(list.first())

        for (i in 1 until list.size) {
            val currentElement = list[i]
            if (currentElement != list[i - 1]) {
                filteredList.add(currentElement)
            }
        }
        return filteredList
    }

    @JvmStatic
    private fun getIntersectedLineStartPointIndex(
        line: MathUtil.Line,
        points: List<MathUtil.Point>,
        startIndex: Int,
        endIndex: Int
    ): Int? {
        for (i in startIndex..endIndex) {
            val nextLine =
                MathUtil.Line(points[i], points[i + 1])
            if (MathUtil.doIntersect(line, nextLine)) {
                return i
            }
        }
        return null
    }

}