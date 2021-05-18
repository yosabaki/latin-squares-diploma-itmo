package printers

import expressions.*
import utils.*

class LogLatinSquareEncoder(private val reduced: Boolean, val n: Int, val k: Int, val q: Int) : CnfEncoder {
    private val coreVariables: List<List<List<List<Literal>>>> = if (reduced) {
        listOf(initFirstReducedLogMatrix(n)) + (1 until k).map {
            initReducedLogMatrix(n)
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
        val breakingSymmetry = if (reduced) {
            breakingSymmetry(ceitinVars[0][1])
        } else {
            and()
        }
        val ceitinEqExprs = and((0 until k).map { t ->
            and((0 until n).map { i ->
                and((0 until n).map { j ->
                    and((0 until n).map { v ->
                        iff(ceitinVars[t][i][j][v], logEqual(coreVariables[t][i][j], v))
                    })
                })
            })
        })
        val lines = ceitinVars.map { logLatin(it) }
        val ortho = (0 until k).flatMap { i ->
            (i + 1 until k).map { j ->
                orthogonalLogSquare(ceitinVars[i], ceitinVars[j], q)
            }
        }
        val core = List(coreSize) { Variable("${it + 1}") }
        return CNF(
            (ceitinEqExprs and and(lines) and and(ortho.map { it.expr }) and breakingSymmetry) as And,
            core,
            incremental = ortho.map { it.variables }.transpose().flatten(),
            net = net
        )
    }
}

class LogLatinSquareEncoderBuilder(private val reduced: Boolean) : LatinCnfEncoderBuilder {
    override fun invoke(n: Int, k: Int, q: Int) = LogLatinSquareEncoder(reduced, n, k, q)

}