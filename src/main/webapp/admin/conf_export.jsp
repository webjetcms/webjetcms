<%@page import="java.util.List"%><% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "application/xml"); %><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.system.*,java.util.*,
java.beans.XMLEncoder,java.io.BufferedOutputStream,java.io.FileOutputStream,sk.iway.iwcm.Identity,
net.sourceforge.stripes.action.Resolution,sk.iway.iwcm.users.*"%><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@
taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %><%@
taglib uri="/WEB-INF/struts-html.tld" prefix="html" %><%@
taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><%@
taglib uri="/WEB-INF/displaytag.tld" prefix="display" %><iwcm:checkLogon admin="true" perms="menuConfig"/><%!
/**
 * Skonvertuje objekt do XML streamu
 * @param o
 * @param out
 */
private void encodeObject(Object o, ServletOutputStream out, HttpServletResponse res, HttpServletRequest req)
{
	 //res.setContentType("application/xml");
	 res.setHeader("Content-Disposition", "attachment; filename=conf_export_"+Tools.getServerName(req).replace(".", "_")+".xml");
	try
	{
		XMLEncoder encoder = new XMLEncoder(out);
		encoder.writeObject(o);
	   encoder.close();
	}
	catch(Exception e)
	{
	 	sk.iway.iwcm.Logger.error(e);
	}
}
%><%
Identity user = UsersDB.getCurrentUser(request);
if(user != null && !user.isAdmin())
{
	out.print("!!User nie je admin");
	return;
}

List conf = ConfDB.getConfig();
List<ConfDetails> confSearch = new ArrayList<ConfDetails>();

Iterator iter;
iter = conf.iterator();
ConfDetails con;

while (iter.hasNext())
{
	con = (ConfDetails)iter.next();
	//TODO: add prefix capability
	confSearch.add(con);
}
encodeObject(confSearch, response.getOutputStream(), response, request);
%>