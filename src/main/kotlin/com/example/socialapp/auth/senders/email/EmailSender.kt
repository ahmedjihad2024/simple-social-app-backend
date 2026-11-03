package com.example.socialapp.auth.senders.email

import com.example.socialapp.identity.IdentifierType
import com.example.socialapp.auth.senders.OtpSender
import org.springframework.scheduling.annotation.Async

interface EmailSender : OtpSender {
    override val type: IdentifierType get() = IdentifierType.EMAIL

    @Async("emailExecutor")
    override fun sendVerificationCode(target: String, code: String)
}
