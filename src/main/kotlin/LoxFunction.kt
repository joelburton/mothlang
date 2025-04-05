import Interpreter.Return

class LoxFunction(
    val declaration: Stmt.Function, val closure: Environment,
) : ILoxCallable {
    override fun call(interp: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        for (i in declaration.params.indices) {
            environment[declaration.params[i].lexeme] = arguments[i]
        }
        try {
            interp.executeBlock(declaration.body, environment)
        } catch (rv: Return) {
            return rv.value
        }
        return null
    }

    override val arity: Int = declaration.params.size
    override fun toString(): String = "<fn ${declaration.name.lexeme}>"
}