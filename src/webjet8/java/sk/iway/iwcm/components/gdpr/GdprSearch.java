package sk.iway.iwcm.components.gdpr;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.gdpr.model.GdprRegExpBean;
import sk.iway.iwcm.components.gdpr.model.GdprRegExpDB;
import sk.iway.iwcm.components.gdpr.model.GdprResults;

public class GdprSearch {
    List<GdprRegExpBean> regexps;
    List<GdprModule> modules;

    public GdprSearch(List<GdprRegExpBean> regexps, List<GdprModule> modules) {
        this.regexps = regexps;
        this.modules = modules;
    }

    public GdprResults search(HttpServletRequest request) {
        if(request!=null && request.getParameter("quickSearch") != null && Tools.isNotEmpty(request.getParameter("quickSearch")) )
        {
            regexps = new ArrayList<GdprRegExpBean>();
            GdprRegExpBean gdprBean = new GdprRegExpBean();
            gdprBean.setRegexpValue(Tools.getParameter(request,"quickSearch"));
            gdprBean.setRegexpName(Tools.getParameter(request,"quickSearch"));
            regexps.add(gdprBean);
        }
        else
        {
            if (regexps==null || regexps.size()<1) regexps = GdprRegExpDB.getInstance().getAll(); //getAllRegexpString();
        }

        if (regexps == null || regexps.isEmpty()) {
            throw new IllegalArgumentException("Regexps cannot be null or empty");
        }

        if (modules == null || modules.isEmpty()) {
            throw new IllegalArgumentException("Modules cannot be null or empty");
        }

        GdprResults results = new GdprResults();
        for (GdprModule module : modules) {
            results.put(module, module.getDB().search(regexps, request));
        }

        return results;
    }
}
