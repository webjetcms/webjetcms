package sk.iway.iwcm.branch;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Explicit content classification; unknown persistent columns must be reviewed. */
public record BranchFields(Set<String> supported, Set<String> derived,
        Set<String> operational, Set<String> outsideScope) {

    public BranchFields {
        supported = immutable(supported);
        derived = immutable(derived);
        operational = immutable(operational);
        outsideScope = immutable(outsideScope);
        Set<String> all = new LinkedHashSet<>();
        for (Set<String> category : java.util.List.of(supported, derived, operational, outsideScope)) {
            for (String field : category) {
                if (!all.add(field)) throw new IllegalArgumentException("Duplicate classification: " + field);
            }
        }
    }

    public Set<String> columns() {
        Set<String> columns = new LinkedHashSet<>(supported);
        columns.addAll(derived);
        columns.addAll(operational);
        columns.addAll(outsideScope);
        return Collections.unmodifiableSet(columns);
    }

    /** Refuse schema drift instead of silently copying newly added content. */
    public void verifyColumns(Set<String> actual) {
        if (!columns().equals(actual)) {
            Set<String> unknown = new LinkedHashSet<>(actual);
            unknown.removeAll(columns());
            Set<String> missing = new LinkedHashSet<>(columns());
            missing.removeAll(actual);
            throw new IllegalStateException("Unclassified columns " + unknown + "; missing columns " + missing);
        }
    }

    public static BranchFields forType(BranchType type) {
        return switch (type) {
            case DOCUMENT -> DOCUMENT;
            case GROUP -> GROUP;
            case MEDIA -> MEDIA;
            case FILE -> FILE;
        };
    }

    static java.util.Map<String, Object> supportedValues(BranchFields fields, java.util.Map<String, ?> source) {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        for (String column : fields.supported()) {
            if (!source.containsKey(column)) throw new IllegalArgumentException("Incomplete content: " + column);
            Object value = source.get(column);
            if (BOOLEAN_COLUMNS.contains(column) && value instanceof Number number) value = number.intValue() != 0;
            result.put(column, value);
        }
        return result;
    }

    static java.util.Map<String, Object> project(BranchFields fields, java.util.Map<String, ?> work,
            java.util.Map<String, ?> live, java.util.Map<String, ?> initial, java.util.Map<String, ?> derived) {
        java.util.Map<String, Object> result = supportedValues(fields, work);
        java.util.Map<String, ?> stable = live == null ? initial : live;
        for (String name : fields.columns()) {
            if (fields.supported().contains(name)) continue;
            java.util.Map<String, ?> source = fields.derived().contains(name) ? derived : stable;
            if (!source.containsKey(name)) throw new IllegalArgumentException("Missing projection input: " + name);
            result.put(name, source.get(name));
        }
        return java.util.Collections.unmodifiableMap(result);
    }

    static java.util.Map<String, Object> nullValues(BranchFields fields) {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        fields.columns().forEach(name -> values.put(name, null));
        return values;
    }

    /** IDs are compared as an ordered set but written with the existing comma-token format. */
    public static java.util.List<Integer> normalizedIds(Object raw) {
        java.util.SortedSet<Integer> ids = new java.util.TreeSet<>();
        if (raw == null) return java.util.List.of();
        Iterable<?> items = raw instanceof Iterable<?> iterable ? iterable : java.util.List.of(raw.toString().replace("[", "").replace("]", "").split(","));
        for (Object item : items) {
            String token = item.toString().trim();
            if (token.isEmpty()) continue;
            int id;
            try { id = new java.math.BigDecimal(token).intValueExact(); }
            catch (ArithmeticException | NumberFormatException e) { throw new IllegalArgumentException("Invalid category ID: " + token, e); }
            if (id <= 0) throw new IllegalArgumentException("Category IDs must be positive");
            ids.add(id);
        }
        return java.util.List.copyOf(ids);
    }

    public static String idTokens(Object raw) {
        java.util.List<Integer> ids = normalizedIds(raw);
        return ids.isEmpty() ? "" : "," + ids.stream().map(Object::toString).collect(java.util.stream.Collectors.joining(",")) + ",";
    }

    private static final Set<String> BOOLEAN_COLUMNS = fields("available searchable cacheable require_ssl generate_url_from_title url_inherit_group show_in_menu show_in_navbar show_in_sitemap logged_show_in_menu logged_show_in_navbar logged_show_in_sitemap internal hidden_in_admin force_group_template");

    private static Set<String> immutable(Set<String> fields) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(fields));
    }

    private static Set<String> fields(String names) {
        return names.isBlank() ? Set.of() : new LinkedHashSet<>(java.util.List.of(names.split(" +")));
    }

    public static final BranchFields DOCUMENT = new BranchFields(
        fields("title data external_link navbar date_created author_id group_id temp_id searchable follow_links available cacheable sort_priority header_doc_id menu_doc_id footer_doc_id password_protected html_head html_data perex_place perex_image perex_group show_in_menu event_date virtual_path logon_page_doc_id right_menu_doc_id field_a field_b field_c field_d field_e field_f field_g field_h field_i field_j field_k field_l field_m field_n field_o field_p field_q field_r field_s field_t require_ssl temp_field_a_docid temp_field_b_docid temp_field_c_docid temp_field_d_docid show_in_navbar show_in_sitemap logged_show_in_menu logged_show_in_navbar logged_show_in_sitemap url_inherit_group generate_url_from_title editor_virtual_path"),
        fields("data_asc file_name root_group_l1 root_group_l2 root_group_l3"),
        fields("views_total views_month file_change forum_count planned_media_initialized"),
        fields("doc_id sync_id sync_status publish_start publish_end disable_after_end publish_after_start"));

    public static final BranchFields GROUP = new BranchFields(
        fields("group_name internal parent_group_id navbar default_doc_id temp_id sort_priority password_protected menu_type url_dir_name html_head logon_page_doc_id new_page_docid_template field_a field_b field_c field_d logged_menu_type link_group_id lng hidden_in_admin force_group_template show_in_navbar show_in_sitemap logged_show_in_navbar logged_show_in_sitemap"),
        Set.of(), Set.of(), fields("group_id sync_id sync_status domain_name install_name"));

    public static final BranchFields MEDIA = new BranchFields(
        fields("media_fk_id media_fk_table_name media_title_sk media_title_cz media_title_de media_title_en media_link media_thumb_link media_group media_info_sk media_info_cz media_info_de media_info_en media_sort_order field_a field_b field_c field_d field_e field_f"),
        fields("last_update"), Set.of(), fields("media_id domain_id"));

    public static final BranchFields FILE = new BranchFields(
        fields("entry_kind logical_path content_hash"), Set.of(), fields("size_bytes mime_type"), Set.of());
}
