import TokenType.*

class Interpreter : Expr.Visitor<Any?> {
    class RuntimeError(val token: Token, message: String) : RuntimeException(message)

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

    private fun isEqual(a: Any?, b: Any?) = if (a == null) b == null else a == b
    private fun stringify(obj: Any?) =
        when (obj) {
            null -> "nil"
            is Double -> obj.toString().removeSuffix(".0")
            else -> obj.toString()
        }

    private fun evaluate(expr: Expr?): Any? = expr?.accept(this)

    fun interpret(expr: Expr?) {
        try {
            val value = evaluate(expr)
            println(stringify(value))
        } catch (err: RuntimeError) {
            Lox.runtimeError(err)
        }
    }
}