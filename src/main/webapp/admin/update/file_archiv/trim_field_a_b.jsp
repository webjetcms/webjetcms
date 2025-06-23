<%
    sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="sk.iway.iwcm.components.file_archiv.FileArchivatorDB" %>
<%@ page import="java.util.List" %>
<%@ page import="sk.iway.iwcm.components.file_archiv.FileArchivatorBean" %>
<%@ page import="sk.iway.iwcm.i18n.Prop" %>
<%@ page import="sk.iway.iwcm.users.UsersDB" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

    /**
     * Nie je potrebne spustat ani pri novej instalacii, bolo to potrebne pre preklopenie funkcneho mfsr na novu stromovu strukturu.
     */
    Identity user =  UsersDB.getCurrentUser(request);
    Prop prop = Prop.getInstance(request);
    if(user == null || !user.isAdmin())
    {
        out.print(prop.getText("components.chat.error.notloggedin"));
        return;
    }


    List<FileArchivatorBean> fabList = FileArchivatorDB.getInstance().getAll();

    int count = 0;
    boolean isEdit = false;
    for(FileArchivatorBean fab:fabList)
    {
        isEdit = false;
        if(Tools.isNotEmpty(fab.getFieldB()) && fab.getFieldB().endsWith(" "))
        {
            fab.setFieldB(fab.getFieldB().trim());
            isEdit = true;
            out.print("<br>"+fab.getId()+" pred:'"+fab.getFieldB()+"'po:'"+fab.getFieldB().trim()+"'");
            count++;
        }

        if(Tools.isNotEmpty(fab.getFieldA()) && fab.getFieldA().endsWith(" "))
        {
            fab.setFieldA(fab.getFieldA().trim());
            isEdit = true;
            out.print("<br>"+fab.getId()+" pred:'"+fab.getFieldA()+"' po:'"+fab.getFieldA().trim()+"'");
            count++;
        }

        if(isEdit)
        {
            fab.save();
        }
    }

    out.print("<br><br><br>pocet: "+count);
%>