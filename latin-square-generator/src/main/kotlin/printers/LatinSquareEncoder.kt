package printers

import expressions.And
import expressions.CNF
import expressions.Variable
import expressions.and
import utils.*

class LatinSquareEncoder(reduced: Boolean, val n: Int, val k: Int, val q: Int) : CnfEncoder {
    val matrixes = if (reduced) {
        listOf(initReducedMatrix(n)) + (1 until k).map {
            initMatrix(n)
        }
    } else {
        (0 until k).map {
            initMatrix(n)
        }
    }
    private val coreSize = varCounter

    override fun cnf(): CNF {
        val lines = matrixes.map { latin(it) }
        val ortho = (0 until k).flatMap { i ->
            (i + 1 until k).map { j ->
                orthogonalSquare(matrixes[i], matrixes[j], q)
            }
        }
        val core = List(coreSize) { Variable("${it + 1}") }
        val meta = List(n * n * k) { i -> List(n) { Variable("${i * n + it + 1}") } }
        return CNF((and(lines) and and(ortho)) as And, core, meta)
    }
}

class LatinSquareEncoderBuilder(private val reduced: Boolean) : LatinCnfEncoderBuilder {
    override fun invoke(n: Int, k: Int, q: Int) = LatinSquareEncoder(reduced, n, k, q)
}