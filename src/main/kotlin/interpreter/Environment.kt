package com.joelburton.mothlang.interpreter

import com.joelburton.mothlang.scanner.Token

/** Environment for variables.
 *
 * Each block starts a new environment (this is what makes `var x = 1` have
 * block scope). Each environment points to its enclosing environment; the
 * global scope has a null pointer for its enclosing environment.
 */
class Environment(val enclosing: Environment? = null) {

    class UndefinedVarError(name: Token) :
        Interpreter.RuntimeError(name, "Undefined variable '${name.lexeme}'")

    private val values: MutableMap<String, Any?> = mutableMapOf()

    /** Return ancestor environment [distance] up (0 is current environ) */
    private fun ancestor(distance: Int): Environment? {
        var environment: Environment? = this
        for (i in 0 until distance) environment = environment?.enclosing
        return environment
    }

    /** Set this variable in the current environment: `env["x"] = 1` */
    operator fun set(name: String, value: Any?) =
        values.put(name, value)

    /** Get from current environment, or recurse upward to find: `env["x"]` */
    operator fun get(name: Token): Any? =
        if (name.lexeme in values) values[name.lexeme]
        else if (enclosing != null) enclosing[name]
        else throw UndefinedVarError(name)

    /** Find the correct environ to set this (it must exist there). */
    fun assign(name: Token, value: Any?) {
        if (name.lexeme in values) values[name.lexeme] = value
        else if (enclosing != null) enclosing.assign(name, value)
        else throw Interpreter.RuntimeError(
            name,
            "Undefined variable '${name.lexeme}'")
    }

    /** Get value directly from ancestor [distance] up from here. */
    fun getAt(distance: Int, name: String) =
        ancestor(distance)!!.values[name]

    /** Set value directly in ancestor [distance] up from here. */
    fun assignAt(distance: Int, name: Token, value: Any?) {
        ancestor(distance)!!.values[name.lexeme] = value
    }
}