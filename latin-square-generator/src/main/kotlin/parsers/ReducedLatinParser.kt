package parsers

import expressions.Variable
import utils.initMatrix
import utils.initReducedMatrix
import utils.transpose
import java.io.BufferedReader

class ReducedLatinParser(
    val n: Int,
    val k: Int,
    reader: BufferedReader,
    format: OutputFormat
) : Parser(reader, format) {
    override fun construct(variables: List<Boolean>): List<List<List<Int>>> {
        val reducedVarMatrix = initReducedMatrix(n)
        val reducedMatrix = reducedVarMatrix.map { line ->
            line.map { vars ->
                val nVars = vars.count { it is Variable }
                parseIntOneHot(readNVars(nVars), vars)
            }
        }
        val varMatrixes = (0 until k - 1).map { initMatrix(n) }
        val matrixes = listOf(reducedMatrix) + varMatrixes.map { matrix ->
            matrix.map { line ->
                line.map { vars ->
                    parseIntOneHot(readNVars(n), vars)
                }
            }
        }
//        val chis = mutableListOf<List<Boolean>>()
//        println(currentIndex)
//        chis += readNVars(n * n)
//        val svars = List(n) { i ->
//            List(n) { j ->
//                readNVars(n * n)
//            }
//        }
//        println(chis)
//        println(svars)
//        val svars = variables.subList(matrixCount * q * q * q + q * q + q * q * q * q, variables.size - 1)
        (0 until k).forEach { i ->
            (i + 1 until k).forEach { j ->
                println(matrixes[i].flatten().zip(matrixes[j].flatten()).distinct().size)
            }
        }
        matrixes.forEach { matrix ->
            assert(matrix.all { it.distinct().size == it.size })
            assert(matrix.transpose().all { it.distinct().size == it.size })
            matrix.forEach { array ->
                println(array.joinToString(" ") {
                    "%2d".format(it)
                })
            }
            println()
        }
        return matrixes
    }
}