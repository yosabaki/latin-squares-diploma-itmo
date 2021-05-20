package utils

import expressions.*
import java.util.*

fun initFirstReducedLogMatrix(n: Int, m: Int = n, q: Int = log2(n)): List<List<List<Literal>>> =
    List(n) { i ->
        List(m) { j ->
            List(q) { k ->
                if (i == 0) {
                    if ((j shr k) and 1 == 1) {
                        True
                    } else {
                        False
                    }
                } else if (j == 0) {
                    if ((i shr k) and 1 == 1) {
                        True
                    } else {
                        False
                    }
                } else {
                    newVariable
                }
            }
        }
    }

fun initReducedLogMatrix(n: Int, m: Int = n, q: Int = log2(n)): List<List<List<Literal>>> =
    List(n) { i ->
        List(m) { j ->
            List(q) { k ->
                if (j == 0) {
                    if ((i shr k) and 1 == 1) {
                        True
                    } else {
                        False
                    }
                } else {
                    newVariable
                }
            }
        }
    }

fun initMatrix(n: Int, m: Int = n, q: Int = n): List<List<List<Variable>>> =
    List(n) {
        List(m) {
            List(q) { i ->
                newVariable
            }
        }
    }

fun initReducedArray(s: Int, size: Int, n: Int): List<List<List<Literal>>> =
    List(s) { i ->
        List(size) { j ->
            List(n) { k ->
                if (i == 0) {
                    if (k == j / n) {
                        True
                    } else {
                        False
                    }
                } else if (i == 1) {
                    if (k == j % n) {
                        True
                    } else {
                        False
                    }
                } else {
                    newVariable
                }
            }
        }
    }

fun getCycles(n: Int): List<List<Int>> {
    val curCycle = if (n % 2 == 0) {
        MutableList(n / 2) { 2 }
    } else {
        MutableList(n / 2) { i -> if (i == n / 2 - 1) 3 else 2 }
    }
    val cycles = mutableListOf(curCycle.toList())
    var sum = n
    while (curCycle.size != 1) {
        sum -= curCycle.last()
        curCycle.removeLast()
        curCycle[curCycle.lastIndex]++
        sum++
        val value = curCycle.last()
        if (value > n - sum) {
            curCycle[curCycle.lastIndex] += n - sum
        } else {
            for (i in 0 until (n - sum) / value - 1) {
                curCycle += value
                sum += value
            }
            curCycle += n - sum
        }
        sum = n
        cycles += curCycle.toList()
    }
    return cycles
}

fun breakingSymmetry(variables: List<List<Literal>>, firstIndex: Int = 0, cycle: Int = -1): Expression {
    val cycles = getCycles(variables.size - firstIndex)
    val breakingSymmetryVars = List(cycles.size) { newVariable }
    val phis = cycles.indices.map { i ->
        val cycle = cycles[i]
        var index = firstIndex
        val eqExpr = and(cycle.flatMap { n ->
            (0 until n).map {
                equal(variables[index + it], index + 1 + (it + 1) % n)
            }.also {
                index += n
            }
        })
        iff(breakingSymmetryVars[i], eqExpr)
    }
    return and(phis) and or(breakingSymmetryVars) and (if (cycle == -1) { True } else { breakingSymmetryVars[cycle] })
}


fun initReducedMatrix(n: Int, m: Int = n, q: Int = n): List<List<List<Literal>>> =
    List(n) { i ->
        List(m) { j ->
            List(q) { k ->
                if (j == 0) {
                    if (i == k) {
                        True
                    } else {
                        False
                    }
                } else if (i == 0) {
                    if (j == k) {
                        False
                    } else {
                        newVariable
                    }
                } else {
                    if (i == k) {
                        False
                    } else {
                        newVariable
                    }
                }
            }
        }
    }

fun initFirstReducedMatrix(n: Int, m: Int = n, q: Int = n): List<List<List<Literal>>> =
    List(n) { i ->
        List(m) { j ->
            List(q) { k ->
                if (i == 0) {
                    if (j == k) {
                        True
                    } else {
                        False
                    }
                } else if (j == 0) {
                    if (i == k) {
                        True
                    } else {
                        False
                    }
                } else {
                    if (k == j) {
                        False
                    } else if (k == i) {
                        False
                    } else {
                        newVariable
                    }
                }
            }
        }
    }

fun logLatin(ceitinVars: List<List<List<Literal>>>): Expression {
    return and(ceitinVars.flatMap { matrix ->
        listOf(codeSufficient(matrix.transpose()))
    } + ceitinVars.transpose().flatMap { matrix ->
        listOf(codeSufficient(matrix.transpose()))
    })
}

fun latin(square: List<List<List<Literal>>>): Expression =
    and(square.flatMap { matrix ->
        listOf(codeLine(matrix))
    } + square.transpose().flatMap { matrix ->
        listOf(codeSufficient(matrix.transpose()))
    })

fun logOrthogonal(a: List<List<Literal>>, b: List<List<Literal>>, q: Int): VariabledExpression {
    val size = a.size
    val n = b[0].size
    val chis = List(n) { List(n) { newVariable } }
    val pairwiseOrtho = and((1..n).map { aN ->
        and((1..n).map { bN ->
            val equalExprs: List<And> = (0 until size).map { i ->
                (a[i][aN - 1] and b[i][bN - 1]) as And
            }
            val equalVars = (0 until size).map { newVariable }
            val phis = equalExprs.zip(equalVars).map { (expr, v) -> iff(v, expr) }
            val addend = if (q == size) {
                and((equalVars.indices).flatMap { i ->
                    (i + 1 until equalVars.size).map { j ->
                        not(equalVars[i]) or not(equalVars[j])
                    }
                })
            } else {
                and()
            }
            val chi = and(iff(chis[aN - 1][bN - 1], Or(equalVars)))
            and(phis + chi) and addend
        })
    })
    val (greater, vars) = greaterOrEqual(chis.flatten(), n * n - q)
    return VariabledExpression(and(pairwiseOrtho, greater), vars)
}

fun Pair<Int, Int>.next(n: Int): Pair<Int, Int> {
    if (first == n - 1 && second == n - 1) {
        return this
    }
    val s = (second + 1) % n
    val f = s + if (s == 0) {
        1
    } else {
        0
    }
    return f to s
}

fun fullyOrthogonalSquare(a: List<List<List<Literal>>>, b: List<List<List<Literal>>>): Expression {
    val code = mutableListOf<Expression>()
    for (i1 in a.indices) {
        for (j1 in a[i1].indices) {
            for (i2 in (i1 + 1 until a.size)) {
                for (j2 in (0 until a[i2].size)) {
                    if (j2 == j1) continue
                    for (k1 in a[i1][i2].indices) {
                        for (k2 in a[i1][i2].indices) {
                            code += or(not(a[i1][j1][k1]), not(a[i2][j2][k1]), not(b[i1][j1][k2]), not(b[i2][j2][k2]))
                        }
                    }
                }
            }
        }
    }
    return and(code)
}

fun orthogonal(a: List<List<Literal>>, b: List<List<Literal>>, q: Int): VariabledExpression {
    val size = a.size
    val n = b[0].size
    val chis = List(n) { List(n) { newVariable } }
    val pairwiseOrtho = and((1..n).map { aN ->
        and((1..n).map { bN ->
            val equalExprs: List<Expression> = (0 until size).map { i ->
                (equal(a[i], aN) and equal(b[i], bN))
            }
            val equalVars = (0 until size).map { newVariable }
            val phis = equalExprs.zip(equalVars).map { (expr, v) -> iff(v, expr) }
            val addend = if (q == size) {
                and((equalVars.indices).flatMap { i ->
                    (i + 1 until equalVars.size).map { j ->
                        not(equalVars[i]) or not(equalVars[j])
                    }
                })
            } else {
                and()
            }
            val chi = and(iff(chis[aN - 1][bN - 1], Or(equalVars)))
            and(phis + chi).also { it.args.forEach { it.isImportant = false } } and addend
        })
    })
    val (greater, vars) = greaterOrEqual(chis.flatten().shuffled(), n * n - q)
    return VariabledExpression(and(pairwiseOrtho, greater), vars)
}

fun iff(variable: Variable, expr: Expression) =
    when (expr) {
        is And -> {
            net.add { and(variable, expr.args) }
            and(expr.args.map {
                or(not(variable), it) // (!variable V (^expr))
            } + or(expr.args.map { not(it) } + variable))
        } // (variable V !(^expr))
        is Or -> {
            net.add { or(variable, expr.args) }
            and(expr.args.map {
                or(variable, not(it)) // (variable V !(Vexpr))
            } + or(expr.args.map { it } + not(variable)))
        } // (!variable V (Vexpr))
        is Variable -> {
            net.add { and(variable, expr) }
            and(or(variable, expr), or(not(variable), not(expr)))
        }
        is True -> {
            net.add { newVal(variable, true) }
            variable
        }
        is False -> {
            net.add { newVal(variable, false) }
            not(variable)
        }
        is Not -> error("not completed iff")
    }

fun orthogonalLogSquare(
    a: List<List<List<Literal>>>,
    b: List<List<List<Literal>>>,
    q: Int
): VariabledExpression = if (q != a.size * a.size) {
    logOrthogonal(a.flatten(), b.flatten(), q)
} else {
    VariabledExpression(fullyOrthogonalSquare(a, b))
}

fun orthogonalSquare(a: List<List<List<Literal>>>, b: List<List<List<Literal>>>, q: Int): VariabledExpression =
    if (q != a.size * a.size) {
        orthogonal(a.flatten(), b.flatten(), q)
    } else {
        VariabledExpression(fullyOrthogonalSquare(a, b))
    }

fun cycle(list: List<List<Literal>>, n: Int, index: Int): List<LinkedList<Int>> =
    if (n == 2) {
        (index until list.size).flatMap { i ->
            (i + 1 until list.size).map { j ->
                LinkedList(listOf(i, j))
            }
        }
    } else {
        list.indices.flatMap { i ->
            cycle(list, n - 1, i + 1).map { it ->
                it.addFirst(0)
                it
            }
        }
    }
//list.indices.flatMap {
//    i ->
//    list[i].indices.fold(listOf()) { exprs, j1 ->
//
//    }
//}

//fun isomorph(list: List<List<List<Literal>>>): List<Expression>

fun breakingSymmetrySquare(a: List<List<List<Variable>>>) =
    and(a.indices.map { i ->
        a[0][i][i] and a[i][0][i]
    })

fun codeRandom(matrix: List<List<List<Variable>>>, values: List<List<Int>>, ratio: Int): And {
    val n = values.size
    val q = (n * n) * ratio / 100
    val array = (0 until n).shuffled()
    return and((0 until q).map {
        val (i, j) = array[it / n] to array[it % n]
        equal(matrix[i][j], values[i][j])
    }) as And
}

class ValueNode(variable: Variable, val value: Boolean) : Node(variable) {
    override fun toString() = "value $variable $value"
}

class AndIffNode(variable: Variable, val args: List<Expression>) : Node(variable) {
    override fun toString() = "andIff $variable ${args.joinToString(" ") { "$it" }}"
}

class OrIffNode(variable: Variable, val args: List<Expression>) : Node(variable) {
    override fun toString() = "orIff $variable ${args.joinToString(" ") { "$it" }}"
}

class OrAndNode(variable: Variable, val args: List<List<Expression>>) : Node(variable) {
    override fun toString() = "or_and $variable ${
        args.joinToString(" 0 ", postfix = " 0") { and ->
            and.joinToString(" ") { "$it" }
        }
    }"
}

sealed class Node(val variable: Variable)

class NetBuilder(val net: Net) {
    fun and(variable: Variable, args: List<Expression>) {
        net.nodes += AndIffNode(variable, args)
    }

    fun and(variable: Variable, addend: Expression) {
        net.nodes += AndIffNode(variable, listOf(addend))
    }

    fun or(variable: Variable, args: List<Expression>) {
        net.nodes += OrIffNode(variable, args)
    }

    fun or(variable: Variable, orend: Expression) {
        net.nodes += OrIffNode(variable, listOf(orend))
    }

    fun orAnd(variable: Variable, args: List<List<Expression>>) {
        net.nodes += OrAndNode(variable, args)
    }

    fun newVal(variable: Variable, value: Boolean) {
        net.nodes += ValueNode(variable, value)
    }
}

class Net {
    val nodes = mutableListOf<Node>()

    fun add(func: NetBuilder.() -> Unit) = NetBuilder(net).apply(func)
}

val net = Net()