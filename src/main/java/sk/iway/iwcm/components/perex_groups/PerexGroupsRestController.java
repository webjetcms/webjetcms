package sk.iway.iwcm.components.perex_groups;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

@RestController
@RequestMapping("/admin/rest/prex_groups")
@PreAuthorize("@WebjetSecurityService.hasPermission('editor_edit_perex')")
@Datatable
public class PerexGroupsRestController extends DatatableRestControllerV2<PerexGroupsEntity, Long>{

    private final PerexGroupsRepository perexGroupsRepository;

    @Autowired
    public PerexGroupsRestController(PerexGroupsRepository perexGroupsRepository) {
        super(perexGroupsRepository);
        this.perexGroupsRepository = perexGroupsRepository;
    }

    @Override
    public Page<PerexGroupsEntity> getAllItems(Pageable pageable) {
        List<PerexGroupsEntity> items;
        int[] editableGroups = Tools.getTokensInt(getUser().getEditableGroups(), ",");

        if(editableGroups.length < 1) {
            items = perexGroupsRepository.findAll();
        } else {
            DocDB docDB = DocDB.getInstance(false);
            items = PerexGroupBeanToPerexGroupsEntityMapper.INSTANCE.perexGroupBeanListToPerexGroupsEntityList(
                docDB.getPerexGroups(editableGroups, true)
            );
        }

        Page<PerexGroupsEntity> page = new DatatablePageImpl<>(items);
        processFromEntity(page, ProcessItemAction.GETALL);
        return page;
    }

    @Override
    public PerexGroupsEntity getOneItem(long id) {
        PerexGroupsEntity entity;
        if(id < 1) entity = new PerexGroupsEntity();
        else entity = perexGroupsRepository.findById(id).get();

        processFromEntity(entity, ProcessItemAction.GETONE);
        return entity;
    }

    @Override
    public PerexGroupsEntity insertItem(PerexGroupsEntity entity) {
        processToEntity(entity, ProcessItemAction.CREATE);
        perexGroupsRepository.save(entity);
        DocDB.getInstance(true);
        return entity;
    }

    @Override
    public PerexGroupsEntity editItem(PerexGroupsEntity entity, long id) {
        processToEntity(entity, ProcessItemAction.EDIT);
        perexGroupsRepository.save(entity);
        DocDB.getInstance(true);
        return entity;
    }

    @Override
    public PerexGroupsEntity processFromEntity(PerexGroupsEntity entity, ProcessItemAction action) {
        if(entity != null && entity.getEditorFields() == null) {
            PerexGroupsEditorFields pgef = new PerexGroupsEditorFields();
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
    public void afterDelete(PerexGroupsEntity entity, long id) {
        DocDB.getInstance(true);
    }
}
