package printers

import expressions.*

class HornifyCnfEncoder(val hcnf: CNF, val n: Int = hcnf.clauses.size, val nop: Boolean) : CnfEncoder {
    override fun cnf(): WCNF {
        val m = hcnf.clauses.size
        val variables = (1..m * 2).map { Variable("$it") }
        val mapKeys = (1..m).map { Not(Variable("$it")) } +
                (1..m).map { Variable("$it") }
        val mapping: Map<Expression, Expression> = mutableMapOf<Expression, Expression>().apply {
            for (i in mapKeys.indices) {
                put(mapKeys[i], Not(variables[i]))
            }
        }
        val soft = and(variables.subList(0, n) + variables.subList(m, m + n)) as And
        val hard = mutableListOf<Expression>()
        if (!nop) {
            for (i in 0 until m) {
                hard += Not(variables[i]) or Not(variables[i + m])
            }
        }
        for (expr in hcnf.clauses) {
            hard += or(expr.args.map { mapping[it] ?: it })
        }
        return WCNF(CNF(and(hard) as And), CNF(soft))
    }
}