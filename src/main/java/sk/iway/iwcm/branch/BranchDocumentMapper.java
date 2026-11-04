package sk.iway.iwcm.branch;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/** Explicit row mapping; no JPA-managed or shared cached document is mutated. */
public final class BranchDocumentMapper {
    private BranchDocumentMapper() { }

    public static BranchSnapshot snapshot(Map<String, ?> row) {
        return snapshot(row, BranchSnapshot.State.ACTIVE);
    }

    public static BranchSnapshot snapshot(Map<String, ?> row, BranchSnapshot.State state) {
        if (state == BranchSnapshot.State.ABSENT) return BranchSnapshot.absent();
        Map<String, Object> values = BranchFields.supportedValues(BranchFields.DOCUMENT, row);
        values.put("perex_group", BranchFields.normalizedIds(row.get("perex_group")));
        return new BranchSnapshot(state, values);
    }

    /** Derived inputs must come from the effective page/tree, without executing JSP or fulltext hooks. */
    public static Map<String, Object> project(Map<String, ?> work, Map<String, ?> live,
            Map<String, ?> initial, Map<String, ?> derived) {
        Map<String, Object> values = new java.util.LinkedHashMap<>(BranchFields.project(BranchFields.DOCUMENT, work, live, initial, derived));
        values.put("perex_group", BranchFields.idTokens(work.get("perex_group")));
        return Collections.unmodifiableMap(values);
    }

    /** New pages never inherit an individual publishing plan from a template. */
    public static Map<String, Object> initialValues(int id, int userId, Instant createdAt, boolean autoPublishStart) {
        if (id <= 0 || userId <= 0 || createdAt == null) throw new IllegalArgumentException("Invalid new document identity");
        Map<String, Object> values = BranchFields.nullValues(BranchFields.DOCUMENT);
        for (String field : java.util.List.of("title", "data", "data_asc", "navbar", "virtual_path", "editor_virtual_path", "perex_place", "perex_image", "perex_group", "password_protected")) values.put(field, "");
        for (String field : java.util.List.of("group_id", "temp_id", "sort_priority", "sync_id", "sync_status", "views_total", "views_month", "forum_count", "follow_links")) values.put(field, 0);
        for (String field : java.util.List.of("header_doc_id", "menu_doc_id", "footer_doc_id", "right_menu_doc_id", "logon_page_doc_id", "temp_field_a_docid", "temp_field_b_docid", "temp_field_c_docid", "temp_field_d_docid")) values.put(field, -1);
        for (String field : java.util.List.of("available", "searchable", "cacheable", "require_ssl", "show_in_menu", "generate_url_from_title", "url_inherit_group", "disable_after_end", "publish_after_start", "planned_media_initialized")) values.put(field, false);
        values.put("doc_id", id);
        values.put("author_id", userId);
        values.put("date_created", java.sql.Timestamp.from(createdAt));
        values.put("publish_start", autoPublishStart ? java.sql.Timestamp.from(createdAt) : null);
        return Collections.unmodifiableMap(values);
    }
}
