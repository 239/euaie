package euaie

import kotlin.math.*

typealias M0 = Map<String, L0>
typealias MM0 = MutableMap<String, L0>

data class L0(val path: String, val size: Long, val time: Long) {
    val name = path.split('/').dropLastWhile { it.isEmpty() }.lastOrNull().orEmpty()
    val file = size >= 0
    val real = time >= 0
    fun e(a: L0) = ep(a) && es(a) && et(a)
    fun ep(a: L0) = path == a.path
    fun es(a: L0) = size == a.size
    fun et(a: L0) = if (tolerance == 0L) time == a.time else
        abs(time - a.time).let { it <= tolerance || it == 3600000L }
    fun link() = time == LINK
    fun toLine() = "$path$D$size$D$time"
    fun toWord() = if (real) if (file) if (link()) "link" else "file" else "folder" else " "
    override fun toString() = "[$path|${if (file) size else '/'}|${if (real) time else '°'}]"

    companion object {
        const val D = Char.MIN_VALUE
        const val LINK = 239L
        val fake = L0("", 0, -1)
        var tolerance = 0L
        fun fromLine(line: String): L0? {
            val l = line.split(D)
            val p = l.getOrNull(0)
            val s = l.getOrNull(1)?.toLongOrNull()
            val t = l.getOrNull(2)?.toLongOrNull()
            return if (p != null && s != null && t != null) L0(p, s, t) else null
        }
    }
}
