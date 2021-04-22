package printers

import expressions.And
import expressions.CNF
import expressions.and
import parsers.Parser
import utils.*
import java.io.PrintWriter

class ReadyLatinSquarePrinter(
    val parser: Parser,
    val n: Int,
    val ratios: IntArray,
    val k: Int,
    val q: Int,
) : CnfBuilder {
    override fun cnf(): CNF {
        val matrixes = (0 until k).map { initMatrix(n) }
        val lines = matrixes.map { latin(it) }
        val ortho = (0 until k).flatMap { i ->
            (i + 1 until k).map { j ->
                if (j == 1) {
                    And()
                } else {
                    orthogonalSquare(matrixes[i], matrixes[j], q)
                }
            }
        }
        val parsedMatrixes = (parser.parse() as List<List<List<Int>>>)
        val codeRandom = ratios.indices.map { i ->
            codeRandom(matrixes[i], parsedMatrixes[i], ratios[i])
        }
        return CNF(and(and(lines), and(ortho), and(codeRandom)) as And)
    }
}