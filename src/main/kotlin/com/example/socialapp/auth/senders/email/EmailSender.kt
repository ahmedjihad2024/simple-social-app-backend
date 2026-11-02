package com.example.socialapp.auth.senders.email

import org.springframework.scheduling.annotation.Async

interface EmailSender {
    @Async("emailExecutor")
    fun sendVerificationCode(email: String, code: String)
}
