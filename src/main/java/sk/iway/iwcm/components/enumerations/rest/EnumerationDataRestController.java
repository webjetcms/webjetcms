package sk.iway.iwcm.components.enumerations.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
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

    private static final String ENUMERATION_TYPE_ID = "enumerationTypeId";

    @Autowired
    public EnumerationDataRestController(EnumerationDataRepository enumerationDataRepository, EnumerationTypeRepository enumerationTypeRepository) {
        super(enumerationDataRepository);
        this.enumerationDataRepository = enumerationDataRepository;
        this.enumerationTypeRepository = enumerationTypeRepository;
    }

    private EnumerationTypeBean getActualSelectedType() {
        EnumerationTypeBean actualSelectedType;
        Integer enumerationTypeId = Tools.getIntValue(getRequest().getParameter(ENUMERATION_TYPE_ID), -1);

        if(enumerationTypeId == -1) actualSelectedType = enumerationTypeRepository.findFirstByHiddenOrderById(false);
        else actualSelectedType = enumerationTypeRepository.getNonHiddenByEnumId(enumerationTypeId, false);

        return actualSelectedType;
    }

    @Override
    public Page<EnumerationDataBean> getAllItems(Pageable pageable) {
        EnumerationTypeBean actualSelectedType = getActualSelectedType();
        DatatablePageImpl<EnumerationDataBean> page;

        Integer enumerationTypeId = Tools.getIntValue(getRequest().getParameter(ENUMERATION_TYPE_ID), -1);
        if(enumerationTypeId == -1) {
            //In FE, first  enumTypes is selected by default, soo get this default enumTypes and return its values
            if (actualSelectedType != null) page = new DatatablePageImpl<>(enumerationDataRepository.findAllByTypeIdAndHiddenFalse(actualSelectedType.getEnumerationTypeId(), pageable));
            else page = new DatatablePageImpl<>(new ArrayList<>());
        } else {
            page = new DatatablePageImpl<>(enumerationDataRepository.findAllByTypeIdAndHiddenFalse(enumerationTypeId, pageable));
        }

        processFromEntity(page, ProcessItemAction.GETALL);

        return page;
    }

    @Override
    public EnumerationDataBean getOneItem(long id) {
        EnumerationDataBean entity;

        if(id == -1) entity = new EnumerationDataBean();
        else entity = enumerationDataRepository.getNonHiddenByEnumId(id, false);

        processFromEntity(entity, ProcessItemAction.GETONE, 1);

        return entity;
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<EnumerationDataBean> root, CriteriaBuilder builder) {
        //Only hidden = false records (non soft deleted)
        predicates.add(builder.isFalse(root.get("hidden")));

        int typeId = Tools.getIntValue(params.get(ENUMERATION_TYPE_ID), -1);
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
    public EnumerationDataBean processFromEntity(EnumerationDataBean entity, ProcessItemAction action, int rowCount) {
        if (entity == null) entity = new EnumerationDataBean();
        EnumerationTypeBean actualSelectedType = getActualSelectedType();

        //If action is CREATE - Set sort priority
        if(entity.getId() == null || entity.getId() == -1) {
            Integer maxSortPriority = enumerationDataRepository.findMaxSortPriorityByTypeId(getActualSelectedType().getEnumerationTypeId()).orElse(0);
            entity.setSortPriority(maxSortPriority + 1);
        }

        //If EnumerationType is not set, we cant set editor fields
        if(actualSelectedType == null) return entity;

        if(entity.getEditorFields() == null) {
            EnumerationDataEditorFields edef = new EnumerationDataEditorFields();

            //If get all data, prepare custom FIELDS only once
            edef.fromEnumerationData(entity, actualSelectedType, rowCount == 1, getProp());
        }

        return entity;
    }

    @Override
    public EnumerationDataBean processToEntity(EnumerationDataBean entity, ProcessItemAction action) {
        EnumerationTypeBean actualSelectedType = getActualSelectedType();
        //If EnumerationType is not set, we cant check duplicity so we cant transfer data from editor fields to bean
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

    @RequestMapping( value="/enum-type", params={ENUMERATION_TYPE_ID})
    public EnumerationTypeBean getEnumerationType(@RequestParam(ENUMERATION_TYPE_ID) Integer enumTypeId) {
        return enumerationTypeRepository.getNonHiddenByEnumId(enumTypeId, false);
    }

    @Override
    public boolean deleteItem(EnumerationDataBean entity, long id) {
        enumerationDataRepository.deleteEnumDataById(entity.getId(), true);
        Adminlog.add(Adminlog.TYPE_UPDATEDB, "DELETE/HIDE:\nid: "+id+"\nstring1: "+entity.getString1(), (int)id, -1);
        Cache.getInstance().removeObjectStartsWithName("enumeration.");
        return true;
    }

    @GetMapping("/autocomplete-parent")
    public List<String> getAutocomplete(@RequestParam String term, @RequestParam("DTE_Field_typeId") Integer typeId, @RequestParam("DTE_Field_string1") String name) {
        return EnumerationService.getEnumDataAutocomplete(term, typeId, name, getProp());
    }

    @GetMapping("/autocomplete-child")
    public List<String> getAutocomplete(@RequestParam String term) {
        return EnumerationService.getEnumTypeAutocomplete(term, getProp());
    }

    @Override
    public Map<Long, EnumerationDataBean> preImportDataEdit(HttpServletRequest request, Map<Long, EnumerationDataBean> data, String importMode) {
        if("update".equals(importMode) == true) {
            int enumerationTypeId = Tools.getIntValue(request.getParameter(ENUMERATION_TYPE_ID), -1);
            if(enumerationTypeId == -1) return data;

            // Get all data ids for this DEST enumeration
            Cache cache = Cache.getInstance();
            String cacheKey = "sk.iway.iwcm.components.enumerations-" + enumerationTypeId + "-" + CloudToolsForCore.getDomainId();

            @SuppressWarnings("unchecked")
            List<Long> dataIds = cache.getObject(cacheKey, List.class);

            if(dataIds == null) {
                dataIds = enumerationDataRepository.getAllDataIdsByTypeId(enumerationTypeId).orElse(null);
                cache.setObject(cacheKey, dataIds, 10); //Set for 10 minutes
            } else {
                //refresh object  -> so it will be in cache for another 10 minutes (not gonna expire in middle of import)
                cache.setObject(cacheKey, dataIds, 10); //Set for 10 minutes
            }

            // DEST enumeration have no data so do INSERT
            if(dataIds == null) {
                data.forEach((key, value) -> value.setId(-1L));
                return data;
            }

            // Check every ID from source data, if belongs to DESC enumeration
            final List<Long> finalDataIds = dataIds;
            data.forEach((key, value) -> {
                //It's value from another enumeration (probably) so do INSERT instead of UPDATE
                if(finalDataIds.contains(value.getId()) == false) value.setId(-1L);
            });
        }

        return data;
    }
}