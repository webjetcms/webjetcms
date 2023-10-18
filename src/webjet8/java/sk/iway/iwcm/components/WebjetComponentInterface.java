package sk.iway.iwcm.components;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interface pre prácu s WebjetKomponentami
 */
public interface WebjetComponentInterface {
    void init();
    void init(HttpServletRequest request, HttpServletResponse response);
    String getViewFolder();
}
