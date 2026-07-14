package sk.iway.iwcm.headless.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.headless.dto.PageResponse;
import sk.iway.iwcm.headless.service.HeadlessNavigationService;
import sk.iway.iwcm.headless.service.HeadlessPageService;

class HeadlessPageRestControllerTest {

    private HeadlessPageService headlessPageService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        headlessPageService = mock(HeadlessPageService.class);
        HeadlessPageRestController controller = spy(new HeadlessPageRestController(
                headlessPageService, mock(HeadlessNavigationService.class)));
        doReturn(true).when(controller).isIpAddressAllowed(any(HttpServletRequest.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new HeadlessExceptionHandler())
                .build();
    }

    @Test
    void getPageByPathNormalizesPathBeforeResolvingPage() throws Exception {
        PageResponse page = new PageResponse();
        page.setDocId(12);
        page.setTitle("News");
        page.setVirtualPath("/news");

        DocDB docDB = mock(DocDB.class);
        try (MockedStatic<DocDB> docDbMock = mockStatic(DocDB.class)) {
            docDbMock.when(() -> DocDB.getDomain(any(HttpServletRequest.class))).thenReturn("example.com");
            docDbMock.when(DocDB::getInstance).thenReturn(docDB);
            when(docDB.getVirtualPathDocId("/news", "example.com")).thenReturn(12);
            when(headlessPageService.resolvePage(anyString(), anyString(), anyBoolean(),
                    any(HttpServletRequest.class), any(HttpServletResponse.class))).thenReturn(page);

            mockMvc.perform(get("/rest/headless/v1/pages/by-path").param("path", "news"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/json"))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("\"docId\":12")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("\"virtualPath\":\"/news\"")));
        }
    }

    @Test
    void getPageByPathRejectsEmptyPath() throws Exception {
        mockMvc.perform(get("/rest/headless/v1/pages/by-path").param("path", "   "))
                .andExpect(status().isBadRequest())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "Path parameter is required.")));
    }
}
