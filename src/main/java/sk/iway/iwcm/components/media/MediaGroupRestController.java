package sk.iway.iwcm.components.media;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerAvailableGroups;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.spirit.model.MediaGroupBean;
import sk.iway.spirit.model.MediaGroupEditorFields;

@RestController
@Datatable
@RequestMapping("/admin/rest/media-group")
@PreAuthorize("@WebjetSecurityService.hasPermission('editor_edit_media_group')")
public class MediaGroupRestController extends DatatableRestControllerAvailableGroups<MediaGroupBean, Long> {

    @Autowired
    public MediaGroupRestController(MediaGroupRepository mediaGroupRepository) {
        super(mediaGroupRepository, "id", "availableGroups");
    }

    @Override
    public List<MediaGroupBean> filterByPerms(List<MediaGroupBean> all) {
        List<MediaGroupBean> filteredSuper = super.filterByPerms(all);

        if (InitServlet.isTypeCloud()==false) return filteredSuper;
        if (CloudToolsForCore.isControllerDomain()) return all;

        //for multiweb we need to filter groups with empty availableGroups field
        List<MediaGroupBean> filtered = new ArrayList<>();
        for (MediaGroupBean bean : filteredSuper) {
            if (Tools.isNotEmpty(bean.getAvailableGroups())) {
                filtered.add(bean);
            }
        }

        return filtered;
    }

    @Override
    public MediaGroupBean processToEntity(MediaGroupBean entity, ProcessItemAction action) {
        MediaGroupEditorFields editorFields = entity.getEditorFields();
        if (editorFields != null) {
            entity.setAvailableGroups(entity.getEditorFields().toMediaGroupBean(entity));
        }
        return entity;
    }

    @Override
    public MediaGroupBean processFromEntity(MediaGroupBean entity, ProcessItemAction action) {
        MediaGroupEditorFields editorFields = new MediaGroupEditorFields();
        editorFields.fromMediaGroupBean(entity);
        entity.setEditorFields(editorFields);
        return entity;
    }
}
