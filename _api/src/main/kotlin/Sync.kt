package euaie

import java.nio.file.*
import java.security.*
import kotlin.io.path.*
import org.tinylog.kotlin.Logger as L

class Sync(val rootL: String, val rootR: String, val include: Set<String>, val exclude: Set<String>) {
    val task = Task()
    val scan = Task()
    val copy = Task()
    val pathL = Path(rootL).absolutePathString()
    val pathR = Path(rootR).absolutePathString()
    private val scanL = Scan(rootL, include, exclude, hash(pathL + L0.D + pathR), scan)
    private val scanR = Scan(rootR, include, exclude, hash(pathR + L0.D + pathL), scan)
    private val finish = mutableListOf<Triple<Path, Path, Boolean>>()
    private var result = emptyList<L3>()
    private var copyThreshold = 0

    companion object {
        const val SLEEP = 239L
        private const val SUFFIX = "euaie"
        private val copyOptions = if (Scan.optionSymbolicLink == OptionSymbolicLink.FOLLOW)
            arrayOf<CopyOption>(StandardCopyOption.COPY_ATTRIBUTES)
        else
            arrayOf<CopyOption>(StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS)
        var optionStateless = false
        var optionCopyThreshold = 512
    }

    fun list(): List<L3> = result

    fun compare(save: Boolean = false) {
        L.info { "-----------------------compare" }
        L.debug { "$rootL $rootR +$include -$exclude" }
        scan.start(true)
        if (pathL == pathR) {
            L.error { "identical root paths" }
            return
        }
        val sl = scanL.scan(save)
        val sr = scanR.scan(save)
        val ll = scanL.load(optionStateless)
        val lr = scanR.load(optionStateless)
        val bp = link(ll, sl)
        val qd = link(sr, lr)
        val pq = link(sl, sr)
        result = wrap(link(bp, pq, qd).sortedBy { it.pq.x.path })
        L.debug { "${result.size}" }
        if (scan.started()) scan.finish() else result = emptyList()
    }

    fun execute() {
        val m = mutableMapOf<Op, MutableList<L1>>()
        L.info { "-----------------------execute" }
        copyThreshold = optionCopyThreshold.coerceIn(1, 1024) * 1024 * 1024 //1 MiB .. 1 GiB
        task.start(true)
        task.goal.set(result.count { it.actual == Di.L || it.actual == Di.R }.toLong())
        for (l in result) m.getOrPut(map(l)) { mutableListOf() }.add(l.l2.pq)
        loop@ for (o in listOf(Op.ML, Op.MR, Op.DL, Op.DR, Op.CL, Op.CR)) when (o) {
            Op.CL, Op.ML -> m[o]?.sortedBy { it.y.path }
            Op.CR, Op.MR -> m[o]?.sortedBy { it.x.path }
            Op.DL        -> m[o]?.sortedByDescending { it.y.path }
            Op.DR        -> m[o]?.sortedByDescending { it.x.path }
            else         -> m[o]
        }?.forEach {
//            Thread.sleep(1000) //debug slowdown
            while (task.paused()) Thread.sleep(SLEEP)
            if (task.canceled()) break@loop
            task.done.incrementAndGet()
            operate(it, o)
        }
        for (t in finish) move(t.first, t.second, t.third)
        L.debug { "finish: ${finish.size}" }
        finish.clear()
        result = emptyList()
        if (task.started()) task.finish()
        if (task.finished()) compare(true) //save new state
    }

    private fun operate(l: L1, o: Op) {
        L.debug { "${l.x} ${l.y} ${l.c} $o" }
        when (o) {
            Op.CL -> copy(Path(rootR, l.y.path), Path(rootL, l.x.path))
            Op.CR -> copy(Path(rootL, l.x.path), Path(rootR, l.y.path))
            Op.ML -> move(Path(rootL, l.x.path), Path(rootL, l.y.path))
            Op.MR -> move(Path(rootR, l.y.path), Path(rootR, l.x.path))
            Op.DL -> delete(Path(rootL, l.x.path))
            Op.DR -> delete(Path(rootR, l.y.path))
            else  -> L.debug { "operate: skip" }
        }
    }

    private fun copy(source: Path, target: Path) {
        val exists = target.exists()
        val t = if (!exists) target
        else target.resolveSibling("${target.name}_${System.currentTimeMillis()}.$SUFFIX")
        if (exists && source.isDirectory()) return
        try {
            L.info { "copy $source to $t" }
            t.createParentDirectories()
            if (source.fileSize() < copyThreshold)
                source.copyTo(t, *copyOptions)
            else
                source.copyTo(t, copy)
        } catch (e: Exception) {
            L.error { "copy: ${e.message}" }
        }
        if (exists) move(t, target, true)
    }

    private fun move(source: Path, target: Path, overwrite: Boolean = false) {
        var t = target
        if (!overwrite && target.exists()) {
            t = target.resolveSibling("${target.name}_${System.currentTimeMillis()}.$SUFFIX")
            finish.add(Triple(t, target, false))
        }
        try {
            L.info { "move $source to $t" }
            t.createParentDirectories()
            source.moveTo(t, overwrite)
        } catch (e: Exception) {
            L.error { "move: ${e.message}" }
        }
    }

    private fun delete(source: Path) {
        try {
            L.info { "delete $source" }
            source.deleteExisting() //TODO backup?
        } catch (e: Exception) {
            L.error { "delete: ${e.message}" }
        }
    }
}

fun hash(s: String) = MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).toHexString()

fun Path.copyTo(target: Path, task: Task, bufferKiB: Int = 64) {
    task.start(true)
    task.goal.set(fileSize())
    inputStream().use { input ->
        target.outputStream().use { output ->
            val buffer = ByteArray(bufferKiB * 1024)
            var check = 0
            while (check-- > 0 || task.enabled()) { //do not check task on every loop
                if (check < 0) check = 64
                while (task.paused()) Thread.sleep(Sync.SLEEP)
                val bytes = input.read(buffer)
                if (bytes >= 0) {
                    output.write(buffer, 0, bytes)
                    task.done.addAndGet(bytes.toLong())
                } else task.finish()
//                Thread.sleep(10) //debug slowdown
            }
        }
    }
    if (task.finished()) target.setLastModifiedTime(getLastModifiedTime()) //after stream.use!
    else target.deleteExisting()
}
