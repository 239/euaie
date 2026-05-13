package euaie

import com.varabyte.truthish.*
import kotlin.test.Test

class TestScan {
    val root = "src/test/resources/scan"

    @Test
    fun `including and excluding`() {
        Scan.optionInsensitive = true
        Scan(root, setOf("iNcLuDe::C"), setOf("::.eXc"), "", Task()).scan().also {
            assertThat(it.keys).containsExactly(setOf("included/EmPtY.iNc"))
        }
        Scan(root, setOf("o/n/l/y/::this"), setOf(":not:"), "", Task()).scan().also {
            assertThat(it.keys).containsExactly(setOf("O/n/L/y/this"))
        }
        Scan(root, setOf("inc-0::.inc"), setOf(":not"), "", Task()).scan().also {
            assertThat(it.keys).containsExactly(setOf("inc-0/okay.inc"))
        }
    }
}
