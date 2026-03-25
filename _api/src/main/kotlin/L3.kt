package euaie

class L3(val l2: L2, val proposed: Di, var actual: Di = proposed)

fun wrap(ll2: List<L2>): List<L3> = ll2.map {
    L3(it, when (it.pq.c) {
        Ch.U -> Di.N
        else -> when {
            it.qd.c == it.pq.c && it.bp.c.u() -> Di.L
            it.bp.c == it.pq.c && it.qd.c.u() -> Di.R
            else                              -> Di.U
        }
    })
}

fun map(l3: L3): Op = when (l3.actual) {
    Di.L -> when (l3.l2.pq.c) {
        Ch.A, Ch.R -> if (l3.l2.pq.y.real) Op.CL else Op.DL
        Ch.C       -> Op.CL
        Ch.M       -> Op.ML
        else       -> Op.NO
    }
    Di.R -> when (l3.l2.pq.c) {
        Ch.A, Ch.R -> if (l3.l2.pq.x.real) Op.CR else Op.DR
        Ch.C       -> Op.CR
        Ch.M       -> Op.MR
        else       -> Op.NO
    }
    else -> Op.NO
}
