package sk.iway.iwcm.stat.rest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.stat.heat_map.HeatMapAccess;
import sk.iway.iwcm.stat.heat_map.HeatMapHistoryService;
import sk.iway.iwcm.stat.heat_map.HeatMapStorage;
import sk.iway.iwcm.stat.heat_map.HeatMapStorage.WidthCount;
import sk.iway.iwcm.stat.heat_map.HeatMapHistoryService.ReportRange;
import sk.iway.iwcm.stat.heat_map.HeatMapHistoryService.VersionPeriod;
import static sk.iway.iwcm.stat.heat_map.HeatMapCoordinateMode.DOCUMENT;
import static sk.iway.iwcm.stat.heat_map.HeatMapCoordinateMode.CONTAINER;
import sk.iway.iwcm.stat.jpa.HeatMapPageDTO;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies that every heatmap report applies authorization before accessing click data. */
class HeatMapRestControllerTest extends BaseWebjetTest {
    private static final LocalDate FROM = LocalDate.of(2026, 8, 31);
    private static final LocalDate TO = LocalDate.of(2026, 9, 2);

    private static final Date[] DATES = {
            Date.from(FROM.atStartOfDay(ZoneId.systemDefault()).toInstant()),
            Date.from(TO.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1)) };
    private static final ReportRange RANGE = ReportRange.of(DATES);

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final HeatMapAccess access = mock(HeatMapAccess.class);
    private final HeatMapHistoryService history = mock(HeatMapHistoryService.class);

    private HeatMapRestController controller() {
        HeatMapRestController controller = spy(new HeatMapRestController(access, history));
        doReturn(request).when(controller).getRequest();
        when(access.currentDomain(request)).thenReturn("public.example");
        when(access.dateRange("period")).thenReturn(DATES);
        return controller;
    }

    /** The preview uses its source viewport and the selected public domain's selector. */
    @Test
    void containerMetadataKeepsThePublicDomainConfigurationAndViewport() {
        HeatMapRestController controller = controller();
        DocDetails document = new DocDetails();
        document.setTitle("autotest container");
        when(access.requireDocument(request, 123)).thenReturn(document);
        when(history.resolve(request, 123, "period", null)).thenReturn(
                new HeatMapHistoryService.PreviewSelection(null, null, "current", false, List.of(), RANGE, document));
        RequestBean requestBean = new RequestBean();
        requestBean.setDomain("admin.example");
        try (MockedStatic<HeatMapStorage> storage = mockStatic(HeatMapStorage.class);
                MockedStatic<DocDB> docs = mockStatic(DocDB.class);
                MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<SetCharacterEncodingFilter> requests = mockStatic(SetCharacterEncodingFilter.class)) {
            requests.when(SetCharacterEncodingFilter::getCurrentRequestBean).thenReturn(requestBean);
            constants.when(() -> Constants.getString("statHeatMapMainContainerSelector")).thenAnswer(invocation ->
                    "public.example".equals(requestBean.getDomain()) ? ".public-container" : ".admin-container");
            storage.when(() -> HeatMapStorage.getWidths("public.example", 123, FROM, TO)).thenReturn(List.of(
                    new WidthCount(1140, CONTAINER, 20, 1369), new WidthCount(1140, DOCUMENT, 2, 1140)));
            assertEquals(2, controller.widths(123, "period", null).size());
            Map<String, Object> metadata = controller.metadata(123, "period", 1140, CONTAINER, null);
            assertEquals(RANGE, metadata.get("clickRange"));
            assertEquals(1369, metadata.get("previewWidth"));
            assertEquals(20L, metadata.get("clicks"));
            assertEquals(".public-container", metadata.get("containerSelector"));
            assertEquals("admin.example", requestBean.getDomain());
            assertEquals(2L, controller.metadata(123, "period", 1140, DOCUMENT, null).get("clicks"));
            assertThrows(ResponseStatusException.class, () -> controller.metadata(123, "period", 960, CONTAINER, null));
            storage.verify(() -> HeatMapStorage.getWidths("public.example", 123, FROM, TO), times(1));
        }
    }

    /** A cached result belongs to one domain, page and effective day range, and expires after 30 seconds. */
    @Test
    void scopesWidthCountsAndRefreshesExpiredReports() {
        HeatMapRestController controller = controller();
        when(history.versionRange(123, DATES, 7)).thenReturn(new ReportRange(RANGE.from(), RANGE.from() + 1000));
        List<WidthCount> updated = List.of(new WidthCount(390, DOCUMENT, 1, 390));
        try (MockedStatic<HeatMapStorage> storage = mockStatic(HeatMapStorage.class);
                MockedStatic<Tools> time = mockStatic(Tools.class)) {
            time.when(Tools::getNow).thenReturn(1000L);
            storage.when(() -> HeatMapStorage.getWidths("public.example", 123, FROM, TO)).thenReturn(List.of(), updated);
            assertEquals(List.of(), controller.widths(123, "period", null));
            controller.widths(123, "period", 7);
            controller.widths(456, "period", null);
            when(access.currentDomain(request)).thenReturn("other.example");
            controller.widths(123, "period", null);
            storage.verify(() -> HeatMapStorage.getWidths("public.example", 123, FROM, FROM));
            storage.verify(() -> HeatMapStorage.getWidths("public.example", 456, FROM, TO));
            storage.verify(() -> HeatMapStorage.getWidths("other.example", 123, FROM, TO));

            when(access.currentDomain(request)).thenReturn("public.example");
            time.when(Tools::getNow).thenReturn(30_999L);
            assertEquals(List.of(), controller.widths(123, "period", null));
            time.when(Tools::getNow).thenReturn(31_000L);
            assertEquals(updated, controller.widths(123, "period", null));
            storage.verify(() -> HeatMapStorage.getWidths("public.example", 123, FROM, TO), times(2));
        }
    }

    /** The version picker shows up to 100 newest versions while the report includes clicks from the full period. */
    @ParameterizedTest
    @ValueSource(ints = { 99, 100, 101 })
    void limitsVersionsShownInHeatMapMetadata(int versionCount) {
        HeatMapRestController controller = controller();
        DocDetails document = new DocDetails();
        when(access.requireDocument(request, 123)).thenReturn(document);
        List<VersionPeriod> versions = IntStream.iterate(versionCount, id -> id > 0, id -> id - 1)
                .mapToObj(id -> new VersionPeriod(id, RANGE.from() + id, new ReportRange(RANGE.from() + id, RANGE.from() + id + 1)))
                .toList();
        when(history.resolve(request, 123, "period", null)).thenReturn(
                new HeatMapHistoryService.PreviewSelection(versionCount, versions.get(0).effectiveFrom(), "history", false, versions, RANGE, document));
        try (MockedStatic<HeatMapStorage> storage = mockStatic(HeatMapStorage.class);
                MockedStatic<DocDB> docs = mockStatic(DocDB.class)) {
            storage.when(() -> HeatMapStorage.getWidths("public.example", 123, FROM, TO))
                    .thenReturn(List.of(new WidthCount(390, DOCUMENT, 250, 390)));
            Map<String, Object> metadata = controller.metadata(123, "period", 390, DOCUMENT, null);
            assertEquals(versions.subList(0, Math.min(versionCount, 100)), metadata.get("versions"));
            assertEquals(RANGE, metadata.get("clickRange"));
            assertEquals(250L, metadata.get("clicks"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "public.example", "" })
    void widthsUseTrustedDomainAndInclusiveDaysAndOrderPopularWidthsFirst(String domain) {
        HeatMapRestController controller = controller();
        when(access.currentDomain(request)).thenReturn(domain);
        request.addParameter("domain", "untrusted.example");
        try (MockedStatic<HeatMapStorage> storage = mockStatic(HeatMapStorage.class)) {
            storage.when(() -> HeatMapStorage.getWidths(domain, 123, FROM, TO))
                    .thenReturn(List.of(new WidthCount(390, DOCUMENT, 7, 390), new WidthCount(1280, DOCUMENT, 2, 1280), new WidthCount(1440, DOCUMENT, 2, 1440)));
            assertEquals(List.of(new WidthCount(390, DOCUMENT, 7, 390), new WidthCount(1280, DOCUMENT, 2, 1280),
                    new WidthCount(1440, DOCUMENT, 2, 1440)), controller.widths(123, "period", null));
            verify(access).requireDocument(request, 123);
            storage.verify(() -> HeatMapStorage.getWidths(domain, 123, FROM, TO));
        }
    }

    /** Both click resources use the same server-selected version interval within the report. */
    @Test
    void appliesVersionRangeToCountsAndTiles() throws Exception {
        HeatMapRestController controller = controller();
        ReportRange version = new ReportRange(RANGE.from() + 1000, RANGE.from() + 2000);
        when(history.versionRange(eq(123), any(Date[].class), eq(7))).thenReturn(version);
        try (MockedStatic<HeatMapStorage> storage = mockStatic(HeatMapStorage.class)) {
            controller.widths(123, "period", 7);
            controller.tile(123, "period", 390, DOCUMENT, 0, 0, 7);
            storage.verify(() -> HeatMapStorage.getWidths("public.example", 123, FROM, FROM));
            storage.verify(() -> HeatMapStorage.getTile("public.example", 123, FROM, FROM, 390, DOCUMENT, 0, 0));
        }
    }

    @Test
    void rejectedDocumentNeverReachesTheStorageOrImageCache() {
        HeatMapRestController controller = controller();
        when(access.requireDocument(request, 456)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        try (MockedStatic<HeatMapStorage> storage = mockStatic(HeatMapStorage.class)) {
            assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                    () -> controller.widths(456, "period", null)).getStatusCode());
            assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                    () -> controller.metadata(456, "period", 390, DOCUMENT, null)).getStatusCode());
            assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                    () -> controller.tile(456, "period", 390, DOCUMENT, 0, 0, null)).getStatusCode());
            storage.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "public.example", "" })
    void authorizedTileUsesTheSameScopeAndIsNeverCachedByTheBrowser(String domain) throws Exception {
        HeatMapRestController controller = controller();
        when(access.currentDomain(request)).thenReturn(domain);
        byte[] png = { 1, 2, 3 };
        try (MockedStatic<HeatMapStorage> storage = mockStatic(HeatMapStorage.class)) {
            storage.when(() -> HeatMapStorage.getTile(domain, 123, FROM, TO, 390, DOCUMENT, 0, 2)).thenReturn(png);
            var response = controller.tile(123, "period", 390, DOCUMENT, 0, 2, null);
            assertArrayEquals(png, response.getBody());
            assertEquals(MediaType.IMAGE_PNG, response.getHeaders().getContentType());
            assertEquals("no-store", response.getHeaders().getCacheControl());
            verify(access).requireDocument(request, 123);
            storage.verify(() -> HeatMapStorage.getTile(domain, 123, FROM, TO, 390, DOCUMENT, 0, 2));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "public.example", "" })
    void pageListHidesUnauthorizedAndDeletedDocuments(String domain) {
        HeatMapRestController controller = controller();
        when(access.currentDomain(request)).thenReturn(domain);
        Identity user = mock(Identity.class);
        doReturn(user).when(controller).getUser();
        request.addParameter("dateRange", "period");
        DocDetails visible = mock(DocDetails.class);
        DocDetails forbidden = mock(DocDetails.class);
        when(visible.getDocId()).thenReturn(123);
        when(visible.getTitle()).thenReturn("Visible page");
        when(access.canViewDocument(user, domain, visible)).thenReturn(true);
        DocDB docs = mock(DocDB.class);
        when(docs.getBasicDocDetails(123, false)).thenReturn(visible);
        when(docs.getBasicDocDetails(456, false)).thenReturn(forbidden);
        try (MockedStatic<HeatMapStorage> storage = mockStatic(HeatMapStorage.class);
                MockedStatic<DocDB> docLookup = mockStatic(DocDB.class)) {
            storage.when(() -> HeatMapStorage.getPageCounts(domain, FROM, TO)).thenReturn(Map.of(123, 5L, 456, 9L, 789, 8L));
            docLookup.when(DocDB::getInstance).thenReturn(docs);
            docLookup.when(() -> DocDB.getURLFromDocId(123, request)).thenReturn("/visible.html");
            List<HeatMapPageDTO> rows = controller.getAllItems(Pageable.unpaged()).getContent();
            assertEquals(1, rows.size());
            assertEquals(123L, rows.get(0).getId());
            assertEquals(5L, rows.get(0).getClicks());
            assertEquals("/visible.html", rows.get(0).getUrl());
        }
    }

    @Test
    void writeHooksAndUnboundedTileCoordinatesAreRejected() {
        HeatMapRestController controller = controller();
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, assertThrows(ResponseStatusException.class,
                () -> controller.beforeSave(new HeatMapPageDTO())).getStatusCode());
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, assertThrows(ResponseStatusException.class,
                () -> controller.deleteItem(new HeatMapPageDTO(), 123)).getStatusCode());
        try (MockedStatic<HeatMapStorage> storage = mockStatic(HeatMapStorage.class)) {
            assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                    () -> controller.tile(123, "period", 390, DOCUMENT, Integer.MAX_VALUE, 0, null)).getStatusCode());
            assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                    () -> controller.metadata(123, "period", 0, DOCUMENT, null)).getStatusCode());
            storage.verifyNoInteractions();
        }
    }
}
