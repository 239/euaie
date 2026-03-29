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
    "0.$t-$c-$i-$b"
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
    description = ["simple file synchronization"],
    mixinStandardHelpOptions = true, //TODO use custom options?
    name = "euaie",
    showAtFileInUsageHelp = true,
    sortOptions = false,
    version = [$$"${sys:$$keyVersion}"],
)
class CLI : Callable<Int> {
    @Parameters(index = "0")
    lateinit var rootL: String

    @Parameters(index = "1")
    lateinit var rootR: String

    //0
    @Option(names = ["-e", "--exclude"], arity = "*") //TODO add description!
    var exclude: Set<String> = emptySet()

    @Option(names = ["-i", "--include"], arity = "*")
    var include: Set<String> = emptySet()

    //1
    @Option(names = ["-s", "--symlinks"],
        description = ["set policy for symbolic links", $$"${COMPLETION-CANDIDATES}"])
    var symlinks: OptionSymbolicLink = Scan.optionSymbolicLink

    @Option(names = ["-t", "--tolerance"],
        description = ["set allowed time difference (ms)"])
    var tolerance: Long = L0.tolerance

    @Option(names = ["-x", "--exit-when-done"],
        description = ["exit when both sides are equal"])
    var exit: Boolean = optionExitWhenDone

    //2
    @Option(names = ["-C", "--copy-threshold"],
        description = ["set threshold for interruptable copy mode (MiB)"])
    var threshold: Int = optionCopyThreshold

    @Option(names = ["-I", "--ignore-filter-case"],
        description = ["use case insensitive filters"])
    var ignore: Boolean = Scan.optionIgnoreFilterCase

    @Option(names = ["-S", "--stateless"],
        description = ["ignore previous state"])
    var stateless: Boolean = optionStateless

    override fun call(): Int {
        Scan.optionSymbolicLink = symlinks
        L0.tolerance = tolerance.coerceAtLeast(0L)
        optionExitWhenDone = exit
        optionCopyThreshold = threshold.coerceAtLeast(0)
        Scan.optionIgnoreFilterCase = ignore
        optionStateless = stateless
        runTUI(rootL, rootR, include, exclude)
        return 0 //TODO errors?
    }
}
