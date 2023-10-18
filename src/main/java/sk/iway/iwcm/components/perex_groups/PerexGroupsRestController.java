package sk.iway.iwcm.components.perex_groups;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

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

        List<PerexGroupsEntity> items =  perexGroupsRepository.findAll();
        for(PerexGroupsEntity perex : items) {
            if(perex.getEditorFields() == null) {
                PerexGroupsEditorFields pef = new PerexGroupsEditorFields();
                pef.fromPerexGroupsEntity(perex);
                perex.setEditorFields(pef);
            }
        }
        DatatablePageImpl<PerexGroupsEntity> page = new DatatablePageImpl<>(items);
        return page;
    }

    @Override
    public PerexGroupsEntity getOneItem(long id) {

        PerexGroupsEntity entity;
        if(id == -1) {
            entity = new PerexGroupsEntity();
        } else {
            entity = perexGroupsRepository.findById(id).get();
        }
        ArrayList<PerexGroupsEntity> result = new ArrayList<>();

        if(entity != null) {
            PerexGroupsEditorFields pef = new PerexGroupsEditorFields();
            pef.fromPerexGroupsEntity(entity);
            result.add(entity);
        }

        return entity;
    }

    @Override
    public PerexGroupsEntity insertItem(PerexGroupsEntity entity) {

        if(entity != null) {
            PerexGroupsEditorFields pef = new PerexGroupsEditorFields();
            pef.toPerexGroupsEntity(entity);
        }

        perexGroupsRepository.save(entity);
        DocDB.getInstance(true);
        return entity;
    }

    @Override
    public PerexGroupsEntity editItem(PerexGroupsEntity entity, long id) {

        PerexGroupsEditorFields pef = new PerexGroupsEditorFields();
        pef.toPerexGroupsEntity(entity);
        perexGroupsRepository.save(entity);
        DocDB.getInstance(true);
        return entity;
    }
}
