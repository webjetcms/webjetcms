package sk.iway.iwcm.components.enumerations.rest;

import java.util.ArrayList;
import java.util.HashMap;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataEditorFields;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataRepository;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeBean;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

@RestController
@RequestMapping("/admin/rest/enumeration/enumeration-data")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_enumerations')")
@Datatable
public class EnumerationDataRestController extends DatatableRestControllerV2<EnumerationDataBean, Long> {

    private final EnumerationDataRepository enumerationDataRepository;
    private final EnumerationTypeRepository enumerationTypeRepository;

    @Autowired
    public EnumerationDataRestController(EnumerationDataRepository enumerationDataRepository, EnumerationTypeRepository enumerationTypeRepository) {
        super(enumerationDataRepository);
        this.enumerationDataRepository = enumerationDataRepository;
        this.enumerationTypeRepository = enumerationTypeRepository;
    }

    private EnumerationTypeBean getActualSelectedType() {
        EnumerationTypeBean actualSelectedType;
        Integer enumerationTypeId = Tools.getIntValue(getRequest().getParameter("enumerationTypeId"), -1);
        if(enumerationTypeId == -1) {
            actualSelectedType = enumerationTypeRepository.findFirstByHiddenOrderById(false);
        } else {
            actualSelectedType = enumerationTypeRepository.getNonHiddenByEnumId(enumerationTypeId, false);
        }
        return actualSelectedType;
    }

    @Override
    public Page<EnumerationDataBean> getAllItems(Pageable pageable) {

        EnumerationTypeBean actualSelectedType = getActualSelectedType();
        DatatablePageImpl<EnumerationDataBean> page;

        Integer enumerationTypeId = Tools.getIntValue(getRequest().getParameter("enumerationTypeId"), -1);
        if(enumerationTypeId == -1) {
            //In FE, first  enumTypes is selected by default, soo get this default enumTypes and return its values
            if (actualSelectedType!=null) page = new DatatablePageImpl<>(enumerationDataRepository.findAllByTypeIdAndHiddenFalse(actualSelectedType.getEnumerationTypeId(), pageable));
            else page = new DatatablePageImpl<>(new ArrayList<>());
        } else {
            page = new DatatablePageImpl<>(enumerationDataRepository.findAllByTypeIdAndHiddenFalse(enumerationTypeId, pageable));
            actualSelectedType = enumerationTypeRepository.getNonHiddenByEnumId(enumerationTypeId, false);
        }

        processFromEntity(page, ProcessItemAction.GETALL);

        //Enumeration type's - select options
        List<EnumerationTypeBean> enumerationTypes = enumerationTypeRepository.findAll();

        //Mark deleted one
        for(EnumerationTypeBean enumType : enumerationTypes) {
            if(enumType.isHidden()) {
                enumType.setTypeName(getProp().getText("enum_type.deleted_type_mark.js") + enumType.getTypeName());
            }
        }

        page.addDefaultOption("editorFields.childEnumTypeId", "", "-1");
        page.addOptions("editorFields.childEnumTypeId", enumerationTypes, "typeName", "enumerationTypeId", false);

        if (actualSelectedType != null) {
            //Enumeration data's by enumeration type - select options
            List<EnumerationDataBean> enumerationDatasByType = enumerationDataRepository.findAllByTypeId(actualSelectedType.getEnumerationTypeId());

            //Mark deleted one
            for(EnumerationDataBean enumData : enumerationDatasByType) {
                if(enumData.isHidden()) {
                    enumData.setString1(getProp().getText("enum_type.deleted_type_mark.js") + enumData.getString1());
                }
            }

            page.addDefaultOption("editorFields.parentEnumDataId", "-", "-1");
            page.addOptions("editorFields.parentEnumDataId", enumerationDatasByType, "string1", "enumerationDataId", false);
        }

        return page;
    }

    @Override
    public EnumerationDataBean getOneItem(long id) {
        EnumerationDataBean entity;

        if(id == -1) entity = new EnumerationDataBean();
        else entity = enumerationDataRepository.getNonHiddenByEnumId(Integer.valueOf(id+""), false);

        processFromEntity(entity, ProcessItemAction.GETONE);

        return entity;
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<EnumerationDataBean> root, CriteriaBuilder builder) {
        //Only hidden = false records (non soft deleted)
        predicates.add(builder.isFalse(root.get("hidden")));

        int typeId = Tools.getIntValue(params.get("enumerationTypeId"), -1);
        if(typeId > 0) {
            predicates.add(builder.equal(root.get("typeId"), Integer.valueOf(typeId)));
        }

        //vyhladaj podla searchUserFullName
        super.addSpecSearch(params, predicates, root, builder);
    }

    @Override
    public void beforeSave(EnumerationDataBean entity) {
        processToEntity(entity, ProcessItemAction.EDIT);
    }

    @Override
    public EnumerationDataBean processFromEntity(EnumerationDataBean entity, ProcessItemAction action) {
        if (entity == null) entity = new EnumerationDataBean();
        EnumerationTypeBean actualSelectedType = getActualSelectedType();

        //If EnnumerationType is not set, we cant set editor fields
        if(actualSelectedType == null) return entity;

        if(entity.getEditorFields() == null) {
            EnumerationDataEditorFields edef = new EnumerationDataEditorFields();
            edef.fromEnumerationData(entity, actualSelectedType);
        }
        return entity;
    }

    @Override
    public EnumerationDataBean processToEntity(EnumerationDataBean entity, ProcessItemAction action) {
        EnumerationTypeBean actualSelectedType = getActualSelectedType();
        //If EnnumerationType is not set, we cant check duplicity so we cant transfer data from editor fields to bean
        if(actualSelectedType == null) return entity;

        if(entity != null) {
            EnumerationDataEditorFields edef = entity.getEditorFields();

            //If for some reason editorFields is null, return entity
            if(edef == null) return entity;

            edef.toEnumerationData(entity, actualSelectedType, enumerationTypeRepository, enumerationDataRepository, getProp());
        }
        return entity;
    }

    //Inset and Edit must be here !!
    //There is no bonus logic but it fix bug with wrong PK type (dont ask me why)
    @Override
    public EnumerationDataBean insertItem(EnumerationDataBean entity) {
        enumerationDataRepository.save(entity);
        return entity;
    }

    @Override
    public EnumerationDataBean editItem(EnumerationDataBean entity, long id) {
        enumerationDataRepository.save(entity);
        return entity;
    }

    @RequestMapping(value="/enum-types")
    public Map<Integer, String> getEnumerationTypes() {
        HashMap<Integer, String> enumTypesMap = new HashMap<>();
        List<EnumerationTypeBean> enumTypeList = enumerationTypeRepository.getAllNonHiddenOrderedById(false);

        for(EnumerationTypeBean enumType : enumTypeList)
            enumTypesMap.put(enumType.getEnumerationTypeId(), enumType.getTypeName());

        return enumTypesMap;
    }

    @RequestMapping( value="/enum-type", params={"enumerationTypeId"})
    public EnumerationTypeBean getEnumerationType(@RequestParam("enumerationTypeId") Integer enumTypeId) {
        return enumerationTypeRepository.getNonHiddenByEnumId(enumTypeId, false);
    }

    @Override
    public boolean deleteItem(EnumerationDataBean entity, long id) {
        enumerationDataRepository.deleteEnumDataById(entity.getEnumerationDataId(), true);
        Adminlog.add(Adminlog.TYPE_UPDATEDB, "DELETE/HIDE:\nid: "+id+"\nstring1: "+entity.getString1(), (int)id, -1);
        Cache.getInstance().removeObjectStartsWithName("enumeration.");
        return true;
    }
}
