package dev.prasadgaikwad.langchain4jdemo.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoteToolTest {

    private final NoteTool noteTool = new NoteTool(100);

    @Test
    void notesAreScopedPerConversation() {
        noteTool.saveNote("conv-a", "remember to ship");
        noteTool.saveNote("conv-b", "buy milk");
        noteTool.saveNote("conv-a", "pick up dry cleaning");

        assertThat(noteTool.listNotes("conv-a")).contains("1. remember to ship", "2. pick up dry cleaning")
                .doesNotContain("buy milk");
        assertThat(noteTool.listNotes("conv-b")).contains("buy milk");
    }

    @Test
    void emptyConversationHasNoNotes() {
        assertThat(noteTool.listNotes("fresh")).isEqualTo("No notes saved in this conversation yet.");
    }

    @Test
    void leastRecentlyUsedConversationIsEvictedPastTheCap() {
        NoteTool capped = new NoteTool(2);

        capped.saveNote("conv-1", "a");
        capped.saveNote("conv-2", "b");
        capped.listNotes("conv-1");
        capped.saveNote("conv-3", "c");

        assertThat(capped.listNotes("conv-1")).contains("a");
        assertThat(capped.listNotes("conv-2")).isEqualTo("No notes saved in this conversation yet.");
        assertThat(capped.listNotes("conv-3")).contains("c");
    }
}
