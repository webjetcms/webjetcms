package sk.iway.iwcm.rag.indexing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.rag.indexing.SlidingWindowChunker.Chunk;

/** Verifies Markdown block preservation, heading context, and bounded content coverage. */
class MarkdownChunkerTest {
    private final MarkdownContentExtractor extractor = new MarkdownContentExtractor();
    private final MarkdownChunker chunker = new MarkdownChunker(extractor, new SlidingWindowChunker());

    /** Fitting sections stay whole above the target size; larger sections preserve individual blocks. */
    @Test
    void preservesFittingBlocks() {
        List<String> blocks = List.of(
            "Read these settings before enabling registration. Changes apply to all new accounts.",
            "- **Allow registration**: Enable self-registration. Leave disabled for protected groups.\n"
                + "\n  ## Access\n\n  Review access first.\n  - Confirm the address before granting access.",
            "- **Discount**: Set the percentage used by reservations.",
            "```java\nif (enabled) {\n    registerUser();\n}\n```",
            "Setting | Default\n- | -\nApproval | Required\nDiscount | 0"
        );
        List<Chunk> chunks = chunker.chunkWithOffsets(String.join("\n\n", blocks), 140, 0);

        assertTrue(chunks.size() > 1);
        assertTrue(chunks.stream().allMatch(chunk -> chunk.text().length() <= 210));
        for (String block : blocks) {
            assertTrue(chunks.stream().anyMatch(chunk -> chunk.text().contains(block)), "Block must remain intact: " + block);
        }
        String section = "## Settings\n\n" + String.join("\n\n", blocks);
        assertEquals(List.of(new Chunk(0, section)), chunker.chunkWithOffsets(section, section.length() - 100, 20));
    }

    /** Short sibling sections merge with shared context; larger sections and separate roots keep their boundaries. */
    @Test
    void preservesSectionBoundariesAndOffsets() {
        String users = "## Users\n\nUser settings.\n\n### Personal data\n\nName and surname.\n\n"
            + "### Access\n\n```markdown\n## Example\n```\n\nEmail and password.";
        String pages = "Pages\n-----\n\n### Details\n\nKeep the discount at zero.";
        String text = "# Guide\n\nOverview.\n\n" + users + "\n\n" + pages;
        List<Chunk> chunks = chunker.chunkWithOffsets(" \r\n" + text.replace("\n", "\r\n") + "\r\n ", 100, 20);
        List<String> contextual = extractor.addHeadingContext(text, "Guide", chunks);

        assertEquals(List.of("# Guide\n\nOverview.", users, pages), chunks.stream().map(Chunk::text).toList());
        assertEquals("Guide > Users\n\n" + users, contextual.get(1));
        for (Chunk chunk : chunks) {
            assertEquals(chunk.text(), text.substring(chunk.startOffset(), chunk.startOffset() + chunk.text().length()));
        }
        String details = users.substring(users.indexOf("### Personal data"));
        String largeSection = "## Users\n\n" + "x".repeat(130) + "\n\n" + details;
        List<Chunk> packed = chunker.chunkWithOffsets(largeSection, 100, 20);
        assertEquals(List.of("## Users\n\n" + "x".repeat(130), details), packed.stream().map(Chunk::text).toList());
        assertEquals("Guide > Users\n\n" + details, extractor.addHeadingContext(largeSection, "Guide", packed).get(1));

        String shortSections = "# Guide\n\n## Contact\n\n" + "c".repeat(35)
            + "\n\nOptional\n--------\n\nDetails.\n\n## Groups\n\n" + "g".repeat(35);
        String other = "# Other guide\n\nRemain separate.";
        String siblings = shortSections + "\n\n" + other;
        List<Chunk> merged = chunker.chunkWithOffsets(siblings, 200, 20);
        assertEquals(List.of(new Chunk(0, shortSections), new Chunk(shortSections.length() + 2, other)), merged);
        assertEquals("Guide\n\n" + shortSections, extractor.addHeadingContext(siblings, "Guide", merged).get(0));
    }

    /** Overlap may exceed its target for a whole paragraph, while size limits and forward progress still hold. */
    @Test
    void boundsOversizedContentAndOverlapsWholeBlocks() {
        List<String> paragraphs = IntStream.range(0, 4).mapToObj(i -> "Paragraph " + i + ": alpha beta gamma delta.").toList();
        String oversized = IntStream.range(0, 200).mapToObj(i -> "word" + i).collect(Collectors.joining(" "));
        String text = String.join("\n\n", paragraphs) + "\n\n" + oversized;
        List<Chunk> chunks = chunker.chunkWithOffsets(text, 80, 20);
        int previousStart = -1;
        int previousEnd = 0;
        boolean overlapsParagraph = false;
        for (Chunk chunk : chunks) {
            int start = chunk.startOffset();
            int end = start + chunk.text().length();
            assertTrue(chunk.text().isBlank() == false && chunk.text().length() <= 120);
            assertTrue(start > previousStart && end > previousEnd, "Each chunk must advance and add content");
            assertEquals(chunk.text(), text.substring(start, end));
            assertTrue(start <= previousEnd || text.substring(previousEnd, start).isBlank(), "No content may be skipped");
            if (start < previousEnd && start < text.indexOf(oversized)) {
                assertTrue(paragraphs.contains(text.substring(start, previousEnd).trim()), "Overlap must repeat a whole paragraph");
                overlapsParagraph = true;
            }
            previousStart = start;
            previousEnd = end;
        }
        assertTrue(overlapsParagraph);
        assertEquals(text.length(), previousEnd);
    }
}
