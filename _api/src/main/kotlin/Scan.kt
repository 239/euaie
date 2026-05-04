package euaie

import java.io.*
import java.nio.file.*
import kotlin.io.path.*
import org.tinylog.kotlin.Logger as L

class Scan(val root: String, include: Set<String>, exclude: Set<String>, hash: String, val task: Task) {
    val base = Path(root)
    val state = Path(statePath(NAME).pathString, hash)
    private val result = mutableMapOf<String, L0>()
    private val including = parse(include)
    private val excluding = parse(exclude.plus(".$NAME"))
    private var included = 0L
    private var excluded = 0L
    private fun parse(s: Set<String>) = s.asSequence().filter { it.isNotBlank() }
        .map { it.replace(File.separatorChar, '/') }.sorted().map { it.split(D) }
        .map { Triple(it.getOrElse(0) { "" }, it.getOrElse(1) { "" }, it.getOrElse(2) { "" }) }.toSet()

    companion object {
        const val D = ':'
        private val slash = File.separatorChar == '/'
        private val sensitive = Path("a") != Path("A")
        var optionInsensitive = !sensitive //use system default
        var optionSymbolicLink = OptionSymbolicLink.PRESERVE
    }

    fun scan(save: Boolean = false): M0 {
        L.info { "-----------------------scan" }
        L.debug { "scan: $base" }
        L.debug { "filesys: $slash / $sensitive" }
        L.debug { "symlinks: $optionSymbolicLink" }
        if (root.isBlank() || !base.isDirectory()) {
            L.error { "invalid root path '$root'" }
            return emptyMap()
        }
        included = 0
        excluded = 0
        result.clear()
        base.visitFileTree(visitor, followLinks = optionSymbolicLink == OptionSymbolicLink.FOLLOW)
        L.info { "$base (included: $included excluded: $excluded)" }
        if (save) save()
        return result
    }

    fun load(stateless: Boolean = false): M0 {
        val r = mutableMapOf<String, L0>()
        if (!stateless) try {
            state.forEachLine { L0.fromLine(it)?.apply { r[this.path] = this } }
            L.debug { "loaded $state" }
        } catch (e: Exception) {
            L.warn { "failed to load previous state: ${e.message}" }
        }
        r[""] = L0("", 0, 0) //mark loaded (previous state)
        return r
    }

    private fun save() { //TODO java.util.zip.*?
        try {
            state.createParentDirectories()
            state.writeLines(result.values.map { it.toLine() })
            L.debug { "saved $state" }
        } catch (e: Exception) {
            L.error { "save: ${e.message}" }
        }
    }

    private fun match(p: String): Boolean {
        var r = including.isEmpty()
        r = r || including.any { match(p, it, optionInsensitive) }
        r = r && excluding.all { !match(p, it, optionInsensitive) }
        if (r) included++ else excluded++
        L.trace { "${if (r) '+' else '-'} $p" }
        return r
    }

    private fun match(s: String, t: Triple<String, String, String>, ignore: Boolean): Boolean =
        s.startsWith(t.first, ignore) && s.contains(t.second, ignore) && s.endsWith(t.third, ignore)

    private val visitor = fileVisitor {
        onPreVisitDirectory { p, a ->
            val r = p.relativeTo(base).toString() + '/'
            val path = if (slash) r else r.replace(File.separatorChar, '/')
            if (path == "/") FileVisitResult.CONTINUE //p == base
            else if (match(path)) {
                val size = a.size().let { if (it > 0) -it else -1 }
                val time = a.lastModifiedTime().toMillis()
                L.info { "•$path" }
                result[path] = L0(path, size, time)
                task.done.incrementAndGet()
                FileVisitResult.CONTINUE
            } else if (
                including.any { it.first.startsWith(path, optionInsensitive) } ||
                including.any { path.startsWith(it.first, optionInsensitive) }
            ) FileVisitResult.CONTINUE else FileVisitResult.SKIP_SUBTREE
        }
        onPostVisitDirectory { _, e ->
            if (e != null)
                L.warn { "visit: ${e.message}" }
            FileVisitResult.CONTINUE
        }
        onVisitFile { p, a ->
//            Thread.sleep(100) //debug slowdown
            val r = p.relativeTo(base).toString()
            val path = if (slash) r else r.replace(File.separatorChar, '/')
            if (match(path)) {
                val size = a.size()
                val time = if (a.isSymbolicLink) L0.LINK else a.lastModifiedTime().toMillis()
                L.info { " $path" }
                if (optionSymbolicLink == OptionSymbolicLink.IGNORE && a.isSymbolicLink)
                    L.debug { "skipping symbolic link: $path" }
                else result[path] = L0(path, size, time)
                task.done.incrementAndGet()
            }
            if (task.canceled()) FileVisitResult.TERMINATE else FileVisitResult.CONTINUE
        }
        onVisitFileFailed { p, e ->
            val r = p.relativeTo(base).toString() + if (p.isDirectory()) '/' else ""
            if (match(if (slash) r else r.replace(File.separatorChar, '/')))
                L.warn { "visit: ${e.message}" }
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
