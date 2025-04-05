import TokenType.*

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {
    open class RuntimeError(val token: Token, message: String) :
        RuntimeException(message)

    class Return(val value: Any?) : RuntimeException(null, null, false, false)

    var globals = Environment()
    private var environment = globals

    init {
        globals["clock"] = ClockFn
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        val value = stmt.initializer?.let { evaluate(it) }
        environment[stmt.name.lexeme] = value
    }

    override fun visitVariableExpr(expr: Expr.Variable): Any? =
        environment[expr.name]

    override fun visitBlockStmt(stmt: Stmt.Block) {
        executeBlock(stmt.statements, Environment(environment))
    }

    override fun visitExpressionStmt(expr: Stmt.Expression) {
        evaluate(expr.expression)
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any? {
        val value = evaluate(expr.value)
        environment.assign(expr.name, value)
        return value
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expression)
        println(stringify(value))
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any? = expr.value

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? =
        evaluate(expr.expression)

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {
        val right = evaluate(expr.right)
        checkNumberOperand(expr.operator, right)
        return when (expr.operator.type) {
            MINUS -> -(right as Double)
            BANG -> !isTruthy(right)
            else -> null
        }
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body)
        }
    }

    override fun visitLogicalExpr(expr: Expr.Logical): Any? {
        val left = evaluate(expr.left)
        if (expr.operator.type == OR) {
            if (isTruthy(left)) return left
        } else {
            if (!isTruthy(left)) return left
        }
        return evaluate(expr.right)
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
        
        // else: must be nums
        checkNumberOperands(expr.operator, left, right)
        return when (expr.operator.type) {
            MINUS -> left as Double - right as Double
            SLASH -> left as Double / right as Double
            STAR -> left as Double * right as Double
            GREATER -> (left as Double) > (right as Double)
            GREATER_EQUAL -> (left as Double) >= (right as Double)
            LESS -> (left as Double) < (right as Double)
            LESS_EQUAL -> (left as Double) <= (right as Double)
            EQUAL_EQUAL -> isEqual(left, right)
            BANG_EQUAL -> !isEqual(left, right)
            else -> throw RuntimeError(expr.operator, "Invalid operator.")
        }
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        val fn = LoxFunction(stmt, environment)
        environment[stmt.name.lexeme] = fn
        println("Added function to environment: ${stmt.name.lexeme}")
    }

    override fun visitCallExpr(expr: Expr.Call): Any? {
        val callee = evaluate(expr.callee)
        val arguments = expr.arguments.map { evaluate(it) }.toMutableList()
        if (callee !is ILoxCallable)
            throw RuntimeError(
                expr.paren, "Can only call functions and classes.")
        if (arguments.size != callee.arity)
            throw RuntimeError(
                expr.paren,
                "Expected ${callee.arity} arguments but got ${arguments.size}."
            )
        return callee.call(this, arguments)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        if (isTruthy(evaluate(stmt.condition))) execute(stmt.thenBranch)
        else execute(stmt.elseBranch)
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        val value = stmt.value?.let { evaluate(it) }
        throw Return(value)
    }

    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand !is Double)
            throw RuntimeError(operator, "Operand must be a number.")
    }

    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if (left !is Double || right !is Double)
            throw RuntimeError(operator, "Operands must be numbers.")
    }

    private fun isTruthy(obj: Any?) = when (obj) {
        is Boolean -> obj
        null -> false
        else -> true
    }

    private fun isEqual(a: Any?, b: Any?) =
        if (a == null) b == null else a == b

    private fun stringify(obj: Any?) =
        when (obj) {
            null -> "nil"
            is Double -> obj.toString().removeSuffix(".0")
            else -> obj.toString()
        }

    private fun evaluate(expr: Expr?): Any? = expr?.accept(this)
    private fun execute(stmt: Stmt?) = stmt?.accept(this)

    internal fun executeBlock(
        statements: List<Stmt?>,
        environment: Environment,
    ) {
        var previous = this.environment
        try {
            this.environment = environment
            for (statement in statements) statement?.accept(this)
        } finally {
            this.environment = previous
        }
    }

    fun interpret(statements: List<Stmt?>) {
        try {
            for (statement in statements) execute(statement)
        } catch (err: RuntimeError) {
            Lox.runtimeError(err)
        }
    }
}