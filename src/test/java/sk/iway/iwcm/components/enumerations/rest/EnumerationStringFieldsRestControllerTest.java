package sk.iway.iwcm.components.enumerations.rest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsEntity;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsRepository;
import sk.iway.iwcm.components.customfields.rest.CustomFieldsService;
import sk.iway.iwcm.components.enumerations.model.EnumerationDataBean;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeBean;
import sk.iway.iwcm.components.enumerations.model.EnumerationTypeRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatableRequest;

/**
 * Tests Oracle-safe filtering and duplicate validation for enumeration string fields
 * without a bonus class in {@link EnumerationStringFieldsRestController}.
 */
class EnumerationStringFieldsRestControllerTest {

    private static final int DOMAIN_ID = 9;
    private static final int ENUMERATION_TYPE_ID = 42;

    /**
     * Verifies that search criteria match both null and empty bonus class names.
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
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(DOMAIN_ID);

            context.controller().addSpecSearch(Collections.emptyMap(), predicates, root, builder);
        }

        verify(builder).isNull(bonusClassNamePath);
        verify(builder).equal(bonusClassNamePath, "");
        verify(builder).or(nullBonusClassName, emptyBonusClassName);
        assertTrue(predicates.contains(noBonusClassName));
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
            DOMAIN_ID
        )).thenReturn(Optional.of(77L));

        try (MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class)) {
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(DOMAIN_ID);

            context.controller().validateEditor(context.request(), target, null, errors, -1L, entity);
        }

        verify(context.customFieldsRepository()).getEntityIdWithoutBonusClassName(
            EnumerationDataBean.class.getName(),
            "A",
            (long) ENUMERATION_TYPE_ID,
            0L,
            DOMAIN_ID
        );
        assertTrue(errors.hasFieldErrors("errorField.alphabet"));
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
