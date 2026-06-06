package euaie

import com.varabyte.truthish.*
import kotlin.io.path.*
import kotlin.test.Test

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
