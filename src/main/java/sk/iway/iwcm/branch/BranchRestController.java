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

    public record Summary(long id, String name, String domainName, String state) {
        static Summary from(BranchEntity branch) {
            return new Summary(branch.getId(), branch.getName(), branch.getDomainName(), branch.getState());
        }
    }
}
