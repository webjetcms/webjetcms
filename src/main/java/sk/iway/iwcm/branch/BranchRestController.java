package sk.iway.iwcm.branch;

import java.net.URI;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.Identity;

/** Minimal branch management API; content routing and selection are added in P03. */
@RestController
@RequestMapping("/admin/rest/branches")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuBranches')")
public class BranchRestController {
    private final BranchService branches;
    private final BranchAccess access;

    public BranchRestController(BranchService branches, BranchAccess access) {
        this.branches = branches;
        this.access = access;
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

    @PostMapping
    public ResponseEntity<Summary> create(@RequestBody Create input, HttpServletRequest request) {
        Identity user = access.requireUser(request);
        access.requireCsrf(request);
        String domain = access.requireDomain(request, user);
        access.requireExpectedDomain(domain, input.expectedDomainName());
        if (input.name() == null || input.name().isBlank() || input.name().strip().length() > 255)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Branch name must contain 1 to 255 characters");
        try (BranchContexts.Scope ignored = BranchContexts.live(request, domain, user.getUserId())) {
            Summary created;
            try { created = Summary.from(branches.create(domain, input.name().strip(), input.description(), user.getUserId())); }
            catch (BranchService.UnavailableException ex) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Branch prerequisites failed: " + ex.getMessage(), ex);
            }
            return ResponseEntity.created(URI.create("/admin/rest/branches/" + created.id())).body(created);
        }
    }

    public record Create(String name, String description, String expectedDomainName) { }

    @GetMapping("/context")
    public ContextInfo context(HttpServletRequest request) {
        Identity user=access.requireUser(request);
        String domain=access.requireDomain(request,user);
        BranchContext context=BranchSelection.selected(request,user,domain);
        return contextInfo(context);
    }

    @PostMapping("/context")
    public ContextInfo select(@RequestBody Selection input,HttpServletRequest request) {
        Identity user=access.requireUser(request);access.requireCsrf(request);
        String domain=access.requireDomain(request,user);
        access.requireExpectedDomain(domain,input.expectedDomainName());
        if(input.branchId()<0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        if(input.branchId()>0) BranchSelection.requireBranch(input.branchId(),domain);
        request.getSession().setAttribute(BranchSelection.SESSION_KEY,new BranchSelection.Selected(input.branchId(),domain));
        return contextInfo(BranchSelection.selected(request,user,domain));
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

    private ContextInfo contextInfo(BranchContext context) {
        return context.isBranch()?new ContextInfo(context.branchId(),context.domainName(),BranchSelection.requireBranch(context.branchId(),context.domainName()).getName(),false)
            :new ContextInfo(0,context.domainName(),"Produkcia",false);
    }
    public record Selection(long branchId,String expectedDomainName) { }
    public record ContextInfo(long branchId,String domainName,String name,boolean editable) { }

    public record Summary(long id, String name, String domainName, String state) {
        static Summary from(BranchEntity branch) {
            return new Summary(branch.getId(), branch.getName(), branch.getDomainName(), branch.getState());
        }
    }
}
