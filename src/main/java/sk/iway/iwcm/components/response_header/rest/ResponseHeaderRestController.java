package sk.iway.iwcm.components.response_header.rest;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.response_header.jpa.ResponseHeaderEntity;
import sk.iway.iwcm.components.response_header.jpa.ResponseHeaderRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/response-header")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_response-header')")
@Datatable
public class ResponseHeaderRestController extends DatatableRestControllerV2<ResponseHeaderEntity, Long> {

    @Autowired
    public ResponseHeaderRestController(ResponseHeaderRepository responseHeaderRepository) {
        super(responseHeaderRepository);
    }

    @Override
    public void beforeSave(ResponseHeaderEntity entity) {
        //Set domain id
        if (entity.getDomainId() == null) entity.setDomainId(CloudToolsForCore.getDomainId());

        //Set actual datetime of change
        entity.setChangeDate(new Date());
    }

    @Override
    public void afterSave(ResponseHeaderEntity entity, ResponseHeaderEntity saved) {
        //After change in table, delete cached response headers for this domain
        ResponseHeaderService.deleteDomainCache();
    }

    @Override
    public void afterDelete(ResponseHeaderEntity entity, long id) {
        //After change in table, delete cached response headers for this domain
        ResponseHeaderService.deleteDomainCache();
    }
}
