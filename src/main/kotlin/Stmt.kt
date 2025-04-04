abstract class Stmt {
    interface Visitor<R> {
        fun visitExpression(expr: Expression): R
        fun visitPrint(expr: Print): R
    }

    class Expression(val expression: Expr) : Stmt() {
        override fun <R> accept(v: Visitor<R>): R = v.visitExpression(this)
    }

    class Print(val expression: Expr) : Stmt() {
        override fun <R> accept(v: Visitor<R>): R = v.visitPrint(this)
    }

    abstract fun <R> accept(v: Visitor<R>): R
}
