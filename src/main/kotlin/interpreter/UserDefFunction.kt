package com.joelburton.mothlang.interpreter

import com.joelburton.mothlang.interpreter.Interpreter.Return
import com.joelburton.mothlang.ast.Stmt
import com.joelburton.mothlang.scanner.Token

/** User-defined functions.
 *
 * User-defined functions implement the same [ILoxCallable] as builtins, and
 * are called the same way.
 *
 * These functions are first-class and support *proper* closures. Take that,
 * JavaScript.
 *
 * To handle returning (since it can be in a complex inner thing, like a
 * nested loop), a [Return] exception is thrown, and it is caught here,
 * and the value of the return is returned. Absent an explicit return,
 * null ("nil") is returned.
 */
class UserDefFunction(
    val declaration: Stmt.Function,
    val closure: Environment,
    val isInitializer: Boolean = false
) : ILoxCallable {

    override val arity: Int = declaration.params.size

    override fun call(
        interp: Interpreter, arguments: List<Any?>, name: Token): Any? {
        val environment = Environment(closure)
        for (i in declaration.params.indices)
            environment[declaration.params[i].lexeme] = arguments[i]

        try {
            interp.executeBlock(declaration.body, environment)
        } catch (rv: Return) {
            if (isInitializer) return closure.getAt(0, "this")
            return rv.value
        }
        if (isInitializer) return closure.getAt(0, "this")
        return null
    }

    fun bind(instance: LoxInstance): UserDefFunction {
        val environ = Environment(closure)
        environ["this"] = instance
        return UserDefFunction(declaration, environ, isInitializer)
    }

    override fun toString(): String = "<fn ${declaration.name.lexeme}>"
}