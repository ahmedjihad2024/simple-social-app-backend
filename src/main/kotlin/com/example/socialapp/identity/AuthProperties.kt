package com.example.socialapp.identity

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/** `app.auth.identifier=email|phone` — the single switch between email and phone auth. */
@ConfigurationProperties("app.auth")
data class AuthProperties(
    val identifier: IdentifierType = IdentifierType.EMAIL,
)

@Configuration
@EnableConfigurationProperties(AuthProperties::class)
class AuthPropertiesConfig
