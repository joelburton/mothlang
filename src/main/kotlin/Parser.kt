import Lox.Companion.loxError
import TokenType.*

class Parser(private val tokens: List<Token>) {
    private var current: Int = 0

    private val isAtEnd get() = currTok.type == EOF
    private val currTok get() = tokens[current]
    private val prevTok get() = tokens[current - 1]

    private fun match(vararg types: TokenType) =
        types.any { t -> check(t) && advance().let { true } }

    private fun check(type: TokenType) = !isAtEnd && currTok.type == type

    private fun advance(): Token {
        if (!isAtEnd) current++
        return prevTok
    }

    // precedence order of tokens: low->high

    private fun expression(): Expr = equality()

    private fun equality(): Expr {
        var expr = comparison()

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            val operator = prevTok
            val right = comparison()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun comparison(): Expr {
        var expr = term()

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            val operator = prevTok
            val right = term()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun term(): Expr {
        var expr = factor()
        while (match(MINUS, PLUS)) {
            val operator = prevTok
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun factor(): Expr {
        var expr = unary()

        while (match(SLASH, STAR)) {
            val operator = prevTok
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun unary(): Expr =
        if (match(BANG, MINUS)) {
            val operator = prevTok
            val right = unary()
            Expr.Unary(operator, right)
        } else {
            primary()
        }

    private fun primary(): Expr =
        when {
            match(FALSE) -> Expr.Literal(false)
            match(TRUE) -> Expr.Literal(true)
            match(NIL) -> Expr.Literal(null)
            match(NUMBER, STRING) -> Expr.Literal(prevTok.literal)
            match(LEFT_PAREN) -> {
                val expr = expression()
                consume(RIGHT_PAREN, "Expect ')' after expression.")
                Expr.Grouping(expr)
            }

            else -> throw Exception("Unexpected token: ${currTok}")
        }

    @Suppress("SameParameterValue")
    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(currTok, message)
    }

    private fun error(token: Token, message: String): ParseError {
        loxError(token, message)
        return ParseError()
    }

    private class ParseError : RuntimeException()

    @Suppress("unused")
    private fun synchronize() {
        advance()
        if (prevTok.type == SEMICOLON) return
        when (currTok.type) {
            CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> return
            else -> advance()
        }
    }

    fun parse(): Expr? {
        return try {
            expression()
        } catch (_: ParseError) {
            null
        }
    }
}
