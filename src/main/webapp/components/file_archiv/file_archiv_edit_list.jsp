<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*, java.util.*" %>
<%@ page import="sk.iway.iwcm.users.UsersDB" %>
<%@ page import="sk.iway.iwcm.i18n.Prop" %>
<%@ page import="sk.iway.iwcm.components.file_archiv.FileArchivatorDB" %>
<%@ page import="sk.iway.iwcm.components.file_archiv.FileArchivatorKit" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_file_archiv"/>
<%
    String type = Tools.getRequestParameter(request, "type");

    request.setAttribute("dialogTitleKey", "components.file_archiv.edit_property_list_title."+type);
    request.setAttribute("dialogDescKey", "components.file_archiv.edit_property_list_desc."+type);

    Prop prop = Prop.getInstance(request);
    Identity user = UsersDB.getCurrentUser(session);
    if(user.isDisabledItem("cmp_fileArchiv_edit_del_rollback"))
    {
        //@Deprecated nechavame len kvoli historii
        if(Tools.isEmpty(Constants.getString("fileArchivCanEditUsers")) ||
                Tools.containsOneItem(Tools.getTokens(Constants.getString("fileArchivCanEditUsers"), ","), user.getLogin()) == false)
        {
            out.println(prop.getText("components.file_archiv.access"));
            return;
        }
    }

    if(request.getParameter("save") != null)
    {
        int numberOfItems = Tools.getIntValue(request.getParameter("numberOfItems"), 0);
        if(numberOfItems > 0)
        {
        	boolean atLeastOneChanged = false;
            for(int num=0; num<numberOfItems; num++)
            {
                String original = Tools.getParameter(request, "propertyItemOriginal"+num);
                String newValue = Tools.getParameter(request, "propertyItem"+num);
                if(Tools.isNotEmpty(newValue) && original.equals(newValue.trim()) == false)
                {
                    DB.execute("UPDATE file_archiv SET " + type + " = ? WHERE " + type + " = ?", newValue, original);
                    atLeastOneChanged = true;
                }
            }
            if(atLeastOneChanged)
            {
                FileArchivatorKit.deleteFileArchiveCache();
                %>
                <script type="text/javascript">
                    window.opener.location.reload();
                    window.close();
                </script>
                <%
            }
        }
    }

    List<String> propertyList = new ArrayList<>();
    if(type != null)
    {
        propertyList = FileArchivatorDB.getDistinctListByProperty(type);
        request.setAttribute("propertyList", propertyList);
    }
%>
<%@ include file="/admin/layout_top_dialog.jsp" %>
<script type="text/javascript">
function Ok()
{
    if(confirm("<%=prop.getText("components.file_archiv.edit_property_list_save_warning", prop.getText("components.file_archiv.edit_property_list."+type))%>"))
        $("#propertyListFormId").submit();
}
</script>
<div class="padding10">
<form name="propertyListForm" id="propertyListFormId" action="<%=PathFilter.getOrigPath(request)%>">
<table class="sort_table" border="1">
<%
int i=0;
for(String item : propertyList)
{
	if(Tools.isNotEmpty(item))
    {
        %><tr><td><input type="hidden" name="propertyItemOriginal<%=i%>" value="<%=item%>"/>
        <input type="text" name="propertyItem<%=i++%>" value="<%=item%>" size="80"/></td></tr><%
    }
}
%>
</table>
<input type="hidden" name="type" value="<%=type%>"/>
<input type="hidden" name="numberOfItems" value="<%=i%>"/>
<input type="hidden" name="save" value="true" />
</form>
</div>
<%@ include file="/admin/layout_bottom_dialog.jsp" %>