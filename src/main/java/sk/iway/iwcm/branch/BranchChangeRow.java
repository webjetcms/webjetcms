package sk.iway.iwcm.branch;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import lombok.Getter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

/** Table projection of a change in a reviewed publishing package. */
@Getter
public final class BranchChangeRow {
    private static final Set<String> WRITTEN_PHASES = Set.of("DATA", "REFERENCES", "TRASH_WAIT", "TRASH_ROOT", "EFFECTS");

    @DataTableColumn(inputType = DataTableColumnType.ID, title = "&nbsp;", hidden = false, orderable = false, filter = false)
    private final String id;
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "branches.merge.typeLabel.js")
    private final String type;
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "branches.merge.item.js")
    private final String label;
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "branches.merge.operation.js", filter = false)
    private final String operation;
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "branches.merge.status.js", filter = false)
    private final String status;
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "branches.merge.action.js", sortAfter = "type", filter = false, orderable = false)
    private final String comparison;

    private final String objectKey;
    private final String folderPath;
    private final boolean problem;
    private final boolean revertible;

    BranchChangeRow(BranchMergeService.Item item, String folderPath, boolean canRevert) {
        type = item.type();
        objectKey = item.objectKey();
        this.folderPath = folderPath;
        id = type + "-" + Base64.getUrlEncoder().withoutPadding().encodeToString(objectKey.getBytes(StandardCharsets.UTF_8));
        label = item.label() == null || item.label().isEmpty() ? objectKey : item.label();
        operation = "DELETE".equals(item.operation()) ? "deleted" : "ABSENT".equals(item.base().state()) ? "new" : "updated";
        status = status(item);
        comparison = item.isWorkOnly() ? "viewItem" : item.conflict() && item.resolvable() ? "resolve" : "compareItem";
        problem = item.conflict() || item.error() != null || "ERROR".equals(item.applyState());
        revertible = canRevert && item.editable() && ("DOCUMENT".equals(type) || "GROUP".equals(type));
    }

    private static String status(BranchMergeService.Item item) {
        String checkpoint = checkpointStatus(item.applyState(),item.applyPhase(),item.error());
        if (checkpoint != null) return checkpoint;
        if (item.conflict()) return "conflict";
        return item.writeRequired() ? "willPublish" : "noChange";
    }

    static String checkpointStatus(String state,String phase,String error) {
        if ("DONE".equals(state)) return "NOOP".equals(phase) ? "noop" : "done";
        if ("ERROR".equals(state) && phase != null && WRITTEN_PHASES.contains(phase)) return "contentWrittenError";
        if (error != null || "ERROR".equals(state)) return "error";
        return "APPLYING".equals(state) ? "applying" : null;
    }
}
