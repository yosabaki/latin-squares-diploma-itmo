package parsers

import utils.initMatrix
import java.io.BufferedReader

class ArrayParser(val n: Int, val q: Int, reader: BufferedReader, outputFormat: OutputFormat) :
    Parser(reader, outputFormat) {
    override fun construct(variables: List<Boolean>): List<List<Int>> {
        val array = initMatrix(q, n * n, n).map { matrix ->
            matrix.map { vars ->
                parseIntOneHot(readNVars(n), vars)
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