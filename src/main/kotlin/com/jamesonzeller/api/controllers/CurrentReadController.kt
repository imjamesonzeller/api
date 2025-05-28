package com.jamesonzeller.api.controllers

import com.jamesonzeller.api.currentread.services.CurrentReadService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/get_current_read")
class CurrentReadController(
    private val currentReadService: CurrentReadService
) {
    data class CurrentReadResponse(
        val currentRead: String
    )

    @GetMapping
    fun getCurrentRead(): CurrentReadResponse {
        val currentRead = currentReadService.getCurrentRead()
        return CurrentReadResponse(currentRead)
    }
}