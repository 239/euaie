package euaie

class L1(val x: L0, val y: L0, val c: Ch) {
    companion object {
        val fake = L1(L0.fake, L0.fake, Ch.U)
    }
}

fun link(mx: M0, my: M0): List<L1> = buildList {
    val x = mx.toMutableMap()
    val y = my.toMutableMap()
    addAll(andRemove(unchanged(x, y), x, y))
    addAll(andRemove(moved(x, y), x, y))
    addAll(andRemove(changed(x, y), x, y))
    addAll(andRemove(other(x, y), x, y))
}

private fun andRemove(xy: List<L1>, x: MM0, y: MM0) = xy.apply {
    forEach {
        x.remove(it.x.path)
        y.remove(it.y.path)
    }
}

private fun unchanged(mx: M0, my: M0): List<L1> = buildList {
    mx.values.forEach { x ->
        my[x.path]?.let { y ->
            if (!x.file && !y.file || x.e(y)) add(L1(x, y, Ch.U))
        }
    }
}

private fun moved(mx: M0, my: M0): List<L1> = buildList {
    val gx = mx.values.groupBy { it.size + if (L0.tolerance > 0) 0 else it.time } //ignore vague time
    val gy = my.values.groupBy { it.size + if (L0.tolerance > 0) 0 else it.time }
    for (lx in gx.values) for (x in lx) if (x.file) { //TODO files only?
        lx.singleOrNull { it.es(x) && it.et(x) }?.run {
            gy[x.size + if (L0.tolerance > 0) 0 else x.time]
                ?.singleOrNull { it.es(x) && it.et(x) }
                ?.let { add(L1(x, it, Ch.M)) }
        }
    }
}

private fun changed(mx: M0, my: M0): List<L1> = buildList {
    mx.values.forEach { x ->
        my[x.path]?.let { y ->
            if (x.file && y.file && !(x.es(y) && x.et(y)))
                add(L1(x, y, Ch.C))
        }
    }
}

private fun other(mx: M0, my: M0): List<L1> = buildList {
    val ox = mx.containsKey("") // loaded from file
    val oy = my.containsKey("")
    addAll(mx.minus("").values.map {
        L1(it, L0(it.path, 0, -it.time), if (ox) Ch.R else Ch.A)
    })
    addAll(my.minus("").values.map {
        L1(L0(it.path, 0, -it.time), it, if (oy) Ch.R else Ch.A)
    })
}
