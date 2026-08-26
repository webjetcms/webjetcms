package sk.iway.iwcm.system.spring;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class DatatableExceptionHandlerV2Test {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ExceptionController()).build();

    @Test
    void accessDeniedReturnsForbiddenWithoutBasicChallenge() throws Exception {
        mockMvc.perform(get("/test/datatable/access-denied"))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
            .andExpect(jsonPath("$.error").value("Access Denied"));
    }

    @Test
    void otherExceptionsRemainOk() throws Exception {
        mockMvc.perform(get("/test/datatable/error"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.error").value("Unexpected error"));
    }

    @RestController
    static class ExceptionController extends DatatableExceptionHandlerV2 {

        @GetMapping("/test/datatable/access-denied")
        void accessDenied() {
            throw new AuthorizationDeniedException("Access Denied");
        }

        @GetMapping("/test/datatable/error")
        void error() {
            throw new IllegalStateException("Unexpected error");
        }
    }
}
