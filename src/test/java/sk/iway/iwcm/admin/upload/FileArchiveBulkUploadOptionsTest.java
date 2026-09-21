package sk.iway.iwcm.admin.upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.components.file_archiv.FileArchivatorBean;
import sk.iway.iwcm.components.file_archiv.FileArchivatorEditorFields;

/**
 * Verifies partial entity binding, upload-session storage, and validation of bulk-upload metadata.
 */
class FileArchiveBulkUploadOptionsTest {

    /**
     * Supplied entity properties are converted while omitted metadata and upload transport fields remain intact.
     */
    @Test
    void bindsOnlySuppliedMetadata() {
        FileArchivatorBean entity = new FileArchivatorBean();
        entity.setShowFile(false);
        entity.setIndexFile(false);
        entity.setPriority(73);
        entity.setEditorFields(new FileArchivatorEditorFields());
        entity.getEditorFields().setFile("temporary-upload-key");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("category", " Finance ");
        request.addParameter("validFrom", "1800000000000");
        request.addParameter("editorFields.uploadRedundantFile", "true");
        FileArchiveBulkUploadOptions options = FileArchiveBulkUploadOptions.fromRequest(request);

        assertNull(options.getErrorKey());
        assertNull(options.bindTo(entity));
        assertEquals("Finance", entity.getCategory());
        assertEquals(new Date(1800000000000L), entity.getValidFrom());
        assertTrue(entity.getEditorFields().getUploadRedundantFile());
        assertEquals("temporary-upload-key", entity.getEditorFields().getFile());
        assertFalse(entity.getShowFile());
        assertFalse(entity.getIndexFile());
        assertEquals(73, entity.getPriority());
    }

    /**
     * Explicit false and zero values must be applied, while blank text must retain existing metadata.
     */
    @Test
    void distinguishesFalseAndZeroFromOmittedValues() {
        FileArchivatorBean entity = new FileArchivatorBean();
        entity.setCategory("Original category");
        entity.setPriority(73);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("showFile", "false");
        request.addParameter("indexFile", "false");
        request.addParameter("priority", "0");
        request.addParameter("category", "   ");

        assertNull(FileArchiveBulkUploadOptions.fromRequest(request).bindTo(entity));
        assertFalse(entity.getShowFile());
        assertFalse(entity.getIndexFile());
        assertEquals(0, entity.getPriority());
        assertEquals("Original category", entity.getCategory());
    }

    /**
     * Untrusted requests cannot bind identity, ownership, paths, or upload-control properties.
     */
    @Test
    void ignoresPropertiesOutsideBulkDialog() {
        FileArchivatorBean entity = new FileArchivatorBean();
        entity.setId(7L);
        entity.setDomainId(1);
        entity.setFilePath("files/archiv/original/");
        entity.setEditorFields(new FileArchivatorEditorFields());
        entity.getEditorFields().setFile("original-key");
        MockHttpServletRequest request = new MockHttpServletRequest();
        for (String field : new String[] {"id", "domainId", "filePath", "editorFields.file", "editorFields.dir", "editorFields.uploadType", "editorFields.class.classLoader"}) {
            request.addParameter(field, "123");
        }

        assertNull(FileArchiveBulkUploadOptions.fromRequest(request).bindTo(entity));
        assertEquals(7L, entity.getId());
        assertEquals(1, entity.getDomainId());
        assertEquals("files/archiv/original/", entity.getFilePath());
        assertEquals("original-key", entity.getEditorFields().getFile());
        assertNull(entity.getEditorFields().getDir());
        assertNull(entity.getEditorFields().getUploadType());
    }

    /**
     * Binding must reject malformed values before an upload chunk is accepted.
     */
    @ParameterizedTest
    @CsvSource({
        "validFrom,not-a-date", "validFrom,-1", "validTo,253402300800000",
        "showFile,maybe", "showFile,yes", "indexFile,1", "priority,invalid", "priority,0x10",
        "editorFields.saveLater,invalid", "editorFields.uploadRedundantFile,yes"
    })
    void rejectsInvalidPropertyValues(String field, String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter(field, value);
        assertNotNull(FileArchiveBulkUploadOptions.fromRequest(request).getErrorKey());
    }

    /**
     * Validity is checked against retained dates when only one boundary is supplied.
     */
    @Test
    void validatesMergedValidityInterval() {
        FileArchivatorBean entity = new FileArchivatorBean();
        entity.setValidTo(new Date(1800000000000L));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("validFrom", "1800000000001");
        FileArchiveBulkUploadOptions options = FileArchiveBulkUploadOptions.fromRequest(request);

        assertNull(options.getErrorKey());
        assertEquals("components.file_archiv.bulk_upload.error.invalid_validity_interval", options.bindTo(entity));
    }

    /**
     * Delayed-upload metadata survives session serialization and later changes to the original request.
     */
    @Test
    void preservesScheduledMetadataAcrossChunks() throws Exception {
        long uploadTime = System.currentTimeMillis() + 3600000;
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("editorFields.saveLater", "true");
        request.addParameter("editorFields.dateUploadLater", Long.toString(uploadTime));
        request.addParameter("editorFields.emails", "editor@example.com, admin@example.com");
        FileArchiveBulkUploadOptions options = FileArchiveBulkUploadOptions.fromRequest(request);
        assertNull(options.getErrorKey());
        request.setParameter("editorFields.emails", "invalid");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(options);
        }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            options = (FileArchiveBulkUploadOptions) input.readObject();
        }
        FileArchivatorBean entity = new FileArchivatorBean();
        assertNull(options.bindTo(entity));
        assertTrue(entity.getEditorFields().getSaveLater());
        assertEquals(new Date(uploadTime), entity.getEditorFields().getDateUploadLater());
        assertEquals("editor@example.com, admin@example.com", entity.getEditorFields().getEmails());
    }

    /**
     * Delayed uploads require a future date and a nonempty list of valid notification addresses.
     */
    @ParameterizedTest
    @CsvSource({"0,editor@example.com", "future,invalid", "future,'editor@example.com,'", "future,''"})
    void rejectsInvalidScheduledMetadata(String date, String emails) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("editorFields.saveLater", "true");
        request.addParameter("editorFields.dateUploadLater", "future".equals(date) ? Long.toString(System.currentTimeMillis() + 3600000) : date);
        request.addParameter("editorFields.emails", emails);
        assertNotNull(FileArchiveBulkUploadOptions.fromRequest(request).getErrorKey());
    }

    /**
     * Bulk text limits remain enforced for standard metadata and longer notes.
     */
    @ParameterizedTest
    @CsvSource({"category,255", "note,1100"})
    void rejectsOversizedText(String field, int limit) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter(field, "x".repeat(limit + 1));
        assertNotNull(FileArchiveBulkUploadOptions.fromRequest(request).getErrorKey());
    }

    /**
     * Dialogs opened before an application update can still submit prefixed metadata parameters.
     */
    @Test
    void supportsLegacyParameterNames() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("fileArchiveCategory", "Legacy category");
        request.addParameter("fileArchiveUploadRedundantFile", "true");
        FileArchivatorBean entity = new FileArchivatorBean();

        assertNull(FileArchiveBulkUploadOptions.fromRequest(request).bindTo(entity));
        assertEquals("Legacy category", entity.getCategory());
        assertTrue(entity.getEditorFields().getUploadRedundantFile());
    }
}
