package sk.iway.iwcm.components.forms;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.springframework.data.jpa.domain.Specification;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.forms.FormsDeletionService.DeletionResult;
import sk.iway.iwcm.components.forms.archive.FormsArchiveEntity;
import sk.iway.iwcm.components.forms.archive.FormsArchiveRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsConditionsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.components.multistep_form.rest.MultistepFormsService;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.NotifyBean;

/** Tests preservation of live form definitions, complete cleanup, domain scope, and notifications. */
class FormsDeletionServiceTest {
    private static final String NAME = "autotest-form";
    private static final int DOMAIN = 1;
    private static final String INCLUDE = "!INCLUDE(sk.iway.iwcm.components.multistep_form.mvc.MultistepFormApp, ";
    private FormsRepository forms;
    private FormsArchiveRepository archive;
    private FormSettingsRepository settings;
    private FormStepsRepository steps;
    private FormItemsRepository items;
    private FormItemsConditionsRepository conditions;
    private DocDetailsRepository documents;
    private FormsDeletionService service;
    private MockedStatic<CloudToolsForCore> cloud;
    private MockedStatic<Constants> constants;
    private MockedStatic<InitServlet> init;
    private MockedStatic<MultistepFormsService> cache;

    @BeforeEach
    void setUp() {
        forms = mock(FormsRepository.class);
        archive = mock(FormsArchiveRepository.class);
        settings = mock(FormSettingsRepository.class);
        steps = mock(FormStepsRepository.class);
        items = mock(FormItemsRepository.class);
        conditions = mock(FormItemsConditionsRepository.class);
        documents = mock(DocDetailsRepository.class);
        service = new FormsDeletionService(forms, archive, settings, steps, items, conditions, documents);
        cloud = mockStatic(CloudToolsForCore.class);
        cloud.when(CloudToolsForCore::getDomainId).thenReturn(DOMAIN);
        constants = mockStatic(Constants.class);
        constants.when(() -> Constants.getString("DocTools.removeCharsDir")).thenReturn("[^a-zA-Z/_0-9.=\\-]");
        init = mockStatic(InitServlet.class);
        cache = mockStatic(MultistepFormsService.class);
        FormsEntity header = new FormsEntity();
        header.setId(10L);
        header.setDomainId(DOMAIN);
        header.setFormName(NAME);
        when(forms.findFirstByIdAndDomainId(10L, DOMAIN)).thenReturn(Optional.of(header));
    }

    @AfterEach
    void tearDown() {
        cache.close();
        init.close();
        constants.close();
        cloud.close();
    }

    /** A protected form loses responses but keeps its management row and all definition tables. */
    @Test
    void preservesDefinitionForAvailablePageAndArchiveAndReportsBothReasons() {
        when(archive.countAllByFormNameAndDomainId(NAME, DOMAIN)).thenReturn(2);
        usingPages(page(123, true, NAME));

        DeletionResult result = service.delete(10L, false);

        assertTrue(result.deleted());
        assertFalse(result.settingsDeleted());
        assertTrue(result.archivedData());
        assertEquals(List.of(123), result.pageIds());
        verify(forms).deleteAllByFormNameAndDomainIdAndCreateDateNotNull(NAME, DOMAIN);
        verify(forms, never()).deleteAllByFormNameAndDomainIdAndCreateDateNull(any(), any());
        verifyNoInteractions(settings, steps, items, conditions);
        cache.verifyNoInteractions();
        Prop prop = mock(Prop.class);
        when(prop.getText(any(String.class))).thenAnswer(call -> call.getArgument(0));
        when(prop.getTextWithParams("components.forms.delete.pages", "123")).thenReturn("docId: 123");
        NotifyBean notification = result.notification(prop);
        assertEquals("info", notification.getType());
        assertTrue(notification.getText().contains("docId: 123"));
        assertTrue(notification.getText().contains("components.forms.delete.archive"));
    }

    /** Archive data alone is sufficient to preserve all shared definition records. */
    @Test
    void preservesDefinitionForArchiveWithoutPage() {
        when(archive.countAllByFormNameAndDomainId(NAME, DOMAIN)).thenReturn(1);
        assertFalse(service.delete(10L, false).settingsDeleted());
        verify(forms).deleteAllByFormNameAndDomainIdAndCreateDateNotNull(NAME, DOMAIN);
        verifyNoInteractions(settings, steps, items, conditions);
    }

    /** A live page alone protects the form even when it has no archived responses. */
    @Test
    void preservesDefinitionForAvailablePageWithoutArchive() {
        usingPages(page(123, true, NAME));
        assertFalse(service.delete(10L, false).settingsDeleted());
        verify(forms).deleteAllByFormNameAndDomainIdAndCreateDateNotNull(NAME, DOMAIN);
        verifyNoInteractions(settings, steps, items, conditions);
    }

    /** Unavailable pages and similar names do not prevent complete domain-scoped cleanup. */
    @Test
    void removesUnusedDefinitionIncludingConditions() {
        usingPages(page(123, false, NAME), page(124, true, NAME + "-other"));
        DeletionResult result = service.delete(10L, false);
        assertTrue(result.settingsDeleted());
        verify(forms).deleteAllByFormNameAndDomainIdAndCreateDateNull(NAME, DOMAIN);
        var order = inOrder(forms, conditions, items, steps, settings);
        order.verify(forms).deleteAllByFormNameAndDomainIdAndCreateDateNotNull(NAME, DOMAIN);
        order.verify(conditions).deleteAllByFormNameAndDomainId(NAME, DOMAIN);
        order.verify(items).deleteAllByFormNameAndDomainId(NAME, DOMAIN);
        order.verify(steps).deleteAllByFormNameAndDomainId(NAME, DOMAIN);
        order.verify(settings).deleteByFormNameAndDomainId(NAME, DOMAIN);
        order.verify(forms).deleteAllByFormNameAndDomainIdAndCreateDateNull(NAME, DOMAIN);
        cache.verify(() -> MultistepFormsService.clearValidationFieldsCache(NAME, DOMAIN));
        Prop prop = mock(Prop.class);
        assertEquals("success", result.notification(prop).getType());
    }

    /** Condition owner IDs are read before deletion so both condition caches can be evicted, including orphan owners. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void clearsConditionCachesBeforeRemovingFormStructure(boolean archived) {
        if (archived) archiveHeader();
        when(conditions.findConditionOwnerIds(NAME, DOMAIN)).thenReturn(List.of(101L, 102L));
        doAnswer(call -> {
            when(conditions.findConditionOwnerIds(NAME, DOMAIN)).thenReturn(List.of());
            return null;
        }).when(conditions).deleteAllByFormNameAndDomainId(NAME, DOMAIN);
        Cache conditionCache = mock(Cache.class);
        try (MockedStatic<Cache> caches = mockStatic(Cache.class)) {
            caches.when(Cache::getInstance).thenReturn(conditionCache);

            DeletionResult result = service.delete(archived ? 20L : 10L, archived);

            assertTrue(result.settingsDeleted());
            assertFalse(result.cleanupFailed());
            var order = inOrder(conditions, conditionCache, items, steps);
            order.verify(conditions).findConditionOwnerIds(NAME, DOMAIN);
            order.verify(conditions).deleteAllByFormNameAndDomainId(NAME, DOMAIN);
            for (long ownerId : List.of(101L, 102L)) {
                order.verify(conditionCache).removeObject("multistep_form.conditions." + DOMAIN + "." + ownerId + ".VISIBILITY", true);
                order.verify(conditionCache).removeObject("multistep_form.conditions." + DOMAIN + "." + ownerId + ".REQUIREMENT", true);
            }
            order.verify(items).deleteAllByFormNameAndDomainId(NAME, DOMAIN);
            order.verify(steps).deleteAllByFormNameAndDomainId(NAME, DOMAIN);
            verifyNoMoreInteractions(conditions, conditionCache);
        }
    }

    /** Deleting an archive never removes the settings or structure of an existing active form. */
    @Test
    void deletesArchiveWhilePreservingActiveForm() {
        archiveHeader();
        when(forms.countAllByFormNameAndDomainId(NAME, DOMAIN)).thenReturn(1);
        DeletionResult result = service.delete(20L, true);
        assertTrue(result.activeData());
        assertFalse(result.settingsDeleted());
        verify(archive).deleteAllByFormNameAndDomainIdAndCreateDateNull(NAME, DOMAIN);
        verify(forms, never()).deleteAllByFormNameAndDomainIdAndCreateDateNull(any(), any());
        verifyNoInteractions(settings, steps, items, conditions);
    }

    /** The final archive can clean up an unused orphan definition, but a live page still protects it. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deletesLastArchiveWithOrWithoutLivePage(boolean livePage) {
        archiveHeader();
        if (livePage) usingPages(page(123, true, NAME));
        DeletionResult result = service.delete(20L, true);
        assertEquals(!livePage, result.settingsDeleted());
        verify(archive).deleteAllByFormNameAndDomainIdAndCreateDateNull(NAME, DOMAIN);
        if (livePage) verifyNoInteractions(settings, steps, items, conditions);
        else verify(settings).deleteByFormNameAndDomainId(NAME, DOMAIN);
    }

    /** Both ordinary and archived single-response deletion leave settings and management rows intact. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deletesOnlySelectedResponse(boolean archived) {
        FormsEntity response = new FormsEntity();
        response.setId(11L);
        response.setFormName(NAME);
        response.setDomainId(DOMAIN);
        response.setCreateDate(new Date());
        if (archived) {
            FormsArchiveEntity archivedResponse = archiveHeader();
            archivedResponse.setCreateDate(new Date());
            verifyResponseResult(service.delete(20L, true));
            verify(archive).deleteById(20L);
        } else {
            when(forms.findFirstByIdAndDomainId(11L, DOMAIN)).thenReturn(Optional.of(response));
            verifyResponseResult(service.delete(11L, false));
            verify(forms).deleteById(11L);
        }
        verifyNoInteractions(settings, steps, items, conditions, documents);
    }

    /** Missing or foreign-domain IDs cannot trigger data or definition deletion. */
    @Test
    void rejectsMissingAndForeignDomainRecord() {
        assertFalse(service.delete(99L, false).deleted());
        FormsEntity foreign = new FormsEntity();
        foreign.setDomainId(2);
        when(forms.findFirstByIdAndDomainId(99L, DOMAIN)).thenReturn(Optional.of(foreign));
        assertFalse(service.delete(99L, false).deleted());
        verifyNoInteractions(settings, steps, items, conditions, documents, archive);
        verify(forms, never()).deleteById(any());
        verify(forms, never()).deleteAllByFormNameAndDomainIdAndCreateDateNull(any(), any());
    }

    /** Shared-domain installations inspect all domains; partitioned installations ignore foreign pages. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void respectsFormDomainPartitioning(boolean partitioned) {
        constants.when(() -> Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(partitioned);
        DocDetails page = page(123, true, NAME);
        page.setGroupId(200);
        usingPages(page);
        try (MockedStatic<GroupsDB> groups = mockStatic(GroupsDB.class)) {
            GroupsDB db = mock(GroupsDB.class);
            groups.when(GroupsDB::getInstance).thenReturn(db);
            when(db.getDomain(200)).thenReturn("other.example");
            groups.when(() -> GroupsDB.getDomainId("other.example")).thenReturn(2);
            assertEquals(partitioned, service.delete(10L, false).settingsDeleted());
        }
    }

    /** Each cleanup failure leaves deleted responses deleted and the header available for a successful retry. */
    @ParameterizedTest
    @CsvSource({
        "false, conditions", "false, items", "false, steps", "false, settings", "false, header",
        "true, conditions", "true, items", "true, steps", "true, settings", "true, header"
    })
    void retriesPartialCleanupWithoutRestoringResponses(boolean archived, String operation) {
        FormsRepositoryInterface<? extends FormsEntityBasic> repository = forms;
        long id = 10L;
        if (archived) {
            archiveHeader();
            repository = archive;
            id = 20L;
        }
        FormsEntityBasic header = repository.findFirstByIdAndDomainId(id, DOMAIN).orElseThrow();
        AtomicBoolean headerExists = new AtomicBoolean(true);
        AtomicInteger responses = new AtomicInteger(2);
        when(repository.findFirstByIdAndDomainId(id, DOMAIN)).thenAnswer(call ->
            headerExists.get() ? Optional.of(header) : Optional.empty());
        doAnswer(call -> { responses.set(0); return null; }).when(repository)
            .deleteAllByFormNameAndDomainIdAndCreateDateNotNull(NAME, DOMAIN);
        doAnswer(call -> { headerExists.set(false); return null; }).when(repository)
            .deleteAllByFormNameAndDomainIdAndCreateDateNull(NAME, DOMAIN);
        IllegalStateException failure = new IllegalStateException("Database failure");
        switch (operation) {
            case "conditions" -> doThrow(failure).doNothing().when(conditions).deleteAllByFormNameAndDomainId(NAME, DOMAIN);
            case "items" -> doThrow(failure).doNothing().when(items).deleteAllByFormNameAndDomainId(NAME, DOMAIN);
            case "steps" -> doThrow(failure).doNothing().when(steps).deleteAllByFormNameAndDomainId(NAME, DOMAIN);
            case "settings" -> doThrow(failure).doNothing().when(settings).deleteByFormNameAndDomainId(NAME, DOMAIN);
            case "header" -> doThrow(failure).doAnswer(call -> { headerExists.set(false); return null; }).when(repository)
                .deleteAllByFormNameAndDomainIdAndCreateDateNull(NAME, DOMAIN);
            default -> throw new IllegalArgumentException(operation);
        }

        DeletionResult first = service.delete(id, archived);
        assertTrue(first.deleted());
        assertTrue(first.cleanupFailed());
        assertEquals(0, responses.get());
        assertTrue(headerExists.get());
        assertEquals("header".equals(operation), first.settingsDeleted());
        assertEquals("warning", first.notification(mock(Prop.class)).getType());
        cache.verify(() -> MultistepFormsService.clearValidationFieldsCache(NAME, DOMAIN));
        if (!"header".equals(operation))
            verify(repository, never()).deleteAllByFormNameAndDomainIdAndCreateDateNull(NAME, DOMAIN);

        DeletionResult second = service.delete(id, archived);
        assertTrue(second.deleted());
        assertTrue(second.settingsDeleted());
        assertFalse(second.cleanupFailed());
        assertEquals(0, responses.get());
        assertFalse(headerExists.get());
        assertEquals("success", second.notification(mock(Prop.class)).getType());
    }

    /** Failure to delete responses must propagate without reporting successful deletion or removing the header. */
    @Test
    void propagatesResponseDeletionFailureBeforeDefinitionCleanup() {
        doThrow(new IllegalStateException("Database failure")).when(forms)
            .deleteAllByFormNameAndDomainIdAndCreateDateNotNull(NAME, DOMAIN);
        assertThrows(IllegalStateException.class, () -> service.delete(10L, false));
        verify(forms, never()).deleteAllByFormNameAndDomainIdAndCreateDateNull(NAME, DOMAIN);
        verifyNoInteractions(settings, conditions, items, steps);
        cache.verifyNoInteractions();
    }

    /** Quoted, encoded and reordered parameters match the complete form name in any include. */
    @ParameterizedTest
    @ValueSource(strings = {
        "formName=autotest-form", "formName=\"autotest-form\", cssTemplate=\"template-1.css\"",
        "formName=&quot;autotest-form&quot;,&#x20;cssTemplate=\"\"",
        "cssTemplate=\"\",\n formName=&#34;autotest-form&#34;"
    })
    void recognizesEncodedIncludes(String parameters) {
        assertTrue(FormsDeletionService.usesForm(INCLUDE + "formName=other)!" + INCLUDE + parameters + ")!", NAME));
        assertFalse(FormsDeletionService.usesForm(INCLUDE + parameters + ")!", NAME + "-other"));
    }

    /** Simple forms and legacy savedb fields receive the same protection without matching unrelated apps. */
    @Test
    void recognizesSimpleAndLegacyFormsAndRejectsUnrelatedContent() {
        assertTrue(FormsDeletionService.usesForm("!INCLUDE(/components/formsimple/form.jsp, formName=&quot;autotest-form&quot;)!", NAME));
        assertTrue(FormsDeletionService.usesForm("<input type=hidden name=savedb value='autotest-form'>", NAME));
        assertFalse(FormsDeletionService.usesForm("!INCLUDE(other.Component, formName=autotest-form)!", NAME));
        assertFalse(FormsDeletionService.usesForm("<p>autotest-form</p>", NAME));
        assertFalse(FormsDeletionService.usesForm(null, NAME));
    }

    /** FormSimple stores the normalized display name as the key for both supported include formats. */
    @ParameterizedTest
    @ValueSource(strings = {"/components/formsimple/form.jsp", "sk.iway.iwcm.components.formsimple.FormSimpleApp"})
    void preservesSimpleFormReferencedByDisplayName(String component) {
        assertTrue(FormsDeletionService.usesForm("!INCLUDE(" + component + ", formName=Formulár ľahko)!", "formular-lahko"));
        String include = "!INCLUDE(" + component + ", formName=&quot;Formulár ľahko&quot;)!";
        assertTrue(FormsDeletionService.usesForm(include, "formular-lahko"));
        assertFalse(FormsDeletionService.usesForm(include, "formular-lahko-2"));
        assertTrue(FormsDeletionService.usesForm("!INCLUDE(" + component + ", formName=&quot;&quot;)!", "form-simple"));
    }

    private void verifyResponseResult(DeletionResult result) {
        assertTrue(result.deleted());
        assertFalse(result.definition());
        assertNull(result.notification(mock(Prop.class)));
    }

    private FormsArchiveEntity archiveHeader() {
        FormsArchiveEntity header = new FormsArchiveEntity();
        header.setId(20L);
        header.setFormName(NAME);
        header.setDomainId(DOMAIN);
        when(archive.findFirstByIdAndDomainId(20L, DOMAIN)).thenReturn(Optional.of(header));
        return header;
    }

    @SuppressWarnings("unchecked")
    private void usingPages(DocDetails... pages) {
        when(documents.findAll(any(Specification.class))).thenReturn(List.of(pages));
    }

    private DocDetails page(int id, boolean available, String formName) {
        DocDetails page = new DocDetails();
        page.setDocId(id);
        page.setAvailable(available);
        page.setData(INCLUDE + "formName=&quot;" + formName + "&quot;,&#x20;cssTemplate=\"\")!");
        return page;
    }
}
