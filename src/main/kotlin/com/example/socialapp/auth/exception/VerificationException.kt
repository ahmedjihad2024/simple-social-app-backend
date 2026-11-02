package com.example.socialapp.auth.exception

import com.example.socialapp.common.exception.ApiException

sealed class VerificationException(message: String) : ApiException.BadRequest(message) {
    class InvalidCode : VerificationException("error.auth.verification_code_invalid")
    class CodeExpired : VerificationException("error.auth.verification_code_expired")
    class MaxAttemptsExceeded : VerificationException("error.auth.verification_code_max_attempts")
    class NoPendingCode : VerificationException("error.auth.no_pending_code")
}
