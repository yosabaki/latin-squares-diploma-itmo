package parsers

import utils.initMatrix
import utils.transpose
import java.io.BufferedReader

class LatinParser(val q: Int, val matrixCount: Int, reader: BufferedReader, format: OutputFormat) :
    Parser(reader, format) {

    override fun construct(variables: List<Boolean>) : List<List<List<Int>>> {
        val matrixes = (0 until matrixCount).map { initMatrix(q) }.map { matrix ->
            matrix.map { line ->
                line.map { vars ->
                    parseIntOneHot(readNVars(q), vars)
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