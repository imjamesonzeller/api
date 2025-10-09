package com.jamesonzeller.api.config

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.services.calendar.Calendar
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GoogleCalendarConfig(
    @Value("\${GOOGLE_SERVICE_ACCOUNT_JSON}") private val serviceAccountJson: String
) {
    companion object {
        private val SCOPES = listOf("https://www.googleapis.com/auth/calendar.readonly")
    }

    @Bean
    fun googleCredentials(): GoogleCredentials =
        ServiceAccountCredentials
            .fromStream(serviceAccountJson.byteInputStream())
            .createScoped(SCOPES)

    @Bean
    fun httpRequestInitializer(credentials: GoogleCredentials): HttpRequestInitializer =
        HttpCredentialsAdapter(credentials)

    @Bean
    fun calendarService(hri: HttpRequestInitializer): Calendar =
        Calendar.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            hri
        )
            .setApplicationName("Calendar Events")
            .build()
}