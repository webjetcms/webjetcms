package sk.iway.iwcm.system.spring;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface WebjetComponentParserInterface {
    void run(HttpServletRequest request, HttpServletResponse response);
    String parse(HttpServletRequest request, HttpServletResponse response, String html);
    boolean isRedirected(HttpServletResponse response);
    String getRedirectLocation(HttpServletResponse response);
}
