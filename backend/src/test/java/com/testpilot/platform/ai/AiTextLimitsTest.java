package com.testpilot.platform.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class AiTextLimitsTest {

    @Test
    void truncatesOnlyWhenOverLimit() {
        assertEquals("abc", AiTextLimits.truncate("abc", 10));
        assertEquals("ab", AiTextLimits.truncate("abc", 2));
        assertNull(AiTextLimits.truncate(null, 10));
        assertEquals("abc", AiTextLimits.truncate("abc", 0));
    }

    @Test
    void truncateOrEmptyNeverReturnsNull() {
        assertEquals("", AiTextLimits.truncateOrEmpty(null, 10));
        assertEquals("a", AiTextLimits.truncateOrEmpty("ab", 1));
    }

    @Test
    void textFieldLimitsMatchDatabaseColumns() {
        assertEquals(255, AiTextLimits.MAX_TITLE_CHARS);
        assertEquals(128, AiTextLimits.MAX_MODULE_CHARS);
        assertEquals(500, AiTextLimits.MAX_TAGS_CHARS);
        assertEquals(20000, AiTextLimits.DEFAULT_MAX_CONTEXT_CHARS);
    }
}
