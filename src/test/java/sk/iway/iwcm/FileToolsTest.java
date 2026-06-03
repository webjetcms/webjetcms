package sk.iway.iwcm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for FileTools.formatFileSize and formatFileSizeFromKb.
 */
class FileToolsTest {

	// === formatFileSize ===

	@Test
	void testFormatFileSizeZero() {
		assertEquals("0 B", FileTools.formatFileSize(0));
	}

	@Test
	void testFormatFileSizeSmall() {
		assertEquals("500 B", FileTools.formatFileSize(500));
		assertEquals("100 B", FileTools.formatFileSize(100));
		assertEquals("1023 B", FileTools.formatFileSize(1023));
        assertEquals("1023 B", FileTools.getFormatFileSize(1023, false));
        assertEquals("1023 B", FileTools.getFormatFileSize(1023, true));
        assertEquals("500 B", Tools.formatFileSize(500));
		assertEquals("100 B", Tools.formatFileSize(100));
		assertEquals("1023 B", Tools.formatFileSize(1023));
	}

	@Test
	void testFormatFileSizeExactKb() {
		assertEquals("1 kB", FileTools.formatFileSize(1024));
        assertEquals("1,75 kB", FileTools.formatFileSize(1792));
        assertEquals("1,75 kB", Tools.formatFileSize(1792));
        //rounded to 2 kB when exactFormat is false
        assertEquals("2 kB", FileTools.getFormatFileSize(1792, false));
        assertEquals("1,75 kB", FileTools.getFormatFileSize(1792, true));

        assertEquals("1,5 kB", FileTools.formatFileSize(1536));
        assertEquals("1,5 kB", Tools.formatFileSize(1536));
	}

	@Test
	void testFormatFileSizeMultipleKb() {
		assertEquals("200 kB", FileTools.formatFileSize(200 * 1024));
	}

	@Test
	void testFormatFileSizeOnePointFiveMb() {
		assertEquals("1,5 MB", FileTools.formatFileSize((long) (1.5 * 1024 * 1024)));
	}

	@Test
	void testFormatFileSizeExactMb() {
		assertEquals("1 MB", FileTools.formatFileSize(1024 * 1024));
        long size = (1024 * 1024) + 435000; // 1 MB + 435 KB
        assertEquals("1,41 MB", FileTools.formatFileSize(size));
        assertEquals("1,41 MB", FileTools.getFormatFileSize(size, true));
        //rounded to 1,4 MB when exactFormat is false
        assertEquals("1,4 MB", FileTools.getFormatFileSize(size, false));
	}

	@Test
	void testFormatFileSizeExactGb() {
		assertEquals("1 GB", FileTools.formatFileSize(1024L * 1024 * 1024));
		assertEquals("2 GB", FileTools.formatFileSize(2L * 1024 * 1024 * 1024));
	}

	@Test
	void testFormatFileSizeWithDecimal() {
		// 1500 MB = ~1.46 GB → rounds to 1.5 GB
		String result = FileTools.formatFileSize(1500L * 1024 * 1024);
		assertTrue(result.contains("GB"), "Should use GB unit. Got: " + result);
	}

	@Test
	void testFormatFileSizeTb() {
		assertEquals("1 TB", FileTools.formatFileSize(1024L * 1024 * 1024 * 1024));
	}

	@Test
	void testFormatFileSizePb() {
		assertEquals("1 PB", FileTools.formatFileSize(1024L * 1024 * 1024 * 1024 * 1024));
	}

	@Test
	void testFormatFileSizeLargeValueAtPbCap() {
		// 10 PB should still show PB (capped at units.length - 1)
		String result = FileTools.formatFileSize(10L * 1024 * 1024 * 1024 * 1024 * 1024);
		assertTrue(result.contains("PB"), "Should cap at PB. Got: " + result);
	}

	@Test
	void testFormatFileSizeNegative() {
		assertEquals("0 B", FileTools.formatFileSize(-1));
		assertEquals("0 B", FileTools.formatFileSize(Long.MIN_VALUE));
	}

	// === formatFileSizeFromKb ===

	@Test
	void testFormatFileSizeFromKbZero() {
		assertEquals("0 B", FileTools.formatFileSizeFromKb(0));
	}

	@Test
	void testFormatFileSizeFromKbSmall() {
		assertEquals("1 kB", FileTools.formatFileSizeFromKb(1)); // 1 KB = 1024 B
		assertEquals("0 B", FileTools.formatFileSizeFromKb(0));
	}

	@Test
	void testFormatFileSizeFromKbExactKb() {
		assertEquals("1 kB", FileTools.formatFileSizeFromKb(1));
	}

	@Test
	void testFormatFileSizeFromKbMultipleKb() {
		assertEquals("200 kB", FileTools.formatFileSizeFromKb(200));
	}

	@Test
	void testFormatFileSizeFromKbOnePointFiveMb() {
		assertEquals("1,5 MB", FileTools.formatFileSizeFromKb(1536)); // 1536 KB = 1.5 MB
	}

	@Test
	void testFormatFileSizeFromKbExactGb() {
		assertEquals("1 GB", FileTools.formatFileSizeFromKb(1024L * 1024));
	}

	@Test
	void testFormatFileSizeFromKbNegative() {
		assertEquals("0 B", FileTools.formatFileSizeFromKb(-1));
		assertEquals("0 B", FileTools.formatFileSizeFromKb(Long.MIN_VALUE));
	}
}
