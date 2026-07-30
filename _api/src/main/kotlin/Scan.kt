package euaie

import java.nio.file.*
import kotlin.io.path.*
import org.tinylog.kotlin.*

class Scan(root: String, include: Set<String>, exclude: Set<String>, hash: String, task: Task) {
    val base = Path(root).absolute()
    val state = Path(statePath(NAME).pathString, hash)
    private val result = mutableMapOf<String, L0>()
    private val including = parse(include)
    private val excluding = parse(exclude.plus(".$NAME"))
    private var included = 0L
    private var excluded = 0L
    private fun parse(s: Set<String>) = s.asSequence().filter { it.isNotBlank() }
        .map { it.replace(separator, S) }.sorted().map { it.split(D) }
        .map { Triple(it.getOrElse(0) { "" }, it.getOrElse(1) { "" }, it.getOrElse(2) { "" }) }.toSet()

    enum class OptionSymbolicLink { FOLLOW, IGNORE, PRESERVE }
    companion object {
        const val D = ':'
        const val S = '/'
        private val separator = java.io.File.separatorChar
        private val sensitive = Path("a") != Path("A")
        var optionInsensitive = !sensitive // use system default
        var optionSymbolicLink = OptionSymbolicLink.PRESERVE
    }

    fun scan(save: Boolean = false): M0 {
        Logger.debug { "-----------------------scan" }
        Logger.debug { "scan: $base" }
        Logger.debug { "symlinks: $optionSymbolicLink" }
        Logger.debug { "filesystem: '$separator' | $sensitive" }
        included = 0
        excluded = 0
        result.clear()
        base.visitFileTree(visitor, followLinks = optionSymbolicLink == OptionSymbolicLink.FOLLOW)
        Logger.debug { "$base (included: $included excluded: $excluded)" }
        if (save) save()
        return result
    }

    fun load(stateless: Boolean = false): M0 {
        val r = mutableMapOf<String, L0>()
        if (!stateless) try {
            state.forEachLine { L0.fromLine(it)?.apply { r[this.path] = this } }
            Logger.debug { "loaded $state" }
        } catch (e: Exception) {
            Logger.warn { "failed to load previous state: ${e.message}" }
        }
        r[""] = L0("", 0, 0) // mark loaded (previous state)
        return r
    }

    private fun save() { //TODO java.util.zip.*?
        try {
            state.createParentDirectories()
            state.writeLines(result.values.map { it.toLine() })
            Logger.debug { "saved $state" }
        } catch (e: Exception) {
            Logger.error { "save: ${e.message}" }
        }
    }

    private fun valid(p: String, f: Triple<String, String, String>, i: Boolean): Boolean =
        p.startsWith(f.first, i) && p.contains(f.second, i) && p.endsWith(f.third, i)

    private fun valid(p: String, i: Boolean = optionInsensitive): Boolean {
        var r = including.isEmpty()
        r = r || including.any { valid(p, it, i) }
        r = r && excluding.all { !valid(p, it, i) }
        if (r) included++ else excluded++
        Logger.trace { "${if (r) '+' else '-'} $p" }
        return r
    }

    private fun validParent(p: String, i: Boolean = optionInsensitive): Boolean =
        including.any { it.first.startsWith(p, i) || p.startsWith(it.first, i) }
                && excluding.all { !valid(p, it, i) }

    private val visitor = fileVisitor {
        onPreVisitDirectory { p, a ->
            val r = p.relativeTo(base).toString() + S
            val path = if (separator == S) r else r.replace(separator, S)
            if (path == "/") FileVisitResult.CONTINUE // p == base
            else if (valid(path)) {
                val size = a.size().let { if (it > 0) -it else -1 }
                val time = a.lastModifiedTime().toMillis()
                Logger.debug { "•$path" }
                result[path] = L0(path, size, time)
                task.done.incrementAndGet()
                FileVisitResult.CONTINUE
            } else if (validParent(path)) FileVisitResult.CONTINUE
            else FileVisitResult.SKIP_SUBTREE
        }
        onPostVisitDirectory { _, e ->
            if (e != null)
                Logger.warn { "visit: ${e.message}" }
            FileVisitResult.CONTINUE
        }
        onVisitFile { p, a ->
//            Thread.sleep(100) // debug slowdown
            val r = p.relativeTo(base).toString()
            val path = if (separator == S) r else r.replace(separator, S)
            if (valid(path)) {
                val size = a.size()
                val time = if (a.isSymbolicLink) L0.LINK else a.lastModifiedTime().toMillis()
                Logger.debug { " $path" }
                if (optionSymbolicLink == OptionSymbolicLink.IGNORE && a.isSymbolicLink)
                    Logger.debug { "skipping symbolic link: $path" }
                else result[path] = L0(path, size, time)
                task.done.incrementAndGet()
            }
            if (task.canceled()) FileVisitResult.TERMINATE else FileVisitResult.CONTINUE
        }
        onVisitFileFailed { p, e ->
            val r = p.relativeTo(base).toString() + if (p.isDirectory()) S else ""
            if (valid(if (separator == S) r else r.replace(separator, S)))
                Logger.warn { "visit: ${e.message}" }
            FileVisitResult.CONTINUE
        }
    }
}

fun statePath(name: String): Path = when {
    System.getProperty("os.name").startsWith("Windows", true) ->
        Path(System.getenv("LocalAppData"), name, "state")
    System.getProperty("os.name").startsWith("Mac", true)     ->
        Path(System.getProperty("user.home"), "Library", "Application Support", name, "state")
    else                                                      ->
        Path(System.getenv("XDG_STATE_HOME") ?: "${System.getProperty("user.home")}/.local/state", name)
}
