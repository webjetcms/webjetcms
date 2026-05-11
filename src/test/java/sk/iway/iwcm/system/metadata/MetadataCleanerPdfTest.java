package sk.iway.iwcm.system.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.common.PdfTools;
import sk.iway.iwcm.test.BaseWebjetTest;

/**
 * Tests for MetadataCleanerPdf: verifies that PDF metadata (Creator, Producer)
 * is cleaned when metadataCleanFiles contains "pdf-gen".
 */
class MetadataCleanerPdfTest extends BaseWebjetTest {

    private static final String TEST_AUTHOR = "TestOrganization";
    private static final String PD4ML_AUTHOR = "PD4ML v.4.0.21";

    @BeforeEach
    void setUp() {
        Constants.setString("metadataCleanFiles", "pdf-gen");
        Constants.setString("pdfAuthorName", TEST_AUTHOR);
        Constants.setString("pdfBaseUrl", "NULL");
    }

    @AfterEach
    void tearDown() {
        Constants.setString("metadataCleanFiles", "");
        Constants.setString("pdfAuthorName", "");
        Constants.setString("pdfBaseUrl", "");
    }

    /**
     * Directly tests MetadataCleanerPdf.removeMetadata() with a PDF that has
     * Creator and Producer set to the PD4ML library name.
     */
    @Test
    void testRemoveMetadataDirectly() throws IOException {
        // Create a minimal PDF with library metadata
        ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            document.addPage(new org.apache.pdfbox.pdmodel.PDPage());
            PDDocumentInformation info = document.getDocumentInformation();
            info.setCreator(PD4ML_AUTHOR);
            info.setProducer(PD4ML_AUTHOR);
            document.save(pdfOut);
        }

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        MetadataCleanerPdf.removeMetadata(pdfOut, result);

        try (PDDocument cleaned = Loader.loadPDF(result.toByteArray())) {
            PDDocumentInformation info = cleaned.getDocumentInformation();
            assertEquals(TEST_AUTHOR, info.getCreator(), "Creator should be overwritten");
            assertEquals(TEST_AUTHOR, info.getProducer(), "Producer should be overwritten");
        }
    }

    /**
     * Tests that PdfTools.renderHtmlCode cleans PDF metadata when
     * metadataCleanFiles contains "pdf-gen".
     */
    @Test
    void testRenderHtmlCodeCleansMetadata() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PdfTools.renderHtmlCode("<html><body><p>Test</p></body></html>", output, null, null, false);

        try (PDDocument document = Loader.loadPDF(output.toByteArray())) {
            PDDocumentInformation info = document.getDocumentInformation();
            assertEquals(TEST_AUTHOR, info.getCreator(), "Creator should be replaced with pdfAuthorName");
            assertEquals(TEST_AUTHOR, info.getProducer(), "Producer should be replaced with pdfAuthorName");
        }
    }

    /**
     * Tests that PdfTools.renderHtmlCode does NOT clean metadata when
     * metadataCleanFiles does not contain "pdf-gen".
     */
    @Test
    void testRenderHtmlCodeSkipsCleanupWhenNotConfigured() throws IOException {
        Constants.setString("metadataCleanFiles", "");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PdfTools.renderHtmlCode("<html><body><p>Test</p></body></html>", output, null, null, false);

        try (PDDocument document = Loader.loadPDF(output.toByteArray())) {
            PDDocumentInformation info = document.getDocumentInformation();
            // When cleanup is disabled, the raw PD4ML metadata should be present
            assertNotEquals(TEST_AUTHOR, info.getCreator(), "Creator should NOT be replaced when cleanup is disabled");
            assertNotEquals(TEST_AUTHOR, info.getProducer(), "Producer should NOT be replaced when cleanup is disabled");
            assertEquals(PD4ML_AUTHOR, info.getCreator(), "Creator should be PD4ML");
            assertEquals(PD4ML_AUTHOR, info.getProducer(), "Producer should be PD4ML");
        }
    }
}
