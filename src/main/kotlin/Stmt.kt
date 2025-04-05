abstract class Stmt {
    interface Visitor<R> {
        fun visitExpressionStmt(expr: Expression): R
        fun visitPrintStmt(stmt: Print): R
        fun visitVarStmt(stmt: Var): R
        fun visitBlockStmt(stmt: Block): R
        fun visitIfStmt(stmt: If): R
        fun visitWhileStmt(stmt: While): R
        fun visitFunctionStmt(stmt: Function): R
        fun visitReturnStmt(stmt: Return): R
    }

    class Expression(val expression: Expr) : Stmt() {
        override fun <R> accept(v: Visitor<R>): R = v.visitExpressionStmt(this)
    }

    class Print(val expression: Expr) : Stmt() {
        override fun <R> accept(v: Visitor<R>): R = v.visitPrintStmt(this)
    }

    class Var(val name: Token, val initializer: Expr?) : Stmt() {
        override fun <R> accept(v: Visitor<R>): R = v.visitVarStmt(this)
    }

    class Block(val statements: List<Stmt?>) : Stmt() {
        override fun <R> accept(v: Visitor<R>): R = v.visitBlockStmt(this)
    }

    class If(
        val condition: Expr,
        val thenBranch: Stmt,
        val elseBranch: Stmt?,
    ) :
        Stmt() {
        override fun <R> accept(v: Visitor<R>): R = v.visitIfStmt(this)
    }

    class While(val condition: Expr, val body: Stmt) : Stmt() {
        override fun <R> accept(v: Visitor<R>): R = v.visitWhileStmt(this)
    }

    class Function(
        val name: Token, val params: List<Token>, val body: List<Stmt?>,
    ) : Stmt() {
        override fun <R> accept(v: Visitor<R>): R =
            v.visitFunctionStmt(this)
    }

    class Return(val keyword: Token, val value: Expr?) : Stmt() {
        override fun <R> accept(v: Visitor<R>): R = v.visitReturnStmt(this)
    }

    abstract fun <R> accept(v: Visitor<R>): R
}

