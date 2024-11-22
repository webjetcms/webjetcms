package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.test.BaseWebjetTest;

class ToolsTest extends BaseWebjetTest {

    @Test
    void testIsEmail()
    {
        // Valid email addresses
        assertTrue(Tools.isEmail("test@example.com"));
        assertTrue(Tools.isEmail("john.doe@example.co.uk"));
        assertTrue(Tools.isEmail("jane_doe123@example-domain.com"));
        assertTrue(Tools.isEmail("jane_doe123@example-domain.com"));

        assertTrue(Tools.isEmail("a@azet.sk"));
        assertTrue(Tools.isEmail("0@azet.sk"));
        assertTrue(Tools.isEmail("oksi11@i.ua"));
        assertTrue(Tools.isEmail("mail@sl.posta"));
        assertTrue(Tools.isEmail("mail@sl.posta.sk"));

        //Yes, a domain name can start with a number.
        assertTrue(Tools.isEmail("0@00centrum.sk"));
        assertTrue(Tools.isEmail("0@00.com"));
        assertTrue(Tools.isEmail("0@0.com"));

        // Invalid email addresses
        assertFalse(Tools.isEmail("Marek&@gmail.com"));
        assertFalse(Tools.isEmail("peta.miusová@gmail.com"));
        assertFalse(Tools.isEmail("_slavo_niekto@centrum.sk"));
        assertFalse(Tools.isEmail("hasek@domena,cz"));
    }
}