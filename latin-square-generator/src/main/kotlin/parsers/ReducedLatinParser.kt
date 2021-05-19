package parsers

import expressions.False
import expressions.True
import expressions.Variable
import utils.*
import java.io.BufferedReader

class ReducedLatinParser(
    val n: Int,
    val k: Int,
    reader: BufferedReader,
    format: OutputFormat
) : Parser(reader, format) {
    override fun construct(variables: List<Boolean>): List<List<List<Int>>> {
        val otherVarMatrixes = (0 until k).map { initMatrix(n) }
        varCounter = 0
        val reducedVarMatrix = initFirstReducedMatrix(n)
        val varMatrixes = listOf(reducedVarMatrix) + (0 until k - 1).map { initReducedMatrix(n) }
        val matrixes = varMatrixes.mapIndexed { i, matrix ->
            matrix.mapIndexed { j, line ->
                line.mapIndexed { l, vars ->
                    val nVars = vars.count { it is Variable }
                    parseIntOneHot(readNVars(nVars), vars).also {
                        otherVarMatrixes[i][j][l].forEachIndexed { q, otherVar ->
                            parsedUnits[otherVar] = if (q == it - 1) {
                                True
                            } else {
                                False
                            }
                        }
                    }
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
                println(matrixes[i].flatten().zip(matrixes[j].flatten()).filter { it.first != -1 && it.second != -1 }
                    .distinct().size)
            }
        }
        matrixes.indices.forEach { i ->
            val matrix = matrixes[i]
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
                        for (k in otherVarMatrixes[i].indices) {
                            for (variable in otherVarMatrixes[i][k][index]) {
                                parsedUnits.remove(variable)
                            }
                        }
                        for (k in otherVarMatrixes[i][j].indices) {
                            for (variable in otherVarMatrixes[i][j][k]) {
                                parsedUnits.remove(variable)
                            }
                        }
                        RED
                    } else if (distinctLines[j].size == array.size || distinctColumns[index].size == array.size) {
                        for (variable in otherVarMatrixes[i][j][index]) {
                            parsedUnits.remove(variable)
                        }
                        YELLOW
                    } else {
                        for (variable in otherVarMatrixes[i][j][index]) {
                            parsedUnits.remove(variable)
                        }
                        RESET
                    }) + "%2d".format(array[index])
                })
            }
            println(RESET)
        }
        return matrixes
    }
}