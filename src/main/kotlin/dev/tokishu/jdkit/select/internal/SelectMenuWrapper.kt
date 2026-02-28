package dev.tokishu.jdkit.select.internal

data class SelectMenuWrapper(
    override val instance: Any,
    override val method: java.lang.reflect.Method
) : dev.tokishu.jdkit.component.ComponentWrapper
