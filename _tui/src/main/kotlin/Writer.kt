package euaie

import org.tinylog.*
import org.tinylog.core.*
import org.tinylog.writers.*

class MainWriter(properties: MutableMap<String?, String?>?) : AbstractFormatPatternWriter(properties) {
    companion object {
        val log = ArrayDeque<Pair<LogEntry, String>>(SIZE)
        private const val SIZE = 10000
        private val total = IntArray(Level.entries.size)
        fun total(l: Level): Int = total[l.ordinal]
        fun normal(): Boolean = total[Level.WARN.ordinal] + total[Level.ERROR.ordinal] == 0
        fun clear() {
            log.clear()
            total.fill(0)
        }
    }

    override fun write(entry: LogEntry) {
        log.addLast(entry to render(entry))
        if (log.size > SIZE) log.removeFirstOrNull()
        total[entry.level.ordinal]++
    }

    override fun flush() {}

    override fun close() {}
}
