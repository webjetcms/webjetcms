package sk.iway.iwcm;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

/**
 * Date: 14.02.2018
 * Time: 11:54
 * Project: webjet8
 * Company: InterWay a. s. (www.interway.sk)
 * Copyright: InterWay a. s. (c) 2001-2018
 *
 * @author mpijak
 */
public class FreemarkerHelpers {
    HttpServletRequest request;

    public FreemarkerHelpers(HttpServletRequest request) {
        this.request = request;
    }

    public String getPaginatedUrlWithinOtherParameters(int nextPage) {
        String queryString = (String) request.getAttribute("path_filter_query_string");
        if (queryString == null) {
            queryString = "";
        }
        List<NameValuePair> params = URLEncodedUtils.parse(queryString, StandardCharsets.UTF_8);
        BasicNameValuePair currentPage = new BasicNameValuePair("page", Integer.toString(nextPage));

        params.removeIf(nameValuePair -> nameValuePair.getName().equals("page"));
        params.add(0, currentPage);

        return URLEncodedUtils.format(params, StandardCharsets.UTF_8);
    }

    public String requestSetAttribute(String s, String o) {
        if (request != null) {
            request.setAttribute(s, o);
        }
        return "";
    }
}
