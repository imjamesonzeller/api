package com.jamesonzeller.api.security

import ApiKeyFilter
import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory.disable
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfig(
    @Value("\${API_KEY}") private val expectedApiKey: String
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/generate_word_search/**").permitAll()
                    .requestMatchers("/get_current_read").permitAll()
                    .requestMatchers("/error").permitAll()
                    .requestMatchers("/mirror/**").hasRole("API")
                    .anyRequest().authenticated()
            }
            .addFilterAt(apiKeyFilter(), UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }

    @Bean
    fun apiKeyFilter() = ApiKeyFilter(expectedApiKey)
}