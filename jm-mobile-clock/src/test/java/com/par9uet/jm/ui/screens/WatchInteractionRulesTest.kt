package com.par9uet.jm.ui.screens

import com.par9uet.jm.data.models.TagFilterLogic
import com.par9uet.jm.ui.screens.readScreen.parseWatchReaderPage
import com.par9uet.jm.ui.viewModel.CollectComicLocalFilter
import org.junit.Assert.*
import org.junit.Test

class WatchInteractionRulesTest {
    @Test fun directPageEntryRejectsInvalidAndOutOfRangePages() {
        listOf("", "0", "-1", "81", "abc", "999999999999999").forEach {
            assertNull("$it must not turn a page", parseWatchReaderPage(it, 80))
        }
        assertNull(parseWatchReaderPage("1", 0))
    }
    @Test fun directPageEntryUsesHumanPageNumbers() {
        assertEquals(0, parseWatchReaderPage(" 1 ", 80))
        assertEquals(79, parseWatchReaderPage("80", 80))
        assertEquals(8, parseWatchReaderPage("009", 80))
    }
    @Test fun selectedConditionsRemainRemovableWhenMetadataIsMissing() {
        val choices = watchFilterOptions(mapOf("existing" to 8), setOf("saved tag"), "", true)
        assertEquals(listOf(WatchFilterOption("saved tag", 0)), choices)
    }
    @Test fun tagSearchIgnoresCaseAndWhitespaceAndDoesNotMutateSelections() {
        val selected = setOf("Moon")
        val choices = watchFilterOptions(mapOf("Moon" to 9, "Moonlight" to 4, "Sun" to 8), selected, " MOON ", false)
        assertEquals(listOf("Moon", "Moonlight"), choices.map { it.label })
        assertEquals(setOf("Moon"), selected)
        assertEquals(emptyList<WatchFilterOption>(), watchFilterOptions(mapOf("Sun" to 1), selected, "absent", true))
    }
    @Test fun selectingRowsDoesNotReorderTheResults() {
        val counts = mapOf("B" to 9, "A" to 2, "C" to 2)
        assertEquals(watchFilterOptions(counts, emptySet(), "", false), watchFilterOptions(counts, setOf("C"), "", false))
    }
    @Test fun clearingDraftDoesNotClearSearchOrChangeMatchRuleOrOriginal() {
        val original = CollectComicLocalFilter(searchText = "author", selectedTags = setOf("A"),
            selectedAuthors = setOf("B"), selectedRoles = setOf("C"), selectedTypes = setOf("D"), tagLogic = TagFilterLogic.NOT)
        val cleared = original.withoutTagSelections()
        assertEquals(4, original.selectionCount())
        assertEquals(0, cleared.selectionCount())
        assertEquals("author", cleared.searchText)
        assertEquals(TagFilterLogic.NOT, cleared.tagLogic)
    }
}
