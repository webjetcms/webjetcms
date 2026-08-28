package sk.iway.iwcm.components.enumerations.rest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.Validator;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsEntity;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsRepository;
import sk.iway.iwcm.components.customfields.rest.CustomFieldsService;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeBean;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableResponse;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.spring.events.WebjetEventPublisher;

/**
 * Tests Oracle-safe filtering, common-domain scoping, and duplicate validation
 * for enumeration string fields in {@link EnumerationStringFieldsRestController}.
 */
class EnumerationStringFieldsRestControllerTest {

    private static final int CURRENT_DOMAIN_ID = 9;
    private static final int COMMON_DOMAIN_ID = 3;
    private static final int ENUMERATION_TYPE_ID = 42;
    private static final List<String> ALPHABETS = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L");

    private int originalCommonDomainId;

    @BeforeEach
    void setUpCommonDomain() {
        originalCommonDomainId = Constants.getInt("domainIdCommon");
        Constants.setInt("domainIdCommon", COMMON_DOMAIN_ID);
    }

    @AfterEach
    void restoreCommonDomain() {
        Constants.setInt("domainIdCommon", originalCommonDomainId);
    }

    /**
     * Verifies that search criteria match both null and empty bonus class names
     * and use the configured common domain.
     */
    @Test
    @SuppressWarnings("unchecked")
    void searchesBothNullAndEmptyBonusClassName() {
        ControllerContext context = createControllerContext();
        Root<CustomFieldsEntity> root = mock(Root.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Path<Object> classNamePath = mock(Path.class);
        Path<Object> entityIdPath = mock(Path.class);
        Path<Object> bonusClassNamePath = mock(Path.class);
        Path<Object> bonusEntityIdPath = mock(Path.class);
        Path<Object> domainIdPath = mock(Path.class);
        Path<Object> alphabetPath = mock(Path.class);
        Predicate nullBonusClassName = mock(Predicate.class);
        Predicate emptyBonusClassName = mock(Predicate.class);
        Predicate noBonusClassName = mock(Predicate.class);
        when(root.<Object>get("className")).thenReturn(classNamePath);
        when(root.<Object>get("entityId")).thenReturn(entityIdPath);
        when(root.<Object>get("bonusClassName")).thenReturn(bonusClassNamePath);
        when(root.<Object>get("bonusEntityId")).thenReturn(bonusEntityIdPath);
        when(root.<Object>get("domainId")).thenReturn(domainIdPath);
        when(root.<Object>get("alphabet")).thenReturn(alphabetPath);
        when(builder.isNull(bonusClassNamePath)).thenReturn(nullBonusClassName);
        when(builder.equal(bonusClassNamePath, "")).thenReturn(emptyBonusClassName);
        when(builder.or(nullBonusClassName, emptyBonusClassName)).thenReturn(noBonusClassName);
        List<Predicate> predicates = new ArrayList<>();

        try (MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class)) {
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(CURRENT_DOMAIN_ID);

            context.controller().addSpecSearch(Collections.emptyMap(), predicates, root, builder);
        }

        verify(builder).isNull(bonusClassNamePath);
        verify(builder).equal(bonusClassNamePath, "");
        verify(builder).or(nullBonusClassName, emptyBonusClassName);
        verify(builder).equal(domainIdPath, COMMON_DOMAIN_ID);
        assertTrue(predicates.contains(noBonusClassName));
    }

    /**
     * Verifies that list loading uses the common domain instead of the current request domain.
     */
    @Test
    void listsFieldsFromCommonDomain() {
        ControllerContext context = createControllerContext();
        Pageable pageable = Pageable.unpaged();
        Page<CustomFieldsEntity> emptyPage = new PageImpl<>(List.of());
        when(context.customFieldsRepository().findAllEnumerationStringFields(
            EnumerationDataBean.class.getName(),
            (long) ENUMERATION_TYPE_ID,
            COMMON_DOMAIN_ID,
            ALPHABETS,
            pageable
        )).thenReturn(emptyPage);

        Page<CustomFieldsEntity> result;
        try (MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class)) {
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(CURRENT_DOMAIN_ID);

            result = context.controller().getAllItems(pageable);
        }

        assertTrue(result.isEmpty(), "The repository result should be returned unchanged");
        verify(context.customFieldsRepository()).findAllEnumerationStringFields(
            EnumerationDataBean.class.getName(),
            (long) ENUMERATION_TYPE_ID,
            COMMON_DOMAIN_ID,
            ALPHABETS,
            pageable
        );
    }

    /**
     * Verifies that duplicate validation uses the no-bonus query and rejects an existing field.
     */
    @Test
    void usesNoBonusRepositoryQueryForDuplicateLookup() {
        ControllerContext context = createControllerContext();
        CustomFieldsEntity entity = new CustomFieldsEntity();
        entity.setAlphabet("A");
        entity.setType("text");
        DatatableRequest<Long, CustomFieldsEntity> target = new DatatableRequest<>();
        target.setAction("create");
        target.setErrorField(entity);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(target, "datatableRequest");
        when(context.customFieldsService().validateSpecificClass(any(), anyString(), any(), any(), any())).thenReturn(true);
        when(context.customFieldsRepository().getEntityIdWithoutBonusClassName(
            EnumerationDataBean.class.getName(),
            "A",
            (long) ENUMERATION_TYPE_ID,
            0L,
            COMMON_DOMAIN_ID
        )).thenReturn(Optional.of(77L));

        try (MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class)) {
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(CURRENT_DOMAIN_ID);

            context.controller().validateEditor(context.request(), target, null, errors, -1L, entity);
        }

        verify(context.customFieldsRepository()).getEntityIdWithoutBonusClassName(
            EnumerationDataBean.class.getName(),
            "A",
            (long) ENUMERATION_TYPE_ID,
            0L,
            COMMON_DOMAIN_ID
        );
        assertTrue(errors.hasFieldErrors("errorField.alphabet"));
    }

    /**
     * Verifies that the persisted enumeration field context is normalized to the common domain.
     */
    @Test
    void appliesCommonDomainToFieldContext() {
        ControllerContext context = createControllerContext();
        CustomFieldsEntity entity = new CustomFieldsEntity();
        entity.setAlphabet("A");
        entity.setType("text");
        entity.setDomainId(CURRENT_DOMAIN_ID);

        CustomFieldsEntity processed;
        try (MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class)) {
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(CURRENT_DOMAIN_ID);

            processed = context.controller().processToEntity(entity, ProcessItemAction.CREATE);
        }

        assertEquals(COMMON_DOMAIN_ID, processed.getDomainId(), "Enumeration fields must be stored in the common domain");
        assertEquals(EnumerationDataBean.class.getName(), processed.getClassName());
        assertEquals((long) ENUMERATION_TYPE_ID, processed.getEntityId());
        assertEquals("", processed.getBonusClassName());
        assertEquals(0L, processed.getBonusEntityId());
    }

    /**
     * Verifies that the base editor pipeline accepts editing a common-domain record
     * while the current request belongs to another domain.
     */
    @Test
    void handleEditorAcceptsCommonDomainEditFromAnotherDomain() {
        ControllerContext context = createControllerContext();
        long fieldId = 77L;
        CustomFieldsEntity stored = createField(fieldId, COMMON_DOMAIN_ID);
        CustomFieldsEntity edited = createField(fieldId, COMMON_DOMAIN_ID);
        edited.setTooltip("Updated tooltip");

        when(context.customFieldsService().validateSpecificClass(any(), anyString(), any(), any(), any())).thenReturn(true);
        when(context.customFieldsRepository().findById(fieldId)).thenReturn(Optional.of(stored));
        when(context.customFieldsRepository().getEntityIdWithoutBonusClassName(
            EnumerationDataBean.class.getName(),
            "A",
            (long) ENUMERATION_TYPE_ID,
            0L,
            COMMON_DOMAIN_ID
        )).thenReturn(Optional.of(fieldId));
        when(context.customFieldsRepository().save(any(CustomFieldsEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Validator validator = mock(Validator.class);
        when(validator.validate(any(CustomFieldsEntity.class))).thenReturn(Collections.emptySet());
        context.controller().setValidator(validator);

        DatatableRequest<Long, CustomFieldsEntity> request = new DatatableRequest<>();
        request.setAction("edit");
        Map<Long, CustomFieldsEntity> data = new HashMap<>();
        data.put(fieldId, edited);
        request.setData(data);

        ResponseEntity<DatatableResponse<CustomFieldsEntity>> response;
        try (MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class);
                MockedStatic<WebjetEventPublisher> eventPublisher = mockStatic(WebjetEventPublisher.class)) {
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(CURRENT_DOMAIN_ID);

            response = assertDoesNotThrow(
                () -> context.controller().handleEditor(context.request(), request),
                "A common-domain record must not be rejected as belonging to another domain"
            );
        }

        assertNotNull(response.getBody());
        assertNull(response.getBody().getError(), "The editor response must not contain a domain mismatch error");
        assertNotNull(response.getBody().getData());
        assertEquals(COMMON_DOMAIN_ID, response.getBody().getData().get(0).getDomainId());
        verify(context.customFieldsRepository()).save(any(CustomFieldsEntity.class));
    }

    private static CustomFieldsEntity createField(long id, int domainId) {
        CustomFieldsEntity entity = new CustomFieldsEntity();
        entity.setId(id);
        entity.setClassName(EnumerationDataBean.class.getName());
        entity.setEntityId((long) ENUMERATION_TYPE_ID);
        entity.setAlphabet("A");
        entity.setType("text");
        entity.setValue("text");
        entity.setLabel("String field");
        entity.setRequired(Boolean.FALSE);
        entity.setBonusClassName("");
        entity.setBonusEntityId(0L);
        entity.setDomainId(domainId);
        return entity;
    }

    private static ControllerContext createControllerContext() {
        CustomFieldsRepository customFieldsRepository = mock(CustomFieldsRepository.class);
        EnumerationTypeRepository enumerationTypeRepository = mock(EnumerationTypeRepository.class);
        CustomFieldsService customFieldsService = mock(CustomFieldsService.class);
        Prop prop = mock(Prop.class);
        when(prop.getText(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        EnumerationStringFieldsRestController controller = new EnumerationStringFieldsRestController(
            customFieldsRepository,
            enumerationTypeRepository,
            customFieldsService
        ) {
            @Override
            public Prop getProp() {
                return prop;
            }
        };

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("enumerationTypeId", Integer.toString(ENUMERATION_TYPE_ID));
        controller.setRequest(request);

        EnumerationTypeBean enumerationType = new EnumerationTypeBean();
        enumerationType.setId((long) ENUMERATION_TYPE_ID);
        enumerationType.setString1Name("String field");
        when(enumerationTypeRepository.getNonHiddenByEnumId(ENUMERATION_TYPE_ID, false)).thenReturn(enumerationType);

        return new ControllerContext(controller, customFieldsRepository, customFieldsService, request);
    }

    private record ControllerContext(
        EnumerationStringFieldsRestController controller,
        CustomFieldsRepository customFieldsRepository,
        CustomFieldsService customFieldsService,
        MockHttpServletRequest request
    ) {
    }
}
