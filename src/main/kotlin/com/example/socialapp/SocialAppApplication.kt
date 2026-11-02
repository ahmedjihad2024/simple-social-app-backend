package com.example.socialapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration


@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
@EnableScheduling
@EnableCaching
class SocialAppApplication

fun main(args: Array<String>) {
    runApplication<SocialAppApplication>(*args)
}
