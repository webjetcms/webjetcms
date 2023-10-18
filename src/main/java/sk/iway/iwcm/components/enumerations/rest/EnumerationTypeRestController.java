package sk.iway.iwcm.components.enumerations.rest;

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

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataRepository;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeBean;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeEditorFields;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

@RestController
@RequestMapping("/admin/rest/enumeration/enumeration-type")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_enumerations')")
@Datatable
public class EnumerationTypeRestController extends DatatableRestControllerV2<EnumerationTypeBean, Long> {

    private final EnumerationTypeRepository enumerationTypeRepository;
    private final EnumerationDataRepository enumerationDataRepository;

    @Autowired
    public EnumerationTypeRestController(EnumerationTypeRepository enumerationTypeRepository, EnumerationDataRepository enumerationDataRepository) {
        super(enumerationTypeRepository);
        this.enumerationTypeRepository = enumerationTypeRepository;
        this.enumerationDataRepository = enumerationDataRepository;
    }

    @Override
    public Page<EnumerationTypeBean> getAllItems(Pageable pageable) {
        DatatablePageImpl<EnumerationTypeBean> page = new DatatablePageImpl<>(enumerationTypeRepository.findAllByHiddenFalse(pageable));

        processFromEntity(page, ProcessItemAction.GETALL);

        //Enumeration type's - select options
        List<EnumerationTypeBean> enumerationTypes = enumerationTypeRepository.findAll();

        //Mark deleted one
        for(EnumerationTypeBean type : enumerationTypes)
            if(type.isHidden())
                type.setTypeName(getProp().getText("enum_type.deleted_type_mark.js") + type.getTypeName());

        page.addDefaultOption("editorFields.childEnumTypeId", "", "-1");
        page.addOptions("editorFields.childEnumTypeId", enumerationTypes, "typeName", "enumerationTypeId", false);

        return page;
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<EnumerationTypeBean> root, CriteriaBuilder builder) {
        //Only hidden = false records (non soft deleted)
        predicates.add(builder.isFalse(root.get("hidden")));

        //vyhladaj podla searchUserFullName
        super.addSpecSearch(params, predicates, root, builder);
    }

    @Override
    public EnumerationTypeBean getOneItem(long id) {
        EnumerationTypeBean entity;

        if(id == -1) entity = new EnumerationTypeBean();
        else entity = enumerationTypeRepository.getNonHiddenByEnumId((int)id);

        processFromEntity(entity, ProcessItemAction.GETONE);

        return entity;
    }

    @Override
    public void beforeSave(EnumerationTypeBean entity) {
        Long entityId = entity.getId();
        if(entityId != null && entityId != -1) {
            //If allowChildEnumerationType was changed to false, remove from data all set child enum type's
            if(!entity.isAllowChildEnumerationType()) {
                //Only if in DB is value still true
                if(jpaToBoolean(enumerationTypeRepository.isAllowChildEnumerationType(entityId)))
                    enumerationDataRepository.denyChildEnumerationTypeByTypeId(entityId);
            }

            //If allowParentEnumerationData was changed to false, remove from data all set parent enum data's
            if(!entity.isAllowParentEnumerationData()) {
                //Only if in DB is value still true
                if(jpaToBoolean(enumerationTypeRepository.isAllowParentEnumerationData(entityId)))
                    enumerationDataRepository.denyParentEnumerationDataByTypeId(entityId);
            }
        }

        processToEntity(entity, ProcessItemAction.EDIT);
    }

    @Override
    public EnumerationTypeBean processFromEntity(EnumerationTypeBean entity, ProcessItemAction action) {
        if (entity == null) entity = new EnumerationTypeBean();

        if(entity.getEditorFields() == null) {
            EnumerationTypeEditorFields etef = new EnumerationTypeEditorFields();
            etef.fromEnumerationType(entity);
        }
        return entity;
    }

    @Override
    public EnumerationTypeBean processToEntity(EnumerationTypeBean entity, ProcessItemAction action) {
        if(entity != null) {
            EnumerationTypeEditorFields etef = entity.getEditorFields();

            //If for some reason editorFields is null, return entity
            if(etef == null) return entity;

            etef.toEnumerationType(entity, enumerationTypeRepository, getProp());
        }
        return entity;
    }

    @Override
    public boolean deleteItem(EnumerationTypeBean entity, long id) {
        enumerationTypeRepository.deleteEnumTypeById(entity.getEnumerationTypeId());
        //"Delete" all created EnumerationData's under this EnumerationType
        enumerationDataRepository.deleteAllEnumDataByEnumTypeId(entity.getEnumerationTypeId());

        Adminlog.add(Adminlog.TYPE_UPDATEDB, "DELETE/HIDE:\nid: "+id+"\nname: "+entity.getTypeName(), (int)id, -1);
        Cache.getInstance().removeObjectStartsWithName("enumeration.");
        return true;
    }
}
