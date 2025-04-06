package com.joelburton.mothlang.interpreter

import com.joelburton.mothlang.ast.Expr
import com.joelburton.mothlang.ast.Stmt
import com.joelburton.mothlang.scanner.TokenType.*
import com.joelburton.mothlang.scanner.Token
import com.joelburton.mothlang.Lox

/** Interpreter; this holds a chain of environments and interprets code.
 *
 * There are two entry points: one to interpret a program (list of statements),
 * and another for interpreting a single expression (mostly for debugging).
 *
 * It is a client of the Visitor pattern used by [Expr] and [Stmt]; see
 * [Expr] for an explanation.
 *
 */
class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {

    open class RuntimeError(val token: Token, message: String) :
        RuntimeException(message)

    /** Marker exception for catching "return" in a function call. */
    class Return(val value: Any?) : RuntimeException(null, null, false, false)

    /** The global environment, which holds top-level vars and functions. */
    private var globals = Environment().also {
        it["clock"] = ClockFn
        it["input"] = InputFn
        it["stringToNum"] = StringToNumFn
        it["randomNum"] = RandomNumFn
    }

    /** Mapping of an expression to the environment-depth.
     *
     * In order to support proper closures, we need to determine "which
     * variable is this?" at the time of resolving, before the interpretation.
     *
     * The map has:
     * - key: a unique expression
     * - value: depth from the usage of it (0=same func, 1=one-up, etc.)
     *
     * Previous versions didn't use this, but dynamically determine the scope
     * at interpretation time by recursively searching each environment.
     * */
    private var locals = HashMap<Expr, Int>()

    /** The current environ the interpreter thinks is local; this is changed. */
    private var environment = globals

    /** Confirm operand is double or throw error. */
    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand !is Double)
            throw RuntimeError(operator, "Operand must be a number.")
    }

    /** Confirm both operands are doubles or throw error. */
    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if (left !is Double || right !is Double)
            throw RuntimeError(operator, "Operands must be numbers.")
    }

    /** `false` and `nil` are not truthy ("falsey"), everything else is truthy.
     *
     * This includes `0` and an empty string.
     */
    private fun isTruthy(obj: Any?) = when (obj) {
        is Boolean -> obj
        null -> false
        else -> true
    }

    /** Is a==b? (true if both null, false if only one is null) */
    private fun isEqual(a: Any?, b: Any?) =
        if (a == null) b == null else a == b

    /** Convert obj to printable string. */
    private fun stringify(obj: Any?) =
        when (obj) {
            null -> "nil"
            is Double -> obj.toString().removeSuffix(".0")
            else -> obj.toString()
        }

    /** Track this exact expression as a local reference.
     *
     * See [com.joelburton.mothlang.resolver.Resolver] for an explanation of this.
     */
    internal fun resolve(expr: Expr, depth: Int) {
        locals.put(expr, depth)
    }

    /** Look up "right" name by finding the correct depth in [locals].
     *
     * Returns value (which may also come from global environ if not in locals).
     */
    private fun lookUpVar(name: Token, expr: Expr): Any? {
        val distance = locals[expr]
//        println("$name $expr $distance")
        return if (distance != null)
            environment.getAt(distance, name.lexeme)
        else
            globals[name]
    }

    /** Double-dispatcher for expressions. See [Expr] for explanation. */
    private fun evaluate(expr: Expr?): Any? = expr?.accept(this)

    /** Double-dispatcher for statements. See [Expr] for explanation. */
    private fun execute(stmt: Stmt?) = stmt?.accept(this)

    /** Execute a code block of [statements] in passed-in [environment].
     *
     * The "accept" functions don't take an environment (perhaps they should?),
     * which would prevent us from having non-local variables (everything would
     * be global!)
     *
     * Instead, this temporarily switches the interpreter environ to the
     * local passed-in one, and after the block exit, restores the previous
     * environment.
     */
    internal fun executeBlock(
        statements: List<Stmt?>,
        environment: Environment,
    ) {
        var previous = this.environment
        try {
            this.environment = environment
            for (statement in statements) statement?.accept(this)
        } finally {
            // make sure this is in finally, since it needs to happen even if
            // there was an error during the block execution
            this.environment = previous
        }
    }

    // ================== expression visitors

    override fun visitAssignExpr(expr: Expr.Assign): Any? {
        val value = evaluate(expr.value)

        val dist = locals[expr]
        if (dist != null) environment.assignAt(dist, expr.name, value)
        else environment.assign(expr.name, value)

        return value
    }

    override fun visitBinaryExpr(expr: Expr.Binary): Any? {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        // special case: PLUS is both adding nums and strings
        if (expr.operator.type == PLUS) {
            if (left is Double && right is Double) {
                return left + right
            } else if (left is String && right is String) {
                return left + right
            } else {
                throw RuntimeError(
                    expr.operator,
                    "Operands must be two numbers or two strings."
                )
            }
        }

        // can work on heterogeneous types
        when (expr.operator.type) {
            EQUAL_EQUAL -> return isEqual(left, right)
            BANG_EQUAL -> return !isEqual(left, right)
            else -> { }
        }

        // else: must be nums
        checkNumberOperands(expr.operator, left, right)
        left as Double
        right as Double

        return when (expr.operator.type) {
            MINUS -> left - right
            SLASH -> left / right
            STAR -> left * right
            GREATER -> left > right
            GREATER_EQUAL -> left >= right
            LESS -> left < right
            LESS_EQUAL -> left <= right
            else -> throw RuntimeError(expr.operator, "Invalid operator.")
        }
    }

    override fun visitCallExpr(expr: Expr.Call): Any? {
        val callee = evaluate(expr.callee)
        val arguments = expr.arguments.map { evaluate(it) }.toMutableList()
        if (callee !is ILoxCallable)
            throw RuntimeError(
                expr.paren, "Can only call functions and classes."
            )
        if (arguments.size != callee.arity)
            throw RuntimeError(
                expr.paren,
                "Expected ${callee.arity} arguments but got ${arguments.size}."
            )
        return callee.call(this, arguments, expr.paren)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping) = evaluate(expr.expression)

    override fun visitLiteralExpr(expr: Expr.Literal) = expr.value

    override fun visitLogicalExpr(expr: Expr.Logical): Any? {
        val left = evaluate(expr.left)
        if (expr.operator.type == OR) {
            if (isTruthy(left)) return left
        } else { // AND
            if (!isTruthy(left)) return left
        }
        return evaluate(expr.right)
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {
        val right = evaluate(expr.right)
        checkNumberOperand(expr.operator, right)
        return when (expr.operator.type) {
            MINUS -> -(right as Double)
            BANG -> !isTruthy(right)
            else -> throw Exception("??? some other unary type?")
        }
    }

    override fun visitVariableExpr(expr: Expr.Variable) =
        lookUpVar(expr.name, expr)

    // ================= statement visitors

    override fun visitBlockStmt(stmt: Stmt.Block) {
        executeBlock(stmt.statements, Environment(environment))
    }

    override fun visitExpressionStmt(expr: Stmt.Expression) {
        evaluate(expr.expression)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        if (isTruthy(evaluate(stmt.condition))) execute(stmt.thenBranch)
        else execute(stmt.elseBranch)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        val fn = UserDefFunction(stmt, environment)
        environment[stmt.name.lexeme] = fn
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expression)
        println(stringify(value))
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        val value = stmt.value?.let { evaluate(it) }
        // not an error---this is caught so a "return" can leap up as many
        // blocks as needed to get to the function call
        throw Return(value)
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        val value = stmt.initializer?.let { evaluate(it) }
        environment[stmt.name.lexeme] = value
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body)
        }
    }

    // ========================= entry points

    /** Main entry point for running a program. */
    fun interpret(statements: List<Stmt?>) {
        try {
            for (statement in statements) execute(statement)
        } catch (err: RuntimeError) {
            Lox.runtimeError(err)
        }
    }

    /** Main entry point for evaluating an expression. */
    fun evalSingle(expr: Expr): Any? = evaluate(expr)
}