package euaie

import com.varabyte.kotter.foundation.*
import com.varabyte.kotter.foundation.collections.*
import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.foundation.render.*
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.*
import com.varabyte.kotter.runtime.terminal.*
import com.varabyte.kotter.terminal.system.*
import com.varabyte.kotterx.grid.*
import com.varabyte.kotterx.text.*
import kotlin.concurrent.*
import kotlin.io.path.*
import kotlin.math.*
import kotlin.time.*
import org.tinylog.*

enum class Action { DIFF, EXIT, FIND, HELP, MAIN, SCAN, SURE, SYNC, TEST } //TODO KEYS | AUTO?

object TUI {
    val orderCh = setOf(Ch.U, Ch.R, Ch.M, Ch.C, Ch.A)
    val orderDi = setOf(Di.N, Di.L, Di.R, Di.U)
    val orderOp = setOf(Op.NO, Op.DL, Op.ML, Op.CL, Op.DR, Op.MR, Op.CR)
    val keysF = orderCh.joinToString("|") { "${it.icon}" } +
            "|" + orderDi.joinToString("|") { "${it.icon}" } + "|!"
    var optionExitWhenDone = false
    var terminal: Terminal? = null
}

fun start(rootL: String, rootR: String, include: Set<String>, exclude: Set<String>) = session(
    TUI.terminal ?: SystemTerminal()) { //TODO sectionExceptionHandler?
    val sync = Sync(rootL, rootR, include, exclude)
    val cache = mutableMapOf<Pair<Ch?, Di?>, List<L3>>()
    val empty = createTempFile("$NAME-")
    var current = L1.fake
    var active = true //exit flag
    var action = Action.SCAN
    var shift = 0
    var index by liveVarOf(0)
    var order by liveVarOf(Di.U)
    var filter by liveVarOf("")
    var filterCh by liveVarOf<Ch?>(Ch.U)
    var filterDi by liveVarOf<Di?>(null)
    var sortBySize by liveVarOf(false)
    var showBoth by liveVarOf(false)
    var showName by liveVarOf(false)
    var showMore by liveVarOf(false)
    var showTail by liveVarOf(false)
    var showRCPS by liveVarOf(false)
    //val printLog: MainRenderScope.(String, String) -> Unit = { topL, topR ->
    val printLog = fun MainRenderScope.(topL: String, topR: String): Unit {
        underline { textLine(spread(topL, topR, width)) }
        MainWriter.log.forEach {
            if (it.first.level == Level.ERROR) red { text(it.second) }
            if (it.first.level == Level.WARN) yellow { text(it.second) }
        }
        textLine(MainWriter.log.lastOrNull()?.first?.message.orEmpty())
        if (!MainWriter.normal()) {
            red { text("${MainWriter.total(Level.ERROR)}") }; text(" | ")
            yellow { text("${MainWriter.total(Level.WARN)}") }; textLine()
        }
    }
    //val runSync: RunScope.(Boolean) -> Unit = { compare ->
    val runSync = fun RunScope.(compare: Boolean): Unit {
        cache.clear()
        MainWriter.clear()
        val render = timer(period = 100) { rerender() }
        val d = measureTime { if (compare) sync.compare() else sync.execute() }
        org.tinylog.kotlin.Logger.info {
            if (compare && sync.scan.finished() || !compare && sync.task.finished()) "$d" else "canceled"
        }
        render.cancel()
        rerender() //ensure latest state is shown
        action = Action.MAIN
        if (MainWriter.normal()) sendKeys(Keys.Escape)
    }
    while (active) when (action) {
//-------------------------------------------------------------------------------------------------
        Action.SCAN -> section {
            printLog("${sync.scan.textual()} ", "${sync.scan.duration().inWholeSeconds} s")
            if (sync.scan.started())
                bold { text(cut("[Delete|Space] cancel", width)) }
            if (!sync.scan.enabled() && !MainWriter.normal())
                bold { text(cut("[Enter|Esc] continue", width)) }
        }.runUntilKeyPressed(Keys.Enter, Keys.Escape) {
            onKeyPressed {
                when (key) {
                    Keys.Delete, Keys.Space -> if (sync.scan.started()) sync.scan.cancel()
                }
            }
            aside {
//                textLine("═".repeat(50)) //─═ //TODO width not available!?
                textLine()
                textLine("${sync.pathL} | ${sync.pathR}")
            }
            runSync(true)
        }
//-------------------------------------------------------------------------------------------------
        Action.MAIN -> run {
            var confirm = false
            var limit = 0
            var range = 0
            var start = 0L
            var cycle by liveVarOf(0)
            section {
                if (showRCPS) Thread.sleep(1)
                cycle = if (showRCPS) cycle + 1 else 0
                start = if (showRCPS) if (start == 0L) System.currentTimeMillis() else start else 0L
                val rcps = if (showRCPS) cycle * 1000 / (System.currentTimeMillis() - start) else 0
                val list = sync.list()
                val totalCh = LongArray(Ch.entries.size)
                val totalDi = LongArray(Di.entries.size + 1) //also count revised
                val totalOp = LongArray(Op.entries.size)
                val bytes = LongArray(3)
                val sign = if (showTail) -1 else 1
                val static = 11 + arrayOf(filter.isNotBlank(), showMore).count { it } //fixed lines
                val sector = (if (filterCh == Ch.U && filterDi == Di.N)
                    list.filter { it.proposed != it.actual } //dynamic
                else cache.getOrPut(filterCh to filterDi) {
                    list.filter { // static
                        (if (filterCh == Ch.U) it.l2.pq.c != Ch.U //exclude unchanged
                        else filterCh == null || filterCh == it.l2.pq.c) &&
                                (filterDi == null || filterDi == it.proposed)
                    }
                }).run { //TODO filter files/folders
                    if (filter.isNotBlank()) filter {
                        it.l2.pq.x.path.contains(filter, true) || it.l2.pq.y.path.contains(filter, true)
                    } else this
                }.run { if (sortBySize) sortedByDescending { max(it.l2.pq.x.size, it.l2.pq.y.size) } else this }
                limit = sector.size - 1 //can be negative!
                range = max(height - static, 0) //fixed top and bottom lines
                index = max(min(index, limit), 0) //LiveVar: update only once!
                shift = max(min(shift, limit - range + 1), 0) //scroll back but...
                shift = max(min(shift, index), index - range + 1) //follow index
                val item = sector.getOrNull(index)
                current = item?.l2?.pq ?: L1.fake
                if (order != Di.U && item != null) {
                    if (item.proposed != Di.N) item.actual = order
                    if (!item.l2.pq.x.file) list.filter { it.l2.pq.x.path.startsWith(item.l2.pq.x.path) }
                        .forEach { if (it.proposed != Di.N) it.actual = order }
                    if (!item.l2.pq.y.file) list.filter { it.l2.pq.y.path.startsWith(item.l2.pq.y.path) }
                        .forEach { if (it.proposed != Di.N) it.actual = order }
                }
                list.forEach {
                    totalCh[it.l2.pq.c.ordinal]++
                    totalDi[it.proposed.ordinal]++
                    totalOp[map(it).ordinal]++
                    if (it.proposed != it.actual) totalDi[totalDi.lastIndex]++
                }
                sector.forEach {
                    val x = if (it.l2.pq.x.file) abs(it.l2.pq.x.size) else 0L
                    val y = if (it.l2.pq.y.file) abs(it.l2.pq.y.size) else 0L
                    bytes[0] += abs(x - y)
                    bytes[1] += x
                    bytes[2] += y
                }
                confirm = totalOp.sum() - totalOp[Op.NO.ordinal] > totalOp[Op.NO.ordinal]
//overview------
                val sort = if (sortBySize) "size" else "path"
                val path = if (showName) "name" else "full"
                val line = if (showTail) "tail" else "head"
                val topL = "${list.size} (${list.count { it.l2.pq.x.real }} | ${list.count { it.l2.pq.y.real }}) "
                val topR = "$sort | $path | $line | " + if (rcps > 0) "$rcps" else "$width x $height"
                underline { textLine(spread(topL, topR, width)) } //TODO align count with two columns?
                grid(Cols { repeat(5) { star() } }, width - 6, GridCharacters.INVISIBLE,
                    0, Justification.LEFT, 1, HorizontalSeparatorIndices.None) {
                    TUI.orderCh.forEach {
                        cell {
                            scopedState {
                                if (it == filterCh && (filterCh != Ch.U || filterDi != Di.N))
                                    if (it == Ch.U) blue(ColorLayer.BG) else invert()
                                text("${totalCh[it.ordinal]}${it.icon}")
                            }
                        }
                    }
                    TUI.orderDi.forEach {
                        cell {
                            scopedState {
                                if (it == filterDi && (filterCh != Ch.U || filterDi != Di.N)) invert()
                                if (it == Di.U && totalDi[it.ordinal] > 0) red()
                                text("${totalDi[it.ordinal]}${it.icon}")
                            }
                        }
                    }
                    cell {
                        scopedState {
                            if (filterCh == Ch.U && filterDi == Di.N) invert()
                            if (totalDi.last() > 0) green()
                            text("${totalDi.last()}!")
                        }
                    }
                }
                grid(Cols { repeat(7) { star() } }, width - 8, GridCharacters.INVISIBLE,
                    0, Justification.LEFT, 1, HorizontalSeparatorIndices.None) {
                    TUI.orderOp.forEach {
                        cell {
                            scopedState {
                                if (confirm) yellow()
                                text("${totalOp[it.ordinal]}$it")
                            }
                        }
                    }
                }
                if (filter.isNotBlank()) magenta { textLine(cut("find: '$filter'", width * sign)) }
                bytes.map { formatSize(it) }.let {
                    val sL = "${it[0]} (${it[1]} | ${it[2]}) "
                    val sR = "${index + 1} / ${limit + 1}"
                    underline { textLine(spread(sL, sR, width * sign)) }
                }
//list----------
                for (i in shift until minOf(shift + range, sector.size)) {
                    val l = sector[i]
                    val ch = "${l.l2.pq.c.icon} ${l.l2.bp.c.icon}${l.l2.qd.c.icon} "
                    val di = "${l.proposed.icon} "
                    val op = "${map(l)} "
                    val ff = if (l.l2.pq.x.file && l.l2.pq.y.file) ' ' else '•' //TODO '?' in nis!
                    val px = if (showName) l.l2.pq.x.name else l.l2.pq.x.path
                    val py = if (showName) l.l2.pq.y.name else l.l2.pq.y.path
                    val p2 = if (l.l2.pq.c.m()) " | $py" else ""
                    val c0 = "$ch$di$op$ff"
                    scopedState {
                        if (l === item) invert()
                        if (l.proposed == Di.U) red()
                        if (l.proposed != l.actual) green()
                        if (showBoth) {
                            val cw = max((width - c0.length) / 2 - 1, 1)
                            val c1 = if (l.l2.pq.x.real) cutC(px, cw * sign) else ""
                            val c2 = if (l.l2.pq.y.real) cutC(py, cw * sign) else ""
                            val cg = if (l.l2.pq.y.real) " ".repeat(gapC(c1, "", cw + 2)) else ""
                            text(c0); text(c1); text(cg); textLine(c2)
                        } else textLine(c0 + cutC("$px$p2", (width - c0.length) * sign))
                    }
                }
//details-------
                if (item != null) {
                    val pq = item.l2.pq
                    val px = if (pq.x.real) pq.x.path.removeSuffix("/").removeSuffix(pq.x.name) else ""
                    val py = if (pq.y.real) pq.y.path.removeSuffix("/").removeSuffix(pq.y.name) else ""
                    val nx = if (pq.x.real) pq.x.name else ""
                    val ny = if (pq.y.real) pq.y.name else ""
                    val co = if (item.actual == Di.L || item.actual == Di.R) map(item).icons.first else ' '
                    val cd = if (item.actual == Di.L || item.actual == Di.R) item.actual.icon else ' '
                    val tm = pq.c.text
                    val tl = item.l2.bp.c.text
                    val tr = item.l2.qd.c.text
                    val tp = item.proposed.icon
                    val to = map(item).text
                    val sd = formatSize(abs(pq.x.size) - abs(pq.y.size), true)
                    val sx = formatSize(abs(pq.x.size))
                    val sy = formatSize(abs(pq.y.size))
                    val wx = pq.x.toWord()
                    val wy = pq.y.toWord()
                    val td = formatTime(abs(pq.x.time) - abs(pq.y.time), true)
                    val tx = formatTime(if (pq.x.time > 0) abs(pq.x.time) else 0)
                    val ty = formatTime(if (pq.y.time > 0) abs(pq.y.time) else 0)
                    textLine(spreadC("$px ", " $py", width * sign, true, co))
                    invert { textLine(spreadC("$nx ", " $ny", width * sign, true, cd)) }
                    textLine(cut("$tm ($tl | $tr) [$tp] $to", width * sign))
                    textLine(cut("$sd ($sx | $sy) [$wx | $wy]", width * sign))
                    textLine(cut("$td ($tx | $ty)", width * sign))
                }
//keys----------
                val more = "[V] view [F] find [S] sort [D] diff [P] path [,] line [${TUI.keysF}] filter"
                val keysL = if (!showMore)
                    "[Enter] execute [Backspace] compare [←|Space|→] change "
                else
                    "[E] execute [C] compare [U|J|K|I] scroll [H|M|L] change "
                val keysR = if (!showMore)
                    "[.] more [Esc] quit"
                else
                    "[Tab] help [Q] quit"
                if (!TUI.optionExitWhenDone || totalCh.sum() != totalCh[Ch.U.ordinal]) bold {
                    if (showMore) textLine(cut(more, width * sign))
                    text(spread(keysL, keysR, width * sign))
                }
            }.runUntilSignal {
                onKeyPressed {
                    var a = action
                    var i = index
                    var o = Di.U
                    when (key) {
                        Keys.Enter, Keys.E        -> a = Action.SYNC
                        Keys.Backspace, Keys.C    -> a = Action.SCAN
                        Keys.Tab                  -> a = Action.HELP
                        Keys.Escape, Keys.Q       -> a = Action.EXIT
                        Keys.F                    -> a = Action.FIND
                        Keys.D                    -> a = Action.DIFF
                        Keys.Dollar               -> a = Action.TEST
                        Keys.Home, Keys.Y, Keys.Z -> i = 0
                        Keys.End, Keys.O          -> i = limit
                        Keys.PageUp, Keys.I       -> i = index - range + 1
                        Keys.PageDown, Keys.U     -> i = index + range - 1
                        Keys.Up, Keys.K           -> i = index - 1
                        Keys.Down, Keys.J         -> i = index + 1
                        Keys.Left, Keys.H         -> o = Di.L
                        Keys.Right, Keys.L        -> o = Di.R
                        Keys.Space, Keys.M        -> o = Di.N
                        Keys.Digit0    -> showRCPS = !showRCPS
                        Keys.Digit1               -> showBoth = false
                        Keys.Digit2               -> showBoth = true
                        Keys.V                    -> showBoth = !showBoth
                        Keys.P, Keys.N -> showName = !showName
                        Keys.Comma     -> showTail = !showTail
                        Keys.Period               -> showMore = !showMore
                        Keys.S                    -> sortBySize = !sortBySize
                        Keys.Plus                 -> filterCh = if (filterCh == Ch.A) Ch.U else Ch.A
                        Keys.Star                 -> filterCh = if (filterCh == Ch.C) Ch.U else Ch.C
                        Keys.Tilde                -> filterCh = if (filterCh == Ch.M) Ch.U else Ch.M
                        Keys.Minus                -> filterCh = if (filterCh == Ch.R) Ch.U else Ch.R
                        Keys.Equals               -> filterCh = if (filterCh == Ch.U) null else Ch.U
                        Keys.Less                 -> filterDi = if (filterDi == Di.L) null else Di.L
                        Keys.Greater              -> filterDi = if (filterDi == Di.R) null else Di.R
                        Keys.Slash                -> filterDi = if (filterDi == Di.N) null else Di.N
                        Keys.QuestionMark         -> filterDi = if (filterDi == Di.U) null else Di.U
                        Keys.ExclamationMark      -> {
                            filterDi = if (filterCh == Ch.U && filterDi == Di.N) null else Di.N
                            filterCh = Ch.U
                        }
                    }
                    action = when (a) {
                        Action.SYNC -> if (confirm) Action.SURE else a
                        Action.DIFF -> if (current.x.file && current.y.file) a else Action.MAIN
                        else        -> a
                    }
                    index = max(min(i, limit), 0)
                    order = o
                    if (action != Action.MAIN) signal()
                }
                aside { textLine() }
                if (TUI.optionExitWhenDone && sync.list().all { it.l2.pq.c.u() }) {
                    action = Action.EXIT
                    signal()
                }
            }
        }
//-------------------------------------------------------------------------------------------------
        Action.FIND -> section {
            magenta { text("find: "); input(initialText = filter) }
            textLine()
            bold { text(cut("[Enter] apply", width)) }
        }.runUntilInputEntered {
            onInputEntered { filter = input }
            aside { textLine() }
            action = Action.MAIN
        }
//-------------------------------------------------------------------------------------------------
        Action.SURE -> section {
            yellow { textLine(cut("are you sure?", width)) }
            bold { text(spread("[Space|Y] continue", "[Delete|N] cancel", width, true)) }
        }.runUntilKeyPressed(Keys.Delete, Keys.N, Keys.Space, Keys.Y) {
            onKeyPressed {
                action = when (key) {
                    Keys.Space, Keys.Y -> Action.SYNC
                    else               -> Action.MAIN
                }
            }
            aside { textLine() }
        }
//-------------------------------------------------------------------------------------------------
        Action.SYNC -> section {
            printLog("${sync.task.textual()} ", "${sync.task.duration().inWholeSeconds} s")
            if (sync.copy.enabled()) {
                val d = formatSize(sync.copy.done.get())
                val t = formatSize(sync.copy.goal.get())
                val s = formatSize(sync.copy.done.get() / sync.copy.duration().inWholeSeconds)
                val r = (sync.copy.goal.get() - sync.copy.done.get()) /
                        (sync.copy.done.get() / sync.copy.duration().inWholeSeconds)
                val n = sync.copy.progress(width.toDouble()).roundToInt()
                val l = spread("$d / $t ", "$s/s | $r s", width, true)
                invert { text(l.take(n)) }; textLine(l.drop(n))
            }
            if (sync.task.started())
                bold { text(cut("[Space|Tab] pause", width)) }
            if (sync.task.paused())
                bold { text(cut("[Space|Tab] resume [Delete|End] cancel", width)) }
            if (sync.task.finished() && !MainWriter.normal())
                bold { text(cut("[Enter|Esc] continue", width)) }
        }.runUntilKeyPressed(Keys.Enter, Keys.Escape) {
            onKeyPressed {
                when (key) {
                    Keys.Space, Keys.Tab  -> {
                        if (sync.task.paused()) sync.task.start() else if (sync.task.started()) sync.task.pause()
                        if (sync.copy.paused()) sync.copy.start() else if (sync.copy.started()) sync.copy.pause()
                    }
                    Keys.Delete, Keys.End -> {
                        if (sync.task.paused()) sync.task.cancel()
                        if (sync.copy.paused()) sync.copy.cancel()
                    }
                }
            }
            aside { textLine() }
            runSync(false)
        }
//-------------------------------------------------------------------------------------------------
        Action.DIFF -> run {
            var result = listOf("")
            var drop by liveVarOf(0)
            var head by liveVarOf(true)
            if (max(current.x.size, current.x.size) < 8388608L) section { //TODO 8 MiB limit?
                val line = if (head) "head" else "tail"
                underline { textLine(spread("diff ", "$line | $drop/${result.size}", width)) }
                val w = if (head) width else -width
                result.drop(drop).take(max(height - 2, 1)).forEach {
                    when (it.first()) {
                        '<'  -> red { textLine(cut(it, w)) }
                        '>'  -> green { textLine(cut(it, w)) }
                        else -> textLine(cut(it, w))
                    }
                }
                bold { text(cut("[Enter|Esc|Space] return", width)) }
            }.runUntilKeyPressed(Keys.Enter, Keys.Escape, Keys.Space) {
                onKeyPressed {
                    when (key) {
                        Keys.Home  -> drop = 0
                        Keys.End   -> drop = result.size
                        Keys.Up    -> drop = max(drop - 1, 0)
                        Keys.Down  -> drop = min(drop + 1, result.size)
                        Keys.Left  -> head = true
                        Keys.Right -> head = false //TODO scroll horizontally?
                    }
                }
                aside { textLine() }
                result = try {
                    val x = if (current.x.real) "${sync.pathL}/${current.x.path}" else "$empty"
                    val y = if (current.y.real) "${sync.pathR}/${current.y.path}" else "$empty"
                    ProcessBuilder("diff", x, y).redirectErrorStream(true)
                        .start().inputStream.bufferedReader().readLines()
                } catch (e: Exception) {
                    listOf(e.localizedMessage)
                }
                rerender()
            }
            action = Action.MAIN
        }
//-------------------------------------------------------------------------------------------------
        Action.HELP -> section {
            val credits = " made with Kotter + picocli + tinylog + ♥"
            underline { textLine(spread(version.substringBefore('-'), credits, width)) }
            grid(Cols { fit(); fit(maxWidth = max(width - 16, 1)); fit(); fit(maxWidth = max(width - 31, 1)) },
                maxCellHeight = 1, paddingLeftRight = 1, characters = GridCharacters.BOX_THIN,
                horizontalSeparatorIndices = HorizontalSeparatorIndices.None) {
                cell { text("${Ch.U.icon}") }; cell { text("${Ch.U.text} / skip") }
                cell { blue(ColorLayer.BG) { text("${Ch.U.icon}") } }; cell { text("hide unchanged") }
                cell { text("${Ch.R.icon}") }; cell { text("${Ch.R.text} / delete") }
                cell { invert { text("${Ch.R.icon}") } }; cell { text("show only removed") }
                cell { text("${Ch.M.icon}") }; cell { text("${Ch.M.text} / move") }
                cell { invert { text("${Ch.M.icon}") } }; cell { text("show only moved") }
                cell { text("${Ch.C.icon}") }; cell { text("${Ch.C.text} ") }
                cell { invert { text("${Ch.C.icon}") } }; cell { text("show only changed") }
                cell { text("${Ch.A.icon}") }; cell { text("${Ch.A.text} / copy") }
                cell { invert { text("${Ch.A.icon}") } }; cell { text("show only added") }
            }
            grid(Cols { fit(); fit(maxWidth = max(width - 16, 1)) },
                maxCellHeight = 1, paddingLeftRight = 1, characters = GridCharacters.BOX_THIN,
                horizontalSeparatorIndices = HorizontalSeparatorIndices.None) {
                cell { text("${Di.N.icon}") }; cell { text("neutral") }
                cell { text("${Di.L.icon}") }; cell { text("to the left") }
                cell { text("${Di.R.icon}") }; cell { text("to the right") }
                cell { text("${Di.U.icon}") }; cell { text("unclear") }
                cell { text("!") }; cell { text("revised") } //TODO show only!
            }
            bold { text(cut("[Enter|Esc|Tab] return", width)) }
        }.runUntilKeyPressed(Keys.Enter, Keys.Escape, Keys.Tab) {
            aside { textLine() }
            action = Action.MAIN
        }
//-------------------------------------------------------------------------------------------------
        Action.TEST -> run {
            println("run")
            var last by liveVarOf<Key>(Keys.Space)
            section {
                println("section")
                textLine("${System.currentTimeMillis()}")
                textLine("[$last]")
                val s = "a:b：c？"
                for (i in 0..9)
                    textLine("+$i:|${cutC(s, +i)}|")
                for (i in 0..9)
                    textLine("-$i:|${cutC(s, -i)}|")
            }.runUntilKeyPressed(Keys.Escape) {
                println("runUntilKeyPressed")
                onKeyPressed {
                    println("onKeyPressed")
                    last = key
                }
                aside {
                    println("aside")
                    textLine()
                }
                action = Action.EXIT
            }
        }
//-------------------------------------------------------------------------------------------------
        Action.EXIT -> section {}.run { //TODO save settings?
            empty.deleteIfExists()
            active = false
        }
    }
}
