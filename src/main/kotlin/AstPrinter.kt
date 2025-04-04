class AstPrinter : Expr.Visitor<String> {
    fun print(expr: Expr?) = expr?.accept(this)

    override fun visitBinaryExpr(expr: Expr.Binary) =
        parenthesize(expr.operator.lexeme, expr.left, expr.right)

    override fun visitGroupingExpr(expr: Expr.Grouping) =
        parenthesize("group", expr.expression)

    override fun visitLiteralExpr(expr: Expr.Literal) =
        expr.value.toString() // ?: "nil"

    override fun visitUnaryExpr(expr: Expr.Unary) =
        parenthesize(expr.operator.lexeme, expr.right)

    private fun parenthesize(name: String, vararg expressions: Expr) =
        expressions.joinToString(
            prefix = "($name ", separator = " ", postfix = ")"
        ) { it.accept(this) }
}


//fun main() {
//    print(
//        AstPrinter().print(
//            Expr.Binary(
//                Expr.Unary(
//                    Token(TokenType.MINUS, "-", null, 1),
//                    Expr.Literal(123)
//                ),
//                Token(TokenType.STAR, "*", null, 1),
//                Expr.Grouping(Expr.Literal(45.67))
//            )
//        )
//    )
//}