package dev.tokishu.jdkit.button.internal

import dev.tokishu.jdkit.component.ComponentWrapper

data class ButtonWrapper(
    override val instance: Any,
    override val method: java.lang.reflect.Method
) : ComponentWrapper
