package com.joelburton.mothlang.resolver

import com.joelburton.mothlang.interpreter.Interpreter
import com.joelburton.mothlang.ast.Stmt
import com.joelburton.mothlang.ast.Expr
import com.joelburton.mothlang.scanner.Token
import com.joelburton.mothlang.Lox

import java.util.Stack

/** Resolver pass: a semantic analysis (after parsing, before interpretation).
 *
 * This "resolves" variables and functions to where they are lexically
 * defined. It allows proper closures, rather than dynamic ones.
 *
 * For example:
 *
 *  ```
 *  var x = "global";
 *  {
 *    fun showX() { print x; }
 *    showX(); var x = "block"; showX();
 *  }
 *  ```
 *
 * In a dynamic-closure language, this would print "global, block" ---
 * after the local definition of x, showX will find that.
 *
 * That's not how more serious languages implement the idea o a closure, though;
 * it should print "global, global", because at the point of the definition
 * of the function showX, the only reference to x is the global one.
 *
 * JavaScript's closures do dynamic search; ours don't.
 *
 * This work could be done in the parer itself, but it's nice and clean to
 * think of this as a separate pass before the interpreter.
 *
 * This code uses the Visitor pattern; see a description of that in [Expr].
 */

class Resolver(
    val interp: Interpreter,
) : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {

    private enum class FunctionType { NONE, FUNCTION }

    /** Stack of scopes; each is Map of var-name to is-defined-here?
     *
     * Each scope can have its own set of "what variables are defined here?".
     *
     * For example, with this code:
     * ```
     *   {             <- makes a scope
     *     {           <- add a new scope on top of stack
     *       var a;    <- declaration; scope["a"] = false (not yet defined)
     *       a = 42;   <- definition;  scope["a"] = true
     * ```
     *
     * Note that the "global scope" isn't really a scope --- functions that
     * look for the scope of a global will get null.
      */
    private val scopes: Stack<MutableMap<String, Boolean>> = Stack()

    /** Mutable tracker of "what kind of function are we in?" */
    private var currentFunction = FunctionType.NONE

    /** Double-dispatcher for expressions. */
    private fun resolve(expr: Expr) {
        expr.accept(this)
    }

    /** Double-dispatcher for statements. */
    private fun resolve(stmt: Stmt) {
        stmt.accept(this)
    }

    /** Find the scope of an expression, and set this for interpreter.
     *
     * This is the essential action: for a given expression and variable name,
     * find which scope in the stack this was declared in, and tell the
     * interpreter about this so it will track the depth of each unique
     * variable expression.
     *
     * If the variable isn't found in a scope, it may be a global variable,
     * so it won't be in a scope (see note above about how "globals vars"
     * and "global funcs" aren't in a scope, as we use it here)
     */
    private fun resolveLocal(expr: Expr, name: Token) {
        for (i in scopes.indices.reversed()) {
            if (name.lexeme in scopes[i]) {
                // Don't know if I love this approach: this reaches into the
                // interpreter to modify its locals map. I think it might be
                // better to have this step *return* the locals map, and have
                // that passed into the interpreter.
                interp.resolve(expr, scopes.size - 1 - i)
                return
            }
        }
    }

    /** Resolve a function statement.
     *
     * A new scope is created, and each parameter is added to it. The body of
     * the function is resolved in this new scope. Then the scope is popped off.
     */
    @Suppress("SameParameterValue")
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

    /** Add a new empty scope to the stack. */
    private fun beginScope() {
        scopes.push(mutableMapOf())
    }

    /** Discard top scope from stack. */
    private fun endScope() {
        scopes.pop()
    }

    /** Mark a newly-declared variable as in this scope.
     *
     * This also prevents a variable from being redeclared in the same scope.
      */
    private fun declare(name: Token) {
        if (scopes.empty()) return
        val scope = scopes.peek()
        if (name.lexeme in scope)
            Lox.loxError(name, "Already declared in this scope.")
        scope[name.lexeme] = false
    }

    /** Mark a scoped-variable as defined now. */
    private fun define(name: Token) {
        if (scopes.empty()) return
        scopes.peek()[name.lexeme] = true
    }


    // ============================= expression visitors

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
        for (arg in expr.arguments) resolve(arg)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping) {
        resolve(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal) {
        // nothing to resolve
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
            // prevent "var a = a;"
            Lox.loxError(
                expr.name,
                "Can't read local variable in its own initializer.")
        }
        resolveLocal(expr, expr.name)
    }

    // ============================ statement visitors

    override fun visitBlockStmt(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    override fun visitExpressionStmt(expr: Stmt.Expression) {
        resolve(expr.expression)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        if (stmt.elseBranch != null) resolve(stmt.elseBranch)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        declare(stmt.name)
        define(stmt.name)
        resolveFunction(stmt, FunctionType.FUNCTION)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        resolve(stmt.expression)
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        if (currentFunction == FunctionType.NONE)
            Lox.loxError(stmt.keyword, "Can't return from top-level code.")
        if (stmt.value != null) resolve(stmt.value)
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        declare(stmt.name)
        if (stmt.initializer != null) resolve(stmt.initializer)
        define(stmt.name)
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }

    // ========================== entry points

    /** Entry point for this step. */
    fun resolve(statements: List<Stmt?>) {
        for (statement in statements) statement?.accept(this)
    }
}