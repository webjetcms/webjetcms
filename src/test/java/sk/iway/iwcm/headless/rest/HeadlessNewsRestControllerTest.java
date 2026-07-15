package sk.iway.iwcm.headless.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import sk.iway.iwcm.components.news.NewsRestController;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.headless.dto.HeadlessNewsRequest;
import sk.iway.iwcm.headless.dto.HeadlessNewsResponse;
import sk.iway.iwcm.headless.service.HeadlessNewsService;
import sk.iway.iwcm.system.datatable.json.LabelValue;

class HeadlessNewsRestControllerTest {

    private HeadlessNewsService headlessNewsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        headlessNewsService = mock(HeadlessNewsService.class);
        HeadlessNewsRestController controller = spy(
                new HeadlessNewsRestController(headlessNewsService));
        doReturn(true).when(controller).isIpAddressAllowed(any(HttpServletRequest.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new HeadlessExceptionHandler())
                .build();
    }

    @Test
    void listNewsRejectsMissingRequiredGroupIds() throws Exception {
        mockMvc.perform(post("/rest/headless/v1/news")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                        .andExpect(content().string(org.hamcrest.Matchers.containsString("\"items\":[]")));
    }

    @Test
    void listNewsCapsPageSizeAndReturnsServicePaginationTotals() throws Exception {
        HeadlessNewsResponse serviceResponse = new HeadlessNewsResponse();
        serviceResponse.setPage(2);
        serviceResponse.setSize(100);
        serviceResponse.setTotalElements(250);
        serviceResponse.setTotalPages(3);
        serviceResponse.setItems(java.util.List.of());
        when(headlessNewsService.listNews(any(HeadlessNewsRequest.class))).thenReturn(serviceResponse);

        try (MockedStatic<NewsRestController> newsRestControllerMock = mockStatic(NewsRestController.class);
             MockedStatic<GroupsDB> groupsDbMock = mockStatic(GroupsDB.class)) {
            GroupsDB groupsDB = mock(GroupsDB.class);
            newsRestControllerMock.when(() -> NewsRestController.convertIdsToNamePair(
                    eq("constant:newsAdminGroupIds"), isNull(), any(HttpServletRequest.class)))
                    .thenReturn(java.util.List.of(new LabelValue("News", "24")));
            groupsDbMock.when(GroupsDB::getInstance).thenReturn(groupsDB);
            when(groupsDB.expandGroupIdsToChilds(new int[] { 24 }, true)).thenReturn(new int[] { 24, 25 });

            mockMvc.perform(post("/rest/headless/v1/news")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"groupIds\":[25],\"paging\":true,\"pageSize\":1000,\"offset\":100}"))
                    .andExpect(status().isOk())
                            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"page\":2")))
                            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"size\":100")))
                            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"totalElements\":250")))
                            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"totalPages\":3")));
        }

        ArgumentCaptor<HeadlessNewsRequest> requestCaptor = ArgumentCaptor.forClass(HeadlessNewsRequest.class);
        verify(headlessNewsService).listNews(requestCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(100, requestCaptor.getValue().getPageSize());
    }

        @Test
        void listNewsRejectsGroupsOutsideConfiguredNewsFolders() throws Exception {
                try (MockedStatic<NewsRestController> newsRestControllerMock = mockStatic(NewsRestController.class);
                         MockedStatic<GroupsDB> groupsDbMock = mockStatic(GroupsDB.class)) {
                        GroupsDB groupsDB = mock(GroupsDB.class);
                        newsRestControllerMock.when(() -> NewsRestController.convertIdsToNamePair(
                                    eq("constant:newsAdminGroupIds"), isNull(), any(HttpServletRequest.class)))
                                        .thenReturn(java.util.List.of(new LabelValue("News", "24")));
                        groupsDbMock.when(GroupsDB::getInstance).thenReturn(groupsDB);
                        when(groupsDB.expandGroupIdsToChilds(new int[] { 24 }, true)).thenReturn(new int[] { 24, 25 });

                        mockMvc.perform(post("/rest/headless/v1/news")
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content("{\"groupIds\":[26]}"))
                                        .andExpect(status().isBadRequest());

                        verify(headlessNewsService, never()).listNews(any(HeadlessNewsRequest.class));
                }
        }

    @Test
    void listNewsSanitizesUnexpectedServiceExceptions() throws Exception {
        when(headlessNewsService.listNews(any(HeadlessNewsRequest.class)))
                .thenThrow(new RuntimeException("database password: secret"));

        try (MockedStatic<NewsRestController> newsRestControllerMock = mockStatic(NewsRestController.class);
             MockedStatic<GroupsDB> groupsDbMock = mockStatic(GroupsDB.class)) {
            GroupsDB groupsDB = mock(GroupsDB.class);
            newsRestControllerMock.when(() -> NewsRestController.convertIdsToNamePair(
                    eq("constant:newsAdminGroupIds"), isNull(), any(HttpServletRequest.class)))
                    .thenReturn(java.util.List.of(new LabelValue("News", "24")));
            groupsDbMock.when(GroupsDB::getInstance).thenReturn(groupsDB);
            when(groupsDB.expandGroupIdsToChilds(new int[] { 24 }, true)).thenReturn(new int[] { 24 });

            mockMvc.perform(post("/rest/headless/v1/news")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"groupIds\":[24]}"))
                    .andExpect(status().isInternalServerError())
                            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                                    "Internal server error.")))
                            .andExpect(content().string(org.hamcrest.Matchers.not(
                                    org.hamcrest.Matchers.containsString("secret"))));
        }
    }
}
