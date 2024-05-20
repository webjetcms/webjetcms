package sk.iway.iwcm.components.redirects;

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

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.system.RedirectsRepository;
import sk.iway.iwcm.system.UrlRedirectBean;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;


@RestController
@Datatable
@RequestMapping(value = "/admin/rest/settings/redirect")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_redirects')")
public class RedirectRestController extends DatatableRestControllerV2<UrlRedirectBean, Long> {

    @Autowired
    public RedirectRestController(RedirectsRepository redirectsRepository) {
        super(redirectsRepository);
    }

    @Override
    public Page<UrlRedirectBean> getAllItems(Pageable pageable) {
        //Redirect this throu spec search
        DatatablePageImpl<UrlRedirectBean> page = new DatatablePageImpl<>(getAllItemsIncludeSpecSearch(new UrlRedirectBean(), pageable));
        return page;
    }

    @Override
    public UrlRedirectBean getOneItem(long id) {
        UrlRedirectBean item = super.getOneItem(id);
        if(Constants.getBoolean("multiDomainEnabled") && item != null && item.getId()>0 && item.getDomainName()!=null) {
            //verify domainName
            String domainName = CloudToolsForCore.getDomainName();
            if (Tools.isNotEmpty(item.getDomainName()) && domainName.equals(item.getDomainName())==false) {
                return null;
            }
        }
        return item;
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<UrlRedirectBean> root, CriteriaBuilder builder) {

        if(Constants.getBoolean("multiDomainEnabled")) {
            //Domain name is (equal to actual domain name) or (null)
            predicates.add(
                builder.or(
                    builder.equal(root.get("domainName"), CloudToolsForCore.getDomainName()),
                    builder.or(builder.isNull(root.get("domainName")), builder.equal(root.get("domainName"), ""))
                )
            );
        }

        super.addSpecSearch(params, predicates, root, builder);
    }

    @Override
    public void beforeSave(UrlRedirectBean entity) {
        //nastav datum ulozenia
        entity.setInsertDate(new Date());

        //ak nebol zadany kod presmerovania, nastav na predvoleny kod 301
        if (entity.getRedirectCode() == null) entity.setRedirectCode(301);
    }
}