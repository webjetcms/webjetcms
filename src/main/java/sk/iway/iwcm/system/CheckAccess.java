package sk.iway.iwcm.system;

import javax.servlet.http.HttpServletRequest;

public interface CheckAccess {
    boolean checkAccessAllowed(HttpServletRequest request);
}
