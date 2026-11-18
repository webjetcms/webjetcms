package sk.iway.iwcm.branch;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

/** Lists the reviewed changes of one working version in the current administration domain. */
@RestController
@Datatable
@RequestMapping("/admin/rest/branches/{branchId}/changes")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuBranches')")
public class BranchChangesRestController extends DatatableRestControllerV2<BranchChangeRow, Long> {
    private final BranchAccess access;
    private final BranchMergeService merge = new BranchMergeService();

    public BranchChangesRestController(BranchAccess access) {
        super(null);
        this.access = access;
    }

    @Override
    public boolean checkAccessAllowed(HttpServletRequest request) {
        return "GET".equals(request.getMethod()) && "/admin/rest/branches/{branchId}/changes/all"
            .equals(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));
    }

    @Override
    public Page<BranchChangeRow> getAllItems(Pageable pageable) {
        @SuppressWarnings("unchecked")
        var variables = (Map<String, String>) getRequest().getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        long branchId = Tools.getLongValue(variables.get("branchId"), -1);
        if (branchId <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return load(branchId, getRequest());
    }

    @Override
    public void getOptions(DatatablePageImpl<BranchChangeRow> page) {
        for (BranchType type : BranchType.values()) {
            page.addDefaultOption("type", getProp().getText("branches.merge.type." + type.name() + ".js"), type.name());
        }
    }

    /** Reverts the displayed page or folder revision without switching the active editing context. */
    @PostMapping("/revert")
    @PreAuthorize("@WebjetSecurityService.hasPermission('menuBranches&menuWebpages')")
    public BranchMergeService.Report revert(@PathVariable long branchId, @RequestBody BranchEditorRestController.Revert change,
            HttpServletRequest request) {
        Identity user = access.requireUser(request);
        String domain = access.requireDomain(request, user);
        try (BranchContexts.Scope ignored = BranchContexts.open(request, new BranchContext(branchId, domain, user.getUserId(), BranchContext.Mode.ADMIN))) {
            new BranchEditor(request).revert(change.type(), change.id(), change.hash());
        }
        return loadReport(branchId, request, user, domain);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> responseStatus(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(error.getBody());
    }

    private ChangesPage load(long branchId, HttpServletRequest request) {
        Identity user = access.requireUser(request);
        String domain = access.requireDomain(request, user);
        try (BranchContexts.Scope ignored = BranchContexts.live(request, domain, user.getUserId())) {
            BranchMergeService.Report report = merge.detail(branchId, domain, user);
            return new ChangesPage(report, changeRows(report, user.isEnabledItem("menuWebpages")));
        }
    }

    private BranchMergeService.Report loadReport(long branchId, HttpServletRequest request, Identity user, String domain) {
        try (BranchContexts.Scope ignored = BranchContexts.live(request, domain, user.getUserId())) {
            return merge.detail(branchId, domain, user);
        }
    }

    private static List<BranchChangeRow> changeRows(BranchMergeService.Report report, boolean canRevert) {
        GroupsDB groups = GroupsDB.getInstance();
        Map<Integer, Map<String, Object>> workingGroups = new HashMap<>();
        for (BranchMergeService.Item item : report.items()) {
            if ("GROUP".equals(item.type())) workingGroups.put(Integer.parseInt(item.objectKey()), item.work().fields());
        }
        return report.items().stream()
            .map(item -> new BranchChangeRow(item, folderPath(item, groups, workingGroups), canRevert))
            .toList();
    }

    private static String folderPath(BranchMergeService.Item item, GroupsDB groups, Map<Integer, Map<String, Object>> workingGroups) {
        String parentField = switch (item.type()) {
            case "DOCUMENT" -> "group_id";
            case "GROUP" -> "parent_group_id";
            default -> null;
        };
        if (parentField == null || !(item.work().fields().get(parentField) instanceof Number parent)) return null;
        int groupId = parent.intValue();
        Deque<String> names = new ArrayDeque<>();
        Set<Integer> visited = new HashSet<>();
        while (groupId > 0) {
            if (!visited.add(groupId)) return null;
            Map<String, Object> working = workingGroups.get(groupId);
            if (working != null) {
                names.addFirst(Objects.toString(working.get("group_name"), ""));
                groupId = BranchMergeService.number(working, "parent_group_id");
            } else {
                GroupDetails group = groups.findGroup(groupId);
                if (group == null) return null;
                names.addFirst(group.getGroupName());
                groupId = group.getParentGroupId();
            }
        }
        return "/" + String.join("/", names);
    }

    /** Supplies the report used by item comparison, publishing and scheduling with the table data. */
    public static final class ChangesPage extends DatatablePageImpl<BranchChangeRow> {
        private static final long serialVersionUID = 1L;
        private final transient BranchMergeService.Report report;

        private ChangesPage(BranchMergeService.Report report, List<BranchChangeRow> rows) {
            super(rows);
            this.report = report;
        }

        public BranchMergeService.Report getReport() {
            return report;
        }
    }
}
