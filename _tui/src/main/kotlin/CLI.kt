package euaie

import java.util.*
import java.util.concurrent.*
import picocli.CommandLine.*

private const val keyVersion = "euaie.version"
val versionFull = Properties().run {
    load(object {}.javaClass.classLoader.getResourceAsStream("git.properties"))
    val b = getProperty("git.branch").orEmpty()
    val i = getProperty("git.commit.id.abbrev").orEmpty()
    val t = getProperty("git.commit.time").orEmpty()
    val c = getProperty("git.total.commit.count").orEmpty()
    "0.$t.$c-$i-$b"
}
val version = versionFull.substringBefore('-')

fun main(arguments: Array<String>) {
    System.setProperty(keyVersion, versionFull)
    picocli.CommandLine(CLI())
        .setCaseInsensitiveEnumValuesAllowed(true)
        .setUseSimplifiedAtFiles(true)
        .execute(*arguments)
}

@Command(
    name = "euaie",
    description = ["simple file synchronization"],
    version = [$$"${sys:$$keyVersion}"],
    mixinStandardHelpOptions = true,
    showAtFileInUsageHelp = true
)
class CLI : Callable<Int> {
    @Parameters(index = "0")
    lateinit var rootL: String

    @Parameters(index = "1")
    lateinit var rootR: String

    @Option(names = ["-e", "--exclude"], arity = "*")
    var exclude: Set<String> = emptySet()

    @Option(names = ["-i", "--include"], arity = "*")
    var include: Set<String> = emptySet()

    @Option(names = ["-t", "--tolerance"],
        description = ["allowed time difference (ms)"])
    var tolerance: Long = L0.tolerance

    @Option(names = ["-x", "--exit-when-done"],
        description = ["exit when both sides are equal"])
    var exit: Boolean = optionExitWhenDone

    @Option(names = ["-I", "--ignore-filter-case"],
        description = ["case insensitive filters"])
    var ignore: Boolean = optionIgnoreFilterCase

    @Option(names = ["-s", "--symlinks"],
        description = ["policy for symbolic links", $$"${COMPLETION-CANDIDATES}"])
    var symlinks: OptionSymbolicLink = optionSymbolicLink

    @Option(names = ["-c", "--copy-threshold"],
        description = ["interruptable copy threshold (MiB)"])
    var threshold: Int = optionCopyThreshold

    override fun call(): Int {
        L0.tolerance = tolerance.coerceAtLeast(0L)
        optionExitWhenDone = exit
        optionIgnoreFilterCase = ignore
        optionSymbolicLink = symlinks
        optionCopyThreshold = threshold.coerceAtLeast(0)
        runTUI(rootL, rootR, include, exclude)
        return 0
    }
}
