package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLSyntaxErrorException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;

import jakarta.persistence.RollbackException;
import sk.iway.iwcm.i18n.Prop;
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

    @ParameterizedTest
    @ValueSource(strings = {
            "(conn=3895) Column 'price_for_hour' cannot be null",
            "Column \"price_for_hour\" cannot be null",
            "column `price_for_hour` cannot be null"
    })
    void handleTransactionSystemExceptionReturnsFriendlyMessageForRequiredValue(String databaseMessage) {
        SQLIntegrityConstraintViolationException databaseException = new SQLIntegrityConstraintViolationException(
                databaseMessage, "23000", 1048);
        RollbackException rollbackException = new RollbackException((String) null, databaseException);
        TransactionSystemException exception = new TransactionSystemException(
                "Could not commit JPA transaction", rollbackException);

        ResponseEntity<DatatableResponse<Object>> response =
                new DatatableExceptionHandlerV2().handleException(exception);

        assertNotNull(response.getBody());
        assertEquals(
                Prop.getInstance().getText("datatable.error.valueRequired", "price_for_hour"),
                response.getBody().getError());
    }

    @Test
    void handleTransactionSystemExceptionExtractsRequiredValueFromRollbackMessage() {
        String rollbackMessage = "Exception [EclipseLink-4002] (Eclipse Persistence Services): " +
                "org.eclipse.persistence.exceptions.DatabaseException\n" +
                "Internal Exception: java.sql.SQLIntegrityConstraintViolationException: " +
                "(conn=3895) Column 'price_for_hour' cannot be null\nError Code: 1048\n" +
                "Call: UPDATE reservation_object SET price_for_hour = ? WHERE (reservation_object_id = ?)\n" +
                "bind => [null, 5303]";
        TransactionSystemException exception = new TransactionSystemException(
                "Could not commit JPA transaction", new RollbackException(rollbackMessage));

        ResponseEntity<DatatableResponse<Object>> response =
                new DatatableExceptionHandlerV2().handleException(exception);

        assertNotNull(response.getBody());
        assertEquals(
                Prop.getInstance().getText("datatable.error.valueRequired", "price_for_hour"),
                response.getBody().getError());
    }

    @Test
    void handleTransactionSystemExceptionPreservesMessageForUnknownDatabaseError() {
        TransactionSystemException exception = new TransactionSystemException(
                "Could not commit JPA transaction", new RollbackException("Unknown database error"));

        ResponseEntity<DatatableResponse<Object>> response =
                new DatatableExceptionHandlerV2().handleException(exception);

        assertNotNull(response.getBody());
        assertEquals("Could not commit JPA transaction", response.getBody().getError());
    }
}
