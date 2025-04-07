package com.joelburton.mothlang.parser

import com.joelburton.mothlang.scanner.Token
import com.joelburton.mothlang.scanner.TokenType
import com.joelburton.mothlang.scanner.TokenType.*
import com.joelburton.mothlang.Lox.Companion.loxError
import com.joelburton.mothlang.ast.Expr
import com.joelburton.mothlang.ast.Stmt

/** Parses list of [Token] into a list of [Stmt].
 *
 * A straightforward LL(1) descending parser.
 */

class Parser(private val tokens: List<Token>) {
    /** Current location in tokens of parser. */
    private var current: Int = 0

    private val isAtEnd get() = currTok.type == EOF
    private val currTok get() = tokens[current]
    private val prevTok get() = tokens[current - 1]

    /** Given list of [TokenType], advance on match; else stop. Returns T/F. */
    private fun match(vararg types: TokenType) =
        types.any { t -> check(t) && advance().let { true } }

    /** Are we on a token of [type]? */
    private fun check(type: TokenType) = !isAtEnd && currTok.type == type

    /** Return current token & advance to next. */
    private fun advance(): Token {
        if (!isAtEnd) current++
        return prevTok
    }

    /** Expect curr token to be [type]; if so, advance. Else: throw err. */
    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(currTok, message)
    }

    // ================= error handling


    /** Prints error and returns error, which can be thrown. */
    private fun error(token: Token, message: String): ParseError {
        loxError(token, message)
        return ParseError(message)
    }

    /** Marker error which can be used to provide good error messages. */
    private class ParseError(msg :String) : RuntimeException(msg)

    /** "Synchronize" parser to a good error break point.
     *
     * When a parser error happens, we want:
     * - to not just stop parsing entirely with an error
     *   (that's annoying for users, since every time they run their program,
     *   they'll see only the first error and will have an annoying edit-run
     *   cycle)
     * - to not complain about everything being wrong after that point
     *   (otherwise, stuff like not closing an "if" condition might cause
     *   every following token to be an error, forever)
     *
     *   So, this searches forward for a good "error breakpoint" -- it will
     *   advance to that point before starting parsing again.
     *
     *   Of course, the program will be invalid and shouldn't be runnable;
     *   this is handled by the call to [loxError] above, which sets a flag
     *   to prevent moving forward to interpreter.
     */
    private fun synchronize() {
        advance()
        if (prevTok.type == SEMICOLON) return
        when (currTok.type) {
            CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> return
            else -> advance()
        }
    }

    // precedence order of tokens: low->high

    // ================== statements

    /**
     * declaration    ::= varDecl
     *                   | funDecl
     *                   | classDecl
     *                   | statement ;
     */
    private fun declaration(): Stmt? {
        return try {
            if (match(FUN)) function("function")
            else if (match(CLASS)) classDeclaration()
            else if (match(VAR)) varDeclaration()
            else statement()
        } catch (_: ParseError) {
            synchronize()
            null
        }
    }

    /**
     * funDecl        ::= "fun" function ;
     * function       ::= IDENTIFIER "(" parameters? ")" block ;
     * parameters     ::= IDENTIFIER ( "," IDENTIFIER )* ;
     */
    private fun function(@Suppress("SameParameterValue") kind: String): Stmt.Function {
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

    /**
     * classDecl ::= "class" IDENTIFIER ( "<" IDENTIFIER )? "{" function* "}" ;
     */
    private fun classDeclaration(): Stmt.Class {
        val name = consume(IDENTIFIER, "Expect class name.")

        val superclass = if (match(LESS)) {
            consume(IDENTIFIER, "Expect superclass name.")
            Expr.Variable(prevTok)
        } else {
            null
        }
        
        consume(LEFT_BRACE, "Expect '{' before class body.")
        val methods = mutableListOf<Stmt.Function>()
        while (!check(RIGHT_BRACE) && !isAtEnd) methods.add(function("method"))
        consume(RIGHT_BRACE, "Expect '}' after class body.")
        return Stmt.Class(name, superclass, methods)
    }

    /**
     * varDecl        ::= "var" IDENTIFIER ( "=" expression )? ";" ;
     */
    private fun varDeclaration(): Stmt {
        val name = consume(IDENTIFIER, "Expect variable name.")
        val initializer = if (match(EQUAL)) expression() else null
        consume(SEMICOLON, "Expect ';' after variable declaration.")
        return Stmt.Var(name, initializer)
    }

    /**
     * statement      ::= exprStmt
     *                    | forStmt
     *                    | ifStmt
     *                    | printStmt
     *                    | returnStmt
     *                    | whileStmt
     *                    | block ;
     */
    private fun statement(): Stmt =
        when {
            match(FOR) -> forStatement()
            match(IF) -> ifStatement()
            match(PRINT) -> printStatement()
            match(RETURN) -> returnStatement()
            match(WHILE) -> whileStatement()
            match(LEFT_BRACE) -> Stmt.Block(block())
            else -> expressionStatement()
        }

    /**
     * exprStmt       ::= expression ";" ;
     */
    private fun expressionStatement(): Stmt {
        val expr = expression()
        consume(SEMICOLON, "Expect ';' after expression.")
        return Stmt.Expression(expr)
    }

    /**
     * forStmt        ::= "for" "(" ( varDecl | exprStmt | ";" )
     *                       expression? ";"
     *                       expression? ")" statement
     */
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

        // "for" is a construct of the parser; much like in almost all similar
        // languages, any "for" loop can be written as a while loop, with the
        // initialization and incrementing happening directly.
        //
        // Add the requisite parts for a while loop with these features.

        if (increment != null)
            body = Stmt.Block(listOf(body, Stmt.Expression(increment)))
        if (condition != null)
            body = Stmt.While(condition, body)
        if (initializer != null)
            body = Stmt.Block(listOf(initializer, body))
        return body
    }

    /**
     * whileStmt      ::= "while" "(" expression ")" statement ;
     */
    private fun whileStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'while'.")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after condition.")
        val body = statement()
        return Stmt.While(condition, body)
    }

    /**
     * ifStmt         ::= "if" "(" expression ")" statement ( "else" statement )? ;
     */
    private fun ifStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'if'.")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after if condition.")

        val thenBranch = statement()
        val elseBranch = if (match(ELSE)) statement() else null

        return Stmt.If(condition, thenBranch, elseBranch)
    }

    /**
     * ifStmt         ::= "if" "(" expression ")" statement ( "else" statement )? ;
     */
    private fun printStatement(): Stmt {
        val value = expression()
        consume(SEMICOLON, "Expect ';' after value.")
        return Stmt.Print(value)
    }

    /**
     * returnStmt     ::= "return" expression? ";" ;
     */
    private fun returnStatement(): Stmt {
        val keyword = prevTok
        val value = if (!check(SEMICOLON)) expression() else null
        consume(SEMICOLON, "Expect ';' after return value.")
        return Stmt.Return(keyword, value)
    }

    /**
     * block          ::= "{" declaration* "}" ;
     */
    private fun block(): List<Stmt?> {
        val statements = mutableListOf<Stmt?>()
        while (!check(RIGHT_BRACE) && !isAtEnd) statements.add(declaration())
        consume(RIGHT_BRACE, "Expect '}' after block.")
        return statements
    }

    // ==================== expressions

    /**
     * expression     ::= assignment ;
     */
    private fun expression(): Expr = assignment()

    /**
     * assignment     ::= IDENTIFIER "=" assignment
     *                   | logic_or ;
     */
    private fun assignment(): Expr {
        val expr = or()
        if (match(EQUAL)) {
            val equals = prevTok
            val value = assignment()

            if (expr is Expr.Variable) {
                return Expr.Assign(expr.name, value)
            }
            else if (expr is Expr.Get) {
                return Expr.Set(expr.obj, expr.name, value)
            }
            error(equals, "Invalid assignment target.")
        }
        return expr
    }

    /**
     * logic_or       ::= logic_and ( "or" logic_and )* ;
     */
    private fun or(): Expr {
        var expr = and()
        while (match(OR)) {
            val operator = prevTok
            val right = and()
            expr = Expr.Logical(expr, operator, right)
        }
        return expr
    }

    /**
     * logic_and      ::= equality ( "and" equality )* ;
     */
    private fun and(): Expr {
        var expr = equality()
        while (match(AND)) {
            val operator = prevTok
            val right = equality()
            expr = Expr.Logical(expr, operator, right)
        }
        return expr
    }

    /**
     * equality       ::= comparison ( ( "!=" | "==" ) comparison )* ;
     */
    private fun equality(): Expr {
        var expr = comparison()

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            val operator = prevTok
            val right = comparison()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    /**
     * comparison     ::= term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
     */
    private fun comparison(): Expr {
        var expr = term()

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            val operator = prevTok
            val right = term()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    /**
     * term           ::= factor ( ( "-" | "+" ) factor )* ;
     */
    private fun term(): Expr {
        var expr = factor()
        while (match(MINUS, PLUS)) {
            val operator = prevTok
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    /**
     * factor         ::= unary ( ( "/" | "*" ) unary )* ;
     */
    private fun factor(): Expr {
        var expr = unary()

        while (match(SLASH, STAR)) {
            val operator = prevTok
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    /**
     * unary          ::= ( "!" | "-" ) unary
     *                    | call ;
     */
    private fun unary(): Expr =
        if (match(BANG, MINUS)) {
            val operator = prevTok
            val right = unary()
            Expr.Unary(operator, right)
        } else {
            call()
        }

    /**
     * call           ::= primary ( "(" arguments? ")" )* ;
     */
    private fun call(): Expr {
        var expr = primary()
        while (true) {
            if (match(LEFT_PAREN)) expr = finishCall(expr)
            else if (match(DOT)) {
                val name = consume(IDENTIFIER, "Expect property name after '.'.")
                expr = Expr.Get(expr, name)
            }
            else break
        }
        return expr
    }

    /**
     * arguments      ::= expression ( "," expression )* ;
     */
    private fun finishCall(callee: Expr): Expr {
        val arguments = mutableListOf<Expr>()
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

    /**
    * primary        ::= NUMBER | STRING | "true" | "false" | "nil"
    *                   | "(" expression ")"
    *                   | IDENTIFIER
    *                   | "super" "." IDENTIFIER
    *                   ;
     */
    private fun primary(): Expr {
        return when {
            match(FALSE) -> Expr.Literal(false)
            match(TRUE) -> Expr.Literal(true)
            match(NIL) -> Expr.Literal(null)
            match(NUMBER, STRING) -> Expr.Literal(prevTok.literal)
            match(LEFT_PAREN) -> {
                val expr = expression()
                consume(RIGHT_PAREN, "Expect ')' after expression.")
                Expr.Grouping(expr)
            }
            match(SUPER) -> {
                val keyword = prevTok
                consume(DOT, "Expect '.' after 'super'.")
                val method = consume(IDENTIFIER, "Expect superclass method name.")
                Expr.Super(keyword, method)
            }
            match(THIS) -> Expr.This(prevTok)
            match(IDENTIFIER) -> Expr.Variable(prevTok)

            else -> throw error(currTok, "Unexpected token.")
        }
    }

    /** Main entry: parse a program (as list of [Stmt]) */

    fun parse(): List<Stmt?> {
        val statements = mutableListOf<Stmt?>()
        while (!isAtEnd) statements.add(declaration())
        return statements
    }

    /** Alternative entry point for debugging: parse a single [Expr] */

    fun parseExpression() = expression()
}
