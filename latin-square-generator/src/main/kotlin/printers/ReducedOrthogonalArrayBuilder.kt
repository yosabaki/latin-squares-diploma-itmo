package printers

import expressions.And
import expressions.CNF
import expressions.Variable
import expressions.and
import utils.codeSufficient
import utils.initReducedArray
import utils.orthogonal
import utils.varCounter

class ReducedOrthogonalArrayBuilder(val n: Int, val s: Int, val q: Int) : CnfBuilder {
    override fun cnf(): CNF {
        val array = initReducedArray(s, n * n, n)
        val size = varCounter
        val lines = array.map { codeSufficient(it) }
        val firstPair = (0..1).flatMap { i ->
            (2 until s).map { j ->
                orthogonal(array[i], array[j], n * n)
            }
        }
        val secondPair = (2 until s).flatMap { i ->
            (i + 1 until s).map { j ->
                orthogonal(array[i], array[j], q)
            }
        }
        val core = List(size) { Variable("${it + 1}") }
        return CNF(and(and(lines), and(firstPair), and(secondPair)) as And, core)
    }
}