package euaie

import com.varabyte.kotter.runtime.terminal.TerminalSize
import com.varabyte.kotter.terminal.virtual.VirtualTerminal
import com.varabyte.truthish.*
import kotlin.test.Test

class Tests {
    @Test
    fun run() {
        TUI.terminal = VirtualTerminal.create("Test", TerminalSize(60, 30), hideVerticalScrollbar = true)
        picocli.CommandLine(CLI())
            .setCaseInsensitiveEnumValuesAllowed(true)
            .setUseSimplifiedAtFiles(true)
            .execute(*arrayOf("@src/test/args"))
    }

    @Test
    fun cutC() {
        assertThat(cutW("", +1)).isEmpty()
        assertThat(cutW("", +0)).isEmpty()
        assertThat(cutW("", -1)).isEmpty()
        assertThat(cutW("a", +1)).isEqualTo("a")
        assertThat(cutW("a", +0)).isEqualTo("")
        assertThat(cutW("a", -1)).isEqualTo("a")
        assertThat(cutW("ab", +1)).isEqualTo("…")
        assertThat(cutW("ab", +0)).isEqualTo("")
        assertThat(cutW("ab", -1)).isEqualTo("…")
        val s = "a:b：c？"
        assertThat(cutW(s, +9)).isEqualTo("a:b：c？")
        assertThat(cutW(s, +8)).isEqualTo("a:b：c？")
        assertThat(cutW(s, +7)).isEqualTo("a:b：c…")
        assertThat(cutW(s, +6)).isEqualTo("a:b：…")
        assertThat(cutW(s, +5)).isEqualTo("a:b…")
        assertThat(cutW(s, +4)).isEqualTo("a:b…")
        assertThat(cutW(s, +3)).isEqualTo("a:…")
        assertThat(cutW(s, +2)).isEqualTo("a…")
        assertThat(cutW(s, +1)).isEqualTo("…")
        assertThat(cutW(s, +0)).isEqualTo("")
        assertThat(cutW(s, -1)).isEqualTo("…")
        assertThat(cutW(s, -2)).isEqualTo("…")
        assertThat(cutW(s, -3)).isEqualTo("…？")
        assertThat(cutW(s, -4)).isEqualTo("…c？")
        assertThat(cutW(s, -5)).isEqualTo("…c？")
        assertThat(cutW(s, -6)).isEqualTo("…：c？")
        assertThat(cutW(s, -7)).isEqualTo("…b：c？")
        assertThat(cutW(s, -8)).isEqualTo("a:b：c？")
        assertThat(cutW(s, -9)).isEqualTo("a:b：c？")
    }

    @Test
    fun `cutC with emoji`() {
        val e = "\uD83C\uDDFA\uD83C\uDDE6"
        val s = "abc${e}def"
        assertThat(cutW(s, +7)).isEqualTo("abc${e}d…")
        assertThat(cutW(s, -7)).isEqualTo("…c${e}def")
    }
}
