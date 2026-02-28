package dev.tokishu.jdkit.command.internal

import java.lang.reflect.Method

/**
 * Wrapper for an autocomplete method instance.
 */
data class AutocompleteWrapper(
    val instance: Any,
    val method: Method
)
