package dev.ghost.chat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GhostPrefixTest {
    @Test void acceptsOnlyPrefixAtStartAndExtractsQuestion() {
        assertEquals("onde achar ferro?", GhostPrefix.extractQuestion("ghost, onde achar ferro?").orElseThrow());
        assertEquals("qual camada?", GhostPrefix.extractQuestion("GHOST: qual camada?").orElseThrow());
        assertEquals("explique redstone", GhostPrefix.extractQuestion("ghost explique redstone").orElseThrow());
        assertTrue(GhostPrefix.isGhostMessage("ghost"));
    }
    @Test void ignoresGhostInsideOtherTextAndPrefixLookalikes() {
        assertTrue(GhostPrefix.extractQuestion("vi um ghost ontem").isEmpty());
        assertTrue(GhostPrefix.extractQuestion("ghostly, oi").isEmpty());
        assertTrue(GhostPrefix.extractQuestion("alguém conhece o mod ghost?").isEmpty());
    }
}
