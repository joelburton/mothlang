abstract class Expr {
    interface Visitor<R> {
        //        fun visitAssignExpr(expr: Assign?): R?
        fun visitBinaryExpr(expr: Binary): R

        //        fun visitCallExpr(expr: Call?): R?
//        fun visitGetExpr(expr: Get?): R?
        fun visitGroupingExpr(expr: Grouping): R
        fun visitLiteralExpr(expr: Literal): R

        //        fun visitLogicalExpr(expr: Logical?): R?
//        fun visitSetExpr(expr: Set?): R?
//        fun visitSuperExpr(expr: Super?): R?
//        fun visitThisExpr(expr: This?): R?
        fun visitUnaryExpr(expr: Unary): R
//        fun visitVariableExpr(expr: Variable?): R?
    }

    // Nested Expr classes here...
    //> expr-assign
//     class Assign(val name: Token?, val value: Expr?) : Expr() {
//        override fun <R> accept(v: Visitor<R?>?): R? = v?.visitAssignExpr(this)
//    }

    class Binary(
        val left: Expr,
        val operator: Token,
        val right: Expr,
    ) : Expr() {
        override fun <R> accept(v: Visitor<R>): R = v.visitBinaryExpr(this)
    }

//     class Call(
//        val callee: Expr?,
//        val paren: Token?,
//        val arguments: MutableList<Expr?>?,
//    ) : Expr() {
//        override fun <R> accept(v: Visitor<R?>?): R? = v?.visitCallExpr(this)
//    }

//     class Get(val obj: Expr?, val name: Token?) : Expr() {
//        override fun <R> accept(v: Visitor<R?>?): R? = v?.visitGetExpr(this)
//    }

    class Grouping(val expression: Expr) : Expr() {
        override fun <R> accept(v: Visitor<R>): R = v.visitGroupingExpr(this)
    }

    class Literal(val value: Any?) : Expr() {
        override fun <R> accept(v: Visitor<R>): R = v.visitLiteralExpr(this)
    }

//     class Logical(
//        val left: Expr?,
//        val operator: Token?,
//        val right: Expr?,
//    ) : Expr() {
//        override fun <R> accept(v: Visitor<R?>?): R? = v?.visitLogicalExpr(this)
//    }

//     class Set(val obj: Expr?, val name: Token?, val value: Expr?) :
//        Expr() {
//        override fun <R> accept(v: Visitor<R?>?): R? = v?.visitSetExpr(this)
//    }

//     class Super(val keyword: Token?, val method: Token?) : Expr() {
//        override fun <R> accept(v: Visitor<R?>?): R? = v?.visitSuperExpr(this)
//    }

//     class This(val keyword: Token?) : Expr() {
//        override fun <R> accept(v: Visitor<R?>?): R? = v?.visitThisExpr(this)
//    }

    class Unary(val operator: Token, val right: Expr) : Expr() {
        override fun <R> accept(v: Visitor<R>): R = v.visitUnaryExpr(this)
    }

//     class Variable(val name: Token?) : Expr() {
//        override fun <R> accept(v: Visitor<R?>?): R? = v?.visitVariableExpr(this)
//    }

    //< expr-variable
    abstract fun <R> accept(v: Visitor<R>): R
}