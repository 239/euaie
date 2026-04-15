package euaie

import picocli.CommandLine.*

private const val KEY = "$NAME.version"
val version = java.util.Properties().run {
    load(object {}.javaClass.classLoader.getResourceAsStream("git.properties"))
    val b = getProperty("git.branch").orEmpty()
    val i = getProperty("git.commit.id.abbrev").orEmpty()
    val t = getProperty("git.commit.time").orEmpty()
    if (b.matches("""\d+\.\d+\.\d+""".toRegex())) "$b-$i" else "$t-$i-$b"
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

    //0
    @Option(names = ["-e", "--exclude"], arity = "*", paramLabel = "<s:c:e>",
        description = ["filter syntax: '<starts>:<contains>:<ends>'"])
    var exclude: Set<String> = emptySet()

    @Option(names = ["-i", "--include"], arity = "*", paramLabel = "<s:c:e>",
        description = ["filter syntax: '<starts>:<contains>:<ends>'"])
    var include: Set<String> = emptySet()

    //1
    @Option(names = ["-r", "--retain"],
        description = [$$"keep old files in <root>/.$$NAME/ (${DEFAULT-VALUE})"])
    var retain: Boolean = Sync.optionRetain

    @Option(names = ["-s", "--symlinks"], paramLabel = "<policy>",
        description = [$$"set policy for symbolic links (${DEFAULT-VALUE})",
            $$"select from: ${COMPLETION-CANDIDATES}"])
    var symlinks: OptionSymbolicLink = Scan.optionSymbolicLink

    @Option(names = ["-t", "--tolerance"], paramLabel = "<ms>",
        description = [$$"set allowed time difference (${DEFAULT-VALUE})"])
    var tolerance: Long = L0.tolerance

    @Option(names = ["-x", "--exit"],
        description = [$$"exit when both sides are equal (${DEFAULT-VALUE})"])
    var exit: Boolean = TUI.optionExitWhenDone

    //2
    @Option(names = ["-C", "--copy-threshold"], paramLabel = "<MiB>",
        description = [$$"set threshold for interruptable copy (${DEFAULT-VALUE})"])
    var threshold: Int = Sync.optionCopyThreshold

    @Option(names = ["-I", "--insensitive"],
        description = [$$"use case insensitive filters (${DEFAULT-VALUE})"])
    var insensitive: Boolean = Scan.optionInsensitive

    @Option(names = ["-S", "--stateless"],
        description = [$$"ignore previous state (${DEFAULT-VALUE})"])
    var stateless: Boolean = Sync.optionStateless

    @Option(names = ["-V", "--version"], versionHelp = true,
        description = ["print version and exit"])
    var version: Boolean = false

    override fun call(): Int {
        L0.tolerance = tolerance.coerceAtLeast(0L)
        Scan.optionInsensitive = insensitive
        Scan.optionSymbolicLink = symlinks
        Sync.optionCopyThreshold = threshold.coerceAtLeast(0)
        Sync.optionRetain = retain
        Sync.optionStateless = stateless
        TUI.optionExitWhenDone = exit
        start(rootL, rootR, include, exclude)
        return if (version) 1 else 0 //avoiding 'never used' warning
    }
}

fun main(arguments: Array<String>) {
    System.setProperty(KEY, version)
    picocli.CommandLine(CLI())
        .setCaseInsensitiveEnumValuesAllowed(true)
        .setUsageHelpLongOptionsMaxWidth(30)
        .setUseSimplifiedAtFiles(true)
        .execute(*arguments)
}
