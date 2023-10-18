package sk.iway.iwcm.components.gdpr.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.components.gdpr.GdprCron;
import sk.iway.iwcm.test.BaseWebjetTest;

public class GdprCronTest extends BaseWebjetTest {

    @BeforeAll
    public static void initJpa()
    {
        /*
        Constants.setServletContext(new MockServletContext("/") {
            @Override
            public String getRealPath(String string)
            {
                return "C:/Development/workspace/webjet8/WebContent"+string;
            }
        });
        */
        Constants.setString("jpaAddPackages", "sk.iway.iwcm.components.gdpr.model");
        DBPool.getInstance();
        DBPool.jpaInitialize();
        Constants.setBoolean("enableJspJarPackaging", true);
    }

    @Test
    public void testMain() {

        //hladaj rodne cislo
        String[] args = new String[]{"emails=tester@balat.sk", "regexps=[0-9]{6}/[0-9]{3,4}", "modules=WEB_STRANKY,FORMULARE"};
        GdprCron.main(args);
    }
}
