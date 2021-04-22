package printers

import expressions.CNF

interface CnfEncoder {
    fun cnf() : CNF
}

interface LatinCnfEncoderBuilder {
    operator fun invoke(n:Int, k:Int, q:Int): CnfEncoder
}