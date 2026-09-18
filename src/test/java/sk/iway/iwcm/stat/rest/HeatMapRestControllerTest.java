package sk.iway.iwcm.stat.rest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.stat.heat_map.HeatMapAccess;
import sk.iway.iwcm.stat.heat_map.HeatMapHistoryService;
import sk.iway.iwcm.stat.heat_map.HeatMapStorage;
import sk.iway.iwcm.stat.jpa.HeatMapPageDTO;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies that every heatmap report applies authorization before accessing click data. */
class HeatMapRestControllerTest extends BaseWebjetTest {
    private static final LocalDate FROM = LocalDate.of(2026, 8, 31);
    private static final LocalDate TO = LocalDate.of(2026, 9, 2);

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final HeatMapAccess access = mock(HeatMapAccess.class);
    private final HeatMapHistoryService history = mock(HeatMapHistoryService.class);

    private HeatMapRestController controller() {
        HeatMapRestController controller = spy(new HeatMapRestController(access, history));
        doReturn(request).when(controller).getRequest();
        when(access.currentDomain(request)).thenReturn("public.example");
        when(access.dateRange("period")).thenReturn(new Date[] {
                Date.from(FROM.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                Date.from(TO.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1)) });
        return controller;
    }

    @Test
    void widthsUseTrustedDomainAndInclusiveDaysAndOrderPopularWidthsFirst() {
        HeatMapRestController controller = controller();
        request.addParameter("domain", "untrusted.example");
        try (MockedStatic<HeatMapStorage> storage = mockStatic(HeatMapStorage.class)) {
            storage.when(() -> HeatMapStorage.getWidths("public.example", 123, FROM, TO))
                    .thenReturn(Map.of(1440, 2L, 390, 7L, 1280, 2L));
            assertEquals(List.of(new HeatMapRestController.WidthCount(390, 7), new HeatMapRestController.WidthCount(1280, 2),
                    new HeatMapRestController.WidthCount(1440, 2)), controller.widths(123, "period"));
            verify(access).requireDocument(request, 123);
            storage.verify(() -> HeatMapStorage.getWidths("public.example", 123, FROM, TO));
        }
    }

    @Test
    void rejectedDocumentNeverReachesTheStorageOrImageCache() {
        HeatMapRestController controller = controller();
        when(access.requireDocument(request, 456)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        try (MockedStatic<HeatMapStorage> storage = mockStatic(HeatMapStorage.class)) {
            assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                    () -> controller.widths(456, "period")).getStatusCode());
            assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                    () -> controller.metadata(456, "period", 390)).getStatusCode());
            assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                    () -> controller.tile(456, "period", 390, 0, 0)).getStatusCode());
            storage.verifyNoInteractions();
        }
    }

    @Test
    void authorizedTileUsesTheSameScopeAndIsNeverCachedByTheBrowser() throws Exception {
        HeatMapRestController controller = controller();
        byte[] png = { 1, 2, 3 };
        try (MockedStatic<HeatMapStorage> storage = mockStatic(HeatMapStorage.class)) {
            storage.when(() -> HeatMapStorage.getTile("public.example", 123, FROM, TO, 390, 0, 2)).thenReturn(png);
            var response = controller.tile(123, "period", 390, 0, 2);
            assertArrayEquals(png, response.getBody());
            assertEquals(MediaType.IMAGE_PNG, response.getHeaders().getContentType());
            assertEquals("no-store", response.getHeaders().getCacheControl());
            verify(access).requireDocument(request, 123);
            storage.verify(() -> HeatMapStorage.getTile("public.example", 123, FROM, TO, 390, 0, 2));
        }
    }

    @Test
    void pageListHidesUnauthorizedAndDeletedDocuments() {
        HeatMapRestController controller = controller();
        Identity user = mock(Identity.class);
        doReturn(user).when(controller).getUser();
        request.addParameter("dateRange", "period");
        DocDetails visible = mock(DocDetails.class);
        DocDetails forbidden = mock(DocDetails.class);
        when(visible.getDocId()).thenReturn(123);
        when(visible.getTitle()).thenReturn("Visible page");
        when(access.canViewDocument(user, "public.example", visible)).thenReturn(true);
        DocDB docs = mock(DocDB.class);
        when(docs.getBasicDocDetails(123, false)).thenReturn(visible);
        when(docs.getBasicDocDetails(456, false)).thenReturn(forbidden);
        try (MockedStatic<HeatMapStorage> storage = mockStatic(HeatMapStorage.class);
                MockedStatic<DocDB> docLookup = mockStatic(DocDB.class)) {
            storage.when(() -> HeatMapStorage.getPageCounts("public.example", FROM, TO)).thenReturn(Map.of(123, 5L, 456, 9L, 789, 8L));
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
                    () -> controller.tile(123, "period", 390, Integer.MAX_VALUE, 0)).getStatusCode());
            assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                    () -> controller.metadata(123, "period", 0)).getStatusCode());
            storage.verifyNoInteractions();
        }
    }
}
