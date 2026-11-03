package com.example.socialapp.auth.senders.sms

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/** Dev only: prints the code instead of sending an SMS. Add a real provider (Twilio, Vonage…) for prod. */
@Component
@ConditionalOnProperty(name = ["app.sms.backend"], havingValue = "log", matchIfMissing = true)
class LogSmsSender : SmsSender {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendVerificationCode(target: String, code: String) {
        log.info("[SMS] Verification code for {} = {}", target, code)
    }
}
