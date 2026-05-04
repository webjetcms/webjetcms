package sk.iway.iwcm.common;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.GroupsDB;

public class BlogTools {
    /**
     * Vrati true, ak zadane groupId je pre bloggera editovatelne a zaroven sa jedna skutocne o bloggera (ma povoleny modul Blog)
     * @param groupId
     * @param blogger
     * @return
     */
    public static boolean isEditable(int groupId, Identity blogger)
    {
        if (blogger == null || Tools.isEmpty(blogger.getEditableGroups()) || (blogger.isDisabledItem("cmp_blog") && blogger.isDisabledItem("cmp_wiki") && blogger.isDisabledItem("addPage"))) return false;

        if (GroupsDB.isGroupEditable(blogger, groupId)) return true;

        return false;
    }
}
