package euaie

const val NAME = "euaie"

enum class Ch(val icon: Char, val text: String) {
    A('+', "added"),
    C('*', "changed"),
    M('/', "moved"),
    R('-', "removed"),
    U('=', "unchanged");
}

enum class Di(val icon: Char, val text: String) {
    L('<', "to the left"),
    N(':', "neutral"),
    R('>', "to the right"),
    U('?', "unclear");
}

enum class Op(val icons: Pair<Char, Char>, val text: String) {
    CL(Ch.A.icon to Di.L.icon, "copy to the left"),
    CR(Ch.A.icon to Di.R.icon, "copy to the right"),
    DL(Ch.R.icon to Di.L.icon, "delete on the left"),
    DR(Ch.R.icon to Di.R.icon, "delete on the right"),
    ML(Ch.M.icon to Di.L.icon, "move on the left"),
    MR(Ch.M.icon to Di.R.icon, "move on the right"),
    NO(Ch.U.icon to Di.N.icon, "skip");

    fun icons() = "${icons.first}${icons.second}"
}

enum class OptionSymbolicLink { FOLLOW, IGNORE, PRESERVE }
