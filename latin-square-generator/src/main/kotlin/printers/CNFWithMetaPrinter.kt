package printers

import expressions.CNFWithMetaInfo
import expressions.DIMACS
import java.io.PrintWriter

class CNFWithMetaPrinter(val cnfWriter: PrintWriter,val metaWriter: PrintWriter) {
    fun print(dimacs: CNFWithMetaInfo) {
        cnfWriter.use {
            it.print(dimacs)
        }
        metaWriter.use {
            it.print(dimacs.meta())
        }
    }
}