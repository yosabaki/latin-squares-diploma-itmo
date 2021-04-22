package printers

import expressions.DIMACS
import java.io.BufferedWriter
import java.io.Closeable
import java.io.PrintStream
import java.io.PrintWriter

class CnfPrinter(val writer: PrintWriter) : Closeable by writer {
    fun print(dimacs: DIMACS) {
        writer.print(dimacs)
    }
}