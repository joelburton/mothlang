/** Ancestor for all "statement" classes.
 *
 * See the notes on [Expr] to understand this class and the Visitor pattern.
 */

abstract class Stmt {

    /** Visitor methods that clients must handle. */
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

    /** A statement expression, like `1 + 2;` */
    class Expression(val expression: Expr) : Stmt() {
        override fun <R> accept(v: Visitor<R>): R = v.visitExpressionStmt(this)
    }

    /** Print is a statement, not a function: `print 1;` or `print foo;` */
    class Print(val expression: Expr) : Stmt() {
        override fun <R> accept(v: Visitor<R>): R = v.visitPrintStmt(this)
    }

    /** Declaring and possibly-defining a var: `var foo = 42;` */
    class Var(val name: Token, val initializer: Expr?) : Stmt() {
        override fun <R> accept(v: Visitor<R>): R = v.visitVarStmt(this)
    }

    /** A block comprises several statements: `{ var a = 1; print a; }` */
    class Block(val statements: List<Stmt?>) : Stmt() {
        override fun <R> accept(v: Visitor<R>): R = v.visitBlockStmt(this)
    }

    /** If statement: `if (x) { a(); }` or `if (x) { a(); } else { b(); }` */
    class If(
        val condition: Expr,
        val thenBranch: Stmt,
        val elseBranch: Stmt?,
    ) :
        Stmt() {
        override fun <R> accept(v: Visitor<R>): R = v.visitIfStmt(this)
    }

    /** While statement: `while (test) { a = a + 1; }` */
    class While(val condition: Expr, val body: Stmt) : Stmt() {
        override fun <R> accept(v: Visitor<R>): R = v.visitWhileStmt(this)
    }

    /** Function definition: `fun add(x, y) { return x + y; }` */
    class Function(
        val name: Token, val params: List<Token>, val body: List<Stmt?>,
    ) : Stmt() {
        override fun <R> accept(v: Visitor<R>): R =
            v.visitFunctionStmt(this)
    }

    /** Return statement: `return 42;` or `return;` */
    class Return(val keyword: Token, val value: Expr?) : Stmt() {
        override fun <R> accept(v: Visitor<R>): R = v.visitReturnStmt(this)
    }

    /** The double-dispatcher; see [Expr.accept]. */
    abstract fun <R> accept(v: Visitor<R>): R
}

