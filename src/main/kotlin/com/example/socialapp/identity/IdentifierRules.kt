import com.example.socialapp.identity.IdentifierType
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat

object IdentifierRules {
    private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    private val phoneUtil = PhoneNumberUtil.getInstance()

    fun normalize(type: IdentifierType, raw: String): String = when (type) {
        IdentifierType.EMAIL -> raw.trim().lowercase()
        IdentifierType.PHONE -> try {
            phoneUtil.format(phoneUtil.parse(raw.trim(), null), PhoneNumberFormat.E164)
        } catch (_: NumberParseException) {
            raw.trim()
        }
    }

    fun isValid(type: IdentifierType, raw: String): Boolean = when (type) {
        IdentifierType.EMAIL -> normalize(type, raw).let { it.length <= 300 && EMAIL_REGEX.matches(it) }
        IdentifierType.PHONE -> try {
            phoneUtil.isValidNumber(phoneUtil.parse(raw.trim(), null))
        } catch (_: NumberParseException) {
            false
        }
    }
}