package sk.iway.iwcm.system.spring;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface WebjetComponentParserInterface {
    void run(HttpServletRequest request, HttpServletResponse response);
    String parse(HttpServletRequest request, HttpServletResponse response, String html);
    boolean isRedirected(HttpServletResponse response);
    String getRedirectLocation(HttpServletResponse response);
}
