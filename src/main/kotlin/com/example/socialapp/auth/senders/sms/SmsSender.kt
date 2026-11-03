package com.example.socialapp.auth.senders.sms

import com.example.socialapp.identity.IdentifierType
import com.example.socialapp.auth.senders.OtpSender
import org.springframework.scheduling.annotation.Async

interface SmsSender : OtpSender {
    override val type: IdentifierType get() = IdentifierType.PHONE

    @Async("smsExecutor")
    override fun sendVerificationCode(target: String, code: String)
}
