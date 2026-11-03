package com.example.socialapp.auth.senders

import com.example.socialapp.identity.IdentifierType

/** Delivers a one-time code to an email address or a phone number. One implementation per channel. */
interface OtpSender {
    val type: IdentifierType

    fun sendVerificationCode(target: String, code: String)
}
