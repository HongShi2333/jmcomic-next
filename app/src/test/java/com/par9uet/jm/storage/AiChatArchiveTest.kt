package com.par9uet.jm.storage

import com.google.gson.Gson
import com.par9uet.jm.data.models.AiChatConversation
import com.par9uet.jm.data.models.AiChatMessage
import com.par9uet.jm.data.models.AiChatMessageBranch
import com.par9uet.jm.data.models.switchBranch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiChatArchiveTest {
    private val gson = Gson()

    @Test
    fun longConversationRoundTripsWithoutDroppingMessages() {
        val original = AiChatConversation(
            id = "large",
            messages = (0 until 10_000).map { AiChatMessage(id = "$it", role = if (it % 2 == 0) "user" else "assistant", content = "message $it") },
        )
        val restored = roundTrip(original)
        assertEquals(original.messages, restored.messages)
    }

    @Test
    fun legacyActiveTailCopiesAreNotSerializedAgain() {
        val reply = AiChatMessage(id = "answer", role = "assistant", content = "answer")
        val question = AiChatMessage(
            id = "question", role = "user", content = "question",
            branches = listOf(AiChatMessageBranch("question", listOf(reply))),
        )
        val archive = AiChatConversation(id = "one", messages = listOf(question, reply)).toArchive()
        assertEquals(2, archive.nodes.size)
        assertTrue(archive.nodes[0].branches[0].followingRefs.isEmpty())
        assertEquals("answer", archive.toConversation().messages[1].content)
    }

    @Test
    fun alternateBranchAndCurrentReplySurviveSwitchingAndStorage() {
        val original = AiChatConversation(id = "branches", messages = listOf(
            AiChatMessage(
                id = "question", role = "user", content = "new",
                branches = listOf(
                    AiChatMessageBranch("old", listOf(AiChatMessage(id = "old-answer", role = "assistant", content = "old answer"))),
                    AiChatMessageBranch("new"),
                ),
                activeBranchIndex = 1,
            ),
            AiChatMessage(id = "new-answer", role = "assistant", content = "new answer"),
        ))
        val old = roundTrip(original).switchBranch("question", 0)
        assertEquals(listOf("old", "old answer"), old.messages.map { it.content })
        val new = roundTrip(old).switchBranch("question", 1)
        assertEquals(listOf("new", "new answer"), new.messages.map { it.content })
        assertTrue(new.messages[0].branches[1].followingMessages.isEmpty())
    }

    @Test
    fun deepInactiveBranchesDoNotUseRecursiveGsonModels() {
        var message = AiChatMessage(id = "leaf", role = "assistant", content = "leaf")
        repeat(2_000) { depth ->
            message = AiChatMessage(
                id = "$depth", role = "user", content = "active",
                branches = listOf(AiChatMessageBranch("active"), AiChatMessageBranch("alternate", listOf(message))),
            )
        }
        val restored = roundTrip(AiChatConversation(id = "deep", messages = listOf(message)))
        var current = restored.messages.single()
        repeat(2_000) { current = current.branches[1].followingMessages.single() }
        assertEquals("leaf", current.content)
    }

    @Test(expected = IllegalArgumentException::class)
    fun brokenReferencesAreRejectedInsteadOfReplacingHistory() {
        AiChatArchive("bad", "bad", 0, "", listOf(1), emptyList()).toConversation()
    }

    private fun roundTrip(conversation: AiChatConversation): AiChatConversation =
        gson.fromJson(gson.toJson(conversation.toArchive()), AiChatArchive::class.java).toConversation()
}
