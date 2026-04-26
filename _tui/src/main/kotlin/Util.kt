package euaie

import kotlin.math.*
import kotlin.time.*
import org.jline.utils.*

private val ds = java.text.DecimalFormatSymbols().decimalSeparator
private val bi = java.text.BreakIterator.getCharacterInstance(java.util.Locale.ROOT)

fun formatSize(bytes: Long, sign: Boolean = false): String =
    when {
        bytes < 0L    -> "-${formatSize(-bytes, false)}"
        bytes == 0L   -> "${if (sign) "=" else ""}0 B"
        bytes < 1024L -> "${if (sign) "+" else ""}$bytes B"
        else          -> {
            val z = (63 - bytes.countLeadingZeroBits()) / 10
            val x = bytes * 10 / (1L shl (z * 10)) //overflow at 1.6 EiB
            val i = x / 10
            val f = x % 10
            "${if (sign) "+" else ""}$i$ds$f %siB".format(" KMGTPE"[z])
        }
    }

fun formatTime(ms: Long, sign: Boolean = false): String =
    abs(ms).toDuration(DurationUnit.MILLISECONDS)
        .toComponents { d, h, m, s, _ -> "%d:%02d:%02d:%02d".format(d, h, m, s) }.let {
            when {
                ms == 0L && sign -> "=$it"
                ms > 0L && sign  -> "+$it"
                ms < 0L          -> "-$it"
                else             -> it
            }
        }

fun cut(text: String, length: Int): String =
    if (text.length <= abs(length)) text
    else if (length < 0) "…${text.takeLast(max(abs(length) - 1, 0))}"
    else if (length > 0) "${text.take(max(length - 1, 0))}…" else ""

fun gap(left: String, right: String, width: Int): Int =
    max(abs(width) - left.length - right.length, 0)

fun spread(left: String, right: String, width: Int, cutBoth: Boolean = false, filler: Char = ' '): String =
    if (cutBoth) {
        val l = cut(left, width / 2 - width.sign)
        val r = cut(right, width / 2 - width.sign)
        "$l${filler.toString().repeat(gap(l, r, width))}$r"
    } else cut("$left${filler.toString().repeat(gap(left, right, width))}$right", width)

fun cutC(text: String, length: Int): String { //TODO textMetrics.truncateToWidth?
    val t = AttributedString(text)
    val l = t.columnLength()
    return if (l <= abs(length)) text
    else if (length < 0)
        "…${t.columnSubSequence(l + length + 1, l).let { if (it.columnLength() >= -length) it.drop(1) else it }}"
    else if (length > 0)
        "${t.columnSubSequence(0, length - 1)}…"
    else ""
}

fun gapC(left: String, right: String, width: Int): Int =
    max(abs(width) - AttributedString(left).columnLength() - AttributedString(right).columnLength(), 0)

fun spreadC(left: String, right: String, width: Int, cutBoth: Boolean = false, filler: Char = ' '): String =
    if (cutBoth) {
        val l = cutC(left, width / 2 - width.sign)
        val r = cutC(right, width / 2 - width.sign)
        "$l${filler.toString().repeat(gapC(l, r, width))}$r"
    } else cutC("$left${filler.toString().repeat(gapC(left, right, width))}$right", width)

fun cutC2(text: String, length: Int): String {
    val t = AttributedString(text)
    if (t.columnLength() <= abs(length)) return text
    val parts = ArrayDeque<String>()
    var width = 0
    bi.setText(text)
    return if (length < 0) {
        var end = bi.last()
        var start = bi.previous()
        while (start != java.text.BreakIterator.DONE) {
            val part = text.substring(start, end)
            val cl = AttributedString(part).columnLength()
            if (width + cl >= -length) break
            parts.addFirst(part)
            width += cl
            end = start
            start = bi.previous()
        }
        "…${parts.joinToString("")}"
    } else if (length > 0) {
        var start = bi.first()
        var end = bi.next()
        while (end != java.text.BreakIterator.DONE) {
            val part = text.substring(start, end)
            val cl = AttributedString(part).columnLength()
            if (width + cl >= length) break
            parts.addLast(part)
            width += cl
            start = end
            end = bi.next()
        }
        "${parts.joinToString("")}…"
    } else ""
}
