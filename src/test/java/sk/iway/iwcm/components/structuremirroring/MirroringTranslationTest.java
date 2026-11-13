package sk.iway.iwcm.components.structuremirroring;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.http.client.HttpResponseException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.spring.events.WebjetEventType;
import sk.iway.iwcm.system.translation.TranslationEngine;
import sk.iway.iwcm.system.translation.TranslationException;
import sk.iway.iwcm.system.translation.TranslationService;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Exercises the real mirroring field translation without database writes or external API calls. */
class MirroringTranslationTest extends BaseWebjetTest {

    /** Failed folder translations still create linked folders in the source language. */
    @Test
    void createsLinkedFolderWhenTranslationFails() {
        GroupDetails source = new GroupDetails();
        source.setGroupId(42);
        source.setParentGroupId(10);
        source.setSyncId(500);
        source.setGroupName("Akciové ponuky v e-shope");
        source.setSortPriority(70);
        source.setLng("sk");
        GroupDetails destination = new GroupDetails();
        destination.setGroupId(20);
        destination.setLng("en");
        GroupsDB groups = mock(GroupsDB.class);
        GroupDetails mirror = new GroupDetails();
        mirror.setGroupName(source.getGroupName());
        when(groups.getNewGroupDetails(source.getGroupName(), 20)).thenReturn(mirror);

        try (var groupAccess = mockStatic(GroupsDB.class);
                var mapping = mockStatic(MirroringService.class);
                var request = mockStatic(SetCharacterEncodingFilter.class);
                var folderMirrors = mockStatic(GroupMirroringServiceV9.class, CALLS_REAL_METHODS);
                var translators = mockConstruction(TranslationService.class, (translator, context) ->
                    when(translator.translateOrThrow(anyString(), anyString(), anyString()))
                        .thenThrow(new TranslationException("Fixture quota exhausted")))) {
            groupAccess.when(GroupsDB::getInstance).thenReturn(groups);
            groupAccess.when(() -> GroupsDB.sanitizeGroupName(anyString(), anyBoolean())).thenCallRealMethod();
            mapping.when(() -> MirroringService.notifyTranslationFailure(any(TranslationException.class))).thenCallRealMethod();
            request.when(SetCharacterEncodingFilter::getCurrentRequestBean).thenReturn(new RequestBean());
            mapping.when(() -> MirroringService.isEnabled(42)).thenReturn(true);
            mapping.when(() -> MirroringService.getMappingForGroup(10)).thenReturn(List.of(destination));
            folderMirrors.when(() -> GroupMirroringServiceV9.getGroupsBySyncId(500, 42)).thenReturn(List.of());

            List<NotifyBean> notifications = runWithNotifications(() ->
                new GroupMirroringServiceV9().handleGroupSave(source, WebjetEventType.AFTER_SAVE, null));

            assertTranslationWarning(notifications);
            assertTrue(notifications.get(0).getText().contains("Fixture quota exhausted"));
            verify(groups).setGroup(mirror, false);
            assertAll(
                () -> assertEquals("Akciové ponuky v e-shope", mirror.getGroupName()),
                () -> assertEquals(500, mirror.getSyncId()),
                () -> assertEquals(20, mirror.getParentGroupId()),
                () -> assertEquals(70, mirror.getSortPriority())
            );
        }
    }

    /** Existing mirrors receive current source content and synchronize their folder title after a translation failure. */
    @Test
    void updatesExistingPageInSourceLanguageWhenTranslationFails() {
        DocDetails source = new DocDetails();
        source.setDocId(7);
        source.setGroupId(100);
        source.setTitle("Akciové ponuky v e-shope");
        source.setNavbar("Aktuálne ponuky");
        source.setHtmlData("Aktuálny perex");
        source.setData("<p>Aktuálny obsah</p>");
        DocDetails target = new DocDetails();
        target.setDocId(8);
        target.setGroupId(200);
        target.setTitle("Special offers");
        target.setAvailable(false);
        GroupDetails sourceGroup = new GroupDetails();
        sourceGroup.setGroupId(100);
        GroupDetails targetGroup = new GroupDetails();
        targetGroup.setGroupId(200);
        GroupsDB groups = mock(GroupsDB.class);
        DocDB documents = mock(DocDB.class);
        when(groups.getGroup(100)).thenReturn(sourceGroup);
        when(groups.getGroup(200)).thenReturn(targetGroup);
        when(documents.getDoc(8)).thenReturn(target);
        String originalLogin = Constants.getString("structureMirroringAutoTranslatorLogin");

        try (var groupAccess = mockStatic(GroupsDB.class);
                var documentAccess = mockStatic(DocDB.class);
                var mapping = mockStatic(MirroringService.class);
                var titleSync = mockStatic(sk.iway.iwcm.editor.service.GroupsService.class);
                var folderMirrors = mockStatic(GroupMirroringServiceV9.class);
                var pageMirrors = mockStatic(DocMirroringServiceV9.class, CALLS_REAL_METHODS);
                var translators = mockConstruction(TranslationService.class, (translator, context) ->
                    when(translator.translateOrThrow(anyString())).thenThrow(new TranslationException("Fixture quota exhausted")))) {
            Constants.setString("structureMirroringAutoTranslatorLogin", "");
            mapping.when(() -> MirroringService.notifyTranslationFailure(any(TranslationException.class))).thenCallRealMethod();
            groupAccess.when(GroupsDB::getInstance).thenReturn(groups);
            documentAccess.when(DocDB::getInstance).thenReturn(documents);
            titleSync.when(() -> sk.iway.iwcm.editor.service.GroupsService.canSyncTitle(8, 200)).thenReturn(true);
            mapping.when(() -> MirroringService.isEnabled(100)).thenReturn(true);
            mapping.when(() -> MirroringService.getMappingForGroup(100)).thenReturn(List.of(targetGroup));
            pageMirrors.when(() -> DocMirroringServiceV9.getSyncId(7)).thenReturn(500);
            pageMirrors.when(() -> DocMirroringServiceV9.getDocBySyncId(500, 7))
                .thenReturn(List.of(target));
            folderMirrors.when(() -> GroupMirroringServiceV9.getLanguage(sourceGroup)).thenReturn("sk");
            folderMirrors.when(() -> GroupMirroringServiceV9.getLanguage(targetGroup)).thenReturn("en");

            List<NotifyBean> notifications = runWithNotifications(() ->
                new DocMirroringServiceV9().handleDocSave(source, WebjetEventType.AFTER_SAVE));

            documentAccess.verify(() -> DocDB.saveDoc(target, false));
            documentAccess.verify(() -> DocDB.changeGroupTitle(200, 8, source.getTitle(), true));
            verify(groups, never()).setGroup(any(GroupDetails.class), anyBoolean());
            assertAll(
                () -> assertEquals(source.getTitle(), target.getTitle()),
                () -> assertEquals(source.getNavbar(), target.getNavbar()),
                () -> assertEquals(source.getHtmlData(), target.getHtmlData()),
                () -> assertEquals(source.getData(), target.getData()),
                () -> assertEquals("", target.getVirtualPath())
            );
            assertTranslationWarning(notifications);
            assertTrue(notifications.get(0).getText().contains("Fixture quota exhausted"));
        } finally {
            Constants.setString("structureMirroringAutoTranslatorLogin", originalLogin);
        }
    }

    /** New mirrors retain source content, their sync ID and the destination home-page link after any translation failure. */
    @ParameterizedTest
    @ValueSource(strings = { "Akciové ponuky v&nbsp;e-shope", "<p>Slovenský obsah</p>" })
    void createsLinkedPageWhenTranslationFails(String failingText) {
        DocDetails source = new DocDetails();
        source.setDocId(7);
        source.setGroupId(100);
        source.setTitle("Akciové ponuky v&nbsp;e-shope");
        source.setNavbar("Ponuky v&nbsp;obchode");
        source.setHtmlData("Slovenský perex");
        source.setData("<p>Slovenský obsah</p>");
        source.setVirtualPath("/sk/ponuky/");
        GroupDetails sourceGroup = new GroupDetails();
        sourceGroup.setGroupId(100);
        GroupDetails targetGroup = new GroupDetails();
        targetGroup.setGroupId(200);
        GroupsDB groups = mock(GroupsDB.class);
        DocDB documents = mock(DocDB.class);
        when(groups.getGroup(100)).thenReturn(sourceGroup);
        when(groups.getGroup(200)).thenReturn(targetGroup);
        String originalLogin = Constants.getString("structureMirroringAutoTranslatorLogin");
        try (var groupAccess = mockStatic(GroupsDB.class);
                var documentAccess = mockStatic(DocDB.class);
                var mapping = mockStatic(MirroringService.class);
                var folderMirrors = mockStatic(GroupMirroringServiceV9.class);
                var request = mockStatic(SetCharacterEncodingFilter.class);
                var pageMirrors = mockStatic(DocMirroringServiceV9.class, CALLS_REAL_METHODS);
                var translators = mockConstruction(TranslationService.class, (translator, context) ->
                    when(translator.translateOrThrow(anyString())).thenAnswer(invocation -> {
                        String text = invocation.getArgument(0, String.class);
                        if (failingText.equals(text)) throw new TranslationException("Fixture quota exhausted");
                        return "Translated " + text;
                    }))) {
            Constants.setString("structureMirroringAutoTranslatorLogin", "");
            mapping.when(() -> MirroringService.notifyTranslationFailure(any(TranslationException.class))).thenCallRealMethod();
            groupAccess.when(GroupsDB::getInstance).thenReturn(groups);
            documentAccess.when(DocDB::getInstance).thenReturn(documents);
            request.when(SetCharacterEncodingFilter::getCurrentRequestBean).thenReturn(new RequestBean());
            mapping.when(() -> MirroringService.isEnabled(100)).thenReturn(true);
            mapping.when(() -> MirroringService.getMappingForGroup(100)).thenReturn(List.of(targetGroup));
            pageMirrors.when(() -> DocMirroringServiceV9.getSyncId(7)).thenReturn(500);
            pageMirrors.when(() -> DocMirroringServiceV9.getDocBySyncId(500, 7)).thenReturn(List.of());
            folderMirrors.when(() -> GroupMirroringServiceV9.getLanguage(sourceGroup)).thenReturn("sk");
            folderMirrors.when(() -> GroupMirroringServiceV9.getLanguage(targetGroup)).thenReturn("en");
            documentAccess.when(() -> DocDB.saveDoc(any(DocDetails.class), anyBoolean())).thenAnswer(invocation -> {
                DocDetails mirror = invocation.getArgument(0);
                assertAll(
                    () -> assertEquals(500, mirror.getSyncId()),
                    () -> assertEquals(200, mirror.getGroupId()),
                    () -> assertEquals("Akciové ponuky v e-shope", mirror.getTitle()),
                    () -> assertEquals("Ponuky v obchode", mirror.getNavbar()),
                    () -> assertEquals(source.getHtmlData(), mirror.getHtmlData()),
                    () -> assertEquals(source.getData(), mirror.getData()),
                    () -> assertEquals("", mirror.getVirtualPath())
                );
                mirror.setDocId(8);
                return true;
            });

            List<NotifyBean> notifications = runWithNotifications(() ->
                new DocMirroringServiceV9().handleDocSave(source, WebjetEventType.AFTER_SAVE));

            assertTranslationWarning(notifications);
            assertTrue(notifications.get(0).getText().contains("Fixture quota exhausted"));
            documentAccess.verify(() -> DocDB.saveDoc(any(DocDetails.class), anyBoolean()), times(1));
            verify(groups).setGroup(targetGroup, false);
            assertEquals(8, targetGroup.getDefaultDocId());
            assertEquals("/sk/ponuky/", source.getVirtualPath());
        } finally {
            Constants.setString("structureMirroringAutoTranslatorLogin", originalLogin);
        }
    }

    /** A failure in any field copies all source fields together instead of saving a partially translated page. */
    @ParameterizedTest
    @ValueSource(strings = { "Akciové ponuky v e-shope", "Ponuky", "Slovenský perex", "<p>Slovenský obsah</p>" })
    void copiesSourceWhenTranslationFails(String failingText) {
        DocDetails source = new DocDetails();
        source.setTitle("Akciové ponuky v e-shope");
        source.setNavbar("Ponuky");
        source.setHtmlData("Slovenský perex");
        source.setData("<p>Slovenský obsah</p>");

        DocDetails target = new DocDetails();
        target.setTitle("Special offers in e-shop");
        target.setNavbar("Offers");
        target.setHtmlData("English summary");
        target.setData("<p>English content</p>");
        target.setVirtualPath("/en/special-offers/");

        TranslationEngine engine = mock(TranslationEngine.class, CALLS_REAL_METHODS);
        when(engine.translate(anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            String text = invocation.getArgument(0, String.class);
            if (failingText.equals(text)) throw new IllegalStateException("Translation quota exhausted");
            return "Translated " + text;
        });
        TranslationService translator = mock(TranslationService.class, CALLS_REAL_METHODS);
        translator.setFromLanguage("sk");
        translator.setToLanguage("en");
        translator.setTranslationEngine(engine);

        List<NotifyBean> notifications = runWithNotifications(() ->
            assertFalse(DocMirroringServiceV9.translateDoc(source, target, translator)));

        assertAll(
            () -> assertEquals(source.getTitle(), target.getTitle()),
            () -> assertEquals(source.getNavbar(), target.getNavbar()),
            () -> assertEquals(source.getHtmlData(), target.getHtmlData()),
            () -> assertEquals(source.getData(), target.getData()),
            () -> assertEquals("", target.getVirtualPath())
        );
        assertTranslationWarning(notifications);
    }

    /** Successful translation applies every field and regenerates the destination URL. */
    @Test
    void appliesSuccessfulTranslation() {
        TranslationService translator = mock(TranslationService.class);
        when(translator.translateOrThrow("Ponuky")).thenReturn("<strong>Offers in&nbsp;store</strong>");
        when(translator.translateOrThrow("Perex")).thenReturn("Summary");
        when(translator.translateOrThrow("<p>Obsah</p>")).thenReturn("<p>Content in&nbsp;store</p>");
        DocDetails source = new DocDetails();
        source.setTitle("Ponuky");
        source.setNavbar("Ponuky");
        source.setHtmlData("Perex");
        source.setData("<p>Obsah</p>");
        DocDetails target = new DocDetails();
        target.setVirtualPath("/old-url/");

        List<NotifyBean> notifications = runWithNotifications(() ->
            assertTrue(DocMirroringServiceV9.translateDoc(source, target, translator)));
        assertNull(notifications);
        assertAll(
            () -> assertEquals("<strong>Offers in store</strong>", target.getTitle()),
            () -> assertEquals("<strong>Offers in store</strong>", target.getNavbar()),
            () -> assertEquals("Summary", target.getHtmlData()),
            () -> assertEquals("<p>Content in&nbsp;store</p>", target.getData()),
            () -> assertEquals("", target.getVirtualPath())
        );
    }

    /** Repeated translation failures in one request produce a single user-facing warning. */
    @Test
    void deduplicatesTranslationWarnings() {
        TranslationService translator = mock(TranslationService.class);
        when(translator.translateOrThrow(anyString())).thenThrow(new TranslationException("Fixture quota exhausted"));
        DocDetails source = new DocDetails();
        source.setTitle("Ponuky");
        List<NotifyBean> notifications = runWithNotifications(() -> {
            DocMirroringServiceV9.translateDoc(source, new DocDetails(), translator);
            DocMirroringServiceV9.translateDoc(source, new DocDetails(), translator);
        });
        assertTranslationWarning(notifications);
        assertTrue(notifications.get(0).getText().contains("Fixture quota exhausted"));
    }

    /** Wrapped HTTP errors expose the provider reason and status in the user-facing notification. */
    @ParameterizedTest
    @CsvSource({ "456, Quota Exceeded", "400, Unsupported language pair", "500, Internal Server Error" })
    void includesProviderFailureInWarning(int status, String message) {
        TranslationService translator = mock(TranslationService.class);
        when(translator.translateOrThrow(anyString())).thenThrow(new TranslationException("DeepL translation failed",
            new HttpResponseException(status, message)));
        DocDetails source = new DocDetails();
        source.setTitle("Ponuky");

        List<NotifyBean> notifications = runWithNotifications(() ->
            DocMirroringServiceV9.translateDoc(source, new DocDetails(), translator));

        assertTranslationWarning(notifications);
        assertTrue(notifications.get(0).getText().contains(message));
        assertTrue(notifications.get(0).getText().contains(Integer.toString(status)));
    }

    /** Failures without a message still display the translated fallback warning. */
    @ParameterizedTest
    @NullAndEmptySource
    void preservesWarningWithoutFailureMessage(String message) {
        TranslationService translator = mock(TranslationService.class);
        when(translator.translateOrThrow(anyString())).thenThrow(new TranslationException(message));
        DocDetails source = new DocDetails();
        source.setTitle("Ponuky");

        List<NotifyBean> notifications = runWithNotifications(() ->
            DocMirroringServiceV9.translateDoc(source, new DocDetails(), translator));

        assertTranslationWarning(notifications);
        assertFalse(notifications.get(0).getText().contains("<br>"));
    }

    /** Provider text is escaped because notification bodies support HTML. */
    @Test
    void escapesProviderFailureInWarning() {
        TranslationService translator = mock(TranslationService.class);
        when(translator.translateOrThrow(anyString())).thenThrow(new TranslationException("Unsupported <pair> & language"));
        DocDetails source = new DocDetails();
        source.setTitle("Ponuky");

        List<NotifyBean> notifications = runWithNotifications(() ->
            DocMirroringServiceV9.translateDoc(source, new DocDetails(), translator));

        assertTranslationWarning(notifications);
        assertTrue(notifications.get(0).getText().contains("Unsupported &lt;pair&gt; &amp; language"));
        assertFalse(notifications.get(0).getText().contains("<pair>"));
    }

    private List<NotifyBean> runWithNotifications(Runnable operation) {
        DatatableRestControllerV2<DocDetails, Long> controller = new DatatableRestControllerV2<>() {
            @Override
            public boolean processAction(DocDetails entity, String action) {
                operation.run();
                return true;
            }
        };
        var response = controller.action("mirror", new Long[] { -1L }).getBody();
        assertNotNull(response);
        assertNull(response.getError());
        return response.getNotify();
    }

    private void assertTranslationWarning(List<NotifyBean> notifications) {
        assertNotNull(notifications);
        assertEquals(1, notifications.size());
        NotifyBean warning = notifications.get(0);
        assertEquals("warning", warning.getType());
        assertNotEquals("components.mirroring.translation_failed.title", warning.getTitle());
        assertNotEquals("components.mirroring.translation_failed.text", warning.getText());
        assertFalse(warning.getTitle().isBlank());
        assertFalse(warning.getText().isBlank());
    }
}
