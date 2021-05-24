package expressions

import utils.Net

data class MetaVariable(
    val matrix: Int,
    val row: Int,
    val column: Int,
    val values: Map<Int, List<Expression>>
) {
    fun propagate(units: Map<Variable, Literal>): MetaVariable? {
        val new = mutableMapOf<Int, List<Expression>>()
        for (entry in values) {
            val expr = and(entry.value).propagate(units)
            if (expr == False) {
                continue
            } else if (expr == True) {
                return null
            } else {
                new += entry.key to expr.args.filter { it != False && it != True }
            }
        }
        if (new.isEmpty()) {
            return null;
        }
        return this.copy(values = new)
    }
}

open class CNF(
    val clauses: List<Expression>,
    val variables: Set<String>,
    val coreVariables: List<Variable> = emptyList(),
    val metaVariables: List<MetaVariable> = emptyList(),
    val incremental: List<Expression> = emptyList(),
    val net: Net = Net(),
    val weights: List<Int> = listOf()
) {
    override fun toString(): String {
        val sb = StringBuilder()
        if (coreVariables.isNotEmpty()) {
            sb.append("c core variables:\n")
            sb.append(coreVariables.joinToString(" ", "c ", "\n") { "$it" })
        }
        if (metaVariables.isNotEmpty()) {
            sb.append("c metaVariables:\n")
            metaVariables.forEach { metaVar ->
                sb.append("c newVar ${metaVar.matrix} ${metaVar.row} ${metaVar.column}:\n")
                for (entry in metaVar.values) {
                    sb.append("c ${entry.key} ${entry.value.joinToString(" ") { "$it" }}\n")
                }
            }
        }
        if (incremental.isNotEmpty()) {
            sb.append("c incrementalExprs:\n")
            for (inc in incremental) {
                sb.append("c expr:\n")
                sb.append(inc.args.joinToString("\nc ", "c ", "\n") { "$it" })
            }
        }
        if (net.nodes.isNotEmpty()) {
            sb.append("c computationalNet:\n")
            sb.append(net.nodes.joinToString("\nc ", "c ", "\n") { "$it" })
        }
        if (weights.isNotEmpty()) {
            sb.append("c weights:\n")
            sb.append("c")
            for (i in weights.indices) {
                sb.append(" ${weights[i]}")
            }
            sb.append('\n')
        }
        sb.append("c toCompute:\n")
        sb.append("c")
        for (i in clauses.indices) {
            if (clauses[i].isImportant) {
                sb.append(" $i")
            }
        }
        sb.append("\n")
        val prefix = "p cnf ${variables.size} ${clauses.size}\n"
        val operand = " 0\n"
        val postfix = " 0\n"
        sb.append(clauses.joinToString(operand, prefix, postfix) { "$it" })
        return "$sb"
    }

    open fun propagate(units: Map<Variable, Literal>): CNF {
        val newClauses = and(propagateClauses(clauses, units)).args.distinct()
        return CNF(
            newClauses,
            variables,
            filterNotPropagated(coreVariables, units),
            metaVariables.mapNotNull { it.propagate(units) },
            incremental,
            net
        )
    }

    constructor(and: And) : this(and.args.distinct(), and.variables)
    constructor(and: And, core: List<Variable>) : this(and.args.distinct(), and.variables, core)
    constructor(and: And, core: List<Variable>, meta: List<MetaVariable>) : this(
        and.args.distinct(),
        and.variables,
        core,
        meta
    )

    constructor(
        and: And,
        core: List<Variable>,
        meta: List<MetaVariable> = emptyList(),
        incremental: List<Expression>,
        net: Net,
        weights: List<Int> = listOf()
    ) : this(and.args.distinct(), and.variables, core, meta, incremental, net, weights)
}

private fun filterNotPropagated(variables: List<Variable>, units: Map<Variable, Literal>) =
    variables.filter { it !in units.keys }

private fun propagateClauses(clauses: List<Expression>, units: Map<Variable, Literal>) =
    clauses.map { it.propagate(units) } + and(units.map { (variable, literal) ->
        or(
            when (literal) {
                False -> not(variable)
                True -> variable
                else -> error("propagation error")
            }
        )
    })


class WCNF(
    hardArgs: List<Expression>,
    private val softArgs: List<Expression>,
    variables: Set<String>
) : CNF(hardArgs, variables) {
    override fun toString(): String {
        val weight = softArgs.size + 1
        val prefix = "p wcnf ${variables.size} ${softArgs.size + clauses.size} $weight\n"
        val operand = " 0\n"
        val postfix = " 0\n"
        val firstPart = clauses.joinToString(operand, prefix, postfix) { "$weight $it" }
        val secondPart = softArgs.joinToString(operand, "", postfix) { "1 $it" }
        return firstPart + secondPart
    }

    override fun propagate(units: Map<Variable, Literal>): WCNF {
        val newClauses = propagateClauses(clauses, units)
        val newSoftArgs = propagateClauses(softArgs, units)
        return WCNF(
            newClauses,
            newSoftArgs,
            variables
        )
    }

    constructor(hard: CNF, soft: CNF) : this(hard.clauses, soft.clauses, hard.variables + soft.variables)
}

sealed class ILPMarker

data class VariabledExpression(val expr: Expression, val variables: List<Expression> = emptyList())

sealed class Expression(
    private val defArgs: List<Expression>,
    val variables: Set<String> = defArgs.fold(mutableSetOf()) { a, b -> a.apply { addAll(b.variables) } },
    var isImportant: Boolean = true
) {
    open val args: List<Expression>
        get() = defArgs
    abstract val prefix: String
    abstract val postfix: String
    abstract val operand: String
    override fun toString() = defArgs.joinToString(operand, prefix, postfix) { "$it" }
    abstract fun propagate(units: Map<Variable, Literal>): Expression
}

class And(args: List<Expression>) : Expression(args.flatMap { if (it is And) it.args else listOf(it) }) {
    constructor(vararg args: Expression) : this(args.toList())

    override val prefix: String
        get() = "p cnf ${variables.size} ${args.size}\n"
    override val postfix: String
        get() = " 0\n"
    override val operand: String
        get() = " 0\n"

    override fun equals(other: Any?): Boolean =
        if (other is And) {
            other.args == args
        } else {
            false
        }

    override fun hashCode() = args.hashCode()

    override fun propagate(units: Map<Variable, Literal>) = and(args.map { it.propagate(units) })
}

class Or(args: Collection<Expression>) : Expression(args.flatMap { if (it is Or) it.args else listOf(it) }) {
    override val prefix: String
        get() = ""
    override val postfix: String
        get() = ""
    override val operand: String
        get() = " "

    override fun equals(other: Any?): Boolean =
        if (other is Or) {
            other.args == args
        } else {
            false
        }

    override fun hashCode() = args.hashCode()
    override fun propagate(units: Map<Variable, Literal>): Expression = or(args.map { it.propagate(units) })
}

class Not(private val expr: Expression) : Expression(listOf(expr)) {
    override val prefix: String
        get() = "-"
    override val postfix: String
        get() = ""
    override val operand: String
        get() = ""

    override fun equals(other: Any?): Boolean =
        if (other is Not) {
            other.expr == expr
        } else {
            false
        }

    override fun hashCode() = args.hashCode() + 104729
    override fun propagate(units: Map<Variable, Literal>): Expression = not(expr.propagate(units))
}

sealed class Literal(val name: String = "") : Expression(listOf(), if (name == "") emptySet() else setOf(name)) {
    override val args: List<Expression>
        get() = listOf(this)
    override val prefix: String
        get() = name ?: ""
    override val postfix: String
        get() = ""
    override val operand: String
        get() = ""
}

object False : Literal() {
    override fun propagate(units: Map<Variable, Literal>): Expression = this
}

object True : Literal() {
    override fun propagate(units: Map<Variable, Literal>): Expression = this
}

open class Variable(name: String) : Literal(name) {
    override fun equals(other: Any?): Boolean =
        if (other is Variable) {
            name == other.name
        } else {
            false
        }

    override fun hashCode() = name.hashCode()
    override fun propagate(units: Map<Variable, Literal>): Expression =
        units.getOrDefault(this, this)
}

fun not(expr: Expression): Expression =
    when (expr) {
        is Not -> expr.args.single()
        is True -> False
        is False -> True
        else -> Not(expr)
    }

fun and(exprs: Collection<Expression>) = and(*exprs.toTypedArray())

fun and(vararg exprs: Expression): Expression =
    And(exprs.flatMap {
        when (it) {
            is And -> and(it.args).args
            is True -> listOf()
            is False -> return False
            else -> listOf(it)
        }
    }.also { if (it.isEmpty()) return True })

fun or(exprs: Collection<Expression>) = or(*exprs.toTypedArray())

fun or(vararg exprs: Expression): Expression =
    Or(exprs.flatMap {
        when (it) {
            is Or -> or(it.args).args
            is True -> return True
            is False -> listOf()
            is And -> error("not cnf")
            else -> listOf(it)
        }
    }.also { if (it.isEmpty()) return False })

infix fun Expression.and(other: Expression): Expression =
    and(this, other)

infix fun Expression.or(other: Expression): Expression =
    or(this, other)

