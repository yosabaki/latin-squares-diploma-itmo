package parsers

import expressions.Literal
import expressions.True
import expressions.Variable
import java.io.BufferedReader
import parsers.OutputFormat.*
import parsers.Result.*

enum class OutputFormat {
    MINISAT,
    DIMACS,
    MAXSAT
}

enum class Result {
    SAT,
    UNSAT,
    UNKNOWN
}

abstract class Parser(val reader: BufferedReader, val format: OutputFormat = MINISAT) {
    fun parseHeader(): Result {
        var line = reader.readLine()
        val sat: String
        val unsat: String
        when (format) {
            MINISAT -> {
                sat = "SAT"
                unsat = "UNSAT"
            }
            DIMACS -> {
                while (line[0] != 's') {
                    line = reader.readLine()
                }
                sat = "s SATISFIABLE"
                unsat = "s UNSATISFIABLE"
            }
            MAXSAT -> {
                while (line[0] != 's') {
                    line = reader.readLine()
                }
                sat = "s OPTIMUM FOUND"
                unsat = ""
            }
        }
        return when (line) {
            sat -> SAT
            unsat -> UNSAT
            else -> UNKNOWN
        }
    }

    private fun parseVariable(variable: String): Boolean =
        !variable.startsWith('-')

    private fun parseBody(): List<Boolean> =
        when (format) {
            MINISAT -> reader.readLine().split(" ").map { parseVariable(it) }
            MAXSAT, DIMACS -> reader.readLines().filter { it[0] == 'v' }.flatMap { string ->
                string.substring(2).split(" ").map { parseVariable(it) }
            }

        }

    protected fun parseIntLog(values: List<Boolean>, vars: List<Literal>): Int {
        var result = 0
        var index = 0
        var pow = 1
        for (t in vars.indices) {
            if (vars[t] is True) {
                result += pow
            } else if (vars[t] is Variable) {
                if (values[index++]) {
                    result += pow
                }
            }
            pow *= 2
        }
        return result
    }


    protected fun parseIntOneHot(values: List<Boolean>, vars: List<Literal>): Int {
        var result = 0
        for (t in vars.indices) {
            if (vars[t] is True) {
                return t + 1
            } else if (vars[t] is Variable) {
                if (values[result++]) {
                    return t + 1
                }
            }
        }
        error("result is invalid")
    }

    var currentIndex = 0
    var variables: List<Boolean> = listOf()

    fun readNVars(n: Int): List<Boolean> =
        variables.subList(currentIndex, currentIndex + n).also {
            currentIndex += n
        }

    protected abstract fun construct(variables: List<Boolean>) : Any

    fun parse() : Any {
        val result = parseHeader()
        if (result != SAT) {
            error("result is $result")
        }
        variables = parseBody()

        return construct(variables)
    }
}