package euaie

import kotlin.io.path.*
import org.tinylog.kotlin.*
import picocli.CommandLine.*

private const val KEY = "$NAME.version"
val version = java.util.Properties().run {
    load(object {}.javaClass.classLoader.getResourceAsStream("git.properties"))
    val b = getProperty("git.branch").orEmpty()
    val i = getProperty("git.commit.id.abbrev").orEmpty()
    val t = getProperty("git.commit.time").orEmpty()
    if (b.matches("""\d+\.\d+\.\d+""".toRegex()))
        "$b-$i" else "$t-$i-$b"
}

@Command(
    description = ["simple file synchronization"],
    mixinStandardHelpOptions = false,
    name = NAME,
    showAtFileInUsageHelp = true,
    showDefaultValues = false,
    sortOptions = false,
    usageHelpAutoWidth = false,
    version = [$$"${sys:$$KEY}"]
)
class CLI : java.util.concurrent.Callable<Int> {
    @Parameters(index = "0")
    lateinit var rootL: String

    @Parameters(index = "1")
    lateinit var rootR: String

    //1
    @Option(names = ["-e", "--exclude"], arity = "*", paramLabel = "<s:c:e>",
        description = ["filter syntax: '<starts>:<contains>:<ends>'"])
    var exclude: Set<String> = emptySet()

    @Option(names = ["-i", "--include"], arity = "*", paramLabel = "<s:c:e>",
        description = ["filter syntax: '<starts>:<contains>:<ends>'"])
    var include: Set<String> = emptySet()

    @Option(names = ["-r", "--retain"],
        description = [$$"keep old files in <root>/.$$NAME/ (${DEFAULT-VALUE})"])
    var retain: Boolean = Sync.optionRetain

    @Option(names = ["-s", "--symlinks"], paramLabel = "<policy>",
        description = [$$"set policy for symbolic links (${DEFAULT-VALUE})",
            $$"policies: ${COMPLETION-CANDIDATES}"])
    var symlinks: OptionSymbolicLink = Scan.optionSymbolicLink

    @Option(names = ["-t", "--tolerance"], paramLabel = "<ms>",
        description = [$$"set tolerated time difference (${DEFAULT-VALUE})",
            "negative values: detect automatically"])
    var tolerance: Long = -1L //L0.tolerance

    //2
    @Option(names = ["-A", "--automatic"],
        description = ["run automatically ignoring unclear items"])
    var automatic: Boolean = false

    @Option(names = ["-C", "--copy-threshold"], paramLabel = "<MiB>",
        description = [$$"set threshold for interruptable copy (${DEFAULT-VALUE})"])
    var threshold: Int = Sync.optionCopyThreshold

    @Option(names = ["-I", "--insensitive"],
        description = [$$"use case insensitive filters (${DEFAULT-VALUE})"])
    var insensitive: Boolean = Scan.optionInsensitive

    @Option(names = ["-Q", "--quit"],
        description = [$$"exit when both sides are equal (${DEFAULT-VALUE})"])
    var quit: Boolean = TUI.optionQuitWhenDone

    @Option(names = ["-S", "--stateless"],
        description = [$$"ignore previous state (${DEFAULT-VALUE})"])
    var stateless: Boolean = Sync.optionStateless

    @Option(names = ["-V", "--version"], versionHelp = true,
        description = ["print version and exit"])
    var version: Boolean = false

    override fun call(): Int {
        if (tolerance < 0L) tolerance = runCatching {
            val typeL = Path(rootL).fileStore().type().uppercase()
            val typeR = Path(rootR).fileStore().type().uppercase()
            Logger.debug { "$typeL | $typeR" }
            if ("FAT" in "$typeL$typeR") 2000L else 0L
        }.getOrDefault(0L)
        L0.tolerance = tolerance
        Scan.optionInsensitive = insensitive
        Scan.optionSymbolicLink = symlinks
        Sync.optionCopyThreshold = threshold.coerceAtLeast(0)
        Sync.optionRetain = retain
        Sync.optionStateless = stateless
        TUI.optionQuitWhenDone = quit
        Sync(rootL, rootR, include, exclude).run {
            if (automatic) { //TODO improve!
                compare()
                result().groupBy { it.l2.pq.c }.entries.run {
                    println(joinToString("  ") { "${it.value.size}${it.key.icon}" })
                }
                execute()
                result().groupBy { it.actual }.entries.run {
                    println(joinToString("  ") { "${it.value.size}${it.key.icon}" })
                }
            } else start(this)
        }
        return if (version) 1 else 0 //avoiding 'never used' warning
    }
}

fun main(arguments: Array<String>) {
    if (arguments.size == 1) { //TODO --edit?
        val path = Path(arguments[0])
        if (path.isRegularFile()) arguments[0] = "@${arguments[0]}"
        if (path.isDirectory()) {
            val files = path.listDirectoryEntries().filter { it.isRegularFile() }.sorted()
            files.forEachIndexed { i, f -> println("${i + 1}: ${f.fileName}") }
            var index = -1
            while (index !in 0..files.size) {
                print("select by index (0 to cancel): ")
                index = readlnOrNull()?.toIntOrNull() ?: -1
            }
            files.getOrNull(index - 1)?.let { arguments[0] = "@${it}" }
            println(arguments[0])
        }
    }
    System.setProperty(KEY, version)
    picocli.CommandLine(CLI())
        .setCaseInsensitiveEnumValuesAllowed(true)
        .setUsageHelpLongOptionsMaxWidth(30)
        .setUseSimplifiedAtFiles(true)
        .execute(*arguments)
}
