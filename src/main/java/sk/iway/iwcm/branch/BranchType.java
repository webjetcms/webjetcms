package sk.iway.iwcm.branch;

/** The four explicitly supported core content identities. */
public enum BranchType {
    DOCUMENT("documents", "doc_id", "branch_documents"),
    GROUP("groups", "group_id", "branch_groups"),
    MEDIA("media", "media_id", "branch_media"),
    FILE(null, "logical_path", "branch_files");

    private final String liveTable;
    private final String idColumn;
    private final String workTable;

    BranchType(String liveTable, String idColumn, String workTable) {
        this.liveTable = liveTable;
        this.idColumn = idColumn;
        this.workTable = workTable;
    }

    public String liveTable() { return liveTable; }
    public String idColumn() { return idColumn; }
    public String workTable() { return workTable; }
}
