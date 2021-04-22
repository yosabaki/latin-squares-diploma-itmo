package printers

import expressions.CNF
import expressions.Literal
import expressions.Variable
import java.io.PrintWriter

class PropagationCnfPrinter(
    writer: PrintWriter,
    private val propagationUnits: Map<Variable, Literal>
) : CnfPrinter(writer) {
    override fun print(cnf: CNF) {
        writer.print(cnf.propagate(propagationUnits))
    }
}