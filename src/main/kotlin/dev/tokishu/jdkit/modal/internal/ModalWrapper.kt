package dev.tokishu.jdkit.modal.internal

data class ModalWrapper(
    override val instance: Any,
    override val method: java.lang.reflect.Method
) : dev.tokishu.jdkit.component.ComponentWrapper
