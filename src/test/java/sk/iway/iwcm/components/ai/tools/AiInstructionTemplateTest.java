package sk.iway.iwcm.components.ai.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Checks the reference grammar and the rendering of the same instructions for different callers. */
class AiInstructionTemplateTest {

    @Test
    void rendersRepeatedReferencesInPlaceAndLeavesSkillLinksLiteralInSkillBodies() {
        var template = AiInstructionTemplate.parse("@skill(04) @tool(x.$-1) @group(g) @tool(x.$-1) @skill(4)");

        assertEquals(List.of(4L), List.copyOf(template.skillIds()));
        assertEquals(List.of("x.$-1"), List.copyOf(template.toolNames()));
        assertEquals(List.of("g"), List.copyOf(template.groupNames()));
        assertEquals("#4 x.$-1 [$1] x.$-1 #4",
                template.render(name -> name, group -> "[$1]", id -> "#" + id));
        assertEquals("@skill(04) hidden [] hidden @skill(4)",
                template.render(name -> "hidden", group -> "[]", null));
    }

    @Test
    void preservesUnrecognizedReferencesAndTreatsOverflowAsAnUnavailableId() {
        String malformed = "mail@example.com @tool() @group(two words) @skill(-1) @skill(no)";
        assertFalse(AiInstructionTemplate.parse(malformed).hasReferences());
        var template = AiInstructionTemplate.parse(malformed + " @skill(9223372036854775808)");
        assertEquals(List.of(-1L), List.copyOf(template.skillIds()));
        assertEquals(malformed + " [-1]", template.render(name -> name, group -> group, id -> "[" + id + "]"));
    }
}
