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
        val original = L0(path, 123456L, 1622548800000L)
        val serialized = original.toLine()
        val deserialized = L0.fromLine(serialized)
        assertThat(deserialized).isEqualTo(original)
        assertThat(deserialized?.path).isEqualTo(path)
        assertThat(deserialized?.size).isEqualTo(123456L)
        assertThat(deserialized?.time).isEqualTo(1622548800000L)
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
