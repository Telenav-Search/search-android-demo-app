package telenav.demo.app.utils

object MathUtil {

    data class Point(
        var x: Double,
        var y: Double
    )

    data class Line(
        var startPoint: Point,
        var endPoint: Point
    )

    @JvmStatic
    fun doIntersect(line1: Line, line2: Line): Boolean {
        return handleCommonPoints(
            line1.startPoint,
            line1.endPoint,
            line2.startPoint,
            line2.endPoint
        )
    }

    private fun handleCommonPoints(p1: Point, q1: Point, p2: Point, q2: Point): Boolean {
        if (p1 == p2 || p1 == q2 || q1 == p2 || q1 == q2) {
            return false
        }
        return doIntersect(p1, q1, p2, q2)
    }

    @JvmStatic
    private fun doIntersect(p1: Point, q1: Point, p2: Point, q2: Point): Boolean {
        // Find the four orientations needed for general and
        // special cases
        val o1 = orientation(p1, q1, p2)
        val o2 = orientation(p1, q1, q2)
        val o3 = orientation(p2, q2, p1)
        val o4 = orientation(p2, q2, q1)

        // General case
        if (o1 != o2 && o3 != o4) return true

        // Special Cases
        // p1, q1 and p2 are collinear and p2 lies on segment p1q1
        if (o1 == 0 && onSegment(p1, p2, q1)) return true

        // p1, q1 and q2 are collinear and q2 lies on segment p1q1
        if (o2 == 0 && onSegment(p1, q2, q1)) return true

        // p2, q2 and p1 are collinear and p1 lies on segment p2q2
        if (o3 == 0 && onSegment(p2, p1, q2)) return true

        // p2, q2 and q1 are collinear and q1 lies on segment p2q2
        return o4 == 0 && onSegment(p2, q1, q2)
    }

    @JvmStatic
    private fun orientation(p: Point, q: Point, r: Point): Int {
        val res = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y)

        if (res == 0.0) return 0

        return if (res > 0.0) 1 else 2
    }

    @JvmStatic
    private fun onSegment(p: Point, q: Point, r: Point): Boolean {
        return (q.x <= p.x.coerceAtLeast(r.x) && q.x >= p.x.coerceAtMost(r.x) &&
                q.y <= p.y.coerceAtLeast(r.y) && q.y >= p.y.coerceAtMost(r.y))
    }
}