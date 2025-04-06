package com.joelburton.mothlang.interpreter

import com.joelburton.mothlang.scanner.Token

class Environment(val enclosing: Environment? = null) {
    class UndefinedVarError(name: Token) :
        Interpreter.RuntimeError(name, "Undefined variable '${name.lexeme}'")

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
        else throw Interpreter.RuntimeError(
            name,
            "Undefined variable '${name.lexeme}'"
        )
    }

    fun getAt(distance: Int, name: String) =
        ancestor(distance)!!.values.get(name)

    fun assignAt(distance: Int, name: Token, value: Any?) {
        ancestor(distance)!!.values[name.lexeme] = value
    }

    private fun ancestor(distance: Int): Environment? {
        var environment: Environment? = this
        for (i in 0 until distance) environment = environment?.enclosing
        return environment
    }
}