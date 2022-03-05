package telenav.demo.app.utils

object StringUtil {

    fun formatName(name: String): String {
        var formatName = name
        if (formatName.length > 15) {
            formatName = formatName.substring(0, 15) + "..."
        } else {
            val size = 15 - formatName.length
            for (i in 0..size / 2) {
                formatName += " "
            }
            for (i in 0..size / 2) {
                formatName = " $formatName"
            }
        }
        return formatName
    }

}