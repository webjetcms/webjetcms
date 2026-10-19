package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLSyntaxErrorException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;

import jakarta.persistence.RollbackException;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatableResponse;
import sk.iway.iwcm.test.BaseWebjetTest;

class DatatableExceptionHandlerV2Test extends BaseWebjetTest {

    @Test
    void handleTransactionSystemExceptionReturnsFriendlyDuplicateMessageFromRollbackException() {
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
                Prop.getInstance().getText("datatable.error.duplicateValue"),
                response.getBody().getError());
    }

    @Test
    void handleDataIntegrityViolationExceptionReturnsFriendlyDuplicateMessage() {
        SQLIntegrityConstraintViolationException databaseException = new SQLIntegrityConstraintViolationException(
                "Duplicate entry 'example' for key 'name'", "23000", 1062);
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Could not execute statement", databaseException);

        ResponseEntity<DatatableResponse<Object>> response =
                new DatatableExceptionHandlerV2().handleException(exception);

        assertNotNull(response.getBody());
        assertEquals(Prop.getInstance().getText("datatable.error.duplicateValue"), response.getBody().getError());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void handleTransactionSystemExceptionReturnsFriendlyMessageForReferencedRecord(boolean nestedCause) {
        String databaseMessage = "(conn=3895) Cannot delete or update a parent row: a foreign key constraint fails " +
                "(`webjet`.`child`, CONSTRAINT `fk_parent` FOREIGN KEY (`parent_id`) REFERENCES `parent` (`id`))";
        RollbackException rollbackException = nestedCause
                ? new RollbackException((String) null,
                        new SQLIntegrityConstraintViolationException(databaseMessage, "23000", 1451))
                : new RollbackException("org.eclipse.persistence.exceptions.DatabaseException\n" + databaseMessage +
                        "\nError Code: 1451\nCall: DELETE FROM parent WHERE id = ?\nbind => [42]");
        TransactionSystemException exception = new TransactionSystemException(
                "Could not commit JPA transaction", rollbackException);

        ResponseEntity<DatatableResponse<Object>> response =
                new DatatableExceptionHandlerV2().handleException(exception);

        assertNotNull(response.getBody());
        assertEquals(Prop.getInstance().getText("datatable.error.recordInUse"), response.getBody().getError());
    }

    @Test
    void handleDataIntegrityViolationExceptionReturnsFriendlyMessageForReferencedRecord() {
        SQLIntegrityConstraintViolationException databaseException = new SQLIntegrityConstraintViolationException(
                "Cannot delete or update a parent row: a foreign key constraint fails", "23000", 1451);
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Could not execute statement", databaseException);

        ResponseEntity<DatatableResponse<Object>> response =
                new DatatableExceptionHandlerV2().handleException(exception);

        assertNotNull(response.getBody());
        assertEquals(Prop.getInstance().getText("datatable.error.recordInUse"), response.getBody().getError());
    }

    @Test
    void handleTransactionSystemExceptionDoesNotTreatMissingParentAsReferencedRecord() {
        SQLIntegrityConstraintViolationException databaseException = new SQLIntegrityConstraintViolationException(
                "Cannot add or update a child row: a foreign key constraint fails", "23000", 1452);
        TransactionSystemException exception = new TransactionSystemException(
                "Could not commit JPA transaction", new RollbackException(databaseException));

        ResponseEntity<DatatableResponse<Object>> response =
                new DatatableExceptionHandlerV2().handleException(exception);

        assertNotNull(response.getBody());
        assertEquals("Could not commit JPA transaction", response.getBody().getError());
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
