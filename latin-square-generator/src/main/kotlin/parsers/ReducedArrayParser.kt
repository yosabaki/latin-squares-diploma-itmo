package parsers

import expressions.Variable
import utils.initReducedArray
import java.io.BufferedReader

class ReducedArrayParser(val n: Int, val q: Int, reader: BufferedReader, outputFormat: OutputFormat) :
    Parser(reader, outputFormat) {
    override fun construct(variables: List<Boolean>) : List<List<Int>> {
        val array = initReducedArray(q, n * n, n).map { matrix ->
            matrix.map { vars ->
                val nVars = vars.count { it is Variable }
                parseIntOneHot(readNVars(nVars), vars)
            }
        }
        (0 until q).forEach { i ->
            (i + 1 until q).forEach { j ->
                println(array[i].zip(array[j]).distinct().size)
            }
        }
        array.forEach { arr ->
            println(arr.joinToString(" ") {
                "%2d".format(it)
            })
        }
        return array
    }
}