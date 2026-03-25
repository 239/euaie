package euaie

import com.varabyte.truthish.*
import kotlin.test.Test

class Tests {
    @Test
    fun cutC() {
        assertThat(cutC("", +1)).isEmpty()
        assertThat(cutC("", +0)).isEmpty()
        assertThat(cutC("", -1)).isEmpty()
        assertThat(cutC("a", +1)).isEqualTo("a")
        assertThat(cutC("a", +0)).isEqualTo("")
        assertThat(cutC("a", -1)).isEqualTo("a")
        assertThat(cutC("ab", +1)).isEqualTo("…")
        assertThat(cutC("ab", +0)).isEqualTo("")
        assertThat(cutC("ab", -1)).isEqualTo("…")
        val s = "a:b：c？"
        assertThat(cutC(s, +9)).isEqualTo("a:b：c？")
        assertThat(cutC(s, +8)).isEqualTo("a:b：c？")
        assertThat(cutC(s, +7)).isEqualTo("a:b：c…")
        assertThat(cutC(s, +6)).isEqualTo("a:b：…")
        assertThat(cutC(s, +5)).isEqualTo("a:b…")
        assertThat(cutC(s, +4)).isEqualTo("a:b…")
        assertThat(cutC(s, +3)).isEqualTo("a:…")
        assertThat(cutC(s, +2)).isEqualTo("a…")
        assertThat(cutC(s, +1)).isEqualTo("…")
        assertThat(cutC(s, +0)).isEqualTo("")
        assertThat(cutC(s, -1)).isEqualTo("…")
        assertThat(cutC(s, -2)).isEqualTo("…")
        assertThat(cutC(s, -3)).isEqualTo("…？")
        assertThat(cutC(s, -4)).isEqualTo("…c？")
        assertThat(cutC(s, -5)).isEqualTo("…c？")
        assertThat(cutC(s, -6)).isEqualTo("…：c？")
        assertThat(cutC(s, -7)).isEqualTo("…b：c？")
        assertThat(cutC(s, -8)).isEqualTo("a:b：c？")
        assertThat(cutC(s, -9)).isEqualTo("a:b：c？")
    }

    @Test
    fun `cutC with emoji`() {
        val e = "\uD83C\uDDFA\uD83C\uDDE6"
        val s = "123${e}456"
        assertThat(cutC(s, +9)).isEqualTo("123${e}4…")
        assertThat(cutC(s, -9)).isEqualTo("…3${e}456")
    }
}
