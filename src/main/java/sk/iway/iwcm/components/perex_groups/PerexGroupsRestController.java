package sk.iway.iwcm.components.perex_groups;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerAvailableGroups;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

@RestController
@RequestMapping("/admin/rest/prex_groups")
@PreAuthorize("@WebjetSecurityService.hasPermission('editor_edit_perex')")
@Datatable
public class PerexGroupsRestController extends DatatableRestControllerAvailableGroups<PerexGroupsEntity, Long>{

    @Autowired
    public PerexGroupsRestController(PerexGroupsRepository perexGroupsRepository) {
        super(perexGroupsRepository, "id", "availableGroups");
    }

    @Override
    public PerexGroupsEntity processFromEntity(PerexGroupsEntity entity, ProcessItemAction action, int rowCount) {
        if(entity != null && entity.getEditorFields() == null) {
            PerexGroupsEditorFields pgef = new PerexGroupsEditorFields();

            //Set "volitelne polia"
            if(rowCount == 1)
                pgef.setFieldsDefinition( pgef.getFields(entity, "components.perex", 'F') );

            pgef.fromPerexGroupsEntity(entity);
        }
        return entity;
    }

    @Override
    public PerexGroupsEntity processToEntity(PerexGroupsEntity entity, ProcessItemAction action) {
        if(entity != null) entity.getEditorFields().toPerexGroupsEntity(entity);
        return entity;
    }

    @Override
    public PerexGroupsEntity insertItem(PerexGroupsEntity entity) {
        return editItem(entity, -1);
    }

    @Override
    public PerexGroupsEntity editItem(PerexGroupsEntity entity, long id) {
        return processFromEntity(PerexGroupsService.save(entity, (PerexGroupsRepository)getRepo()), ProcessItemAction.EDIT, 1);
    }

    @Override
    public void afterDelete(PerexGroupsEntity entity, long id) {
        DocDB.getInstance().getPerexGroups(true);
    }

    @Override
    public void afterSave(PerexGroupsEntity entity, PerexGroupsEntity saved) {
        DocDB.getInstance().getPerexGroups(true);
    }

    @Override
    public PerexGroupsEntity getOneItem(long id) {
        if(id < 1) {
            PerexGroupsEntity entity = new PerexGroupsEntity();
            return processFromEntity(entity, ProcessItemAction.GETONE, 1);
        }
        return super.getOneItem(id);
    }
}