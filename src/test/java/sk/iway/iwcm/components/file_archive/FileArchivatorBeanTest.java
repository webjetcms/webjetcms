package sk.iway.iwcm.components.file_archive;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.file_archiv.FileArchivatorBean;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
