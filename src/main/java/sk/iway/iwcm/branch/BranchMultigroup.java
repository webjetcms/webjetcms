package sk.iway.iwcm.branch;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.tags.support.ResponseUtils;

/** Existing LIVE links used to synchronize document rows without writing the mapping table. */
record BranchMultigroup(int masterId, Map<Integer, Boolean> copies) {
    static BranchMultigroup read(Connection connection, int docId) throws SQLException {
        String sql = """
            SELECT master_id, doc_id, redirect FROM multigroup_mapping
            WHERE master_id = COALESCE((SELECT master_id FROM multigroup_mapping WHERE doc_id = ?), ?)
            ORDER BY doc_id
            """;
        int masterId = docId;
        Map<Integer, Boolean> copies = new LinkedHashMap<>();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, docId);
            statement.setInt(2, docId);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    masterId = rows.getInt("master_id");
                    copies.put(rows.getInt("doc_id"), rows.getBoolean("redirect"));
                }
            }
        }
        return new BranchMultigroup(masterId, copies);
    }

    boolean matchesFolders(List<GroupDetails> submitted, BranchView view) {
        if (submitted == null) return true;
        Set<Integer> folders = new HashSet<>();
        for (int id : copies.keySet()) {
            DocDetails copy = view.metadataDocument(id);
            if (copy == null) return false;
            folders.add(copy.getGroupId());
        }
        return folders.size() == submitted.size() && submitted.stream()
            .allMatch(group -> group != null && folders.remove(group.getGroupId()));
    }

    private String hash() {
        return copies.isEmpty() ? null : DigestUtils.sha256Hex(masterId + ":" + copies);
    }

    static String label(String title, int docId) {
        return ResponseUtils.filter(Objects.toString(title, "")) + " (#" + docId + ")";
    }

    void recordLinks(Map<String, Object> row) {
        if (row.containsKey("base_snapshot")) checkUnchanged(row);
        row.put("multigroup_hash", hash());
    }

    void checkUnchanged(Map<String, Object> work) {
        if (!Objects.equals(work.get("multigroup_hash"), hash()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, Prop.getInstance().getText("branches.multigroup.changed",
                label((String) work.get("title"), ((Number) work.get("doc_id")).intValue())));
    }
}
