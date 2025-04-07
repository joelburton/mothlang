package com.joelburton.mothlang.interpreter

import com.joelburton.mothlang.scanner.Token
import kotlin.random.Random

/** clock() -> returns # of microseconds since some epoch. */

object ClockFn : ILoxCallable {
    override val arity: Int = 0
    override fun call(
        interp: Interpreter, arguments: List<Any?>, name: Token,
    ): Any? {
        return System.currentTimeMillis() / 1000.0
    }

    override fun toString(): String = "<clock native fn>"
}

/** input(prompt) -> prints prompt and reads a line of input. */

object InputFn : ILoxCallable {
    override val arity: Int = 1
    override fun call(
        interp: Interpreter, arguments: List<Any?>, name: Token,
    ): Any? {
        val argument =
            arguments[0] as? String ?: throw Interpreter.RuntimeError(
                name, "Argument to 'prompt' must be a string"
            )
        print(argument)
        return readLine()
    }

    override fun toString(): String = "<input native fn>"
}

/** stringToNum(string) -> returns double or nil if it cannot be converted. */

object StringToNumFn : ILoxCallable {
    override val arity: Int = 1
    override fun call(
        interp: Interpreter,
        arguments: List<Any?>,
        name: Token,
    ): Any? {
        val argument =
            arguments[0] as? String ?: throw Interpreter.RuntimeError(
                name, "Argument to 'str' must be a string"
            )
        return argument.toDoubleOrNull()
    }
}

/** numToString(num) -> returns string of number. */

object NumToStringFn : ILoxCallable {
    override val arity: Int = 1
    override fun call(
        interp: Interpreter,
        arguments: List<Any?>,
        name: Token,
    ): Any? {
        val argument =
            arguments[0] as? Double ?: throw Interpreter.RuntimeError(
                name, "Argument 'num' must be a number"
            )
        return argument.toString().removeSuffix(".0")
    }
}

/** randomNum(1, 10) -> random integer (Double) between 1 and 10, inclusive. */

object RandomNumFn : ILoxCallable {
    override val arity: Int = 2
    override fun call(
        interp: Interpreter,
        arguments: List<Any?>,
        name: Token,
    ): Any? {
        val fromArg = arguments[0] as? Double
            ?: throw Interpreter.RuntimeError(name, "'from' must be a number")
        val toArg = arguments[1] as? Double
            ?: throw Interpreter.RuntimeError(name, "'to' must be a number")

        return try {
            Random.nextInt(fromArg.toInt(), toArg.toInt() + 1).toDouble()
        } catch (e: IllegalArgumentException) {
            throw Interpreter.RuntimeError(name, e.message ?: "Invalid range")
        }
    }
}