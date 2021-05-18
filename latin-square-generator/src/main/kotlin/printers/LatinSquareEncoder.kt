package printers

import expressions.*
import utils.*

class LatinSquareEncoder(private val reduced: Boolean, val n: Int, val k: Int, val q: Int) : CnfEncoder {
    private val coreVariables = if (reduced) {
        listOf(initFirstReducedMatrix(n)) + (1 until k).map {
            initReducedMatrix(n)
        }
    } else {
        (0 until k).map {
            initMatrix(n)
        }
    }
    private val coreSize = varCounter

    override fun cnf(): CNF {
        val weights = mutableListOf<Int>()
        val lines = coreVariables.map { latin(it) }
        weights += List(and(lines).args.size) { 1 }
        val breakingSymmetry = if (reduced) {
//            and()
            breakingSymmetry(coreVariables[0][1])
        } else {
            and()
        }
        weights += List(breakingSymmetry.args.size) { 1 }
        val ortho = (0 until k).flatMap { i ->
            (i + 1 until k).map { j ->
                orthogonalSquare(coreVariables[i], coreVariables[j], q)
            }
        }
        weights += List(and(ortho.map { it.expr }).args.size) { 1 }
        val core = List(coreSize) { Variable("${it + 1}") }
        val metaVars = coreVariables.flatMapIndexed { matrix, coreRows ->
            coreRows.flatMapIndexed { row, coreColumns ->
                coreColumns.mapIndexed { column, metaVar ->
                    MetaVariable(
                        matrix,
                        row,
                        column,
                        (1..n).associateWith { equal(metaVar, it).args }.mapValues { entry ->
                            entry.value.filter { it != True && it != False }
                        }.filterValues { it.isNotEmpty() }
                    )
                }
            }
        }.filter { it.values.isNotEmpty() }

        return CNF(
            (and(lines) and breakingSymmetry and and(ortho.map { it.expr })) as And,
            core,
            metaVars,
            ortho.map { it.variables }.transpose().flatten(),
            net,
            weights
        )
    }
}

class LatinSquareEncoderBuilder(private val reduced: Boolean) : LatinCnfEncoderBuilder {
    override fun invoke(n: Int, k: Int, q: Int) = LatinSquareEncoder(reduced, n, k, q)
}