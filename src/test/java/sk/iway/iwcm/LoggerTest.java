package sk.iway.iwcm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import sk.iway.iwcm.system.ConstantsV9;
import sk.iway.iwcm.test.BaseWebjetTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoggerTest extends BaseWebjetTest {

    @BeforeEach
    public void setUp() {
        Constants.clearValues();
        ConstantsV9.clearValuesWebJet9();
    }

    private void testCommonClassNames() {
        assertTrue(Logger.looksLikeClassName("sk.iway.iwcm.Logger"));
        assertTrue(Logger.looksLikeClassName("org.slf4j.Logger"));
        assertTrue(Logger.looksLikeClassName("net.example.Logger"));
        assertTrue(Logger.looksLikeClassName("com.example.Logger"));
        assertTrue(Logger.looksLikeClassName("cz.webactive.TestClass"));
        assertTrue(Logger.looksLikeClassName("sk.company.TestClass"));
        assertFalse(Logger.looksLikeClassName("unknown.Logger"));
    }

    @Test
    @Order(1)
    public void testLooksLikeClassName_withKnownPrefixes() {
        testCommonClassNames();
        assertFalse(Logger.looksLikeClassName("io.company.TestClass"));
        assertFalse(Logger.looksLikeClassName("edu.test.Logger"));

        assertFalse(Logger.getLogger("io.company.TestClass").isDebugEnabled());
    }

    @Test
    @Order(2)
    public void testLooksLikeClassName_withLogWebjetPackages() {
        Constants.setString("logWebjetPackages", "sk.iway,cz.webactive,io.company");
        testCommonClassNames();
        assertTrue(Logger.looksLikeClassName("io.company.TestClass"));
        assertFalse(Logger.looksLikeClassName("edu.test.Logger"));
    }

    @Test
    @Order(3)
    public void testLooksLikeClassName_withLogLevels() {
        Constants.setString("logLevels", "sk.iway.iwcm.io=INFO\nio.company=WARN\nedu.test=DEBUG");

        Logger.setWJLogLevels(Logger.getLogLevelsMap(Constants.getString("logLevels")));

        testCommonClassNames();
        assertTrue(Logger.looksLikeClassName("io.company.Logger"));
        assertTrue(Logger.looksLikeClassName("edu.test.Logger"));
        assertFalse(Logger.getLogger("io.company.TestClass").isDebugEnabled());
        assertTrue(Logger.getLogger("io.company.TestClass").isWarnEnabled());

        assertTrue(Logger.getLogger("edu.test.TestClass").isDebugEnabled());
    }
}
