package utils

import expressions.*
import java.util.*

fun initReducedLogMatrix(n: Int, m: Int = n, q: Int = log2(n)): List<List<List<Literal>>> =
    List(n) { i ->
        List(m) { j ->
            List(q) { k ->
                if (i == 0) {
                    if ((j shr k) and 1 == 1) {
                        True()
                    } else {
                        False()
                    }
                } else if (j == 0) {
                    if ((i shr k) and 1 == 1) {
                        True()
                    } else {
                        False()
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
                        True()
                    } else {
                        False()
                    }
                } else if (i == 1) {
                    if (k == j % n) {
                        True()
                    } else {
                        False()
                    }
                } else {
                    newVariable
                }
            }
        }
    }

fun initReducedMatrix(n: Int, m: Int = n, q: Int = n): List<List<List<Literal>>> =
    List(n) { i ->
        List(m) { j ->
            List(q) { k ->
                if (i == 0) {
                    if (j == k) {
                        True()
                    } else {
                        False()
                    }
                } else if (j == 0) {
                    if (i == k) {
                        True()
                    } else {
                        False()
                    }
                } else {
                    if (k == j) {
                        False()
                    } else if (k == i) {
                        False()
                    } else {
                        newVariable
                    }
                }
            }
        }
    }

fun logLatin(ceitinVars: List<List<List<Literal>>>): Expression {
    return and(ceitinVars.map { matrix ->
        and(matrix.map { or(it) })
    } + ceitinVars.map { matrix ->
        and(matrix.transpose().map { or(it) })
    })
}

fun latin(square: List<List<List<Literal>>>): Expression =
    and(square.flatMap { matrix ->
        listOf(codeLine(matrix))
    } + square.transpose().flatMap { matrix ->
        listOf(codeSufficient(matrix.transpose()))
    })

fun logOrthogonal(a: List<List<Literal>>, b: List<List<Literal>>, q: Int): Expression {
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
    return and(pairwiseOrtho, greaterOrEqual(chis.flatten(), n * n - q))
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

fun orthogonal(a: List<List<Literal>>, b: List<List<Literal>>, q: Int): Expression {
    val size = a.size
    val n = b[0].size
    val chis = List(n) { List(n) { newVariable } }
    println("chis:")
    println(chis.flatten())
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
            val chi = and(equalVars.map { expr ->
                or(
                    chis[aN - 1][bN - 1],
                    not(expr)
                )
            } + or(equalVars + not(chis[aN - 1][bN - 1])))
            and(phis + chi) and addend
        })
    })
    return and(pairwiseOrtho, greaterOrEqual(chis.flatten(), n * n - q))
}

fun iff(variable: Variable, expr: Expression) =
    when (expr) {
        is And -> and(expr.args.map {
            or(not(variable), it) // (!variable V (^expr))
        } + or(expr.args.map { not(it) } + variable)) // (variable V !(^expr))
        is Or -> and(expr.args.map {
            or(variable, not(it)) // (variable V !(Vexpr))
        } + or(expr.args.map { it } + not(variable))) // (!variable V (Vexpr))
        is Variable -> and(or(variable, expr), or(not(variable), not(expr)))
        is True -> variable
        is False -> not(variable)
        is Not -> error("not completed iff")
    }

fun orthogonalLogSquare(
    a: List<List<List<Literal>>>,
    b: List<List<List<Literal>>>,
    q: Int
): Expression = if (q != a.size * a.size) {
    logOrthogonal(a.flatten(), b.flatten(), q)
} else {
    fullyOrthogonalSquare(a, b)
}

fun orthogonalSquare(a: List<List<List<Literal>>>, b: List<List<List<Literal>>>, q: Int): Expression =
    if (q != a.size * a.size) {
        orthogonal(a.flatten(), b.flatten(), q)
    } else {
        fullyOrthogonalSquare(a, b)
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