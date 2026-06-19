package com.par9uet.jm.data.models

data class AiChatMessage(
    val id: String = "",
    val role: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class AiChatConversation(
    val id: String = "",
    val title: String = "新对话",
    val messages: List<AiChatMessage> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)

