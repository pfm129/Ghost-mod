package dev.ghost.ai;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OpenAiProviderTest {
    @Test void parsesResponseTextAndCitations() throws Exception {
        var json = JsonParser.parseString("""
                {"output":[{"type":"message","content":[{"type":"output_text","text":"Olá!","annotations":[{"type":"url_citation","url":"https://example.org"}]}]}]}
                """).getAsJsonObject();
        AiReply reply = OpenAiProvider.parseReply(json);
        assertEquals("Olá!", reply.text());
        assertEquals("https://example.org", reply.sources().getFirst());
    }
    @Test void rejectsMissingText() {
        var json = JsonParser.parseString("{\"output\":[]}").getAsJsonObject();
        AiException ex = assertThrows(AiException.class, () -> OpenAiProvider.parseReply(json));
        assertEquals(AiException.Kind.FORMAT, ex.kind());
    }
}
