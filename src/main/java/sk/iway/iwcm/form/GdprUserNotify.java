package sk.iway.iwcm.form;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.AdminlogBean;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

public class GdprUserNotify {

    //private Prop prop ;
    private int currentUserId = -1;
    //private String gdprApproved = "gdprApproved";
    String description;
    int cacheInMinutes = 300;
    HttpServletRequest request;

    //private boolean approvedInCache = false;

    public GdprUserNotify(HttpServletRequest request) {
        //this.prop =  Prop.getInstance(request);
        UserDetails ud = UsersDB.getCurrentUser(request);
        if(ud != null)
            currentUserId = ud.getUserId();

        description = "Zobrazeny formular (" + Tools.getParameter(request, "formName")+")";
    }

    public static int[] getAdminlogType()
    {
        int[] logTypes = new int[1];
        logTypes[0] = Adminlog.TYPE_FORM_VIEW;
        return logTypes;
    }

    public  boolean addNotify()
    {
        Adminlog.add(Adminlog.TYPE_FORM_VIEW, currentUserId, description, -1, -1);
        return true;
    }

    public boolean hasUserGdprApproved()
    {
        List<AdminlogBean> adminLogResults = Adminlog.searchAdminlog(getAdminlogType(), -1, -1, currentUserId, -1, -1, description, -1, -1, null, null);
        if(adminLogResults.size() == 0)
        {
            // potrebujeme si preposlat aj parametre
//            Enumeration e = request.getParameterNames();
//            String nameParam ="", urlParams="?";
//            while(e.hasMoreElements())
//            {
//                nameParam = (String)e.nextElement();
//                if(!nameParam.equals("docid"))
//                {
//                    urlParams += nameParam+"="+request.getParameter(nameParam)+"&";
//                }
//            }
            /*
            <script type="text/javascript">
            if(confirm('<%=prop.getText("components.forms.alert.gdpr")%>')) {
                window.location.href = "<%=PathFilter.getOrigPath(request)+urlParams+gdprApproved%>=true";
            }
            else
                window.location.href="/admin/welcome.jsp";
            </script>
               */
            return false;//Pokym nemame suhlas, nic nezobrazime
        }
        return true;
    }

    /*private  String getCacheObjectName()
    {
        return "Adminlog.TYPE_FORM_VIEW.user."+currentUserId+".description."+description;
    }*/
}
