package sk.iway.iwcm.components.template_groups;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.doc.TemplatesGroupBean;
import sk.iway.iwcm.doc.TemplatesGroupDB;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/templates/temps-groups-list")
@Datatable
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuTemplatesGroup')")
public class TemplateGroupsController extends DatatableRestControllerV2<TemplatesGroupBean, Long> {

    private final TemplateGroupsService templateGroupsService;

    @Autowired
    public TemplateGroupsController(TemplateGroupsService templateGroupsService) {
        super(null);
        this.templateGroupsService = templateGroupsService;
    }

    @Override
    public Page<TemplatesGroupBean> getAllItems(Pageable pageable) {
        DatatablePageImpl<TemplatesGroupBean> page = new DatatablePageImpl<>(templateGroupsService.getAllTemplateGroups(getRequest()));
        page.addOptions("directory", templateGroupsService.getDirectories(), "label", "value", false);
        page.addOptions("inlineEditingMode", templateGroupsService.getInlineEditors(getProp()), "label", "value", false);
        return page;
    }

    @Override
    public TemplatesGroupBean insertItem(TemplatesGroupBean templatesGroupBean) {
        return templateGroupsService.saveTemplateGroup(templatesGroupBean, getRequest());
    }

    @Override
    public TemplatesGroupBean editItem(TemplatesGroupBean templatesGroupBean, long id) {
        return templateGroupsService.editTemplateGroupBean(templatesGroupBean, id, getRequest());
    }

    @Override
    public boolean deleteItem(TemplatesGroupBean templatesGroupBean, long id) {
        return templateGroupsService.deleteTemplateGroupBean(id);
    }

    @Override
    public boolean checkItemPerms(TemplatesGroupBean entity, Long id) {
        if (InitServlet.isTypeCloud() && entity.getId() != null && entity.getId().longValue()>0) {
            TemplatesGroupBean old = TemplatesGroupDB.getInstance().getById(entity.getId());
            if (old != null) {
                List<TemplatesGroupBean> all = new ArrayList<>();
                all.add(old);
                if (templateGroupsService.filterByUser(all, getUser()).isEmpty()) return false;
            }
        }
        return true;
    }
}
