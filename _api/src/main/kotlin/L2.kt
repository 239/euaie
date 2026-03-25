package euaie

class L2(val bp: L1, val pq: L1, val qd: L1)

fun link(bp: List<L1>, pq: List<L1>, qd: List<L1>): List<L2> {
    val mbp = mutableMapOf<String, L1>()
    val mqd = mutableMapOf<String, L1>()
    val fake = L1(L0.fake, L0.fake, Ch.U)
    bp.forEach {
        mbp[it.x.path] = it
        mbp[it.y.path] = it
    }
    qd.forEach {
        mqd[it.y.path] = it
        mqd[it.x.path] = it
    }
    return pq.map {
        val ebp = mbp.getOrDefault(it.x.path, fake) //TODO linking to fake items?
        val eqd = mqd.getOrDefault(it.y.path, fake)
        var epq = it
        if (it.c.a() && ebp.c.r() && eqd.c.u()) epq = L1(it.x, it.y, Ch.R)
        if (it.c.a() && ebp.c.u() && eqd.c.r()) epq = L1(it.x, it.y, Ch.R)
        L2(ebp, epq, eqd)
    }
}
