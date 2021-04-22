package printers

import expressions.CNF
import java.io.Closeable
import java.io.PrintWriter

open class CnfPrinter(val writer: PrintWriter) : Closeable by writer {
    open fun print(cnf: CNF) {
        writer.print(cnf)
    }
}