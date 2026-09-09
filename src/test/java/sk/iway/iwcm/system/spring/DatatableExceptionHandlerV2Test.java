package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.SQLSyntaxErrorException;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;

import jakarta.persistence.RollbackException;
import sk.iway.iwcm.system.datatable.DatatableResponse;
import sk.iway.iwcm.test.BaseWebjetTest;

class DatatableExceptionHandlerV2Test extends BaseWebjetTest {

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

    @Test
    void handleTransactionSystemExceptionReturnsFriendlyMessageForValueTooLong() {
        SQLSyntaxErrorException databaseException = new SQLSyntaxErrorException(
                "(conn=2211) Data too long for column 'field_i' at row 1", "22001", 1406);
        RollbackException rollbackException = new RollbackException("Error while committing the transaction", databaseException);
        TransactionSystemException exception = new TransactionSystemException(
                "Could not commit JPA transaction", rollbackException);

        ResponseEntity<DatatableResponse<Object>> response =
                new DatatableExceptionHandlerV2().handleException(exception);

        assertNotNull(response.getBody());
        assertEquals(
                "Obsah poľa „field_i“ prekračuje maximálnu povolenú dĺžku. Skráťte ho a skúste záznam uložiť znova.",
                response.getBody().getError());
    }
}
