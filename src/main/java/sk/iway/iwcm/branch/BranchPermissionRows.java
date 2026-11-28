package sk.iway.iwcm.branch;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import sk.iway.iwcm.DB;

/** Request-local source and destination metadata for the package's item permission checks. */
final class BranchPermissionRows {
    private record Key(BranchType type, int id, boolean work) { }
    private final Connection connection;
    private final long branchId;
    private final Map<Key,Map<String,Object>> rows = new HashMap<>();

    BranchPermissionRows(Connection connection,long branchId) {
        this.connection = connection;
        this.branchId = branchId;
    }

    static String columns(BranchType type) {
        return switch (type) {
            case DOCUMENT -> "doc_id,group_id,password_protected";
            case GROUP -> "group_id,parent_group_id,domain_name,internal,group_name";
            case MEDIA -> "media_id,media_fk_id";
            case FILE -> "logical_path";
        };
    }

    /** The polling query already has the WORK rows. Load their LIVE counterparts in one query. */
    void loadSources(BranchType type,Collection<Map<String,Object>> workRows) throws SQLException {
        if (workRows.isEmpty() || type == BranchType.FILE) return;
        var ids = new ArrayList<Integer>(workRows.size());
        for (var row : workRows) {
            int id = BranchMergeService.number(row,type.idColumn());
            rows.put(new Key(type,id,true),row);
            rows.put(new Key(type,id,false),null);
            ids.add(id);
        }
        String sql = "SELECT "+columns(type)+" FROM "+type.liveTable()+" WHERE "+DB.getInCondition(type.idColumn(),ids);
        try (var s = connection.prepareStatement(sql); var r = s.executeQuery()) {
            while (r.next()) rows.put(new Key(type,r.getInt(type.idColumn()),false),BranchPublishRows.row(r));
        }
    }

    Map<String,Object> get(BranchType type,int id,boolean work) throws SQLException {
        Key key = new Key(type,id,work);
        if (!rows.containsKey(key)) {
            String sql = "SELECT "+columns(type)+" FROM "+(work ? type.workTable() : type.liveTable())+" WHERE "+type.idColumn()+"=?"+(work ? " AND branch_id=?" : "");
            try (var s = connection.prepareStatement(sql)) {
                s.setInt(1,id);
                if (work) s.setLong(2,branchId);
                try (var r = s.executeQuery()) {
                    rows.put(key,r.next() ? BranchPublishRows.row(r) : null);
                }
            }
        }
        return rows.get(key);
    }

    Map<Integer,Map<String,Object>> liveAncestors(int id) throws SQLException {
        Map<Integer,Map<String,Object>> ancestors = new HashMap<>();
        while (id > 0 && !ancestors.containsKey(id)) {
            var folder = get(BranchType.GROUP,id,false);
            if (folder == null) break;
            ancestors.put(id,folder);
            id = BranchMergeService.number(folder,"parent_group_id");
        }
        return ancestors;
    }

    Map<String,Object> effective(BranchType type,int id) throws SQLException {
        var work = get(type,id,true);
        return work != null ? work : get(type,id,false);
    }
}
