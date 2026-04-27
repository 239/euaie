package euaie

import com.varabyte.truthish.*
import kotlin.test.Test

class TestScan {
    val root = "src/test/resources/scan"

    @Test
    fun `including and excluding`() {
        val s0 = Scan(root, emptySet(), emptySet(), "", Task()).scan()
        val s1 = Scan(root, setOf("include"), emptySet(), "", Task()).scan()
        val s2 = Scan(root, setOf("include"), setOf("::.exc"), "", Task()).scan()
        Scan.optionInsensitive = true
        val s3 = Scan(root, setOf("include"), setOf("::.exc"), "", Task()).scan()
        val s4 = Scan(root, setOf("include:tY"), setOf("::.exc"), "", Task()).scan()
        val s5 = Scan(root, setOf("iNcLuDe:tY:c"), setOf("::.eXc"), "", Task()).scan()
        assertThat(s0.size).isEqualTo(7)
        assertThat(s1.size).isEqualTo(6)
        assertThat(s2.size).isEqualTo(5)
        assertThat(s3.size).isEqualTo(4)
        assertThat(s4.size).isEqualTo(3)
        assertThat(s5.size).isEqualTo(2)
        assertThat(s5.keys).containsExactly(setOf("included/empty.inc", "included/EmPtY.iNc"))
    }
}
