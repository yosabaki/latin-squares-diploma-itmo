package printers

import expressions.And
import expressions.CNF
import expressions.Variable
import expressions.and
import utils.*

class OrthogonalArrayEncoder(reduced: Boolean, val n: Int, val s: Int, val q: Int) : CnfEncoder {
    val array = if (reduced) {
        initReducedArray(s, n * n, n)
    } else {
        initMatrix(s, n * n, n)
    }
    val coreSize = varCounter

    override fun cnf(): CNF {
        val array = initMatrix(s, n * n, n)
        val lines = array.map { codeSufficient(it) }
        val firstPair = (0..1).flatMap { i ->
            (i + 1 until s).map { j ->
                orthogonal(array[i], array[j], n * n)
            }
        }
        val secondPair = (2 until s).flatMap { i ->
            (i + 1 until s).map { j ->
                orthogonal(array[i], array[j], q)
            }
        }
        val core = List(coreSize) { Variable("${it + 1}") }
        val meta = List(n * n * s) { i -> List(n) { Variable("${i * n + it + 1}") } }
        return CNF(and(and(lines), and(firstPair), and(secondPair)) as And, core, meta)
    }
}

class OrthogonalArrayEncoderBuilder(private val reduced: Boolean) : LatinCnfEncoderBuilder {
    override fun invoke(n: Int, k: Int, q: Int) = OrthogonalArrayEncoder(reduced, n, k + 2, q)
}