package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import jakarta.persistence.Column;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;

/** Verifies complete WORK mapping, legacy aliases and preservation of server-owned values. */
class BranchEditorRowsTest {
    @Test void perexMappingUsesIdsAndKeepsUnsupportedValuesFromTheServer() {
        DocDetails doc=new DocDetails(); doc.setTitle("autotest mapper"); doc.setPerexGroups(new Integer[]{17,3,17}); doc.setDateCreated(1000);
        doc.setSyncId(999); doc.setPublishStartDate(new java.util.Date(9000));
        var stored=new LinkedHashMap<>(BranchDocumentMapper.initialValues(1,1,Instant.EPOCH,false));
        stored.put("sync_id",7); stored.put("views_total",123); stored.put("publish_start",new java.sql.Timestamp(2000));
        var row=BranchEditorRows.document(doc,stored);
        assertEquals(",3,17,",BranchFields.idTokens(row.get("perex_group")));
        assertEquals(List.of(3,17),BranchFields.normalizedIds(BranchDocumentMapper.snapshot(row).fields().get("perex_group")));
        assertEquals(new java.sql.Timestamp(1000),row.get("date_created"));
        assertEquals(7,row.get("sync_id")); assertEquals(123,row.get("views_total"));
        assertEquals(new java.sql.Timestamp(2000),row.get("publish_start"));
        assertNull(row.get("publish_end")); assertEquals(false,row.get("publish_after_start"));
    }

    @Test void everySupportedDocumentAndFolderColumnIsReadWithoutAStoredFallback() {
        DocDetails doc=new DocDetails(); doc.setTitle("autotest mapper"); doc.setEditorVirtualPath("article.html"); doc.setEventDateDate(new java.util.Date(3000));
        var document=BranchEditorRows.document(doc,Map.of());
        assertEquals(BranchFields.DOCUMENT.supported(),document.keySet());
        assertEquals("article.html",document.get("editor_virtual_path"));
        assertEquals(new java.util.Date(3000),document.get("event_date"));
        GroupDetails group=new GroupDetails(); group.setNavbarName("Navigation"); group.setNewPageDocIdTemplate(42); group.setForceTheUseOfGroupTemplate(true);
        var folder=BranchEditorRows.group(group,Map.of());
        assertEquals(BranchFields.GROUP.supported(),folder.keySet());
        assertEquals("Navigation",folder.get("navbar")); assertEquals(42,folder.get("new_page_docid_template")); assertEquals(true,folder.get("force_group_template"));
    }

    @Test void newColumnsUseJpaMetadataOrTheNamingConventionWithoutAnotherMappingList() throws Exception {
        var readers=BranchEditorRows.readers(FutureBean.class,Set.of("future_column","future_value"),Map.of());
        FutureBean bean=new FutureBean();
        assertEquals("annotated",readers.get("future_column").invoke(bean));
        assertEquals("conventional",readers.get("future_value").invoke(bean));
    }

    @Test void missingGetterFailsInsteadOfKeepingThePreviousStoredValue() {
        var error=assertThrows(IllegalStateException.class,()->BranchEditorRows.readers(FutureBean.class,Set.of("forgotten_column"),Map.of()));
        assertTrue(error.getMessage().contains("forgotten_column"));
    }

    @Test void ambiguousColumnMetadataIsRejected() {
        var error=assertThrows(IllegalStateException.class,()->BranchEditorRows.readers(AmbiguousBean.class,Set.of("duplicate"),Map.of()));
        assertTrue(error.getMessage().contains("Ambiguous column duplicate"));
    }

    public static class FutureBean {
        @Column(name="future_column ") private String renamedProperty="annotated";
        public String getRenamedProperty() { return renamedProperty; }
        public String getFutureValue() { return "conventional"; }
    }
    public static class AmbiguousBean {
        @Column(name="duplicate") private String first;
        @Column(name="duplicate") private String second;
        public String getFirst() { return first; }
        public String getSecond() { return second; }
    }
}
