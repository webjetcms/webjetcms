package sk.iway.iwcm.common;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;

public class BasketDBTools {

    /**
     * Funkcia vrati z requestu zobrazovanu menu, ak sa v requeste nenachadza, vrati default z Constants.getString("basketDisplayCurrency")
     * v pripade cloudu kontroluje nastavenia root grupy fieldC az potom berie basketDisplayCurrency
     * @param request
     * @return
     */
    public static String getDisplayCurrency(HttpServletRequest request)
    {
        String curr = "";
        if("cloud".equals(Constants.getInstallName()))
        {
            int rootGroupId = CloudToolsForCore.getRootGroupId(request);
            GroupDetails rootGroup = GroupsDB.getInstance().getGroup(rootGroupId);

            if (rootGroup != null && Tools.isNotEmpty(rootGroup.getFieldC()))
            {
                curr = CloudToolsForCore.getValue(rootGroup.getFieldC(), "curr");
            }
            if (Tools.isEmpty(curr)) curr = Constants.getString("basketDisplayCurrency");
        }
        else
        {

            String reqCurr = (String)request.getAttribute("displayCurrency");
            if (Tools.isNotEmpty(reqCurr))
                return reqCurr;

            curr = Constants.getString("basketDisplayCurrency");
        }
        return curr;
    }

}
