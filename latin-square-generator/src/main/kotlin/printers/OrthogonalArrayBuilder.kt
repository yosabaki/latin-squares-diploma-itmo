package printers

import expressions.*
import utils.codeSufficient
import utils.initMatrix
import utils.orthogonal

class OrthogonalArrayBuilder(val n: Int, val s: Int, val q: Int) : CnfBuilder {
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
        val core = List(n * n * n * s) { Variable("${it + 1}") }
        val meta = List(n * n * s) { i -> List(n) { Variable("${i * n + it + 1}") } }
        return CNF(and(and(lines), and(firstPair), and(secondPair)) as And, core, meta)
    }
}