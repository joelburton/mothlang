package com.joelburton.mothlang.ast

/** AST printer.
 *
 * It's common to have AST-printers output in s-expressions, like Scheme.
 * However, this outputs what should be syntactically correct (even if
 * overly verbose on parentheses!) code in the language. This is helpful
 * because round-tripping this (feeding this into the parser and then
 * evaluating it) can make sure this output performs the same as the source.
 * It can also help the developer spot bugs in the parser/interpreter and
 * can allow brave end-users to check to see if they correctly understand
 * things like the order of precedence for operators.
 */

open class AstExprPrinter : Expr.Visitor<String> {
    private fun parenthesize(name: String, vararg expressions: Expr) =
        expressions.joinToString(
            prefix = "($name ", separator = " ", postfix = ")"
        ) { it.accept(this) }

    override fun visitAssignExpr(expr: Expr.Assign) =
        "(${expr.name.lexeme} = ${expr.value.accept(this)})"

    override fun visitBinaryExpr(expr: Expr.Binary): String {
        val left = expr.left.accept(this)
        val right = expr.right.accept(this)
        return "($left ${expr.operator.lexeme} $right)"
    }

    override fun visitCallExpr(expr: Expr.Call): String {
        val callee = expr.callee.accept(this)
        val args = expr.arguments
            .joinToString(", ") { it?.accept(this) ?: "nil" }
        return "($callee (${args}))"
    }

    override fun visitGroupingExpr(expr: Expr.Grouping) =
        parenthesize("", expr.expression)

    override fun visitLiteralExpr(expr: Expr.Literal) =
        when (expr.value) {
            null -> "nil"
            is Double -> expr.value.toString()
            else -> "\"${expr.value}\""
        }

    override fun visitLogicalExpr(expr: Expr.Logical): String {
        val left = expr.left.accept(this)
        val right = expr.right.accept(this)
        return "($left ${expr.operator.lexeme} $right)"
    }

    override fun visitUnaryExpr(expr: Expr.Unary) =
        parenthesize(expr.operator.lexeme, expr.right)

    override fun visitVariableExpr(expr: Expr.Variable) = expr.name.lexeme

    /** Entry point to get string of an expression. */
    fun print(expr: Expr?) = expr?.accept(this)
}


/** Printer for AST statements. Subclasses [AstExprPrinter]. */

class AstStmtPrinter : AstExprPrinter(), Stmt.Visitor<String> {
    override fun visitExpressionStmt(expr: Stmt.Expression): String {
        return "${expr.expression.accept(this)};"
    }

    override fun visitPrintStmt(stmt: Stmt.Print): String {
        return "print ${stmt.expression.accept(this)};"

    }

    override fun visitVarStmt(stmt: Stmt.Var): String {
        return "var ${stmt.name.lexeme}" +
                if (stmt.initializer != null)
                    " = ${stmt.initializer.accept(this)};" else ";"
    }

    override fun visitBlockStmt(stmt: Stmt.Block): String {
        return "{\n${stmt.statements
            .joinToString(separator = "\n") { it?.accept(this) ?: "" }}\n}"
    }

    override fun visitIfStmt(stmt: Stmt.If): String {
        return "if (${stmt.condition.accept(this)}) " +
                stmt.thenBranch.accept(this) +
                if (stmt.elseBranch != null)
                    " else ${stmt.elseBranch.accept(this)}\n" else ""
    }

    override fun visitWhileStmt(stmt: Stmt.While): String {
        return "while (${stmt.condition.accept(this)}) " +
                stmt.body.accept(this) + ""
    }

    override fun visitFunctionStmt(stmt: Stmt.Function): String {
        return "fun ${stmt.name.lexeme}" +
                "(${stmt.params
                    .joinToString(", ") { it.lexeme }}) " +
                "{\n${stmt.body
                    .joinToString(separator = "\n") { it?.accept(this) ?: "" }}\n}"
    }

    override fun visitReturnStmt(stmt: Stmt.Return): String {
        return "return ${stmt.value?.accept(this) ?: "nil"};"
    }

    // ======================= entry points

    /** Entry point to get string of a statement. */
    fun print(stmt: Stmt?) = stmt?.accept(this)
}
