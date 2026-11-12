package sk.iway.iwcm.branch;

import java.util.List;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.json.LabelValue;

/** Working version management, selection and preview links through the standard DataTable controller. */
@RestController
@RequestMapping("/admin/rest/branches")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuBranches')")
@Datatable
public class BranchRestController extends DatatableRestControllerV2<BranchEntity, Long> {
    private final BranchService branches;
    private final BranchAccess access;

    public BranchRestController(BranchRepository repository, BranchService branches, BranchAccess access) {
        super(repository, BranchEntity.class);
        this.branches = branches;
        this.access = access;
    }

    @Override
    public boolean checkAccessAllowed(HttpServletRequest request) {
        access.requireUser(request);
        return true;
    }

    @Override
    public void beforeSave(BranchEntity entity) {
        if (isImporting()) throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED);
        BranchReadiness.requireEnabled();
        Identity user = getUser();
        Timestamp now = Timestamp.from(Instant.now());
        if (entity.getId() == null || entity.getId() < 1) {
            try { BranchReadiness.verifyActivation(); }
            catch (SQLException | IllegalStateException ex) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Branch prerequisites failed: " + ex.getMessage(), ex);
            }
            entity.setState("DRAFT");
            entity.setRevision(0L);
            entity.setCreatedBy(user.getUserId());
            entity.setCreatedAt(now);
            entity.setPublishAt(null);
            entity.setScheduledBy(null);
            entity.setMergeStartedAt(null);
            entity.setMergeFinishedAt(null);
            entity.setLastError(null);
        }
        if (entity.getName() != null) entity.setName(entity.getName().strip());
        entity.setUpdatedBy(user.getUserId());
        entity.setUpdatedAt(now);
    }

    @Override
    public boolean deleteItem(BranchEntity entity, long id) {
        branches.delete(id,getDomainId());
        return true;
    }

    @Override
    public void afterDelete(BranchEntity entity, long id) {
        var session=getRequest().getSession();
        if (session.getAttribute(BranchSelection.SESSION_KEY) instanceof BranchSelection.Selected selected
                && selected.branchId()==id) {
            session.setAttribute(BranchSelection.SESSION_KEY,new BranchSelection.Selected(0,selected.domainId()));
            session.removeAttribute(BranchProductionSection.ATTRIBUTE);
        }
        setRedirect("/admin/v9/webpages/branches/");
    }

    @Override
    public void beforeDuplicate(BranchEntity entity) {
        throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Override
    public void getOptions(DatatablePageImpl<BranchEntity> page) {
        page.addOptions("state", List.of("DRAFT", "SCHEDULED", "MERGING", "MERGE_FAILED", "NEEDS_ATTENTION", "MERGED", "CANCELLED")
            .stream().map(state -> new LabelValue(getProp().getText("branches.merge.state." + state + ".js"), state)).toList(), "label", "value", false);
        page.addOptions("color", List.of("default", "red", "peach", "amber", "mint", "cyan", "blue", "lavender", "gray")
            .stream().map(color -> new LabelValue(getProp().getText("admin.dashboard.shortcutColor." + color + ".js"), color)).toList(), "label", "value", false);
    }

    /** Preserves HTTP error statuses for context selection and preview requests. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> responseStatus(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(error.getBody());
    }

    @GetMapping
    public List<Summary> list(@RequestParam(defaultValue = "false") boolean includeClosed,
            HttpServletRequest request) {
        Identity user = access.requireUser(request);
        String domain = access.requireDomain(request, user);
        try (BranchContexts.Scope ignored = BranchContexts.live(request, domain, user.getUserId())) {
            return branches.list(domain, includeClosed).stream().map(Summary::from).toList();
        }
    }

    @GetMapping("/context")
    public ContextInfo context(HttpServletRequest request) {
        Identity user=access.requireUser(request);
        String domain=access.requireDomain(request,user);
        BranchContext context=BranchSelection.selected(request,user,domain);
        return contextInfo(context,request);
    }

    @PostMapping("/context")
    public ContextInfo select(@RequestBody Selection input,HttpServletRequest request) {
        Identity user=access.requireUser(request);access.requireCsrf(request);
        String domain=access.requireDomain(request,user);
        access.requireExpectedDomain(BranchDomains.id(domain),input.expectedDomainId());
        if(input.branchId()<0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        if(input.branchId()>0) BranchSelection.requireBranch(input.branchId(),domain);
        synchronized (request.getSession()) {
            request.getSession().removeAttribute(BranchProductionSection.ATTRIBUTE);
            request.getSession().setAttribute(BranchSelection.SESSION_KEY,new BranchSelection.Selected(input.branchId(),BranchDomains.id(domain)));
        }
        return contextInfo(BranchSelection.selected(request,user,domain),request);
    }

    @GetMapping("/{id}/preview-link")
    public java.util.Map<String,String> previewLink(@PathVariable long id,@RequestParam(defaultValue="0") int docId,HttpServletRequest request) {
        Identity user=access.requireUser(request);
        String domain=BranchSelection.previewDomain(id,user);
        try(BranchContexts.Scope ignored=BranchContexts.open(request,new BranchContext(id,domain,user.getUserId(),BranchContext.Mode.PREVIEW))) {
            if(docId==0) docId=BranchView.current().groups().stream().filter(group->domain.equals(group.getDomainName())&&group.getParentGroupId()==0&&group.getDefaultDocId()>0).mapToInt(sk.iway.iwcm.doc.GroupDetails::getDefaultDocId).findFirst().orElse(0);
            var doc=BranchReadService.getDoc(docId);
            if(doc==null||!doc.isAvailable()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Page unavailable in selected result");
            return java.util.Map.of("url",request.getContextPath()+BranchPreviewUrls.PREFIX+id+doc.getVirtualPath());
        }
    }

    private ContextInfo contextInfo(BranchContext context,HttpServletRequest request) {
        if (!context.isBranch()) return new ContextInfo(0,context.domainId(),context.domainName(),Prop.getInstance(request).getText("branches.main.js"),false,"default");
        BranchEntity branch=BranchSelection.requireBranch(context.branchId(),context.domainName());
        return new ContextInfo(context.branchId(),context.domainId(),context.domainName(),branch.getName(),"DRAFT".equals(branch.getState()),branch.getColor());
    }
    public record Selection(long branchId,Integer expectedDomainId) { }
    public record ContextInfo(long branchId,int domainId,String domainName,String name,boolean editable,String color) { }

    public record Summary(long id, String name, int domainId, String domainName, String state, String color) {
        static Summary from(BranchEntity branch) {
            return new Summary(branch.getId(), branch.getName(), branch.getDomainId(), branch.getDomainName(), branch.getState(), branch.getColor());
        }
    }
}
