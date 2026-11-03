package com.example.socialapp.auth.controller

import com.example.socialapp.account.enums.Role
import com.example.socialapp.auth.AuthService
import com.example.socialapp.auth.dto.AuthResponse
import com.example.socialapp.auth.dto.LoginRequest
import com.example.socialapp.auth.dto.UserRegisterRequest
import com.example.socialapp.common.dto.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * User registration and login under /api/user/auth.
 * Email verification, token refresh, and logout are persona-agnostic and
 * stay shared in [AuthController] under /api/auth.
 */
@Tags(Tag(name = "User"), Tag(name = "Auth"))
@RestController
@RequestMapping("api/user/auth")
class UserAuthController(
    private val authService: AuthService,
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody body: UserRegisterRequest): ApiResponse<AuthResponse.SetUpRequired> =
        ApiResponse.ok(
            authService.register(
                body.firstName, body.lastName, body.identifier, body.password, Role.USER,
            )
        )

    @PostMapping("/login")
    fun login(@Valid @RequestBody body: LoginRequest): ApiResponse<AuthResponse> =
        ApiResponse.ok(authService.login(body.identifier, body.password, Role.USER))
}
