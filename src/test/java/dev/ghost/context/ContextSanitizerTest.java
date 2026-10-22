package dev.ghost.context;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContextSanitizerTest {
    @Test void removesControlCharactersAndLimitsLength() {
        assertEquals("abc", ContextSanitizer.clean("a\u0000b\nc", 4).replace("\n", ""));
        assertEquals("abcd", ContextSanitizer.clean("abcdefgh", 4));
        assertEquals("", ContextSanitizer.clean(null, 80));
    }
}
