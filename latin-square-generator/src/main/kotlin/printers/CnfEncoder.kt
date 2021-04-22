package printers

import expressions.DIMACS

interface CnfEncoder {
    fun cnf() : DIMACS
}

interface LatinCnfEncoderBuilder {
    operator fun invoke(n:Int, k:Int, q:Int): CnfEncoder
}