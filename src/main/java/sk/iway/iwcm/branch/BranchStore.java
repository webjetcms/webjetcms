package sk.iway.iwcm.branch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.PkeyGenerator;

/**
 * Internal P02 storage operations for trusted services/fixtures, not an editor API.
 * Callers must authorize content and normalize URLs/references before supplying a row.
 * All row writes use one JDBC connection and increment the package revision.
 */
public final class BranchStore {
    private final BranchService.Connections connections;
    public BranchStore() { this(DBPool::getConnection); }
    BranchStore(BranchService.Connections connections) { this.connections = connections; }

    /** Reserves the native LIVE identity and persists WORK before returning it. */
    Map<String, Object> create(BranchContext context, BranchType type, Map<String, ?> values) throws SQLException {
        if (type == BranchType.FILE) throw new IllegalArgumentException("Use putFile for file identities");
        return write(context, connection -> create(connection, context, type, values));
    }

    Map<String,Object> create(Connection connection, BranchContext context, BranchType type, Map<String,?> values) throws SQLException {
        int id = type == BranchType.MEDIA ? Math.toIntExact(PkeyGenerator.getNextValueAsLongStrict("media")) : reserveNative(connection, type);
        Map<String, Object> row = new LinkedHashMap<>(values);
        row.put(type.idColumn(), id);
        if (type == BranchType.MEDIA && !"documents".equals(row.get("media_fk_table_name")))
            throw new IllegalArgumentException("Only document-owned media are supported");
        BranchSnapshot snapshot = snapshot(type, row);
        insertWork(connection, context, type, row, BranchSnapshot.absent(), snapshot, "UPSERT");
        return readWork(connection, context.branchId(), type, id).orElseThrow();
    }

    /** Captures BASE once, rejects a stale first edit, and preserves BASE on subsequent edits. */
    Map<String, Object> put(BranchContext context, BranchType type, int id, Map<String, ?> values,
            String expectedHash, boolean delete) throws SQLException {
        if (type == BranchType.FILE || id <= 0) throw new IllegalArgumentException("Invalid content identity");
        return write(context, connection -> put(connection, context, type, id, values, expectedHash, delete));
    }

    Map<String,Object> put(Connection connection, BranchContext context, BranchType type, int id, Map<String,?> values, String expectedHash, boolean delete) throws SQLException {
        Optional<Map<String, Object>> existing = readWork(connection, context.branchId(), type, id);
        BranchSnapshot base;
        if (existing.isPresent()) {
            Map<String, Object> old = existing.get();
            BranchSnapshot oldWork = "DELETE".equals(old.get("operation")) ? deletedSnapshot(type, old) : snapshot(type, old);
            if (!oldWork.hash().equals(expectedHash)) throw new IllegalStateException("WORK changed; reload the object");
            base = BranchSnapshot.fromJson((String)old.get("base_snapshot"));
        } else {
            Map<String, Object> live = readLive(connection, type, id).orElseThrow(() -> new IllegalStateException("LIVE object is absent"));
            base = snapshot(type, live);
            if (!base.hash().equals(expectedHash)) throw new IllegalStateException("LIVE changed; reload the object");
        }
        Map<String, Object> row = new LinkedHashMap<>(values);
        row.put(type.idColumn(), id);
        if (delete && base.state() == BranchSnapshot.State.ABSENT)
            throw new IllegalArgumentException("Remove a new working item instead of storing a DELETE over ABSENT");
        BranchSnapshot work = delete ? deletedSnapshot(type,row) : snapshot(type,row);
        insertWork(connection, context, type, row, base, work, delete ? "DELETE" : "UPSERT");
        return readWork(connection,context.branchId(),type,id).orElseThrow();
    }

    /** Persists fixture/service file metadata; P05 adds staging bytes and editor authorization. */
    Map<String, Object> putFile(BranchContext context, String logicalPath, String kind, BranchSnapshot base,
            String workContentHash, Long sizeBytes, String mimeType, boolean delete) throws SQLException {
        String path = BranchFilePaths.normalize(logicalPath);
        if (!"FILE".equals(kind) && !"DIRECTORY".equals(kind)) throw new IllegalArgumentException("Invalid entry kind");
        if ("DIRECTORY".equals(kind) && (delete || workContentHash != null || sizeBytes != null || mimeType != null))
            throw new IllegalArgumentException("Directories support only a marker UPSERT without binary metadata");
        if ("FILE".equals(kind) && !delete && (sizeBytes == null || sizeBytes < 0))
            throw new IllegalArgumentException("Missing file byte size");
        if (delete && base.state() == BranchSnapshot.State.ABSENT)
            throw new IllegalArgumentException("Remove a new working file instead of storing DELETE over ABSENT");
        if (base.state() != BranchSnapshot.State.ABSENT && !path.equals(base.fields().get("logical_path")))
            throw new IllegalArgumentException("File BASE must belong to the same normalized path");
        if (delete && "DIRECTORY".equals(base.fields().get("entry_kind")))
            throw new IllegalArgumentException("Directory deletion is not supported");
        return write(context, connection -> putFile(connection,context,path,kind,base,workContentHash,sizeBytes,mimeType,delete));
    }

    /** Updates one logical file while retaining the original BASE and internal identity. */
    Map<String,Object> putFile(Connection connection, BranchContext context, String path, String kind,
            BranchSnapshot base, String workContentHash, Long sizeBytes, String mimeType, boolean delete) throws SQLException {
        String galleryPath = "DIRECTORY".equals(kind) ? path : path.substring(0,path.lastIndexOf('/'));
        if (sk.iway.iwcm.gallery.GalleryDB.isGalleryFolder(galleryPath))
            throw new IllegalArgumentException("Gallery paths are read-only");
        Optional<Map<String,Object>> old=readWork(connection,context.branchId(),BranchType.FILE,path);
        if(old.isPresent()) base=BranchSnapshot.fromJson((String)old.get().get("base_snapshot"));
        if("DIRECTORY".equals(kind) && ("FILE".equals(base.fields().get("entry_kind"))
                ||old.isPresent()&&"FILE".equals(old.get().get("entry_kind"))))
            throw new IllegalArgumentException("A file identity cannot be replaced with a directory marker");
        Map<String,Object> row=new LinkedHashMap<>();
        row.put("branch_id",context.branchId()); row.put("operation",delete ? "DELETE" : "UPSERT");
        row.put("base_exists",base.state()!=BranchSnapshot.State.ABSENT); row.put("base_snapshot",base.json()); row.put("base_hash",base.hash());
        row.put("updated_by",context.userId());row.put("updated_at",Timestamp.from(Instant.now()));row.put("apply_state","PENDING");
        row.put("logical_path",path);row.put("entry_kind",kind);row.put("path_key",BranchFilePaths.key(path));
        row.put("base_content_hash",base.fields().get("content_hash"));row.put("work_content_hash",delete ? null : workContentHash);
        row.put("size_bytes",sizeBytes);row.put("mime_type",mimeType);
        if(old.isEmpty()) insert(connection,"branch_files",row);
        else {
            for(String decision:List.of("resolved_snapshot","resolved_choice","resolved_live_hash","resolved_work_hash","apply_phase","applied_hash","apply_error")) row.put(decision,null);
            String assignments=String.join(",",row.keySet().stream().map(name->"`"+name+"`=?").toList());
            try(PreparedStatement statement=connection.prepareStatement("UPDATE branch_files SET "+assignments+" WHERE row_id=?")) {
                int index=1;for(Object value:row.values())statement.setObject(index++,value);
                statement.setLong(index,((Number)old.get().get("row_id")).longValue());statement.executeUpdate();
            }
        }
        return readWork(connection,context.branchId(),BranchType.FILE,path).orElseThrow();
    }

    void remove(Connection connection, BranchContext context, BranchType type, int id, String expectedHash) throws SQLException {
        Map<String,Object> row = readWork(connection,context.branchId(),type,id).orElseThrow(() -> new IllegalStateException("No working change"));
        if (!change(type,row).work().hash().equals(expectedHash)) throw new IllegalStateException("WORK changed; reload the object");
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM `"+type.workTable()+"` WHERE branch_id=? AND "+type.idColumn()+"=?")) {
            statement.setLong(1,context.branchId()); statement.setInt(2,id); statement.executeUpdate();
        }
    }

    <T> T write(BranchContext context, Work<T> work) throws SQLException {
        if (context.mode() != BranchContext.Mode.ADMIN || context.branchId() <= 0 || context.userId() <= 0)
            throw new IllegalArgumentException("An explicit ADMIN branch context is required");
        try (Connection connection = connections.open()) {
            BranchReadiness.verify(connection);
            connection.setAutoCommit(false);
            boolean completed = false;
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT state FROM branches WHERE id=? AND domain_id=? FOR UPDATE")) {
                    statement.setLong(1, context.branchId()); statement.setInt(2, context.domainId());
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next() || !"DRAFT".equals(result.getString(1))) throw new IllegalStateException("Package is absent or not editable");
                    }
                }
                T result = work.apply(connection);
                try (PreparedStatement statement = connection.prepareStatement("UPDATE branches SET revision=revision+1,updated_by=?,updated_at=? WHERE id=?")) {
                    statement.setInt(1,context.userId()); statement.setTimestamp(2,Timestamp.from(Instant.now())); statement.setLong(3,context.branchId()); statement.executeUpdate();
                }
                connection.commit();
                completed = true;
                return result;
            } catch (SQLException | RuntimeException ex) {
                try { connection.rollback(); completed = true; }
                catch (SQLException rollback) {
                    ex.addSuppressed(rollback);
                    try { connection.abort(Runnable::run); } catch (SQLException abort) { ex.addSuppressed(abort); }
                }
                throw ex;
            } finally { if (completed && !connection.isClosed()) connection.setAutoCommit(true); }
        }
    }

    @FunctionalInterface interface Work<T> { T apply(Connection connection) throws SQLException; }

    static int reserveNative(Connection connection, BranchType type) throws SQLException {
        if (connection.getAutoCommit()) throw new SQLException("Reservation requires a transaction");
        String sql = switch (type) {
            case DOCUMENT -> "INSERT INTO documents(title,data,data_asc,navbar) VALUES ('','','','')";
            case GROUP -> "INSERT INTO `groups`(group_name) VALUES ('')";
            default -> throw new IllegalArgumentException("No native allocator for " + type);
        };
        int id;
        try (PreparedStatement statement = connection.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            statement.executeUpdate();
            try (ResultSet result = statement.getGeneratedKeys()) {
                if (!result.next()) throw new SQLException("Native identity was not returned");
                long generated = result.getLong(1);
                if (generated <= 0 || generated > Integer.MAX_VALUE) throw new SQLException("Content ID range exhausted");
                id = (int)generated;
            }
        }
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM `"+type.liveTable()+"` WHERE "+type.idColumn()+"=?")) {
            statement.setInt(1,id);
            if (statement.executeUpdate()!=1) throw new SQLException("Technical reservation row disappeared");
        }
        return id;
    }

    private static void insertWork(Connection connection, BranchContext context, BranchType type, Map<String, ?> row,
            BranchSnapshot base, BranchSnapshot work, String operation) throws SQLException {
        Map<String, Object> columns = new LinkedHashMap<>();
        columns.put("branch_id",context.branchId()); columns.put("operation",operation);
        columns.put("base_exists",base.state()!=BranchSnapshot.State.ABSENT);
        columns.put("base_snapshot",base.json()); columns.put("base_hash",base.hash());
        columns.put("updated_by",context.userId()); columns.put("updated_at",Timestamp.from(Instant.now()));
        columns.put("apply_state","PENDING");
        for (String column:BranchFields.forType(type).columns()) {
            if (!row.containsKey(column)) throw new IllegalArgumentException("Missing typed field " + column);
            columns.put(column,"perex_group".equals(column) ? BranchFields.idTokens(row.get(column)) : row.get(column));
        }
        if (type==BranchType.MEDIA) columns.put("group_ids_snapshot",BranchFields.normalizedIds(row.get("group_ids_snapshot")).toString());
        Object key = row.get(type.idColumn());
        Optional<Map<String, Object>> old = readWork(connection,context.branchId(),type,key);
        if (old.isEmpty()) insert(connection,type.workTable(),columns);
        else {
            for (String decision : List.of("resolved_snapshot","resolved_choice","resolved_live_hash","resolved_work_hash","apply_phase","applied_hash","apply_error")) columns.put(decision,null);
            String assignments = String.join(",",columns.keySet().stream().map(name -> "`"+name+"`=?").toList());
            try (PreparedStatement statement = connection.prepareStatement("UPDATE `"+type.workTable()+"` SET "+assignments+" WHERE row_id=?")) {
                int index=1;for (Object value:columns.values())statement.setObject(index++,value);
                statement.setLong(index,((Number)old.get().get("row_id")).longValue());statement.executeUpdate();
            }
        }
    }

    static void insert(Connection connection,String table,Map<String, ?> columns) throws SQLException {
        String names=String.join(",",columns.keySet().stream().map(name->"`"+name+"`").toList());
        String placeholders=String.join(",",java.util.Collections.nCopies(columns.size(),"?"));
        try (PreparedStatement statement=connection.prepareStatement("INSERT INTO `"+table+"` ("+names+") VALUES ("+placeholders+")")) {
            int i=1; for (Object value:columns.values()) statement.setObject(i++,value); statement.executeUpdate();
        }
    }

    static Optional<Map<String, Object>> readLive(Connection connection,BranchType type,int id) throws SQLException {
        Optional<Map<String, Object>> row=read(connection,type.liveTable(),type.idColumn(),id,null);
        if (type==BranchType.MEDIA && row.isPresent()) {
            List<Integer> groups=new ArrayList<>();
            try (PreparedStatement statement=connection.prepareStatement("SELECT media_group_id FROM media_group_to_media WHERE media_id=? ORDER BY media_group_id")) {
                statement.setInt(1,id); try (ResultSet result=statement.executeQuery()) {while (result.next())groups.add(result.getInt(1));}
            }
            row.get().put("group_ids_snapshot",groups.toString());
        }
        return row;
    }

    static Optional<Map<String, Object>> readWork(Connection connection,long branchId,BranchType type,Object key) throws SQLException {
        return read(connection,type.workTable(),type.idColumn(),key,branchId);
    }

    private static Optional<Map<String, Object>> read(Connection connection,String table,String key,Object value,Long branchId) throws SQLException {
        try (PreparedStatement statement=connection.prepareStatement("SELECT * FROM `"+table+"` WHERE `"+key+"`=?"+(branchId==null?"":" AND branch_id=?"))) {
            statement.setObject(1,value); if (branchId!=null) statement.setLong(2,branchId);
            try (ResultSet result=statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                Map<String, Object> row=new LinkedHashMap<>();
                for (int i=1;i<=result.getMetaData().getColumnCount();i++) row.put(result.getMetaData().getColumnName(i),readColumn(result,i));
                return Optional.of(row);
            }
        }
    }

    /** MariaDB TINYINT(1) enum values such as -1 must not become Boolean true. */
    static Object readColumn(ResultSet result,int index)throws SQLException {
        return readColumn(result,index,result.getMetaData().getColumnName(index));
    }

    static Object readColumn(ResultSet result,int index,String name)throws SQLException {
        if(java.util.Set.of("menu_type","logged_menu_type","follow_links","show_in_navbar","show_in_sitemap","logged_show_in_navbar","logged_show_in_sitemap").contains(name)) {
            int value=result.getInt(index);return result.wasNull()?null:value;
        }
        return result.getObject(index);
    }

    /** Converts a typed row to the common comparison envelope without a handler registry. */
    static BranchChange change(BranchType type, Map<String, ?> row) {
        BranchSnapshot base = BranchSnapshot.fromJson((String)row.get("base_snapshot"));
        if (!base.hash().equals(row.get("base_hash"))) throw new IllegalStateException("Corrupt BASE snapshot");
        BranchChange.Operation operation = BranchChange.Operation.valueOf((String)row.get("operation"));
        BranchSnapshot work;
        if (operation == BranchChange.Operation.DELETE) work = deletedSnapshot(type,row);
        else if (type == BranchType.FILE) work = BranchSnapshot.file(BranchSnapshot.State.ACTIVE,
            (String)row.get("logical_path"),(String)row.get("entry_kind"),(String)row.get("work_content_hash"));
        else work = snapshot(type,row);
        BranchDecision.StoredChoice choice = row.get("resolved_choice") == null ? null : new BranchDecision.StoredChoice(
            BranchDecision.Source.valueOf((String)row.get("resolved_choice")),
            (String)row.get("resolved_live_hash"),(String)row.get("resolved_work_hash"));
        String key = type == BranchType.FILE ? (String)row.get("logical_path") : String.valueOf(((Number)row.get(type.idColumn())).intValue());
        return new BranchChange(((Number)row.get("branch_id")).longValue(),((Number)row.get("row_id")).longValue(),type,key,operation,base,work,choice);
    }

    static BranchSnapshot deletedSnapshot(BranchType type, Map<String, ?> row) {
        return switch (type) {
            case DOCUMENT -> BranchDocumentMapper.snapshot(row,BranchSnapshot.State.TRASH);
            case GROUP -> BranchGroupMapper.snapshot(row,BranchSnapshot.State.TRASH);
            case MEDIA, FILE -> BranchSnapshot.absent();
        };
    }

    static BranchSnapshot snapshot(BranchType type, Map<String, ?> row) {
        return switch (type) {
            case DOCUMENT -> BranchDocumentMapper.snapshot(row);
            case GROUP -> BranchGroupMapper.snapshot(row);
            case MEDIA -> BranchMediaMapper.snapshot(row,row.get("group_ids_snapshot"));
            case FILE -> throw new IllegalArgumentException("File snapshot uses its logical path and byte hash");
        };
    }
}
