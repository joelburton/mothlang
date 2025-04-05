import Interpreter.RuntimeError

class Environment(val enclosing: Environment? = null) {
    class UndefinedVarError(name: Token) :
        RuntimeError(name, "Undefined variable '${name.lexeme}'")

    private val values: MutableMap<String, Any?> = mutableMapOf()

    operator fun set(name: String, value: Any?) =
        values.put(name, value)

    operator fun get(name: Token): Any? =
        if (name.lexeme in values) values[name.lexeme]
        else if (enclosing != null) enclosing[name]
        else throw UndefinedVarError(name)

    fun assign(name: Token, value: Any?) {
        if (name.lexeme in values) values[name.lexeme] = value
        else if (enclosing != null) enclosing.assign(name, value)
        else throw RuntimeError(name, "Undefined variable '${name.lexeme}'")
    }
}