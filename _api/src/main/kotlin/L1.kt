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

private fun andRemove(xy: List<L1>, mx: MM0, my: MM0): List<L1> = xy.onEach {
    mx.remove(it.x.path)
    my.remove(it.y.path)
}

private fun unchanged(mx: M0, my: M0): List<L1> = buildList {
    mx.values.forEach { x ->
        my[x.path]?.let { y ->
            if (!x.file && !y.file || x.e(y)) add(L1(x, y, Ch.U))
        }
    }
}

private fun moved(mx: M0, my: M0): List<L1> = buildList {
    fun hash(l: L0) = (l.size * 1000003L) xor if (L0.tolerance > 0) 0L else l.time // ignore vague time
    val gx = mx.values.groupBy { hash(it) }
    val gy = my.values.groupBy { hash(it) }
    for ((hx, lx) in gx) for (x in lx) if (x.file) {
        lx.singleOrNull { it.es(x) && it.et(x) }?.run {
            gy[hx]?.singleOrNull { it.es(x) && it.et(x) }?.let { add(L1(x, it, Ch.M)) }
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
