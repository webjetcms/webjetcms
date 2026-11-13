package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import sk.iway.iwcm.doc.DocBasic;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.spirit.model.Media;

class BranchMappingTest {
    @Test void groupNavigationUsesEnumsWhileDocumentNavigationUsesBooleans() {
        Map<String,Object> group=new HashMap<>(BranchGroupMapper.initialValues(20,"example.sk"));
        for(String column:java.util.List.of("show_in_navbar","show_in_sitemap","logged_show_in_navbar","logged_show_in_sitemap"))group.put(column,2);
        BranchSnapshot two=BranchGroupMapper.snapshot(group);
        assertEquals(new BigDecimal("2"),two.fields().get("show_in_navbar"));
        group.put("show_in_navbar",3);assertNotEquals(two.hash(),BranchGroupMapper.snapshot(group).hash());
        var doc=new HashMap<>(BranchDocumentMapper.initialValues(9,1,Instant.now(),false));doc.put("show_in_navbar",1);assertEquals(true,BranchDocumentMapper.snapshot(doc).fields().get("show_in_navbar"));
    }
    @Test
    void everyJpaPersistentColumnHasExplicitClassification() {
        for (Class<?> entity : java.util.List.of(DocBasic.class, DocDetails.class)) {
            assertMapped(entity, BranchFields.DOCUMENT);
        }
        assertMapped(Media.class, BranchFields.MEDIA);
        assertThrows(IllegalStateException.class, () -> BranchFields.DOCUMENT.verifyColumns(Set.of("title", "future_content")));
        BranchFields.DOCUMENT.verifyColumns(BranchFields.DOCUMENT.columns());
    }

    private void assertMapped(Class<?> entity, BranchFields fields) {
        for (Field field : entity.getDeclaredFields()) {
            Column column = field.getAnnotation(Column.class);
            if (column != null) assertTrue(fields.columns().contains(column.name().trim()), entity.getSimpleName() + "." + field.getName());
        }
    }

    @Test
    void canonicalSnapshotsIgnorePropertyOrderNumericRepresentationAndTransientState() {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("b", 100L);
        a.put("a", null);
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("a", null);
        b.put("b", new BigDecimal("100.000"));
        BranchSnapshot snapshot = new BranchSnapshot(BranchSnapshot.State.ACTIVE, a);
        assertEquals(snapshot.hash(), new BranchSnapshot(BranchSnapshot.State.ACTIVE, b).hash());
        assertEquals(snapshot.hash(), BranchSnapshot.fromJson(snapshot.json()).hash());
        a.put("b", 200);
        assertEquals(snapshot.hash(), new BranchSnapshot(BranchSnapshot.State.ACTIVE, b).hash());
        assertNotEquals(snapshot.hash(), BranchSnapshot.absent().hash());
        assertNotEquals(snapshot.hash(), new BranchSnapshot(BranchSnapshot.State.TRASH, b).hash());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.fields().put("bad", 1));
    }

    @Test
    void normalizedCategorySetsAndFullDocumentContentDriveHashes() {
        Map<String, Object> a = new HashMap<>(BranchDocumentMapper.initialValues(9, 1, Instant.parse("2026-11-04T12:00:00Z"), false));
        a.put("perex_group", ",12,3,12,");
        a.put("data", "<p>" + "html".repeat(80000) + "</p>");
        BranchSnapshot original = BranchDocumentMapper.snapshot(a);
        Map<String, Object> b = new HashMap<>(a);
        b.put("perex_group", ",3,12,");
        b.put("available", 0);
        b.put("views_total", 8000);
        b.put("publish_start", java.sql.Timestamp.from(Instant.now()));
        b.put("sync_id", 100);
        b.put("transientPreviewData", "ignored");
        assertEquals(original.hash(), BranchDocumentMapper.snapshot(b).hash());
        assertEquals(original.hash(), BranchSnapshot.fromJson(original.json()).hash());
        b.put("data", "other");
        assertNotEquals(original.hash(), BranchDocumentMapper.snapshot(b).hash());
        b.remove("data");
        assertThrows(IllegalArgumentException.class, () -> BranchDocumentMapper.snapshot(b));
        assertThrows(IllegalArgumentException.class, () -> BranchFields.normalizedIds(",0,1,"));
    }

    @Test
    void projectUsesWorkContentCurrentLiveRuntimeAndExplicitDerivedInputs() {
        Map<String, Object> initial = BranchDocumentMapper.initialValues(9, 1, Instant.now(), true);
        Map<String, Object> work = new HashMap<>(initial);
        work.put("title", "WORK");
        work.put("perex_group", java.util.List.of(3, 1, 3));
        Map<String, Object> live = new HashMap<>(initial);
        live.put("title", "LIVE");
        live.put("views_total", 8000);
        live.put("sync_id", 1234);
        live.put("publish_start", java.sql.Timestamp.from(Instant.parse("2030-01-01T00:00:00Z")));
        Map<String, Object> derived = new HashMap<>();
        for (String field : BranchFields.DOCUMENT.derived()) derived.put(field, initial.get(field));
        derived.put("data_asc", "derived from WORK");
        Map<String, Object> projected = BranchDocumentMapper.project(work, live, initial, derived);
        assertEquals("WORK", projected.get("title"));
        assertEquals(",1,3,", projected.get("perex_group"));
        assertEquals(8000, projected.get("views_total"));
        assertEquals(1234, projected.get("sync_id"));
        assertEquals(live.get("publish_start"), projected.get("publish_start"));
        assertEquals("derived from WORK", projected.get("data_asc"));
        assertEquals(BranchFields.DOCUMENT.columns(), projected.keySet());
        assertThrows(IllegalArgumentException.class, () -> BranchDocumentMapper.project(work, live, initial, Map.of()));
    }

    @Test
    void newObjectsHaveNoInheritedIndividualPlanAndAllColumnsHaveInitialValues() {
        Instant now = Instant.parse("2026-11-04T12:00:00Z");
        Map<String, Object> doc = BranchDocumentMapper.initialValues(10, 1, now, true);
        assertEquals(BranchFields.DOCUMENT.columns(), doc.keySet());
        assertEquals(java.sql.Timestamp.from(now), doc.get("publish_start"));
        assertNull(doc.get("publish_end"));
        assertEquals(false, doc.get("publish_after_start"));
        assertEquals(false, doc.get("disable_after_end"));
        assertEquals(false, doc.get("planned_media_initialized"));
        assertNull(BranchDocumentMapper.initialValues(10, 1, now, false).get("publish_start"));
        Map<String, Object> group = BranchGroupMapper.initialValues(20, "example.sk");
        assertEquals(BranchFields.GROUP.columns(), group.keySet());
        assertEquals("example.sk", group.get("domain_name"));
        assertNotEquals(BranchGroupMapper.snapshot(group).hash(), BranchGroupMapper.snapshot(group, BranchSnapshot.State.TRASH).hash());
    }

    @Test
    void mediaCategoryMembershipConflictsButCutoffIsDerivedFromEffectiveOwner() {
        Map<String, Object> row = BranchMediaMapper.initialValues(30, 10, 1);
        Map<String, Object> changed = new HashMap<>(row);
        changed.put("last_update", java.sql.Timestamp.from(Instant.now()));
        assertEquals(BranchMediaMapper.snapshot(row, java.util.List.of(9, 2, 9)).hash(), BranchMediaMapper.snapshot(changed, ",2,9,").hash());
        assertNotEquals(BranchMediaMapper.snapshot(row, ",2,9,").hash(), BranchMediaMapper.snapshot(row, ",2,").hash());
        Instant owner = Instant.parse("2020-01-01T01:02:03.999Z");
        Map<String, Object> projection = BranchMediaMapper.project(row, row, row, owner);
        assertEquals(Instant.parse("2020-01-01T01:02:03Z"), ((java.util.Date) projection.get("last_update")).toInstant());
        assertEquals(BranchFields.MEDIA.columns(), projection.keySet());
    }
}
