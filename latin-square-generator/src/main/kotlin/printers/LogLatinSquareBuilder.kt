package printers

import expressions.And
import expressions.CNF
import expressions.Variable
import expressions.and
import utils.*

class LogLatinSquareBuilder(val n: Int, val k: Int, val q: Int, val reduced: Boolean) : CnfBuilder {
    override fun cnf(): CNF {
        val matrixes = (0 until k).map {
            initMatrix(n, n, log2(n))
        }
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
        val core = List(n * n * log2(n) * k) { Variable("${it + 1}") }
        return CNF((ceitinEqExprs and and(lines) and and(ortho)) as And, core)
    }
}