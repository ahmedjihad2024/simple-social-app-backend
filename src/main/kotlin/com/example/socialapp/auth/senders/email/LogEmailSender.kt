package com.example.socialapp.auth.senders.email

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.mail.backend"], havingValue = "log", matchIfMissing = true)
class LogEmailSender : EmailSender {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendVerificationCode(target: String, code: String) {
        log.info("[EMAIL] Verification code for {} = {}", target, code)
    }
}
