package com.example.socialapp.security.ratelimit.config

import com.example.socialapp.security.ratelimit.filter.GlobalRateLimitFilter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy

@Configuration
@EnableAspectJAutoProxy
@EnableConfigurationProperties(RateLimitProperties::class)
class RateLimitConfiguration {

    /**
     * Prevent Spring Boot from registering the filter directly
     * with the servlet container.
     *
     * Register it only inside the Spring Security filter chain.
     */
    @Bean
    fun disableGlobalRateLimitServletRegistration(
        filter: GlobalRateLimitFilter
    ): FilterRegistrationBean<GlobalRateLimitFilter> {

        return FilterRegistrationBean(filter).apply {
            isEnabled = false
        }
    }
}