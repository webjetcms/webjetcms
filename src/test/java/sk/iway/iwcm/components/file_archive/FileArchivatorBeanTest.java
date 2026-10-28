package sk.iway.iwcm.components.file_archive;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.file_archiv.FileArchivatorBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.util.Date;

public class FileArchivatorBeanTest {

    @Test
    public void testDateValidation() {
        FileArchivatorBean entity = new FileArchivatorBean();
        Long now = Tools.getNow();

        // both null - true
        assertEquals(true, entity.isValidDates());

        // From minut back now - true
        entity.setValidFrom(new Date(now - 1000));
        assertEquals(true, entity.isValidDates());

        // From minut after now - false
        entity.setValidFrom(new Date(now + 1000));
        assertEquals(false, entity.isValidDates());

        // To minut before now -  false
        entity.setValidFrom(null);
        entity.setValidTo(new Date(now - 1000));
        assertEquals(false, entity.isValidDates());

        // To minut after now -  true
        entity.setValidTo(new Date(now + 1000));
        assertEquals(true, entity.isValidDates());

        // Range From valid, To valid - true
        entity.setValidFrom(new Date(now - 1000));
        entity.setValidTo(new Date(now + 1000));
        assertEquals(true, entity.isValidDates());

        // Range From invalid, To valid - false
        entity.setValidFrom(new Date(now + 1000));
        assertEquals(false, entity.isValidDates());

        // Range From valid, To invalid - false
        entity.setValidFrom(new Date(now - 1000));
        entity.setValidTo(new Date(now - 1000));
        assertEquals(false, entity.isValidDates());
    }

    /** Only uploaded historical versions can bypass validity checks when configured. */
    @Test
    public void testHistoryValidityConfiguration() {
        FileArchivatorBean entity = new FileArchivatorBean();
        entity.setReferenceId(10);
        entity.setValidTo(new Date(0));

        try (var constants = mockStatic(Constants.class)) {
            constants.when(() -> Constants.getBoolean("fileArchivCheckHistoryValid")).thenReturn(true);
            assertFalse(entity.isValidDates());

            constants.when(() -> Constants.getBoolean("fileArchivCheckHistoryValid")).thenReturn(false);
            assertTrue(entity.isValidDates());

            entity.setReferenceId(-1);
            assertFalse(entity.isValidDates());

            entity.setReferenceId(10);
            entity.setUploaded(0);
            assertFalse(entity.isValidDates());
        }
    }
}
