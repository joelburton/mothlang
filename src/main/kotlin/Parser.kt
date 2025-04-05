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

    private fun declaration(): Stmt? {
        return try {
            if (match(FUN)) function("function")
            else if (match(VAR)) varDeclaration() else statement()
        } catch (_: ParseError) {
            synchronize()
            null
        }
    }

    private fun function(kind: String): Stmt.Function {
        val name = consume(IDENTIFIER, "Expect $kind name.")
        consume(LEFT_PAREN, "Expect '(' after $kind name.")
        val parameters = mutableListOf<Token>()
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size >= 255) {
                    error(currTok, "Can't have more than 255 parameters.")
                }
                parameters.add(consume(IDENTIFIER, "Expect parameter name."))
            } while (match(COMMA))
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters")

        consume(LEFT_BRACE, "Expect '{' before $kind body")
        val body = block()
        return Stmt.Function(name, parameters, body)
    }

    private fun varDeclaration(): Stmt {
        val name = consume(IDENTIFIER, "Expect variable name.")
        var initializer: Expr? = null
        if (match(EQUAL)) initializer = expression()
        consume(SEMICOLON, "Expect ';' after variable declaration.")
        return Stmt.Var(name, initializer)
    }

    private fun statement(): Stmt =
        when {
            match(IF) -> ifStatement()
            match(FOR) -> forStatement()
            match(PRINT) -> printStatement()
            match(WHILE) -> whileStatement()
            match(LEFT_BRACE) -> Stmt.Block(block())
            match(RETURN) -> retunStatement()
            else -> expressionStatement()
        }

    private fun retunStatement(): Stmt {
        val keyword = prevTok
        val value = if (!check(SEMICOLON)) expression() else null
        consume(SEMICOLON, "Expect ';' after return value.")
        return Stmt.Return(keyword, value)
    }
    
    private fun forStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'for'.")
        val initializer: Stmt? = when {
            match(SEMICOLON) -> null
            match(VAR) -> varDeclaration()
            else -> expressionStatement()
        }
        val condition = if (!check(SEMICOLON)) expression() else null
        consume(SEMICOLON, "Expect ';' after loop condition.")
        val increment = if (!check(RIGHT_PAREN)) expression() else null
        consume(RIGHT_PAREN, "Expect ')' after for clauses.")
        var body = statement()
        if (increment != null)
            body = Stmt.Block(listOf(body, Stmt.Expression(increment)))
        if (condition != null)
            body = Stmt.While(condition, body)
        if (initializer != null)
            body = Stmt.Block(listOf(initializer, body))
        return body
    }

    private fun whileStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'while'.")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after condition.")
        val body = statement()
        return Stmt.While(condition, body)
    }

    private fun ifStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'if'.")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after if condition.")
        val thenBranch = statement()
        val elseBranch = if (match(ELSE)) statement() else null
        return Stmt.If(condition, thenBranch, elseBranch)
    }
    
    private fun printStatement(): Stmt {
        val value = expression()
        consume(SEMICOLON, "Expect ';' after value.")
        return Stmt.Print(value)
    }

    private fun block(): List<Stmt?> {
        val statements = mutableListOf<Stmt?>()
        while (!check(RIGHT_BRACE) && !isAtEnd) statements.add(declaration())
        consume(RIGHT_BRACE, "Expect '}' after block.")
        return statements
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        consume(SEMICOLON, "Expect ';' after expression.")
        return Stmt.Expression(expr)
    }

    private fun expression(): Expr = assignment()

    private fun assignment(): Expr {
        val expr = or()
        if (match(EQUAL)) {
            val equals = prevTok
            val value = assignment()

            if (expr is Expr.Variable) {
                return Expr.Assign(expr.name, value)
            }
            error(equals, "Invalid assignment target.")
        }
        return expr
    }

    private fun or(): Expr {
        var expr = and()
        while (match(OR)) {
            val operator = prevTok
            val right = and()
            expr = Expr.Logical(expr, operator, right)
        }
        return expr
    }

    private fun and(): Expr {
        var expr = equality()
        while (match(AND)) {
            val operator = prevTok
            val right = equality()
            expr = Expr.Logical(expr, operator, right)
        }
        return expr
    }

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
            call()
        }

    private fun call(): Expr {
        var expr = primary()
        while (true) {
            if (match(LEFT_PAREN)) expr = finishCall(expr)
            else break;
        }
        return expr
    }

    private fun finishCall(callee: Expr): Expr {
        val arguments = mutableListOf<Expr?>()
        if (!check(RIGHT_PAREN)) {
            do {
                arguments.add(expression())
                if (arguments.size >= 255) {
                    error(currTok, "Can't have more than 255 arguments.")
                }
            } while (match(COMMA)) }
        val paren = consume(RIGHT_PAREN, "Expect ')' after arguments.")
        return Expr.Call(callee, paren, arguments)
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
            match(IDENTIFIER) -> Expr.Variable(prevTok)

            else -> throw ParseError("Unexpected token: ${currTok}")
        }

    @Suppress("SameParameterValue")
    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(currTok, message)
    }

    private fun error(token: Token, message: String): ParseError {
        loxError(token, message)
        return ParseError(message)
    }

    private class ParseError(msg :String) : RuntimeException(msg)

    @Suppress("unused")
    private fun synchronize() {
        advance()
        if (prevTok.type == SEMICOLON) return
        when (currTok.type) {
            CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> return
            else -> advance()
        }
    }

    fun parse(): List<Stmt?> {
        val statements = mutableListOf<Stmt?>()
        while (!isAtEnd) statements.add(declaration())
        return statements
    }
}
