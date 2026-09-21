package sk.iway.iwcm.components.enumerations.rest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.jstree.JsTreeItem;
import sk.iway.iwcm.admin.jstree.JsTreeItemState;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsEntity;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsRepository;
import sk.iway.iwcm.components.customfields.rest.CustomFieldsService;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataRepository;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeBean;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeEditorFields;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.multidomain.DomainIdScopeResolver;

@RestController
@RequestMapping("/admin/rest/enumeration/enumeration-type")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_enumerations')")
@Datatable
public class EnumerationTypeRestController extends DatatableRestControllerV2<EnumerationTypeBean, Long> {

    private final EnumerationTypeRepository enumerationTypeRepository;
    private final EnumerationDataRepository enumerationDataRepository;
    private final CustomFieldsRepository customFieldsRepository;

    @Autowired
    public EnumerationTypeRestController(EnumerationTypeRepository enumerationTypeRepository, EnumerationDataRepository enumerationDataRepository, CustomFieldsRepository customFieldsRepository) {
        super(enumerationTypeRepository);
        this.enumerationTypeRepository = enumerationTypeRepository;
        this.enumerationDataRepository = enumerationDataRepository;
        this.customFieldsRepository = customFieldsRepository;
    }

    @Override
    public Page<EnumerationTypeBean> getAllItems(Pageable pageable) {
        boolean showHidden = Boolean.parseBoolean(getRequest().getParameter("showHidden"));
        DatatablePageImpl<EnumerationTypeBean> page = new DatatablePageImpl<>(showHidden
                ? enumerationTypeRepository.findAll(pageable) : enumerationTypeRepository.findAllByHiddenFalse(pageable));
        processFromEntity(page, ProcessItemAction.GETALL);
        EnumerationService.prepareEnumTypesOptions(page, getProp());
        return page;
    }

    /** Lists types under each linked parent, optionally including deleted types, with search ancestors and the selected branch visible. */
    @PostMapping("/tree")
    public Map<String, Object> tree(@RequestParam(defaultValue = "-1") long selectedId,
            @RequestParam(required = false) String selectedNodeId,
            @RequestParam(required = false) String treeSearchValue,
            @RequestParam(defaultValue = "contains") String treeSearchType,
            @RequestParam(defaultValue = "false") boolean showHidden) {
        List<EnumerationTypeBean> types = showHidden ? enumerationTypeRepository.findAll(Sort.by("id"))
                : enumerationTypeRepository.getAllNonHiddenOrderedById(false);
        Map<Long, EnumerationTypeBean> typesById = new LinkedHashMap<>();
        Set<Long> childIds = new HashSet<>();
        for (EnumerationTypeBean type : types) {
            typesById.put(type.getId(), type);
            if (type.getChildEnumerationTypeBean() != null) childIds.add(type.getChildEnumerationTypeBean().getId());
        }
        Map<String, JsTreeItem> nodes = new LinkedHashMap<>();
        Set<Long> displayed = new HashSet<>();
        for (EnumerationTypeBean type : types) {
            if (!childIds.contains(type.getId())) addTreeBranch(type, typesById, nodes, displayed);
        }
        // Existing cyclic links must not hide types that have no root.
        for (EnumerationTypeBean type : types) {
            if (!displayed.contains(type.getId())) addTreeBranch(type, typesById, nodes, displayed);
        }

        boolean searching = Tools.isNotEmpty(treeSearchValue);
        long selection = types.stream().anyMatch(type -> type.getId() == selectedId) ? selectedId
                : types.isEmpty() ? -1 : types.get(0).getId();
        String search = searching ? DB.internationalToEnglish(treeSearchValue).toLowerCase(Locale.ROOT) : "";
        Set<String> visible = new HashSet<>();
        for (JsTreeItem item : nodes.values()) {
            String name = DB.internationalToEnglish(item.getAAttr().get("title")).toLowerCase(Locale.ROOT);
            boolean matches = switch (treeSearchType) {
                case "startwith" -> name.startsWith(search);
                case "endwith" -> name.endsWith(search);
                case "equals" -> name.equals(search);
                default -> name.contains(search);
            };
            if (searching && !matches) continue;
            visible.add(item.getId());
            if (searching) {
                for (JsTreeItem parent = nodes.get(item.getParent()); parent != null; parent = nodes.get(parent.getParent())) {
                    visible.add(parent.getId());
                    parent.getState().setOpened(true);
                }
            }
        }
        List<JsTreeItem> items = nodes.values().stream().filter(item -> visible.contains(item.getId())).toList();
        String selectedTypeId = String.valueOf(searching ? selectedId : selection);
        JsTreeItem selected = nodes.get(selectedNodeId);
        if (selected == null || !visible.contains(selected.getId()) || !selectedTypeId.equals(selected.getAAttr().get("data-type-id"))) {
            selected = items.stream().filter(item -> selectedTypeId.equals(item.getAAttr().get("data-type-id"))).findFirst().orElse(null);
        }
        if (selected != null) {
            selected.getState().setSelected(true);
            for (JsTreeItem parent = nodes.get(selected.getParent()); parent != null; parent = nodes.get(parent.getParent())) {
                parent.getState().setOpened(true);
            }
        }
        return Map.of("result", true, "items", items);
    }

    /** Adds one linked chain with distinct node IDs when a type occurs under several parents. */
    private void addTreeBranch(EnumerationTypeBean type, Map<Long, EnumerationTypeBean> typesById,
            Map<String, JsTreeItem> nodes, Set<Long> displayed) {
        Set<Long> branch = new HashSet<>();
        String parent = "#";
        while (type != null && branch.add(type.getId())) {
            JsTreeItem item = new JsTreeItem();
            item.setId("#".equals(parent) ? String.valueOf(type.getId()) : parent + "-" + type.getId());
            item.setParent(parent);
            item.setText(type.getTypeName());
            item.setIcon(type.isHidden() ? "ti ti-trash" : "ti ti-list");
            item.setChildren(false);
            item.setAAttr(Map.of("title", type.getTypeName(), "data-type-id", String.valueOf(type.getId()),
                    "data-hidden", String.valueOf(type.isHidden())));
            item.setState(new JsTreeItemState());
            nodes.put(item.getId(), item);
            displayed.add(type.getId());
            parent = item.getId();
            type = type.getChildEnumerationTypeBean() == null ? null : typesById.get(type.getChildEnumerationTypeBean().getId());
        }
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<EnumerationTypeBean> root, CriteriaBuilder builder) {
        if (!Boolean.parseBoolean(params.get("showHidden"))) predicates.add(builder.isFalse(root.get("hidden")));

        //vyhladaj podla searchUserFullName
        super.addSpecSearch(params, predicates, root, builder);
    }

    @Override
    public EnumerationTypeBean getOneItem(long id) {
        EnumerationTypeBean entity;

        if(id == -1) entity = new EnumerationTypeBean();
        else entity = enumerationTypeRepository.getByEnumId((int)id);

        processFromEntity(entity, ProcessItemAction.GETONE);

        return entity;
    }

    @Override
    public void beforeSave(EnumerationTypeBean entity) {
        Long entityId = entity.getId();
        if(entityId != null && entityId != -1) {
            //If allowChildEnumerationType was changed to false, remove from data all set child enum type's
            if(entity.isAllowChildEnumerationType()==false) {
                //Only if in DB is value still true
                if(jpaToBoolean(enumerationTypeRepository.isAllowChildEnumerationType(entityId)))
                    enumerationDataRepository.denyChildEnumerationTypeByTypeId(entityId);
            }

            //If allowParentEnumerationData was changed to false, remove from data all set parent enum data's
            if(entity.isAllowParentEnumerationData()==false) {
                //Only if in DB is value still true
                if(jpaToBoolean(enumerationTypeRepository.isAllowParentEnumerationData(entityId)))
                    enumerationDataRepository.denyParentEnumerationDataByTypeId(entityId);
            }
        }

        processToEntity(entity, ProcessItemAction.EDIT);
    }

    /** Restores the type's data only when a deleted type is successfully saved as active. */
    @Override
    public EnumerationTypeBean editItem(EnumerationTypeBean entity, long id) {
        boolean wasHidden = jpaToBoolean(enumerationTypeRepository.getHiddenByEnumTypeId((int) id));
        EnumerationTypeBean saved = super.editItem(entity, id);
        if (wasHidden && !saved.isHidden()) {
            enumerationDataRepository.deleteAllEnumDataByEnumTypeId(saved.getEnumerationTypeId(), false);
        }
        return saved;
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
    public void afterSave(EnumerationTypeBean entity, EnumerationTypeBean saved) {
        if (saved == null || saved.getId() == null) return;

        Cache.getInstance().removeObjectStartsWithName("enumeration.");

        List<CustomFieldsEntity> customFields = customFieldsRepository.findAllByClassNameAndEntityId(
            EnumerationDataBean.class.getName(),
            saved.getId(),
            DomainIdScopeResolver.resolve(EnumerationDataBean.class)
        );
        List<CustomFieldsEntity> changedFields = new ArrayList<>();
        for (CustomFieldsEntity customField : customFields) {
            String alphabet = customField.getAlphabet();
            if (alphabet == null || alphabet.length() != 1 || alphabet.charAt(0) < 'A' || alphabet.charAt(0) > 'L') continue;
            if (customField.getBonusEntityId() != null && customField.getBonusEntityId() != 0) continue;

            String label = EnumerationService.getStringFieldName(saved, alphabet.charAt(0));
            boolean changed = false;
            if (Objects.equals(label, customField.getLabel()) == false) {
                customField.setLabel(label);
                changed = true;
            }
            if (Tools.isEmpty(label) && Tools.isTrue(customField.getRequired())) {
                customField.setRequired(Boolean.FALSE);
                changed = true;
            }
            if (changed) {
                changedFields.add(customField);
            }
        }
        if (changedFields.isEmpty() == false) customFieldsRepository.saveAll(changedFields);
    }

    @Override
    public void beforeDuplicate(EnumerationTypeBean entity, Long originalId) {
        requireActiveType(originalId);
    }

    /** Rejects duplication and deletion using persisted state rather than the submitted hidden flag. */
    private void requireActiveType(Long id) {
        EnumerationTypeBean type = id == null ? null : enumerationTypeRepository.findById(id).orElse(null);
        if (type == null || type.isHidden()) throwError("config.not_permitted_action_err");
    }

    @Override
    public void afterDuplicate(EnumerationTypeBean entity, Long originalId) {
        if (originalId == null || originalId < 1) {
            throw new IllegalArgumentException("Original enumeration type ID is required for duplication.");
        }
        if (entity == null || entity.getId() == null || entity.getId() < 1) {
            throw new IllegalStateException("Duplicated enumeration type must be saved before copying its field settings.");
        }

        int domainId = DomainIdScopeResolver.resolve(EnumerationDataBean.class);
        List<CustomFieldsEntity> sourceFields = customFieldsRepository.findAllByClassNameAndEntityId(
            EnumerationDataBean.class.getName(),
            originalId,
            domainId
        );
        List<CustomFieldsEntity> duplicatedFields = new ArrayList<>();
        for (CustomFieldsEntity sourceField : sourceFields) {
            if (CustomFieldsService.isEnumerationStringAlphabet(sourceField.getAlphabet()) == false) continue;
            if (sourceField.getBonusEntityId() != null && sourceField.getBonusEntityId() != 0) continue;

            CustomFieldsEntity duplicatedField = new CustomFieldsEntity();
            BeanUtils.copyProperties(sourceField, duplicatedField, "id");
            duplicatedField.setEntityId(entity.getId());
            duplicatedFields.add(duplicatedField);
        }

        if (duplicatedFields.isEmpty() == false) customFieldsRepository.saveAll(duplicatedFields);
    }

    @Override
    public boolean deleteItem(EnumerationTypeBean entity, long id) {
        requireActiveType(id);
        enumerationTypeRepository.deleteEnumTypeById(entity.getEnumerationTypeId(), true);
        //"Delete" all created EnumerationData's under this EnumerationType
        enumerationDataRepository.deleteAllEnumDataByEnumTypeId(entity.getEnumerationTypeId(), true);

        Adminlog.add(Adminlog.TYPE_UPDATEDB, "DELETE/HIDE:\nid: "+id+"\nname: "+entity.getTypeName(), (int)id, -1);
        Cache.getInstance().removeObjectStartsWithName("enumeration.");
        return true;
    }
}
