package sk.iway.iwcm.branch;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Explicit effective SQL sources, composed before filtering, counting or paging. */
public final class BranchSql {
    private static final Set<String> DOCUMENT_COLUMNS = BranchFields.DOCUMENT.columns();
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> LEGACY_TEXT = Set.of("data", "data_asc", "html_head", "html_data");
    private BranchSql() { }
    public record Fragment(String sql, List<Object> arguments) {
        public Fragment { arguments = Collections.unmodifiableList(new ArrayList<>(arguments)); }
        public int bind(PreparedStatement statement,int firstIndex) throws SQLException {
            int index = firstIndex;
            for (Object value : arguments) {
                if (Constants.DB_TYPE == Constants.DB_ORACLE && value instanceof String json)
                    statement.setClob(index++, new java.io.StringReader(json));
                else statement.setObject(index++,value);
            }
            return index;
        }
    }
    public static Fragment documentSource(String alias) {
        return documentSource(alias,DOCUMENT_COLUMNS);
    }

    /** Projects only the requested columns through every arm of the effective document union. */
    public static Fragment documentSource(String alias,Collection<String> columns) {
        if (columns.isEmpty() || (columns != DOCUMENT_COLUMNS && !DOCUMENT_COLUMNS.containsAll(columns))) throw new IllegalArgumentException("Unknown document columns");
        if (!alias.matches("[a-zA-Z][a-zA-Z0-9_]*")) throw new IllegalArgumentException("Invalid internal SQL alias");
        BranchView view = BranchView.current();
        if (view == null) return new Fragment("documents "+alias,List.of());
        List<Object> arguments = new ArrayList<>();
        String projection = columns.stream().map(column -> liveColumn(column) + " AS " + column).collect(Collectors.joining(","));
        StringBuilder sql = new StringBuilder("(SELECT " + projection + " FROM documents l WHERE ");
        List<Integer> groups = view.groups().stream().map(sk.iway.iwcm.doc.GroupDetails::getGroupId).toList();
        sql.append(DB.getInCondition("l.group_id", groups));
        List<Integer> replaced = view.selectedDocumentIds().stream().map(Integer::valueOf).toList();
        if (!replaced.isEmpty()) sql.append(" AND ").append(DB.getNotInCondition("l.doc_id", replaced));

        // Keep the request's already resolved WORK snapshot even if another editor saves meanwhile.
        // Each JSON parameter represents a complete set, including explicit nulls.
        if (!view.documentRows().isEmpty()) {
            sql.append(" UNION ALL SELECT ").append(columns.stream().map(BranchSql::workColumn).collect(Collectors.joining(",")))
                .append(" FROM ").append(jsonTable(columns));
            arguments.add(jsonRows(view.documentRows(), columns));
        }
        if (!view.derivedDocumentRows().isEmpty()) {
            Set<String> overrides = new LinkedHashSet<>(List.of("doc_id"));
            for (String column : columns)
                if ((BranchFields.DOCUMENT.derived().contains(column) && !"data_asc".equals(column)) || "virtual_path".equals(column))
                    overrides.add(column);
            List<Map<String,Object>> rows = new ArrayList<>();
            view.derivedDocumentRows().forEach((id, derived) -> {
                Map<String,Object> row = new LinkedHashMap<>(derived);
                row.put("doc_id", id);
                rows.add(row);
            });
            sql.append(" UNION ALL SELECT ").append(columns.stream()
                .map(column -> overrides.contains(column) ? workColumn(column) : liveColumn(column)).collect(Collectors.joining(",")))
                .append(" FROM documents l JOIN ").append(jsonTable(overrides)).append(" ON l.doc_id=j.doc_id");
            arguments.add(jsonRows(rows, overrides));
        }
        return new Fragment(sql.append(") ").append(alias).toString(), arguments);
    }

    static boolean oracleDocumentClob(String name) {
        return Constants.DB_TYPE == Constants.DB_ORACLE && LEGACY_TEXT.contains(name);
    }

    private static String workColumn(String name) {
        // Convert JSON_TABLE value LOBs to ordinary CLOBs for UNION ALL with persistent LIVE locators.
        if (oracleDocumentClob(name)) return "SUBSTR(j." + name + ",1)";
        return "j." + name;
    }

    private static String liveColumn(String name) {
        if (Constants.DB_TYPE == Constants.DB_MSSQL && LEGACY_TEXT.contains(name))
            return "CAST(l." + name + " AS nvarchar(max))";
        if (Constants.DB_TYPE == Constants.DB_ORACLE && columnType(name) == PreviewType.TEXT)
            return "TO_CHAR(l." + name + ")";
        return "l." + name;
    }

    private static String jsonRows(Collection<Map<String,Object>> rows, Collection<String> columns) {
        List<Map<String,Object>> projected = new ArrayList<>(rows.size());
        for (var source : rows) {
            Map<String,Object> row = new LinkedHashMap<>();
            for (String column : columns) {
                Object value = source.get(column);
                if (Constants.DB_TYPE == Constants.DB_ORACLE && value instanceof Boolean flag) value = flag ? 1 : 0;
                row.put(column, value instanceof Timestamp date ? date.toString().replace(' ', 'T') : value);
            }
            projected.add(row);
        }
        try { return JSON.writeValueAsString(projected); }
        catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalStateException("Cannot serialize preview rows", ex); }
    }

    private enum PreviewType {
        TEXT("longtext", "nvarchar(max)", "VARCHAR2(4000)"), LOB("longtext", "nvarchar(max)", "CLOB"),
        INTEGER("int", "int", "NUMBER"), FLAG("boolean", "bit", "NUMBER"), DATE("datetime(6)", "datetime", "TIMESTAMP");

        final String maria, sqlServer, oracle;

        PreviewType(String maria, String sqlServer, String oracle) {
            this.maria = maria;
            this.sqlServer = sqlServer;
            this.oracle = oracle;
        }
    }

    private static PreviewType columnType(String column) {
        if (LEGACY_TEXT.contains(column)) return PreviewType.LOB;
        return switch (column) {
            case "date_created", "event_date", "publish_start", "publish_end", "file_change" -> PreviewType.DATE;
            case "available", "searchable", "cacheable", "show_in_menu", "show_in_navbar", "show_in_sitemap",
                "logged_show_in_menu", "logged_show_in_navbar", "logged_show_in_sitemap", "require_ssl", "url_inherit_group",
                "generate_url_from_title", "disable_after_end", "publish_after_start", "planned_media_initialized" -> PreviewType.FLAG;
            case "follow_links", "sort_priority", "views_total", "views_month", "forum_count", "sync_status",
                "root_group_l1", "root_group_l2", "root_group_l3" -> PreviewType.INTEGER;
            default -> column.endsWith("_id") || column.endsWith("_docid") ? PreviewType.INTEGER : PreviewType.TEXT;
        };
    }

    private static String jsonTable(Collection<String> columns) {
        if (Constants.DB_TYPE == Constants.DB_PGSQL)
            return "json_populate_recordset(NULL::branch_documents, CAST(? AS json)) j";
        List<String> fields = new ArrayList<>();
        for (String column : columns) {
            PreviewType type = columnType(column);
            String definition = switch (Constants.DB_TYPE) {
                case Constants.DB_ORACLE -> type.oracle + " PATH '$." + column + "' ERROR ON ERROR";
                case Constants.DB_MSSQL -> type.sqlServer;
                default -> type.maria + " PATH '$." + column + "'";
            };
            fields.add(column + " " + definition);
        }
        String schema = String.join(",", fields);
        if (Constants.DB_TYPE == Constants.DB_MSSQL) return "OPENJSON(CAST(? AS nvarchar(max))) WITH (" + schema + ") j";
        return "JSON_TABLE(?, '$[*]' COLUMNS (" + schema + ")) j";
    }

    static String notEmpty(String column) {
        return Constants.DB_TYPE == Constants.DB_ORACLE ? column + " IS NOT NULL" : "(" + column + " IS NOT NULL AND " + column + "<>'')";
    }

    /** Locks an existing row, or its key range when publication will insert a reserved ID. */
    static String lockedSelect(String columns, String table, String predicate) {
        if (Constants.DB_TYPE == Constants.DB_MSSQL)
            return "SELECT " + columns + " FROM " + table + " WITH (UPDLOCK,HOLDLOCK) WHERE " + predicate;
        return "SELECT " + columns + " FROM " + table + " WHERE " + predicate + " FOR UPDATE";
    }

}
