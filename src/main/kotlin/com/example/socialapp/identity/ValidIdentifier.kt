package com.example.socialapp.identity

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

/** Validates the field as an email or a phone number, depending on `app.auth.identifier`. */
@MustBeDocumented
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [IdentifierValidator::class])
annotation class ValidIdentifier(
    val message: String = "{validation.invalid_identifier}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class IdentifierValidator(
    private val properties: AuthProperties,
) : ConstraintValidator<ValidIdentifier, String?> {

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value.isNullOrBlank()) return true // @NotBlank reports this one
        if (IdentifierRules.isValid(properties.identifier, value)) return true

        val template = when (properties.identifier) {
            IdentifierType.EMAIL -> "{validation.invalid_email}"
            IdentifierType.PHONE -> "{validation.invalid_phone}"
        }
        context.disableDefaultConstraintViolation()
        context.buildConstraintViolationWithTemplate(template).addConstraintViolation()
        return false
    }
}
