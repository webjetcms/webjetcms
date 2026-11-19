package sk.iway.iwcm.components.ai.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Checks the reference grammar and the rendering of the same instructions for different callers. */
class AiInstructionTemplateTest {

    @Test
    void rendersRepeatedReferencesInPlaceAndLeavesSkillLinksLiteralInSkillBodies() {
        var template = AiInstructionTemplate.parse("@skill(Výber kvízu) @tool(x.$-1) @group(g) @tool(x.$-1) @skill(Výber kvízu)");

        assertEquals(List.of("Výber kvízu"), List.copyOf(template.skillNames()));
        assertEquals(List.of("x.$-1"), List.copyOf(template.toolNames()));
        assertEquals(List.of("g"), List.copyOf(template.groupNames()));
        assertEquals("#Výber kvízu x.$-1 [$1] x.$-1 #Výber kvízu",
                template.render(name -> name, group -> "[$1]", name -> "#" + name));
        assertEquals("@skill(Výber kvízu) hidden [] hidden @skill(Výber kvízu)",
                template.render(name -> "hidden", group -> "[]", null));
    }

    @Test
    void preservesMalformedLinksAndResolvesNumericSkillNamesLiterally() {
        String malformed = "mail@example.com @tool() @group(two words) @skill()";
        assertFalse(AiInstructionTemplate.parse(malformed).hasReferences());
        var template = AiInstructionTemplate.parse(malformed + " @skill(0042) @skill(42)");
        assertEquals(List.of("0042", "42"), List.copyOf(template.skillNames()));
    }

    @Test
    void readsSkillNamesWithSpecialCharactersInsideBrowserJson() throws Exception {
        String name = "Kvíz (SK) \"50%\" \\ $1";
        String token = AiSkillLink.token(name);
        String json = "{\"context\":\"" + token + " / @skill(Other) @group(quiz) @tool(read) " + token + "\"}";
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
        assertEquals(List.of(name, "Other"), List.copyOf(AiInstructionTemplate.parse(json).skillNames()));
    }
}
