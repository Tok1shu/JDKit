package dev.tokishu.jdkit.command.internal

data class CommandWrapper(
    val instance: Any,
    val method: java.lang.reflect.Method
)