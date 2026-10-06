package sk.iway.iwcm.rag.indexing;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.rag.indexing.SlidingWindowChunker.Chunk;

/** Splits Markdown at block boundaries, preserving fitting structures and their heading context. */
@Component
public class MarkdownChunker {
    private static final Pattern LIST_ITEM = Pattern.compile("[ \\t]*(?:[-+*]|[0-9]+[.)])[ \\t]+.*");
    private static final Pattern TABLE_SEPARATOR = Pattern.compile("[ \\t]*\\|?[ \\t]*:?-+:?[ \\t]*(?:\\|[ \\t]*:?-+:?[ \\t]*)*\\|?[ \\t]*");

    private final MarkdownContentExtractor extractor;
    private final SlidingWindowChunker fallback;

    public MarkdownChunker(MarkdownContentExtractor extractor, SlidingWindowChunker fallback) {
        this.extractor = extractor;
        this.fallback = fallback;
    }

    /**
     * Splits cleaned Markdown using the configured size and overlap.
     *
     * @param text cleaned Markdown
     * @return chunks with source offsets compatible with heading-context extraction
     */
    public List<Chunk> chunkWithOffsets(String text) {
        return chunkWithOffsets(text, Constants.getInt("ragEmbeddingChunkSize"), Constants.getInt("ragEmbeddingChunkOverlap"));
    }

    /**
     * Groups fitting subsections and adjacent short level-two sections, preserving their structures.
     * Level-one headings remain boundaries; overlap resets between unmerged sections.
     * Blocks larger than the 50% size allowance use the ordinary bounded text splitter.
     *
     * @param text cleaned Markdown; code indentation is preserved
     * @param chunkSize target body size; nonpositive values disable splitting
     * @param overlap target overlap within a section; one whole trailing block may exceed it
     * @return passages with offsets in the normalized, trimmed source, excluding heading prefixes
     */
    public List<Chunk> chunkWithOffsets(String text, int chunkSize, int overlap) {
        if (text == null || text.isBlank()) return new ArrayList<>();
        text = text.replace("\r\n", "\n").replace('\r', '\n');
        if (chunkSize <= 0) return new ArrayList<>(List.of(new Chunk(0, text.trim())));
        int leadingWhitespace = skipLeadingWhitespace(text, 0, text.length());
        int maximum = (int) Math.min(Integer.MAX_VALUE, Math.max(2L, chunkSize + (long) chunkSize / 2));
        overlap = Math.max(0, Math.min(overlap, chunkSize - 1));
        List<Chunk> chunks = new ArrayList<>();
        List<Block> blocks = findBlocks(text);
        int sectionStart = 0;
        boolean hasContent = false;
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            if (block.heading() && block.headingLevel() <= 2 && hasContent) {
                int startOffset = blocks.get(sectionStart).start();
                if (block.headingLevel() == 2 && blocks.get(i - 1).end() - startOffset < chunkSize) {
                    int nextEnd = i + 1;
                    while (nextEnd < blocks.size()
                            && (blocks.get(nextEnd).heading() == false || blocks.get(nextEnd).headingLevel() > 2)) nextEnd++;
                    int endOffset = blocks.get(nextEnd - 1).end();
                    if (endOffset - block.start() < chunkSize && endOffset - startOffset <= maximum) continue;
                }
                addSection(text, blocks.subList(sectionStart, i), chunkSize, overlap, maximum, leadingWhitespace, chunks);
                sectionStart = i;
                hasContent = false;
            }
            if (block.heading() == false) hasContent = true;
        }
        addSection(text, blocks.subList(sectionStart, blocks.size()), chunkSize, overlap, maximum, leadingWhitespace, chunks);
        return chunks;
    }

    /** Finds blocks on code-protected lines, mapping their positions back to the original text. */
    private List<Block> findBlocks(String text) {
        MarkdownContentExtractor.ProtectedCode code = extractor.protectCode(text);
        List<MarkdownContentExtractor.Heading> headings = extractor.findHeadings(text);
        List<Block> blocks = new ArrayList<>();
        int offset = 0, start = -1, end = 0, listIndent = -1, headingIndex = 0, headingEnd = -1, headingLevel = 0;
        boolean blank = false, previousCode = false, table = false;
        String[] lines = code.text().split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String original = code.restore(line);
            int nextOffset = offset + original.length() + 1;
            if (original.isBlank()) {
                blank = true;
                offset = nextOffset;
                continue;
            }
            int newline = original.indexOf('\n');
            String firstLine = newline < 0 ? original : original.substring(0, newline);
            int indent = firstLine.length() - firstLine.stripLeading().length();
            boolean codeBlock = code.isBlock(line);
            boolean tableRow = table && blank == false && line.contains("|");
            boolean newTable = codeBlock == false && line.contains("|") && i + 1 < lines.length
                && lines[i + 1].contains("|") && TABLE_SEPARATOR.matcher(lines[i + 1]).matches();
            boolean item = tableRow == false && newTable == false && LIST_ITEM.matcher(firstLine).matches();
            boolean headingLine = headingIndex < headings.size() && headings.get(headingIndex).offset() == offset;
            boolean newHeading = headingLine && (listIndent < 0 || indent <= listIndent);
            boolean continuesItem = listIndent >= 0 && newHeading == false
                && (indent > listIndent || (blank == false && item == false && codeBlock == false));
            boolean headingContinuation = headingLevel > 0 && offset < headingEnd;
            boolean startsBlock = newHeading || headingLevel > 0 || (continuesItem == false
                && (blank || item || codeBlock || previousCode || newTable || (table && tableRow == false)));
            if (start < 0 || (headingContinuation == false && startsBlock)) {
                if (start >= 0) blocks.add(new Block(start, end, headingLevel));
                start = offset;
                headingLevel = newHeading ? headings.get(headingIndex).level() : 0;
                listIndent = item ? indent : -1;
            }
            if (headingLine) headingEnd = headings.get(headingIndex++).endOffset();
            end = offset + original.length();
            blank = false;
            previousCode = codeBlock;
            table = newTable || tableRow;
            offset = nextOffset;
        }
        if (start >= 0) blocks.add(new Block(start, end, headingLevel));
        return blocks;
    }

    /** Keeps fitting sections whole; otherwise packs near the target with whole-unit overlap. */
    private void addSection(String text, List<Block> section, int size, int overlap, int maximum,
            int leadingWhitespace, List<Chunk> chunks) {
        if (section.isEmpty()) return;
        int sectionStart = section.get(0).start();
        int sectionEnd = section.get(section.size() - 1).end();
        if (sectionEnd - sectionStart <= maximum) {
            chunks.add(new Chunk(Math.max(0, sectionStart - leadingWhitespace), text.substring(sectionStart, sectionEnd)));
            return;
        }
        List<Block> units = prepareUnits(text, section, size, maximum);
        int start = 0, end = 0;
        while (end < units.size()) {
            while (units.get(end).end() - units.get(start).start() > maximum) start++;
            int sourceStart = units.get(start).start();
            int sourceEnd = units.get(end++).end();
            while (end < units.size()) {
                int nextEnd = units.get(end).end();
                int currentLength = sourceEnd - sourceStart;
                int nextLength = nextEnd - sourceStart;
                if (nextLength > maximum || Math.abs(nextLength - size) >= Math.abs(currentLength - size)) break;
                sourceEnd = nextEnd;
                end++;
            }
            chunks.add(new Chunk(Math.max(0, sourceStart - leadingWhitespace), text.substring(sourceStart, sourceEnd)));
            int nextStart = end;
            while (overlap > 0 && nextStart > start + 1
                    && (nextStart == end || sourceEnd - units.get(nextStart - 1).start() <= overlap)) nextStart--;
            start = nextStart;
        }
    }

    /** Keeps fitting subsections whole, attaches leading headings, and splits only oversized units. */
    private List<Block> prepareUnits(String text, List<Block> section, int size, int maximum) {
        List<Block> units = new ArrayList<>();
        int index = 0;
        while (index < section.size()) {
            Block block = section.get(index++);
            while (block.heading() && index < section.size()) {
                int subtreeEnd = index;
                while (subtreeEnd < section.size() && (section.get(subtreeEnd).heading() == false
                        || section.get(subtreeEnd).headingLevel() > block.headingLevel())) subtreeEnd++;
                if (subtreeEnd > index && section.get(subtreeEnd - 1).end() - block.start() <= maximum) {
                    block = new Block(block.start(), section.get(subtreeEnd - 1).end(), 0);
                    index = subtreeEnd;
                } else {
                    Block next = section.get(index);
                    if (next.end() - block.start() > maximum) break;
                    block = new Block(block.start(), next.end(), next.headingLevel());
                    index++;
                }
            }
            if (block.end() - block.start() <= maximum) {
                units.add(block);
                continue;
            }
            String body = text.substring(block.start(), block.end());
            int contentStart = skipLeadingWhitespace(text, block.start(), block.end());
            for (Chunk part : fallback.chunkWithOffsets(body, size, 0)) {
                int start = skipLeadingWhitespace(text, contentStart + part.startOffset(), block.end());
                units.add(new Block(start, start + part.text().length(), 0));
            }
        }
        return units;
    }

    private int skipLeadingWhitespace(String text, int start, int end) {
        while (start < end && text.charAt(start) <= ' ') start++;
        return start;
    }

    private record Block(int start, int end, int headingLevel) {
        boolean heading() { return headingLevel > 0; }
    }
}
