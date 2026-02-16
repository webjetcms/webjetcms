package sk.iway.iwcm.components.gdpr.model;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

public interface GdprDB {
    public List<GdprResult> search(List<GdprRegExpBean> regexps, HttpServletRequest request);
}
