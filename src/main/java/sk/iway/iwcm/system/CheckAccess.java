package sk.iway.iwcm.system;

import jakarta.servlet.http.HttpServletRequest;

public interface CheckAccess {
    boolean checkAccessAllowed(HttpServletRequest request);
}
