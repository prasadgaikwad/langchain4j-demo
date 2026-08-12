package dev.prasadgaikwad.langchain4jdemo.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoteToolTest {

    private final NoteTool noteTool = new NoteTool();

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
}
