package sk.iway.iwcm.dmail.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.dmail.jpa.StatClicksEntity;
import sk.iway.iwcm.dmail.jpa.StatClicksRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/dmail/stat-clicks")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuEmail')")
@Datatable
public class StatClicksRestController extends DatatableRestControllerV2<StatClicksEntity, Long> {

    private final StatClicksRepository statClicksRepository;

    @Autowired
    public StatClicksRestController(StatClicksRepository statClicksRepository) {
        super(statClicksRepository);
        this.statClicksRepository = statClicksRepository;
    }

    @Override
    public Page<StatClicksEntity> getAllItems(Pageable pageable) {

        Long campainId = Tools.getLongValue(getRequest().getParameter("campainId"), -1);
        Page<StatClicksEntity> page = statClicksRepository.findByCampainId(campainId, pageable);

        return page;
    }
}