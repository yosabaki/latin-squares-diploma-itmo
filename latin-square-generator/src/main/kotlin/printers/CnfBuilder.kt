package printers

import expressions.DIMACS

interface CnfBuilder {
    fun cnf() : DIMACS
}