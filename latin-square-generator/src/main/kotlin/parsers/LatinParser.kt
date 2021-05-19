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
        matrixes.forEachIndexed {i, matrix ->
            val distinctLines = matrix.map { array ->
                array.groupingBy { it }.eachCount()
            }
            val distinctColumns = matrix.transpose().map { array ->
                array.groupingBy { it }.eachCount()
            }
            println()
            matrix.forEachIndexed { j, array ->
                println(array.indices.joinToString(" ") { index ->
                    (if (distinctLines[j].size == array.size && distinctColumns[index].size == array.size) {
                        GREEN
                    } else if (distinctLines[j][array[index]] != 1 || distinctColumns[index][array[index]] != 1) {
                        RED
                    } else if (distinctLines[j].size == array.size || distinctColumns[index].size == array.size) {
                        YELLOW
                    } else {
                        RESET
                    }) + "%2d".format(array[index])
                })
            }
            println(RESET)
        }
        return matrixes
    }

}