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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;

import jakarta.persistence.RollbackException;
import sk.iway.iwcm.system.datatable.DatatableResponse;

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

    @Test
    void handleTransactionSystemExceptionExtractsDuplicateEntryFromRollbackException() {
        String rollbackMessage = "jakarta.persistence.RollbackException: Exception [EclipseLink-4002] " +
                "(Eclipse Persistence Services): org.eclipse.persistence.exceptions.DatabaseException " +
                "Internal Exception: java.sql.SQLIntegrityConstraintViolationException: (conn=134330) " +
                "Duplicate entry '/files/protected/dir-edit-form-test' for key 'dir_url' Error Code: 1062";
        RollbackException rollbackException = new RollbackException(rollbackMessage);
        TransactionSystemException exception = new TransactionSystemException(
                "Could not commit JPA transaction", rollbackException);

        ResponseEntity<DatatableResponse<Object>> response =
                new DatatableExceptionHandlerV2().handleException(exception);

        assertNotNull(response.getBody());
        assertEquals(
                "Duplicate entry &#39;/files/protected/dir-edit-form-test&#39; for key &#39;dir_url&#39;",
                response.getBody().getError());
    }
}
