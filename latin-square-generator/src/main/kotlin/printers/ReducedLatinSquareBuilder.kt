package printers

import expressions.*
import utils.*

class ReducedLatinSquareBuilder(
    private val n: Int,
    private val k: Int,
    private val q: Int,
) : CnfBuilder {
    override fun cnf(): CNF {
        val matrixes = listOf(initReducedMatrix(n)) + (1 until k).map { initMatrix(n) }
        val size = varCounter
        val lines = matrixes.map { latin(it) }
        val ortho = (0 until k).flatMap { i ->
            (i + 1 until k).map { j ->
                orthogonalSquare(matrixes[i], matrixes[j], q)
            }
        }
        val core = List(size) { Variable("${it + 1}") }
        return CNF(and(and(lines), and(ortho)) as And, core)
    }
}