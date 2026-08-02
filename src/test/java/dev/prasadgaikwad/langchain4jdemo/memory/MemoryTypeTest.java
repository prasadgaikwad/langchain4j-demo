package dev.prasadgaikwad.langchain4jdemo.memory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemoryTypeTest {

    @Test
    void fromLabelAcceptsLabelAndEnumNameCaseInsensitively() {
        assertThat(MemoryType.fromLabel("message-window")).isEqualTo(MemoryType.MESSAGE_WINDOW);
        assertThat(MemoryType.fromLabel("MESSAGE_WINDOW")).isEqualTo(MemoryType.MESSAGE_WINDOW);
        assertThat(MemoryType.fromLabel("Token-Window")).isEqualTo(MemoryType.TOKEN_WINDOW);
    }

    @Test
    void fromLabelRejectsUnknownType() {
        assertThatThrownBy(() -> MemoryType.fromLabel("bogus"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bogus");
    }

    @Test
    void memoryIdEncodesTypeSoSwitchingTypesResetsConversation() {
        assertThat(MemoryType.MESSAGE_WINDOW.memoryId("main")).isEqualTo("message-window:main");
        assertThat(MemoryType.TOKEN_WINDOW.memoryId("main")).isEqualTo("token-window:main");
    }
}
