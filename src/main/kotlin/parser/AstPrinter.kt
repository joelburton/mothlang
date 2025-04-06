package parser//class AstPrinter : parser.Expr.Visitor<String> {
//    fun print(expr: parser.Expr?) = expr?.accept(this)
//
//    override fun visitBinaryExpr(expr: parser.Expr.Binary) =
//        parenthesize(expr.operator.lexeme, expr.left, expr.right)
//
//    override fun visitGroupingExpr(expr: parser.Expr.Grouping) =
//        parenthesize("group", expr.expression)
//
//    override fun visitLiteralExpr(expr: parser.Expr.Literal) =
//        expr.value.toString() // ?: "nil"
//
//    override fun visitUnaryExpr(expr: parser.Expr.Unary) =
//        parenthesize(expr.operator.lexeme, expr.right)
//
//    private fun parenthesize(name: String, vararg expressions: parser.Expr) =
//        expressions.joinToString(
//            prefix = "($name ", separator = " ", postfix = ")"
//        ) { it.accept(this) }
//}


//fun main() {
//    print(
//        AstPrinter().print(
//            parser.Expr.Binary(
//                parser.Expr.Unary(
//                    com.joelburton.mothlang.scanner.Token(scanner.TokenType.MINUS, "-", null, 1),
//                    parser.Expr.Literal(123)
//                ),
//                com.joelburton.mothlang.scanner.Token(scanner.TokenType.STAR, "*", null, 1),
//                parser.Expr.Grouping(parser.Expr.Literal(45.67))
//            )
//        )
//    )
//}