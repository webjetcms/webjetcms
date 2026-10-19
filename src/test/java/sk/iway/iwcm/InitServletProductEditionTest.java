package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

/** Verifies that separate product edition labels preserve the existing diagnostic version format. */
class InitServletProductEditionTest {

    /** Covers every existing edition and installation-specific label in both public version accessors. */
    @ParameterizedTest
    @CsvSource({
        "E, zaskis, Unlimited", "E, zsr, Unlimited", "E, example, Enterprise",
        "P, example, Professional", "D, example, MSG", "I, example, NET", "C, example, Cestovka",
        "M, cloud, Cloud", "M, example, MultiWeb", "O, example, Community (Open Source)", "X, example, Basic"
    })
    void preservesEditionAndDiagnosticVersion(String editionCode, String installation, String expected) {
        try (MockedStatic<Constants> constants = mockStatic(Constants.class)) {
            constants.when(() -> Constants.getString("wjVersion")).thenReturn(editionCode);
            constants.when(Constants::getInstallName).thenReturn(installation);
            constants.when(() -> Constants.getString("clusterMyNodeName")).thenReturn("test-node");
            constants.when(() -> Constants.getString("clusterMyNodeType")).thenReturn("full");
            assertEquals(expected, InitServlet.getProductEdition());
            String version = InitServlet.getActualVersion() + " " + expected + " (test-node / full)";
            String tomcat = System.getProperty("iway.tomcat");
            if (Tools.isNotEmpty(tomcat)) version += " server: " + tomcat;
            assertEquals(version, InitServlet.getActualVersionLong());
        }
    }
}
