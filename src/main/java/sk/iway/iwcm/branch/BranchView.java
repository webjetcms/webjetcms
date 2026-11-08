package sk.iway.iwcm.branch;

import java.sql.*;
import java.util.*;
import sk.iway.iwcm.*;
import sk.iway.iwcm.doc.*;
import sk.iway.iwcm.i18n.Prop;

/** One verified request's effective content and whole-object selection decisions. */
public final class BranchView {
    private static final String ATTRIBUTE = BranchView.class.getName();
    private final BranchContext context;
    private long previewAt = System.currentTimeMillis();
    private String name;
    private boolean conflicts;
    private final Map<Integer, Map<String, Object>> liveGroups = new LinkedHashMap<>();
    private final Map<Integer, Map<String, Object>> groupRows = new LinkedHashMap<>();
    private final Map<BranchType, List<Map<String, Object>>> work = new EnumMap<>(BranchType.class);
    private final Map<String,Boolean> sourceDecisions = new HashMap<>();
    private final Map<BranchType, Set<String>> selected = new EnumMap<>(BranchType.class);
    private final List<Map<String, Object>> documentRows = new ArrayList<>();
    private final Map<Integer,Map<String,Object>> derivedDocuments = new LinkedHashMap<>();
    private final Map<Integer, GroupDetails> groups = new LinkedHashMap<>();
    private GroupsDB groupsDb;
    private Map<Integer,DocDetails> metadata;

    private BranchView(BranchContext context) { this.context = context; }

    /** Returns null for LIVE; a failed branch load never falls back to production. */
    public static BranchView current() {
        BranchContext context = BranchContexts.current();
        if (!context.isBranch()) return null;
        Object cached = RequestBean.getAttribute(ATTRIBUTE);
        if (cached instanceof BranchView view && view.context.equals(context)) return view;
        BranchView view = new BranchView(context);
        RequestBean.setAttribute(ATTRIBUTE, view);
        try { view.load(); return view; }
        catch (SQLException | RuntimeException ex) {
            RequestBean.setAttribute(ATTRIBUTE, null);
            if (ex instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Cannot load branch content", ex);
        }
    }

    private void load() throws SQLException {
        try (Connection connection = DBPool.getConnection()) { load(connection); }
    }

    static void invalidate() { RequestBean.setAttribute(ATTRIBUTE, null); }

    static BranchView reload(Connection connection) throws SQLException {
        BranchView view = new BranchView(BranchContexts.current());
        RequestBean.setAttribute(ATTRIBUTE, view);
        view.load(connection);
        return view;
    }

    private void load(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT state,publish_at,name FROM branches WHERE id=? AND domain_name=?")) {
            statement.setLong(1, context.branchId()); statement.setString(2, context.domainName());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalStateException("Branch is absent in the verified domain");
                Timestamp date = result.getTimestamp(2);
                name = result.getString(3);
                if ("SCHEDULED".equals(result.getString(1)) && date != null && date.getTime() > previewAt) previewAt = date.getTime();
            }
        }
        for (Map<String, Object> row : read(connection, "SELECT * FROM `groups`", false)) liveGroups.put(id(row,"group_id"), row);
        groupRows.putAll(liveGroups);
        for (BranchType type : BranchType.values()) {
            work.put(type, read(connection, "SELECT * FROM `" + type.workTable() + "` WHERE branch_id=? ORDER BY row_id", true));
            selected.put(type, new LinkedHashSet<>());
        }
        for (Map<String, Object> row : workRows(BranchType.GROUP)) {
            int id = id(row,"group_id");
            Map<String, Object> live = liveGroups.get(id);
            BranchSnapshot snapshot = live == null ? BranchSnapshot.absent() : BranchGroupMapper.snapshot(live,
                inTrash(id, liveGroups) ? BranchSnapshot.State.TRASH : BranchSnapshot.State.ACTIVE);
            if (!select(BranchType.GROUP,row,snapshot)) continue;
            if ("DELETE".equals(row.get("operation"))) groupRows.remove(id);
            else groupRows.put(id, BranchGroupMapper.project(row,live,BranchGroupMapper.initialValues(id,context.domainName())));
        }
        for (var entry : new ArrayList<>(groupRows.entrySet())) {
            if (!visibleGroup(entry.getKey(),new HashSet<>())) groupRows.remove(entry.getKey());
        }
        List<Map<String, Object>> ordered = new ArrayList<>(groupRows.values());
        ordered.sort(Comparator.comparingInt((Map<String,Object> row) -> id(row,"sort_priority"))
            .thenComparing(row -> Objects.toString(row.get("group_name"),"")));
        for (Map<String, Object> row : ordered) try (var result = BranchRows.result(row)) {
            GroupDetails group = GroupsDB.fillFieldsByResultSet(result,false);
            if (context.mode()==BranchContext.Mode.ADMIN) {
                GroupEditorField fields=new GroupEditorField();
                fields.setBranchHash(BranchGroupMapper.snapshot(row).hash());
                group.setEditorFields(fields);
            }
            groups.put(group.getGroupId(),group);
        }
        groupsDb = GroupsDB.forBranch(new ArrayList<>(groups.values()));
        for (Map<String, Object> row : workRows(BranchType.DOCUMENT)) {
            int id = id(row,"doc_id");
            Map<String, Object> live = BranchStore.readLive(connection,BranchType.DOCUMENT,id).orElse(null);
            BranchSnapshot snapshot = live == null ? BranchSnapshot.absent() : BranchDocumentMapper.snapshot(live,
                inTrash(id(live,"group_id"), liveGroups) ? BranchSnapshot.State.TRASH : BranchSnapshot.State.ACTIVE);
            if (!select(BranchType.DOCUMENT,row,snapshot) || "DELETE".equals(row.get("operation"))) continue;
            if (!groups.containsKey(id(row,"group_id"))) continue;
            Map<String,Object> initial = row;
            Map<String,Object> derived = derived(row,live != null && id(live,"group_id") != id(row,"group_id"));
            Map<String,Object> projection = new LinkedHashMap<>(BranchDocumentMapper.project(row,live,initial,derived));
            projection.put("virtual_path",derived.get("file_name"));
            documentRows.add(projection);
        }
        // Recompute only metadata of pages below changed ancestors; HTML remains in LIVE SQL.
        List<Integer> affected = groups.values().stream().map(GroupDetails::getGroupId).filter(this::changedAncestor).toList();
        if (!affected.isEmpty()) {
            String ids = affected.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
            for (Map<String,Object> live : read(connection,"SELECT "+documentColumns("",true)+" FROM documents WHERE group_id IN ("+ids+")",false)) {
                int id = id(live,"doc_id");
                if (usesWork(BranchType.DOCUMENT,Integer.toString(id))) continue;
                Map<String,Object> calculated = derived(live);
                calculated.put("virtual_path",calculated.get("file_name"));
                calculated.remove("data_asc");
                derivedDocuments.put(id,calculated);
            }
        }
        // Resolve generated navigation links only after the complete page/tree relation exists.
        metadata();
        for (GroupDetails group:groups.values()) {
            if (metadata.containsKey(group.getDefaultDocId())) group.setDefaultDocId(group.getDefaultDocId());
        }
    }


    private boolean changedAncestor(int groupId) {
        Set<Integer> visited = new HashSet<>();
        while (groupId > 0 && visited.add(groupId)) {
            if (usesWork(BranchType.GROUP,Integer.toString(groupId))) return true;
            GroupDetails group = groups.get(groupId); groupId = group == null ? 0 : group.getParentGroupId();
        }
        return false;
    }

    private boolean select(BranchType type, Map<String,Object> row, BranchSnapshot live) {
        BranchChange change = BranchStore.change(type,row);
        Boolean remembered = sourceDecisions.get(change.identity());
        if (remembered != null) return remembered;
        boolean useWork;
        if (context.mode() == BranchContext.Mode.ADMIN) useWork = true;
        else {
            BranchDecision.Result decision = change.decide(live);
            useWork = decision.source() == BranchDecision.Source.WORK;
            conflicts |= decision.conflict();
        }
        sourceDecisions.put(change.identity(),useWork);
        if (useWork) selected.get(type).add(change.objectKey());
        return useWork;
    }

    /** Shared with media/files: apply the same P02 decision and remember the selected source. */
    public boolean selectWork(BranchType type, Map<String,Object> row, BranchSnapshot live) { return select(type,row,live); }
    public boolean usesWork(BranchType type, String key) { return selected.getOrDefault(type,Set.of()).contains(key); }
    public BranchContext context() { return context; }
    public long previewAt() { return previewAt; }
    public String name() { return name; }
    public boolean hasConflicts() { return conflicts; }
    public List<Map<String,Object>> workRows(BranchType type) { return Collections.unmodifiableList(work.get(type)); }
    public List<Map<String,Object>> documentRows() { return Collections.unmodifiableList(documentRows); }
    public Map<Integer,Map<String,Object>> derivedDocumentRows() { return Collections.unmodifiableMap(derivedDocuments); }
    public Set<String> selectedDocumentIds() { Set<String> replaced = new LinkedHashSet<>(selected.get(BranchType.DOCUMENT)); for (int id:derivedDocuments.keySet()) replaced.add(Integer.toString(id)); return Collections.unmodifiableSet(replaced); }
    public List<GroupDetails> groups() { return new ArrayList<>(groups.values()); }
    public GroupDetails group(int id) { return groups.get(id); }
    public GroupsDB groupsDb() { return groupsDb; }

    private boolean visibleGroup(int id, Set<Integer> path) {
        if (id == 0) return true;
        Map<String,Object> row = groupRows.get(id);
        if (row == null) return false;
        if (!path.add(id)) throw new IllegalStateException("Cycle in branch folder tree");
        String domain = Objects.toString(row.get("domain_name"),"");
        if (!domain.isEmpty() && !"default".equals(domain) && !domain.equals(context.domainName())) return false;
        if (inTrash(id,groupRows)) return false;
        return visibleGroup(id(row,"parent_group_id"),path);
    }

    private static boolean inTrash(int id, Map<Integer,Map<String,Object>> rows) {
        String trash = Prop.getInstance(Constants.getString("defaultLanguage")).getText("config.trash_dir");
        StringBuilder path = new StringBuilder(); Set<Integer> seen = new HashSet<>();
        while (id > 0 && seen.add(id)) {
            Map<String,Object> row = rows.get(id); if (row == null) break;
            path.insert(0,"/"+Objects.toString(row.get("group_name"),"")); id = id(row,"parent_group_id");
        }
        return DB.internationalToEnglish(path.toString()).toLowerCase(Locale.ROOT)
            .contains(DB.internationalToEnglish(trash).toLowerCase(Locale.ROOT));
    }

    private Map<String,Object> derived(Map<String,Object> row) throws SQLException { return derived(row,false); }

    private Map<String,Object> derived(Map<String,Object> row,boolean movedDocument) throws SQLException {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("data_asc", DB.internationalToEnglish(org.jsoup.Jsoup.parse(Objects.toString(row.get("data"),"")).text()).toLowerCase(Locale.ROOT));
        int[] roots = DocDB.getRootGroupL(id(row,"group_id"),null,0);
        for (int level=1;level<=3;level++) result.put("root_group_l"+level,roots[level-1] > 0 ? roots[level-1] : null);
        try (var rs = BranchRows.result(row)) {
            DocDetails doc = DocDB.getDocDetails(rs,false,true);
            if (movedDocument || changedUrlAncestor(doc.getGroupId())) prepareInheritedUrl(doc);
            result.put("file_name",DocDB.getURL(doc,groups.get(doc.getGroupId()),DocDB.getGroupDiskPath(groups(),doc.getGroupId())));
        }
        return result;
    }

    public DocDetails document(int id, boolean noData) {
        if (id <= 0) return null;
        BranchSql.Fragment source = BranchSql.documentSource("d");
        boolean editor=context.mode()==BranchContext.Mode.ADMIN;
        String rawColumns=editor ? BranchFields.DOCUMENT.supported().stream().map(column -> ",raw.`"+column+"` AS branch_raw_"+column).collect(java.util.stream.Collectors.joining()) : "";
        try (Connection connection = DBPool.getConnection(); PreparedStatement statement = connection.prepareStatement(
            "SELECT "+documentColumns("d",noData)+rawColumns+",u.title AS u_title,u.first_name,u.last_name,u.email,u.photo FROM "+source.sql()+" LEFT JOIN users u ON u.user_id=d.author_id"+(editor?" LEFT JOIN documents raw ON raw.doc_id=d.doc_id":"")+" WHERE d.doc_id=?")) {
            int index = source.bind(statement,1); statement.setInt(index,id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                DocDetails doc = DocDB.getDocDetails(result,noData); editorHash(doc,result); normalizeUrl(doc); return doc;
            }
        } catch (SQLException ex) { throw new IllegalStateException("Cannot load branch document",ex); }
    }

    /** Hash the actual row behind the displayed fields, never a later, independently fetched version. */
    private void editorHash(DocDetails doc,ResultSet result) throws SQLException {
        if(context.mode()!=BranchContext.Mode.ADMIN) return;
        Map<String,Object> row=workRows(BranchType.DOCUMENT).stream()
            .filter(workRow -> id(workRow,"doc_id")==doc.getDocId()).findFirst().orElse(null);
        if(row==null) {
            row=new LinkedHashMap<>();
            // Direct LIVE columns keep their physical JDBC types; UNION parameters can coerce them.
            for(String column:BranchFields.DOCUMENT.supported()) row.put(column,BranchStore.readColumn(result,result.findColumn("branch_raw_"+column),column));
        }
        DocEditorFields fields=new DocEditorFields();
        fields.setBranchHash(BranchDocumentMapper.snapshot(row).hash());
        doc.setEditorFields(fields);
    }

    String groupHash(int id) {
        Map<String,Object> row=groupRows.get(id);
        if(row==null) throw new IllegalStateException("Folder is absent from the displayed tree");
        return BranchGroupMapper.snapshot(row).hash();
    }

    public List<DocDetails> documents() { return documents(false); }
    public List<DocDetails> documents(boolean noData) {
        BranchSql.Fragment source = BranchSql.documentSource("d");
        try (Connection connection = DBPool.getConnection(); PreparedStatement statement = connection.prepareStatement(
            "SELECT "+documentColumns("d",noData)+",u.title AS u_title,u.first_name,u.last_name,u.email,u.photo FROM "+source.sql()+" LEFT JOIN users u ON u.user_id=d.author_id ORDER BY d.doc_id")) {
            source.bind(statement,1); List<DocDetails> result = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) { DocDetails doc = DocDB.getDocDetails(rows,noData); normalizeUrl(doc); result.add(doc); }
            }
            return result;
        } catch (SQLException ex) { throw new IllegalStateException("Cannot list branch documents",ex); }
    }

    private Map<Integer,DocDetails> metadata() {
        if (metadata == null) {
            metadata = new LinkedHashMap<>();
            for (DocDetails document:documents(true)) metadata.put(document.getDocId(),document);
        }
        return metadata;
    }

    DocDetails metadataDocument(int id) { return metadata().get(id); }
    int docIdFromUrl(String path) {
        String normalized=DocDB.normalizeVirtualPath(path);
        for (DocDetails doc:metadata().values()) if (normalized.equals(DocDB.normalizeVirtualPath(doc.getVirtualPath()))) return doc.getDocId();
        return -1;
    }

    public void normalizeUrl(DocDetails doc) {
        GroupDetails group = groups.get(doc.getGroupId());
        if (group != null) {
            doc.setVirtualPath(DocDB.getURL(doc,group,DocDB.getGroupDiskPath(groups(),doc.getGroupId())));
        }
    }

    private boolean changedUrlAncestor(int groupId) throws SQLException {
        Set<Integer> visited = new HashSet<>();
        while (groupId > 0 && visited.add(groupId)) {
            GroupDetails group = groups.get(groupId); if (group == null) return false;
            Map<String,Object> original = liveGroups.get(groupId);
            if (original == null || group.getParentGroupId() != id(original,"parent_group_id")
                || !Objects.equals(group.getUrlDirName(),liveUrlDir(original))) return true;
            groupId = group.getParentGroupId();
        }
        return false;
    }

    private static String liveUrlDir(Map<String,Object> row) throws SQLException {
        try (var result=BranchRows.result(row)) { return GroupsDB.fillFieldsByResultSet(result,false).getUrlDirName(); }
    }

    private static void prepareInheritedUrl(DocDetails doc) {
        if (Boolean.TRUE.equals(doc.getUrlInheritGroup())) {
            String relative = doc.getEditorVirtualPath();
            if (Tools.isEmpty(relative)) {
                String path = doc.getVirtualPath();
                relative = path == null || path.endsWith("/") ? "" : path.substring(path.lastIndexOf('/')+1);
            }
            doc.setVirtualPath(relative);
        }
    }

    private static String documentColumns(String alias,boolean noData) {
        String prefix = alias.isEmpty() ? "" : alias+".";
        return BranchFields.DOCUMENT.columns().stream().map(column -> noData && ("data".equals(column) || "data_asc".equals(column))
            ? "NULL AS "+column : prefix+"`"+column+"`").collect(java.util.stream.Collectors.joining(","));
    }

    private List<Map<String,Object>> read(Connection connection,String sql,boolean branch) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (branch) statement.setLong(1,context.branchId());
            try (ResultSet result = statement.executeQuery()) {
                List<Map<String,Object>> rows = new ArrayList<>();
                while (result.next()) {
                    Map<String,Object> row = new LinkedHashMap<>();
                    for (int i=1;i<=result.getMetaData().getColumnCount();i++) row.put(result.getMetaData().getColumnName(i),BranchStore.readColumn(result,i));
                    rows.add(row);
                }
                return rows;
            }
        }
    }
    private static int id(Map<String,?> row,String name) { Object value=row.get(name); return value instanceof Number number?number.intValue():0; }
}
