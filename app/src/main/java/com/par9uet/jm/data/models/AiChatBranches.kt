package com.par9uet.jm.data.models

internal fun AiChatConversation.withSyncedActiveBranch(): AiChatConversation {
    if (messages.none { it.branches.isNotEmpty() }) return this
    return copy(messages = messages.map { message ->
        if (message.branches.isEmpty()) message else {
            val active = message.activeBranchIndex.coerceIn(message.branches.indices)
            message.copy(
                activeBranchIndex = active,
                branches = message.branches.mapIndexed { index, branch ->
                    if (index == active) branch.copy(content = message.content, followingMessages = emptyList())
                    else branch
                },
            )
        }
    })
}

internal fun AiChatConversation.switchBranch(messageId: String, targetIndex: Int): AiChatConversation {
    val active = withSyncedActiveBranch()
    val userIndex = active.messages.indexOfFirst { it.id == messageId && it.role == "user" }
    if (userIndex < 0) return this
    val message = active.messages[userIndex]
    if (targetIndex !in message.branches.indices || targetIndex == message.activeBranchIndex) return this
    val target = message.branches[targetIndex]
    val switched = message.copy(
        content = target.content,
        activeBranchIndex = targetIndex,
        branches = message.branches.mapIndexed { index, branch ->
            when (index) {
                message.activeBranchIndex -> branch.copy(
                    content = message.content,
                    followingMessages = active.messages.drop(userIndex + 1),
                )
                targetIndex -> branch.copy(followingMessages = emptyList())
                else -> branch
            }
        },
    )
    return active.copy(
        messages = active.messages.take(userIndex) + switched + target.followingMessages,
        updatedAt = System.currentTimeMillis(),
    ).withSyncedActiveBranch()
}
