package sk.iway.iwcm;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import freemarker.ext.servlet.FreemarkerServlet;
import freemarker.template.Configuration;
import freemarker.template.TemplateModelException;
import sk.iway.iwcm.users.UsersDB;

/**
 * Date: 12.02.2018
 * Time: 7:45
 * Project: webjet8
 * Company: InterWay a. s. (www.interway.sk)
 * Copyright: InterWay a. s. (c) 2001-2018
 *
 * @author mpijak
 */
public class InitFreemarkerServlet extends FreemarkerServlet {

    private static final long serialVersionUID = 1L;

    /* Vo freemarker templatach .ftl netreba deklarovat PageParams ani lang
    * ${pageParams.getValue("nazovParametra", "default")
    * ${lng}
    *
    * Ostatne global parametre sa daju definovat v metodach createConfiguration a preprocessRequest
    */
    @Override
    protected Configuration createConfiguration() {
        Configuration config = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        config.setDefaultEncoding("windows-1250");
        config.setEncoding(Locale.getDefault(), "windows-1250");
//        config.setSetting();

        try {
            config.setSharedVariable("Tools", new Tools());
//            config.setSharedVariable("iwcm", t.get("/WEB-INF/iwcm.tld");
        } catch (TemplateModelException e) {
            sk.iway.iwcm.Logger.error(e);
        }
        return config;
    }

    /* Tu definovat globalne premenne ktore chceme ziskat pri spracovani requestu */
    @Override
    protected boolean preprocessRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Configuration config = this.getConfiguration();
        //String queryStringParams = this.getQueryStringParams(request);
        String queryString = (String) request.getAttribute("path_filter_query_string");

        try {
            config.setSharedVariable("WebjetHelpers", new FreemarkerHelpers(request));
            config.setSharedVariable("pageParams", new PageParams(request));
            config.setSharedVariable("queryString", queryString);
            config.setSharedVariable("lng", PageLng.getUserLng(request));
            config.setSharedVariable("origPath", PathFilter.getOrigPath(request));
            config.setSharedVariable("currentUser", UsersDB.getCurrentUser(request));
        } catch (TemplateModelException e) {
            sk.iway.iwcm.Logger.error(e);
        }
        return false;
    }

    /*private String getQueryStringParams(HttpServletRequest request) {
        StringBuilder queryStringParams = new StringBuilder();
        Map<String, String[]> params = request.getParameterMap();

        Iterator<Map.Entry<String, String[]>> it = params.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String[]> entry = it.next();
            String queryParamName = entry.getKey();
            String[] queryParamValues = entry.getValue();
            for (int i = 0; i < queryParamValues.length; i++) {
                String qpv = queryParamValues[i];
                queryStringParams
                        .append(queryParamName)
                        .append("=")
                        .append(qpv);
                if (i == 0) {
                    break;
                    // TODO: vyriesit multiple parameter value pre nazov parametra?
                }
                if (i+1 < queryParamValues.length) {
                    queryStringParams.append("&");
                }
            }
            if (it.hasNext()) {
                queryStringParams.append("&");
            }
        }
        return queryStringParams.toString();
    }*/
}