package printers

import expressions.And
import expressions.CNF
import expressions.Variable
import expressions.and
import utils.*

class ReducedLogLatinSquareBuilder(val n: Int, val k: Int, val q: Int) : CnfBuilder {
    override fun cnf(): CNF {
        val matrixes = listOf(initReducedLogMatrix(n, n, log2(n))) + (1 until k).map {
            initMatrix(n, n, log2(n))
        }
        val size = varCounter
        val ceitinVars = (0 until k).map {
            initMatrix(n, n, n)
        }
        val ceitinEqExprs = and((0 until k).map { t ->
            and((0 until n).map { i ->
                and((0 until n).map { j ->
                    and((0 until n).map { v ->
                        iff(ceitinVars[t][i][j][v], logEqual(matrixes[t][i][j], v))
                    })
                })
            })
        })
        val ceitinVarsForLines = ceitinVars.map { it.map { it.transpose() }.transpose() }
        val lines = ceitinVarsForLines.map { logLatin(it) }
        val ortho = (0 until k).flatMap { i ->
            (i + 1 until k).map { j ->
                orthogonalLogSquare(ceitinVars[i], ceitinVars[j], q)
            }
        }
        val core = List(size) { Variable("${it + 1}") }
        return CNF((and(lines) and and(ortho) and ceitinEqExprs) as And, core)
    }
}