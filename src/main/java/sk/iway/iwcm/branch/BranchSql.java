package sk.iway.iwcm.branch;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/** Explicit effective SQL sources, composed before filtering, counting or paging. */
public final class BranchSql {
    private BranchSql() { }
    public record Fragment(String sql, List<Object> arguments) {
        public Fragment { arguments = Collections.unmodifiableList(new ArrayList<>(arguments)); }
        public int bind(PreparedStatement statement,int firstIndex) throws SQLException {
            int index=firstIndex; for (Object value:arguments) statement.setObject(index++,value); return index;
        }
    }
    public static Fragment documentSource(String alias) {
        if (!alias.matches("[a-zA-Z][a-zA-Z0-9_]*")) throw new IllegalArgumentException("Invalid internal SQL alias");
        BranchView view = BranchView.current();
        if (view == null) return new Fragment("documents "+alias,List.of());
        List<String> columns = new ArrayList<>(BranchFields.DOCUMENT.columns());
        List<Object> arguments = new ArrayList<>();
        String projection = columns.stream().map(column->"l.`"+column+"`").collect(Collectors.joining(","));
        StringBuilder sql = new StringBuilder("(SELECT "+projection+" FROM documents l WHERE ");
        List<Integer> groups=view.groups().stream().map(sk.iway.iwcm.doc.GroupDetails::getGroupId).toList();
        if (groups.isEmpty()) sql.append("1=0");
        else { sql.append("l.group_id IN (").append(placeholders(groups.size())).append(")"); arguments.addAll(groups); }
        Set<String> replaced=view.selectedDocumentIds();
        if (!replaced.isEmpty()) { sql.append(" AND l.doc_id NOT IN (").append(placeholders(replaced.size())).append(")"); for (String id:replaced) arguments.add(Integer.parseInt(id)); }
        for (Map<String,Object> row:view.documentRows()) {
            sql.append(" UNION ALL SELECT ").append(placeholders(columns.size()));
            for (String column:columns) arguments.add(row.get(column));
        }
        for (var entry:view.derivedDocumentRows().entrySet()) {
            sql.append(" UNION ALL SELECT ");
            List<String> fields = new ArrayList<>();
            for (String column:columns) {
                if (entry.getValue().containsKey(column)) { fields.add("?"); arguments.add(entry.getValue().get(column)); }
                else fields.add("l.`"+column+"`");
            }
            sql.append(String.join(",",fields)).append(" FROM documents l WHERE l.doc_id=?");
            arguments.add(entry.getKey());
        }
        return new Fragment(sql.append(") ").append(alias).toString(),arguments);
    }
    private static String placeholders(int count) { return String.join(",",Collections.nCopies(count,"?")); }
}
