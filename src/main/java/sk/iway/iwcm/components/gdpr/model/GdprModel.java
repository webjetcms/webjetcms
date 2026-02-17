package sk.iway.iwcm.components.gdpr.model;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

public interface GdprModel {
    public int getId();
    public String getLink();
    public String getName();
    public String getText(List<GdprRegExpBean> regexps);
    public String getLinkView(HttpServletRequest request);
    public String getLinkDelete(HttpServletRequest request);
}
