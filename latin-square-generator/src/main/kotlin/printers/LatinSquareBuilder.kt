package printers

import expressions.And
import expressions.CNF
import expressions.Variable
import expressions.and
import utils.initMatrix
import utils.latin
import utils.orthogonalSquare

class LatinSquareBuilder(val n: Int, val k: Int, val q: Int) : CnfBuilder {
    override fun cnf(): CNF {
        val matrixes = (0 until k).map {
            initMatrix(n)
        }
        val lines = matrixes.map { latin(it) }
        val ortho = (0 until k).flatMap { i ->
            (i + 1 until k).map { j ->
                orthogonalSquare(matrixes[i], matrixes[j], q)
            }
        }
        val core = List(n * n * n * k) { Variable("${it + 1}") }
        val meta = List(n * n * k) { i -> List(n) { Variable("${i * n + it + 1}") } }
        return CNF((and(lines) and and(ortho)) as And, core, meta)
    }
}