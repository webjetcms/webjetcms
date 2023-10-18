package sk.iway.iwcm.editor.rest;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.editor.service.LinkCheckService;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

/**
 * Rest controller pre datatabulky linkCheck
 */
@RestController
@Datatable
@RequestMapping(value = "/admin/rest/link-check")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuWebpages')")
public class LinkCheckRestController extends DatatableRestControllerV2<LinkCheckDto, Long> {

    private final DocDetailsRepository docDetailsRepository;

    @Autowired
    public LinkCheckRestController(DocDetailsRepository docDetailsRepository) {
        super(null);
        this.docDetailsRepository = docDetailsRepository;
    }

    @Override
    public Page<LinkCheckDto> getAllItems(Pageable pageable) {
        int groupId =  Tools.getIntValue(getRequest().getParameter("groupId"), -1);
        String tableType =  Tools.getStringValue(getRequest().getParameter("tableType"), "");
        List<LinkCheckDto> dataList = new ArrayList<>();

        //Both values are needed
        if(groupId == -1 || tableType.isEmpty()) return new DatatablePageImpl<>(dataList);

        LinkCheckService linkCheckService = new LinkCheckService();
        dataList = linkCheckService.linkCheckList(groupId, tableType, docDetailsRepository);

        DatatablePageImpl<LinkCheckDto> page = new DatatablePageImpl<>(dataList);

        return page;
    }

}
