package com.joelburton.mothlang.parser

import com.joelburton.mothlang.scanner.Token

/** Classes for all "expression" tokens.
 *
 * The language is, as many are, divided into expressions and statements.
 * An expression is "anything that could be on the right-hand side of an
 * assignment", basically, like "1", "1 + foo", "myFunc(10, 20)", etc.
 *
 * Since the [Parser] tokens will be used in different ways (just printing
 * them to debug them, interpreting them, resolving variables in them, etc),
 * they might accumulate a method for each of these, so each token class
 * would handle each of those actions. Instead, this code is oriented around
 * the [Visitor pattern](https://en.wikipedia.org/wiki/Visitor_pattern), which
 * "re-orients" the object-orientation so that one class (the [interpreter.Interpreter]
 * or [resolver.Resolver] can gather together all the logic about that system in one
 * class, leaving the parser.Parser classes simpler and focused only on their data.
 * This also means they won't need to be touched if other layers of
 * post-parsing are added.
 *
 * It's not necessary that the subclasses of [Expr] are nested inside of it;
 * this just feels tidy. The [Expr] class is sealed, which may be useful for
 * code that needs to know that it handled every possible parser.Expr type.
 */

sealed class Expr {

    /** The Visitor pattern requires an interface that gathers these methods.
     *
     * These are the functions that a visiting client (like the interpreter.Interpreter)
     * needs to handle. As new parser.Expr classes are added, this needs to be
     * updated.
     *
     * In some implementations, these functions all have the same name, like
     * "visitor", and the differentiation is entirely done by method
     * overloading. This still uses overloading, but it's helpful for the
     * reader for these to have more specific names.
     */
    interface Visitor<R> {
        fun visitAssignExpr(expr: Assign): R
        fun visitBinaryExpr(expr: Binary): R
        fun visitCallExpr(expr: Call): R
        fun visitGroupingExpr(expr: Grouping): R
        fun visitLiteralExpr(expr: Literal): R
        fun visitLogicalExpr(expr: Logical): R
        fun visitUnaryExpr(expr: Unary): R
        fun visitVariableExpr(expr: Variable): R
//        fun visitGetExpr(expr: Get): R
//        fun visitSetExpr(expr: Set): R
//        fun visitSuperExpr(expr: Super): R
//        fun visitThisExpr(expr: This): R
    }

    /** An assignment: "a = 2" */
    class Assign(val name: Token, val value: Expr) : Expr() {
        override fun <R> accept(v: Visitor<R>): R = v.visitAssignExpr(this)
    }

    /** Binary expression, like `1 + 2` */
    class Binary(
        val left: Expr,
        val operator: Token,
        val right: Expr,
    ) : Expr() {
        override fun <R> accept(v: Visitor<R>): R = v.visitBinaryExpr(this)
    }

    /** Function call, like `foo(1, 2, bar)` */
    class Call(
        val callee: Expr,
        val paren: Token,
        val arguments: MutableList<Expr?>,
    ) : Expr() {
        override fun <R> accept(v: Visitor<R>): R = v.visitCallExpr(this)
    }

//     class Get(val obj: parser.Expr?, val name: com.joelburton.mothlang.scanner.Token?) : parser.Expr() {
//        override fun <R> accept(v: Visitor<R>): R = v.visitGetExpr(this)
//    }

    /** Parenthesis-grouping, like `( 1 + 2)` */
    class Grouping(val expression: Expr) : Expr() {
        override fun <R> accept(v: Visitor<R>): R = v.visitGroupingExpr(this)
    }

    /** Literal value: string or double */
    class Literal(val value: Any?) : Expr() {
        override fun <R> accept(v: Visitor<R>): R = v.visitLiteralExpr(this)
    }

    /** Logical expression, like `1 == 2` or `foo >= bar` */
    class Logical(
        val left: Expr,
        val operator: Token,
        val right: Expr,
    ) : Expr() {
        override fun <R> accept(v: Visitor<R>): R = v.visitLogicalExpr(this)
    }

//     class Set(val obj: parser.Expr, val name: com.joelburton.mothlang.scanner.Token, val value: parser.Expr) :
//        parser.Expr() {
//        override fun <R> accept(v: Visitor<R>): R? = v?.visitSetExpr(this)
//    }

//     class Super(val keyword: com.joelburton.mothlang.scanner.Token, val method: com.joelburton.mothlang.scanner.Token) : parser.Expr() {
//        override fun <R> accept(v: Visitor<R>): R? = v.visitSuperExpr(this)
//    }

//     class This(val keyword: com.joelburton.mothlang.scanner.Token?) : parser.Expr() {
//        override fun <R> accept(v: Visitor<R>): R = v.visitThisExpr(this)
//    }

    /** A unary expression: `-1` or `-foo` */
    class Unary(val operator: Token, val right: Expr) : Expr() {
        override fun <R> accept(v: Visitor<R>): R = v.visitUnaryExpr(this)
    }

    /** A variable: `foo` */
    class Variable(val name: Token) : Expr() {
        override fun <R> accept(v: Visitor<R>): R = v.visitVariableExpr(this)
    }

    /** This is the core of the Visitor pattern: accept a visitor.
     *
     * This defines the "accept" method as a generic; each subclass here
     * implements a class-specific version of this, which will ultimately call
     * the "visitMyType" method in the client.
     *
     * (This is sometimes termed "double-dispatch")
     */
    abstract fun <R> accept(v: Visitor<R>): R
}