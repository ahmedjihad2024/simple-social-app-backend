package com.example.socialapp.admin

import com.example.socialapp.account.repository.AccountRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class AdminBootstrapper(
    @Value("\${app.admin.emails:}") private val adminEmails: List<String>,
    private val accountRepository: AccountRepository,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        // Reserved for bootstrapping admin accounts by email when needed
    }
}
