package sk.iway.iwcm.branch;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.json.LabelValue;

/** Domain-scoped management of working version metadata through the standard editor. */
@RestController
@RequestMapping("/admin/rest/branches/datatable")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuBranches')")
@Datatable
public class BranchDatatableRestController extends DatatableRestControllerV2<BranchEntity, Long> {
    private final BranchService branches;
    private final BranchAccess access;

    public BranchDatatableRestController(BranchRepository repository, BranchService branches, BranchAccess access) {
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
    public BranchEntity getOneItem(long id) {
        BranchEntity entity = super.getOneItem(id);
        if (id == -1) {
            entity.setDomainId(getDomainId());
            return entity;
        }
        if (entity == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return entity;
    }

    @Override
    public boolean checkItemPerms(BranchEntity entity, Long id) {
        if (id != null && id > 0) getOneItem(id);
        return true;
    }

    private Identity requireWrite(BranchEntity entity) {
        Identity user = access.requireUser(getRequest());
        access.requireCsrf(getRequest());
        if (entity.getDomainId() != null && entity.getDomainId() > 0 && entity.getDomainId() != getDomainId())
            throwError("datatables.error.domainId");
        if (isImporting()) throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED);
        return user;
    }

    @Override
    public BranchEntity insertItem(BranchEntity entity) {
        Identity user = requireWrite(entity);
        try {
            BranchEntity saved = branches.create(getDomainId(), entity.getName(), entity.getDescription(), entity.getColor(), user.getUserId());
            audit("CREATE", saved);
            return saved;
        } catch (IllegalArgumentException ex) {
            throwError("branches.table.invalidMetadata");
            return null;
        }
    }

    @Override
    public BranchEntity editItem(BranchEntity entity, long id) {
        Identity user = requireWrite(entity);
        getOneItem(id);
        try {
            BranchEntity saved = branches.updateMetadata(id, getDomainId(), entity.getName(), entity.getDescription(), entity.getColor(), user.getUserId());
            audit("UPDATE", saved);
            return saved;
        } catch (IllegalArgumentException ex) {
            throwError("branches.table.invalidMetadata");
            return null;
        }
    }

    private void audit(String action, BranchEntity entity) {
        Adminlog.add(Adminlog.TYPE_CLIENT_SPECIFIC, action + ": working version #" + entity.getId()
            + " (" + entity.getDomainName() + ")\nname: " + entity.getName() + "\ncolor: " + entity.getColor()
            + "\ndescription: " + entity.getDescription(), entity.getId().intValue(), -1);
    }

    @Override
    public boolean beforeDelete(BranchEntity entity) {
        throw new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED);
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
}
