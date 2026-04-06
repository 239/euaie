package euaie

const val NAME = "euaie"

enum class Ch(val icon: Char, val text: String) {
    A('+', "added"),
    C('*', "changed"),
    M('~', "moved"),
    R('-', "removed"),
    U('=', "unchanged");

    fun a() = this == A
    fun c() = this == C
    fun m() = this == M
    fun r() = this == R
    fun u() = this == U
}

enum class Di(val icon: Char) {
    L('<'),
    N('/'),
    R('>'),
    U('?');
}

enum class Op(val icons: Pair<Char, Char>, val text: String) {
    CL('+' to '<', "copy to the left"),
    CR('+' to '>', "copy to the right"),
    DL('-' to '<', "delete on the left"),
    DR('-' to '>', "delete on the right"),
    ML('~' to '<', "move on the left"),
    MR('~' to '>', "move on the right"),
    NO('=' to '/', "skip");

    override fun toString() = "${icons.first}${icons.second}"
}

enum class OptionSymbolicLink { FOLLOW, IGNORE, PRESERVE }
