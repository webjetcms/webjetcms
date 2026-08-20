package sk.iway.iwcm.components.enumerations.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsEntity;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsRepository;
import sk.iway.iwcm.components.customfields.rest.CustomFieldsService;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeBean;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.datatable.json.LabelValue;

@RestController
@RequestMapping("/admin/rest/enumeration/string-fields")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_enumerations')")
@Datatable
public class EnumerationStringFieldsRestController extends DatatableRestControllerV2<CustomFieldsEntity, Long> {

    private static final String ENUMERATION_TYPE_ID = "enumerationTypeId";
    private static final String ENUMERATION_DATA_CLASS_NAME = EnumerationDataBean.class.getName();
    private static final List<String> ALPHABETS = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L");

    private final CustomFieldsRepository customFieldsRepository;
    private final EnumerationTypeRepository enumerationTypeRepository;
    private final CustomFieldsService customFieldsService;

    @Autowired
    public EnumerationStringFieldsRestController(CustomFieldsRepository customFieldsRepository, EnumerationTypeRepository enumerationTypeRepository,
            CustomFieldsService customFieldsService) {
        super(customFieldsRepository, CustomFieldsEntity.class);
        this.customFieldsRepository = customFieldsRepository;
        this.enumerationTypeRepository = enumerationTypeRepository;
        this.customFieldsService = customFieldsService;
    }

    @Override
    public Page<CustomFieldsEntity> getAllItems(Pageable pageable) {
        EnumerationTypeBean enumerationType = getEnumerationType();
        if (enumerationType == null) return new DatatablePageImpl<>(new ArrayList<>());

        Page<CustomFieldsEntity> page = customFieldsRepository.findAllEnumerationStringFields(
            ENUMERATION_DATA_CLASS_NAME,
            enumerationType.getId(),
            CloudToolsForCore.getDomainId(),
            ALPHABETS,
            pageable
        );
        processFromEntity(page, ProcessItemAction.GETALL);
        return page;
    }

    @Override
    public CustomFieldsEntity getOneItem(long id) {
        EnumerationTypeBean enumerationType = getEnumerationType();
        if (enumerationType == null) return null;

        CustomFieldsEntity entity;
        if (id == -1) {
            entity = new CustomFieldsEntity();
            entity.setType("text");
            entity.setTextMaxLength(1024);
            entity.setRequired(Boolean.FALSE);
        } else {
            entity = customFieldsRepository.findById(id).orElse(null);
            if (belongsToContext(entity, enumerationType) == false) return null;
        }

        return processFromEntity(entity, ProcessItemAction.GETONE);
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<CustomFieldsEntity> root, CriteriaBuilder builder) {
        EnumerationTypeBean enumerationType = getEnumerationType();
        if (enumerationType == null) {
            predicates.add(builder.disjunction());
            return;
        }

        predicates.add(builder.equal(root.get("className"), ENUMERATION_DATA_CLASS_NAME));
        predicates.add(builder.equal(root.get("entityId"), enumerationType.getId()));
        predicates.add(builder.equal(root.get("bonusClassName"), ""));
        predicates.add(builder.equal(root.get("bonusEntityId"), 0L));
        predicates.add(builder.equal(root.get("domainId"), CloudToolsForCore.getDomainId()));
        predicates.add(root.get("alphabet").in(ALPHABETS));
        super.addSpecSearch(params, predicates, root, builder);
    }

    @Override
    public void getOptions(DatatablePageImpl<CustomFieldsEntity> page) {
        EnumerationTypeBean enumerationType = getEnumerationType();
        List<LabelValue> alphabetOptions = new ArrayList<>();
        if (enumerationType != null) {
            for (String alphabet : ALPHABETS) {
                if (Tools.isEmpty(EnumerationService.getStringFieldName(enumerationType, alphabet.charAt(0)))) continue;
                alphabetOptions.add(new LabelValue(
                    EnumerationService.getStringFieldLabel(enumerationType, alphabet.charAt(0), getProp()),
                    alphabet
                ));
            }
        }

        page.addOptions("alphabet", alphabetOptions, "label", "value", false);
        page.addOptions("type", CustomFieldsService.getFieldsTypes(getProp()), "label", "value", false);
        page.addOptions("specificFieldsVisibility", CustomFieldsService.getSpecificFieldVisibility(), "label", "value", false);
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, CustomFieldsEntity> target, Identity user, Errors errors, Long id, CustomFieldsEntity entity) {
        super.validateEditor(request, target, user, errors, id, entity);
        if (errors.hasErrors() || ("create".equals(target.getAction()) == false && "edit".equals(target.getAction()) == false)) return;

        EnumerationTypeBean enumerationType = getEnumerationType();
        if (enumerationType == null) {
            errors.rejectValue("errorField.alphabet", null, getProp().getText("enum_type.string_field_type.invalid_error")); //NOSONAR
            return;
        }

        applyContext(entity, enumerationType);
        String alphabet = entity.getAlphabet();
        if (customFieldsService.validateSpecificClass(entity, target.getAction(), errors, id, getProp()) == false) return;

        Long existingId = customFieldsRepository.getEntityId(
            ENUMERATION_DATA_CLASS_NAME,
            alphabet,
            enumerationType.getId(),
            "",
            0L,
            CloudToolsForCore.getDomainId()
        ).orElse(-1L);

        boolean duplicate = "create".equals(target.getAction()) && existingId > 0;
        if ("edit".equals(target.getAction()) && existingId > 0 && existingId.equals(id) == false) duplicate = true;
        if (duplicate) {
            errors.rejectValue("errorField.alphabet", null, getProp().getText("settings.custom-fields.duplicity-err")); //NOSONAR
        }
    }

    @Override
    public void beforeSave(CustomFieldsEntity entity) {
        EnumerationTypeBean enumerationType = getEnumerationType();
        if (enumerationType == null || entity == null || CustomFieldsService.isEnumerationStringAlphabet(entity.getAlphabet()) == false) {
            throw new IllegalArgumentException(getProp().getText("enum_type.string_field_type.invalid_error"));
        }
        applyContext(entity, enumerationType);
    }

    @Override
    public CustomFieldsEntity processFromEntity(CustomFieldsEntity entity, ProcessItemAction action) {
        if (entity == null) return null;

        EnumerationTypeBean enumerationType = getEnumerationType();
        if (enumerationType == null) return null;
        applyContext(entity, enumerationType);

        if (ProcessItemAction.GETONE.equals(action)) {
            CustomFieldsService.fromEntity(entity);
        }
        return entity;
    }

    @Override
    public CustomFieldsEntity processToEntity(CustomFieldsEntity entity, ProcessItemAction action) {
        EnumerationTypeBean enumerationType = getEnumerationType();
        if (enumerationType == null || entity == null || CustomFieldsService.isEnumerationStringAlphabet(entity.getAlphabet()) == false) {
            throw new IllegalArgumentException(getProp().getText("enum_type.string_field_type.invalid_error"));
        }

        applyContext(entity, enumerationType);
        return CustomFieldsService.toEntity(entity);
    }

    @Override
    public CustomFieldsEntity insertItem(CustomFieldsEntity entity) {
        entity.setId(null);
        return super.insertItem(entity);
    }

    @Override
    public CustomFieldsEntity editItem(CustomFieldsEntity entity, long id) {
        entity.setId(id);
        return super.editItem(entity, id);
    }

    @Override
    public boolean checkItemPerms(CustomFieldsEntity entity, Long id) {
        EnumerationTypeBean enumerationType = getEnumerationType();
        if (enumerationType == null) return false;
        if (id == null || id == -1) return entity == null || entity.getId() == null || entity.getId() < 1;

        CustomFieldsEntity stored = customFieldsRepository.findById(id).orElse(null);
        return belongsToContext(stored, enumerationType);
    }

    @Override
    public boolean deleteItem(CustomFieldsEntity entity, long id) {
        EnumerationTypeBean enumerationType = getEnumerationType();
        CustomFieldsEntity stored = customFieldsRepository.findById(id).orElse(null);
        if (belongsToContext(stored, enumerationType) == false) return false;

        customFieldsRepository.delete(stored);
        return true;
    }

    private EnumerationTypeBean getEnumerationType() {
        int enumerationTypeId = Tools.getIntValue(getRequest().getParameter(ENUMERATION_TYPE_ID), -1);
        if (enumerationTypeId < 1) return null;
        return enumerationTypeRepository.getNonHiddenByEnumId(enumerationTypeId, false);
    }

    private void applyContext(CustomFieldsEntity entity, EnumerationTypeBean enumerationType) {
        entity.setClassName(ENUMERATION_DATA_CLASS_NAME);
        entity.setEntityId(enumerationType.getId());
        entity.setDomainId(CloudToolsForCore.getDomainId());
        entity.setBonusClassName("");
        entity.setBonusEntityId(0L);

        if (CustomFieldsService.isEnumerationStringAlphabet(entity.getAlphabet())) {
            entity.setLabel(EnumerationService.getStringFieldName(enumerationType, entity.getAlphabet().charAt(0)));
        }
    }

    private boolean belongsToContext(CustomFieldsEntity entity, EnumerationTypeBean enumerationType) {
        if (entity == null || enumerationType == null) return false;
        return ENUMERATION_DATA_CLASS_NAME.equals(entity.getClassName())
            && Objects.equals(enumerationType.getId(), entity.getEntityId())
            && Tools.isEmpty(entity.getBonusClassName())
            && (entity.getBonusEntityId() == null || entity.getBonusEntityId() == 0)
            && Objects.equals(CloudToolsForCore.getDomainId(), entity.getDomainId())
            && ALPHABETS.contains(entity.getAlphabet());
    }

}
