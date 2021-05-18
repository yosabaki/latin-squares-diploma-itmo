package parsers

import expressions.Variable
import utils.*
import java.io.BufferedReader

class ReducedLogLatinParser(val q: Int, val matrixCount: Int, reader: BufferedReader, format: OutputFormat) :
    Parser(reader, format) {

    override fun construct(variables: List<Boolean>) : List<List<List<Int>>> {
        val reducedVarMatrix = initFirstReducedLogMatrix(q)
        val reducedMatrix = reducedVarMatrix.map { line ->
            line.map { vars ->
                val nVars = vars.count { it is Variable }
                parseIntLog(readNVars(nVars), vars).let {
                    if (it >= q) {
                        -1
                    } else {
                        it + 1
                    }
                }
            }
        }
        val varMatrixes = (0 until matrixCount - 1).map { initReducedLogMatrix(q) }
        val matrixes = listOf(reducedMatrix) + varMatrixes.map { matrix ->
            matrix.map { line ->
                line.map { vars ->
                    val nVars = vars.count { it is Variable }
                     parseIntLog(readNVars(nVars), vars).let {
                         if (it >= q) {
                             -1
                         } else {
                             it + 1
                         }
                     }
                }
            }
        }
//        val chis = mutableListOf<List<Boolean>>()
//        chis += readNVars(q * q)
//        val svars = List(q) { i ->
//            List(q) { j ->
//                readNVars(q * q)
//            }
//        }
//        println(chis)
//        println(svars)
//        val svars = variables.subList(matrixCount * q * q * q + q * q + q * q * q * q, variables.size - 1)
        (0 until matrixCount).forEach { i ->
            (i + 1 until matrixCount).forEach { j ->
                println(matrixes[i].flatten().zip(matrixes[j].flatten()).filter { it.first != -1 && it.second != -1 }.distinct().size)
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