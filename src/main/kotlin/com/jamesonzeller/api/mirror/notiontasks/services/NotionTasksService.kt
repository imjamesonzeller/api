package com.jamesonzeller.api.mirror.notiontasks.services

import com.jamesonzeller.api.mirror.notiontasks.models.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class NotionTasksService(
    @Value("\${DB_ID}") private val dbid: String,
    @Value("\${NOTION_SECRET}") private val notionSecret: String
) {

     fun getTasks(): List<Task> {
         val filter = NotionFilter(
             filter = PropertyFilter(
                 property = "Done",
                 checkbox = CheckboxCondition(false)
             )
         )
         val request = NotionRequest(dbid, notionSecret, filter)
         val tasks = request.fetchTasks()
         return tasks
    }
}