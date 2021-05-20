package printers

import expressions.*
import printers.BreakingSymmetryType.*
import utils.*

class LatinSquareEncoder(
    private val reduced: Boolean,
    private val weighted: Boolean,
    private val breakingSymmetryType: BreakingSymmetryType,
    private val cycleNum: Int = -1,
    val n: Int,
    val k: Int,
    val q: Int
) : CnfEncoder {
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
        weights += List(and(lines).args.size) {
            if (weighted) {
                n
            } else {
                1
            }
        }
        val breakingSymmetry = if (reduced) {
            when (breakingSymmetryType) {
                FIRST -> breakingSymmetry(coreVariables[0][1], cycle = cycleNum)
                SECOND -> breakingSymmetry(coreVariables[1][0], 1, cycleNum)
                else -> and()
            }
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

class LatinSquareEncoderBuilder(
    private val reduced: Boolean,
    private val weighted: Boolean,
    private val breakingSymmetryType: BreakingSymmetryType = NONE,
    private val cycleNum: Int = -1
) : LatinCnfEncoderBuilder {
    override fun invoke(n: Int, k: Int, q: Int) = LatinSquareEncoder(reduced, weighted, breakingSymmetryType, cycleNum, n, k, q)
}