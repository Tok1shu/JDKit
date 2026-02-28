package dev.tokishu.jdkit.modal

/**
 * Annotation to handle Modal form submissions.
 * Place this on a method with `ModalInteractionEvent` parameter.
 * 
 * @param id The exact ID or regex pattern of the modal.
 * @param regex True if the id acts as a regular expression pattern.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class JDKitModal(
    val id: String,
    val regex: Boolean = false
)
