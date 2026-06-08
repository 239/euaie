package euaie

import com.varabyte.truthish.*
import kotlin.io.path.*
import kotlin.test.Test

class TestL0 {
    val path = "some/path/to/test/file"

    @Test
    fun `time tolerance`() {
        val a = L0(path, 100, 10000)
        val b = L0(path, 100, 11500)
        L0.tolerance = 0
        assertThat(a.et(b)).isFalse()
        L0.tolerance = 2000
        assertThat(a.et(b)).isTrue()
        val c = L0(path, 100, 12000)
        assertThat(a.et(c)).isTrue()
        val d = L0(path, 100, 12001)
        assertThat(a.et(d)).isFalse()
        L0.tolerance = 0
    }

    @Test
    fun `time shifts`() {
        val a = L0(path, 100, 10000)
        val b = L0(path, 100, 10000 + 3600000L)
        L0.tolerance = 1
        assertThat(a.et(b)).isTrue()
        val c = L0(path, 100, 10000 + 7200000L)
        assertThat(a.et(c)).isTrue()
        val d = L0(path, 100, 10000 + 10800000L)
        assertThat(a.et(d)).isFalse()
        L0.shifts = setOf(10800000L)
        assertThat(a.et(d)).isTrue()
        assertThat(a.et(b)).isFalse()
        L0.tolerance = 0
        L0.shifts = setOf(3600000L, 7200000L)
    }

    @Test
    fun `string serialization`() {
        val size = 123456L
        val time = 1622548800000L
        val original = L0(path, size, time)
        L0.fromLine(original.toLine())?.also {
            assertThat(it).isEqualTo(original)
            assertThat(it.path).isEqualTo(path)
            assertThat(it.size).isEqualTo(size)
            assertThat(it.time).isEqualTo(time)
        }
    }
}

class TestL1 {
    val path = "some/path/to/test/file"

    @Test
    fun `was moved`() {
        val old = "old/path/to/test/file"
        val new = "new/path/to/test/file"
        val mx = mapOf(old to L0(old, 100, 1000))
        val my = mapOf(new to L0(new, 100, 1000))
        val links = link(mx, my)
        assertThat(links.size).isEqualTo(1)
        assertThat(links[0].c).isEqualTo(Ch.M)
        assertThat(links[0].x.path).isEqualTo(old)
        assertThat(links[0].y.path).isEqualTo(new)
    }

    @Test
    fun `was changed`() {
        val mx = mapOf(path to L0(path, 100, 1000))
        val my = mapOf(path to L0(path, 200, 1000))
        link(mx, my).apply {
            assertThat(size).isEqualTo(1)
            assertThat(last().c).isEqualTo(Ch.C)
        }
    }

    @Test
    fun `was added or removed`() {
        val dummy = L0(path, 100, 1000)
        val marker = L0("", 0, 0)
        link(mapOf(path to dummy), emptyMap()).apply {
            assertThat(size).isEqualTo(1)
            assertThat(last().c).isEqualTo(Ch.A)
        }
        link(mapOf(path to dummy, "" to marker), emptyMap()).apply {
            assertThat(size).isEqualTo(1)
            assertThat(last().c).isEqualTo(Ch.R)
        }
    }
}

class TestScan {
    val root = "src/test/resources/scan"
    val void = emptySet<String>()
    val task = Task()

    @Test
    fun `including and excluding`() {
        Scan.optionInsensitive = true
        Scan(root, setOf("iNcLuDe::C"), setOf("::.eXc"), "", task).scan().also {
            assertThat(it.keys).containsExactly(setOf("included/EmPtY.iNc"))
        }
        Scan(root, setOf("o/n/l/y/::this"), setOf(":not:"), "", task).scan().also {
            assertThat(it.keys).containsExactly(setOf("O/n/L/y/this"))
        }
        Scan(root, setOf("inc-0::.inc"), setOf(":not"), "", task).scan().also {
            assertThat(it.keys).containsExactly(setOf("inc-0/okay.inc"))
        }
    }

    @Test
    fun `case sensitivity`() {
        Scan.optionInsensitive = true
        Scan(root, setOf("included::.inc"), void, "", task).scan().also {
            assertThat(it.keys).containsExactly(setOf("included/EmPtY.iNc"))
        }
        Scan.optionInsensitive = false
        Scan(root, setOf("included::.inc"), void, "", task).scan().also {
            assertThat(it.keys).isEmpty()
        }
        Scan(root, setOf("included::.iNc"), void, "", task).scan().also {
            assertThat(it.keys).containsExactly(setOf("included/EmPtY.iNc"))
        }
    }

    @Test
    fun `symlink options`() {
        createTempDirectory("test-").run {
            try {
                val link = "link.txt"
                val target = "target.txt"
                val path = resolve(target).also { it.writeText(target) }
                val root = toString()
                resolve(link).createSymbolicLinkPointingTo(path)
                Scan.optionSymbolicLink = OptionSymbolicLink.IGNORE
                Scan(root, void, void, "", task).scan().also {
                    assertThat(it.keys).containsExactly(setOf(target))
                }
                Scan.optionSymbolicLink = OptionSymbolicLink.PRESERVE
                Scan(root, void, void, "", task).scan().also {
                    assertThat(it.keys).containsExactly(setOf(target, link))
                    assertThat(it[link]?.time).isEqualTo(L0.LINK)
                }
                Scan.optionSymbolicLink = OptionSymbolicLink.FOLLOW
                Scan(root, void, void, "", task).scan().also {
                    assertThat(it.keys).containsExactly(setOf(target, link))
                    assertThat(it[link]?.time).isNotEqualTo(L0.LINK)
                    assertThat(it[link]?.size).isEqualTo(path.fileSize())
                }
            } finally {
                toFile().deleteRecursively()
            }
        }
    }
}
