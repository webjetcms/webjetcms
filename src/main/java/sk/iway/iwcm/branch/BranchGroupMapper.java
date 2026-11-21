package sk.iway.iwcm.branch;

import java.util.Collections;
import java.util.Map;

/** Explicit folder content mapping, with domain and synchronization values kept LIVE. */
public final class BranchGroupMapper {
    private BranchGroupMapper() { }

    public static BranchSnapshot snapshot(Map<String, ?> row) {
        return snapshot(row, BranchSnapshot.State.ACTIVE);
    }

    public static BranchSnapshot snapshot(Map<String, ?> row, BranchSnapshot.State state) {
        return state == BranchSnapshot.State.ABSENT ? BranchSnapshot.absent()
            : new BranchSnapshot(state, BranchFields.supportedValues(BranchFields.GROUP, row));
    }

    public static Map<String, Object> project(Map<String, ?> work, Map<String, ?> live, Map<String, ?> initial) {
        return BranchFields.project(BranchFields.GROUP, work, live, initial, Map.of());
    }

    public static Map<String, Object> initialValues(int id, String domainName) {
        if (id <= 0 || domainName == null || domainName.isBlank()) throw new IllegalArgumentException("Invalid new folder identity");
        Map<String, Object> values = BranchFields.nullValues(BranchFields.GROUP);
        for (String field : java.util.List.of("group_name", "navbar", "url_dir_name", "password_protected", "html_head", "field_a", "field_b", "field_c", "field_d")) values.put(field, "");
        for (String field : java.util.List.of("parent_group_id", "default_doc_id", "temp_id", "sort_priority", "new_page_docid_template", "sync_id", "sync_status")) values.put(field, 0);
        for (String field : java.util.List.of("internal", "hidden_in_admin", "force_group_template")) values.put(field, false);
        values.put("group_id", id);
        values.put("domain_name", domainName);
        values.put("menu_type", 1);
        values.put("logged_menu_type", -1);
        values.put("link_group_id", -1);
        values.put("logon_page_doc_id", -1);
        return Collections.unmodifiableMap(values);
    }
}
