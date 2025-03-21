package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.test.BaseWebjetTest;

public class ConstantsTest extends BaseWebjetTest {

    @Test
    void testLoad() {
        loadConstantsFromDB();
        assertEquals("aceintegration", Constants.getInstallName());
        assertEquals("aceintegration", Constants.getString("installName"));
    }

}
