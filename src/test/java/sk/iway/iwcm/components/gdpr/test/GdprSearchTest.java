package sk.iway.iwcm.components.gdpr.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.components.gdpr.GdprModule;
import sk.iway.iwcm.components.gdpr.GdprSearch;
import sk.iway.iwcm.components.gdpr.model.GdprRegExpBean;
import sk.iway.iwcm.test.BaseWebjetTest;

public class GdprSearchTest extends BaseWebjetTest {

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
    }

    @Test
    public void testSearch() {
        List<GdprRegExpBean> regexps = new ArrayList<>();
        regexps.add(new GdprRegExpBean("^[H]"));
        regexps.add(new GdprRegExpBean("^[D]"));
        List<GdprModule> modules = new ArrayList<>();
        modules.add(GdprModule.WEB_STRANKY);

        GdprSearch gdprSearch = new GdprSearch(regexps, modules);
        //List<ActiveRecord> ars = gdprSearch.search();
        gdprSearch.search(null);
    }
}
