package com.joelburton.mothlang.interpreter

import com.joelburton.mothlang.scanner.Token

class LoxInstance(val klass: LoxClass) {
    private val fields = HashMap<String, Any?>()

    fun get(name: Token): Any? {
        if (name.lexeme in fields) return fields[name.lexeme]
        val method = klass.findMethod(name.lexeme)
        if (method != null) return method.bind(this)
        throw Interpreter.RuntimeError(
            name, "Undefined property '${name.lexeme}'.")
    }

    fun set(name: Token, value: Any?) {
        fields[name.lexeme] = value
    }

    override fun toString() = "${klass.name} instance"
}