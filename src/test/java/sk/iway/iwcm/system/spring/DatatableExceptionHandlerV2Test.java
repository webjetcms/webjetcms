package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;

import jakarta.persistence.RollbackException;
import sk.iway.iwcm.system.datatable.DatatableResponse;

class DatatableExceptionHandlerV2Test {

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
