package com.par9uet.jm.storage

import com.par9uet.jm.data.models.AiChatConversation
import com.par9uet.jm.data.models.AiChatMessage
import com.par9uet.jm.data.models.AiChatMessageBranch
import java.util.IdentityHashMap

/** Flat references keep long branching histories out of Gson's recursive object traversal. */
internal data class AiChatArchive(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val personaId: String,
    val messageRefs: List<Int>,
    val nodes: List<AiMessageArchive>,
)

internal data class AiMessageArchive(
    val id: String,
    val role: String,
    val content: String,
    val createdAt: Long,
    val durationMs: Long?,
    val activeBranchIndex: Int,
    val branches: List<AiBranchArchive>,
)

internal data class AiBranchArchive(val content: String, val followingRefs: List<Int>)

internal fun AiChatConversation.toArchive(): AiChatArchive {
    val references = IdentityHashMap<AiChatMessage, Int>()
    val pending = mutableListOf<AiChatMessage>()
    fun ref(message: AiChatMessage): Int = references[message] ?: pending.size.also {
        references[message] = it
        pending += message
    }
    val root = messages.map(::ref)
    val nodes = mutableListOf<AiMessageArchive>()
    var index = 0
    while (index < pending.size) {
        val message = pending[index++]
        val active = message.activeBranchIndex.coerceIn(0, maxOf(0, message.branches.lastIndex))
        nodes += AiMessageArchive(
            message.id, message.role, message.content, message.createdAt, message.durationMs, active,
            message.branches.mapIndexed { branchIndex, branch ->
                AiBranchArchive(
                    if (branchIndex == active) message.content else branch.content,
                    // The active suffix already lives in the owning message list.
                    if (branchIndex == active) emptyList() else branch.followingMessages.map(::ref),
                )
            },
        )
    }
    return AiChatArchive(id, title, updatedAt, personaId, root, nodes)
}

internal fun AiChatArchive.toConversation(): AiChatConversation {
    val decoded = arrayOfNulls<AiChatMessage>(nodes.size)
    val visiting = BooleanArray(nodes.size)
    val stack = java.util.ArrayDeque<Pair<Int, Boolean>>()
    messageRefs.asReversed().forEach { stack.addLast(it to false) }
    while (stack.isNotEmpty()) {
        val (index, completing) = stack.removeLast()
        require(index in nodes.indices) { "聊天分支索引损坏" }
        if (decoded[index] != null) continue
        val node = nodes[index]
        if (!completing) {
            require(!visiting[index]) { "聊天分支循环引用" }
            visiting[index] = true
            stack.addLast(index to true)
            node.branches.asReversed().forEach { branch ->
                branch.followingRefs.asReversed().forEach { stack.addLast(it to false) }
            }
        } else {
            decoded[index] = AiChatMessage(
                node.id, node.role, node.content, node.createdAt, node.durationMs,
                node.branches.map { branch ->
                    AiChatMessageBranch(branch.content, branch.followingRefs.map { checkNotNull(decoded[it]) })
                },
                node.activeBranchIndex,
            )
            visiting[index] = false
        }
    }
    return AiChatConversation(id, title, messageRefs.map { checkNotNull(decoded[it]) }, updatedAt, personaId)
}
