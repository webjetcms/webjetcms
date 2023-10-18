package sk.iway.iwcm.components.gdpr.rest;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.gdpr.model.GdprRegExpBean;
import sk.iway.iwcm.components.gdpr.model.GdprRegExpRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/gdpr_regexp")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuGDPRregexp')")
@Datatable
public class GdprRegExpRestController  extends DatatableRestControllerV2<GdprRegExpBean, Long> {

    private final GdprRegExpRepository repository;

    @Autowired
    public GdprRegExpRestController(GdprRegExpRepository repository) {
        super(repository);
        this.repository = repository;
    }

    @Override
    public Page<GdprRegExpBean> getAllItems(Pageable pageable) {

        Page<GdprRegExpBean> items = repository.findAllByDomainId(CloudToolsForCore.getDomainId(), pageable);

        DatatablePageImpl<GdprRegExpBean> page = new DatatablePageImpl<>(items);
        return page;
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<GdprRegExpBean> root, CriteriaBuilder builder) {
        //aby nam hladalo aj podla searchUserFullName musime zavolat aj super metodu
		super.addSpecSearch(params, predicates, root, builder);

        predicates.add(builder.equal(root.get("domainId"), CloudToolsForCore.getDomainId()));
    }

    @Override
	public void beforeSave(GdprRegExpBean entity) {
        //Id of domain where is this entity created
        entity.setDomainId(CloudToolsForCore.getDomainId());

        //Who create this regex
        Identity user = getUser();
        entity.setUserId(user.getUserId());

        //Date of last change
        entity.setDateInsert(new Date());
    }

}
