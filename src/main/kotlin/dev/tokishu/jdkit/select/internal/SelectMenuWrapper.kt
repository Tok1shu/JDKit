package dev.tokishu.jdkit.select.internal

import dev.tokishu.jdkit.component.ComponentWrapper

data class SelectMenuWrapper(
    override val instance: Any,
    override val method: java.lang.reflect.Method
) : ComponentWrapper
