<%@ page import="sk.iway.iwcm.forum.ForumDB" %>
<%@ page import="sk.iway.iwcm.Tools" %>
<%@ page import="sk.iway.iwcm.users.UsersDB" %>
<%@ page import="sk.iway.iwcm.Logger" %>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" %>
<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="menuGDPRDelete"/>
<%

//mazanie diskusie
if("delete".equals(Tools.getRequestParameter(request, "act")) && Tools.getRequestParameter(request, "forumId")!= null && Tools.getRequestParameter(request, "forumDocId")!= null)
{
    ForumDB.deleteMessage(Tools.getIntValue(Tools.getRequestParameter(request, "forumId"),0),
            Tools.getIntValue(Tools.getRequestParameter(request, "forumDocId"),0), UsersDB.getCurrentUser(request));
}
%>ok
