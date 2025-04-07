package com.joelburton.mothlang.interpreter

import com.joelburton.mothlang.scanner.Token

class LoxClass(
    val name: String,
    private val superclass: LoxClass?,
    val methods: Map<String, UserDefFunction>,
) :
    ILoxCallable {
    override val arity: Int get() = findMethod("init")?.arity ?: 0
    
    override fun call(
        interp: Interpreter,
        arguments: List<Any?>,
        name: Token,
    ): Any? {
        val instance = LoxInstance(this)
        val initializer = findMethod("init")
        initializer?.bind(instance)?.call(interp, arguments, name)
        return instance
    }

    fun findMethod(name: String): UserDefFunction? {
        if (name in methods) return methods[name]
        if (superclass != null) return superclass.findMethod(name)
        return null
    }

    override fun toString(): String = name
}