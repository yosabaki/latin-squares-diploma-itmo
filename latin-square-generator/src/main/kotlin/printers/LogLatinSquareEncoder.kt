package printers

import expressions.*
import utils.*

class LogLatinSquareEncoder(reduced: Boolean, val n: Int, val k: Int, val q: Int) : CnfEncoder {
    val matrixes: List<List<List<List<Literal>>>> = if (reduced) {
        listOf(initReducedLogMatrix(n)) + (1 until k).map {
            initMatrix(n, n, log2(n))
        }
    } else {
        (0 until k).map {
            initMatrix(n, n, log2(n))
        }
    }
    private val coreSize = varCounter

    override fun cnf(): CNF {
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
        val core = List(coreSize) { Variable("${it + 1}") }
        return CNF((ceitinEqExprs and and(lines) and and(ortho)) as And, core)
    }
}

class LogLatinSquareEncoderBuilder(private val reduced: Boolean) : LatinCnfEncoderBuilder {
    override fun invoke(n: Int, k: Int, q: Int) = LogLatinSquareEncoder(reduced, n, k, q)

}