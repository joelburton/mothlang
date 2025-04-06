package com.joelburton.mothlang.resolver

import com.joelburton.mothlang.interpreter.Interpreter
import com.joelburton.mothlang.parser.Stmt
import com.joelburton.mothlang.parser.Expr
import com.joelburton.mothlang.scanner.Token
import com.joelburton.mothlang.Lox

import java.util.Stack

class Resolver(
    val interp: Interpreter,
) : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {

    private enum class FunctionType { NONE, FUNCTION }

    private val scopes: Stack<MutableMap<String, Boolean>> = Stack()
    private var currentFunction = FunctionType.NONE

    fun resolve(statements: List<Stmt?>) {
        for (statement in statements) statement?.accept(this)
    }

    private fun resolve(expr: Expr) {
        expr.accept(this)
    }

    private fun resolve(stmt: Stmt) {
        stmt.accept(this)
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        for (i in scopes.indices.reversed()) {
            if (name.lexeme in scopes[i]) {
                interp.resolve(expr, scopes.size - 1 - i)
                return
            }
        }
    }

    private fun resolveFunction(function: Stmt.Function, type: FunctionType) {
        val enclosingFunction = currentFunction
        currentFunction = type

        beginScope()
        for (param in function.params) {
            declare(param)
            define(param)
        }
        resolve(function.body)
        endScope()

        currentFunction = enclosingFunction
    }

    private fun beginScope() {
        scopes.push(mutableMapOf())
    }

    private fun endScope() {
        scopes.pop()
    }

    private fun declare(name: Token) {
        if (scopes.empty()) return
        val scope = scopes.peek()
        if (name.lexeme in scope)
            Lox.loxError(name, "Already declared in this scope.")
        scope[name.lexeme] = false
    }

    private fun define(name: Token) {
        if (scopes.empty()) return
        scopes.peek()[name.lexeme] = true
    }


    // expression visitors

    override fun visitAssignExpr(expr: Expr.Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    override fun visitBinaryExpr(expr: Expr.Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitCallExpr(expr: Expr.Call) {
        resolve(expr.callee)
        for (arg in expr.arguments) resolve(arg!!)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping) {
        resolve(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal) {
        // nothing to do
    }

    override fun visitLogicalExpr(expr: Expr.Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitUnaryExpr(expr: Expr.Unary) {
        resolve(expr.right)
    }

    override fun visitVariableExpr(expr: Expr.Variable) {
        if (!scopes.empty() && scopes.peek()[expr.name.lexeme] == false) {
            Lox.loxError(
                expr.name,
                "Can't read local variable in its own initializer.")
        }
        resolveLocal(expr, expr.name)
    }

    // statement visitors

    override fun visitExpressionStmt(expr: Stmt.Expression) {
        resolve(expr.expression)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        resolve(stmt.expression)
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        declare(stmt.name)
        if (stmt.initializer != null) resolve(stmt.initializer)
        define(stmt.name)
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        if (stmt.elseBranch != null) resolve(stmt.elseBranch)
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        declare(stmt.name)
        define(stmt.name)
        resolveFunction(stmt, FunctionType.FUNCTION)
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        if (currentFunction == FunctionType.NONE)
            Lox.loxError(stmt.keyword, "Can't return from top-level code.")
        if (stmt.value != null) resolve(stmt.value)
    }
}