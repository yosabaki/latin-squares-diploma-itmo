package utils

import expressions.*

fun <T> List<List<T>>.transpose(): List<List<T>> =
    this.indices.map { i ->
        this.map { it[i] }
    }

fun codeDistinct(variables: List<Literal>) =
    and(variables.indices.flatMap { i ->
        (i + 1 until variables.size).map { j ->
            not(variables[i]) or not(variables[j])
        }
    })

fun codeAtLeastOne(variables: List<Literal>) = or(variables)

fun codeSufficient(variables: List<List<Literal>>): Expression {
    return and(variables.map {
        codeAtLeastOne(it) and codeDistinct(it)
    })
}


/**
 * Code line such that there are no repeated numbers
 */
fun codeLine(variables: List<List<Literal>>): Expression {
    return codeSufficient(variables) and codeSufficient(variables.transpose())
}


fun greaterOrEqual(vars: List<Variable>, k: Int): Expression {
    val svars = List(vars.size - 1) { List(k) { newVariable } }
    if (k == 0) {
        return and(vars)
    }
    val head = and((vars[0] or svars[0][0]), and((1 until k).map { not(svars[0][it]) }))
    val middle = (1 until vars.size - 1).map { i ->
        val first = and(
            vars[i] or svars[i][0],
            not(svars[i - 1][0]) or svars[i][0],
        )
        val second = (1 until k).map { j ->
            and(
                vars[i] or not(svars[i - 1][j - 1]) or svars[i][j],
                not(svars[i - 1][j]) or svars[i][j]
            )
        }
        val third = vars[i] or not(svars[i - 1][k - 1])
        and(first, and(second), third)
    }
    val tail = vars.last() or not(svars.last()[k - 1])
    return and(head, and(middle), tail)
}

fun decompose(expr: Expression, vars: Set<String>): Pair<List<Expression>, List<Expression>> {
    val (withArgs, withoutArgs) = mutableListOf<Expression>() to mutableListOf<Expression>()
    for (arg in expr.args) {
        if (arg.variables.any { it in vars }) {
            withArgs += arg
        } else {
            withoutArgs += arg
        }
    }
    return withArgs to withoutArgs
}

fun log2(i: Int) : Int = 31 - Integer.numberOfLeadingZeros(i) + if (Integer.bitCount(i) > 1) 1 else 0

val newVariable: Variable
    get() = Variable("${++varCounter}")

fun equal(vector: List<Literal>, value: Int) =
    and(vector.mapIndexed { i, variable -> if (i == value - 1) variable else not(variable) })

fun logEqual(vector: List<Literal>, value: Int) =
    and(vector.mapIndexed {i, variable -> if (((value ushr i) and 1) == 1) variable else not(variable)})

var varCounter = 0
