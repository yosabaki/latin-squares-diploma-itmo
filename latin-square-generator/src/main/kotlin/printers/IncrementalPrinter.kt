package printers

import expressions.And
import expressions.Variable
import expressions.and
import utils.*
import java.io.File
import java.util.*
import java.util.stream.IntStream

class IncrementalPrinter(val n: Int, val k: Int, val q: Int, private val file: File) {

    fun print() {
        val (main, incremental, vars) = cnf()
        val directory: File
        val mainFile: File
        if (file.isDirectory) {
            directory = file
            mainFile = File(file, "mainInput")
        } else {
            directory = file.parentFile
            mainFile = file
        }
        mainFile.printWriter().use {
            it.print(main)
        }
        incremental.mapIndexed { i, cnf ->
            val additionalFile = File(directory, "${mainFile.name}Expr$i")
            additionalFile.printWriter().use {
                it.print(cnf)
            }
        }
        File(directory, "${mainFile.name}Vars").printWriter().use { writer ->
            vars.forEach { writer.print("$it ") }
        }
    }


    private fun cnf(): DecomposedCnf {
        val matrixes = listOf(initMatrix(n)) + (0 until k - 1).map {
            initMatrix(n)
        }
        val indexes = IntStream.concat(
            Random().ints(k * 2L, 0, n),
            IntStream.empty()
        ).toArray()
        val variables = matrixes.flatMapIndexed { i, matrix ->
            listOf(matrix[indexes[2 * i]][indexes[2 * i + 1]])
        }
        val lines = matrixes.map {
            latin(it)
        }
        val ortho = (0 until k).flatMap { i ->
            (i + 1 until k).map { j ->
                orthogonalSquare(matrixes[i], matrixes[j], q)
            }
        }
        val main = and(lines) and and(ortho) as And
        val additional = variables.fold(listOf(And())) { list, variable ->
            list.flatMap { expr ->
                (1..n).mapNotNull { i ->
                    (equal(variable, i) and expr) as? And
                }
            }
        }
        return DecomposedCnf(
            (and(main)) as And,
            additional,
            variables.flatten()
        )
    }

    private data class DecomposedCnf(val main: And, val decomposed: List<And>, val vars: List<Variable>)
}