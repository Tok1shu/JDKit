package dev.tokishu.jdkit.button.internal

data class ButtonWrapper(
    override val instance: Any,
    override val method: java.lang.reflect.Method
) : dev.tokishu.jdkit.component.ComponentWrapper
