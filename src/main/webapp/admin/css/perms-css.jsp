<%@ page contentType="text/css" import="sk.iway.iwcm.*,java.util.*,sk.iway.iwcm.system.*" %>
<%
response.setHeader("Pragma","No-Cache");
response.setDateHeader("Expires",0);
response.setHeader("Cache-Control","no-Cache");

Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
if (user!=null && user.getDisabledItemsTable()!=null && user.isAdmin())
{
	try
	{
		Iterator<String> e = user.getDisabledItemsTable().keySet().iterator();
		String name;
		while (e.hasNext())
		{
			name = e.next();
			name = Tools.replace(name, ".", "_");
			out.println(".noperms-"+name+" { display: none !important; }");
		}
		if ("B".equals(Constants.getString("wjVersion")))
		{
			out.println(".noperms-ver-bas { display: none !important; }");
		}
		else if ("P".equals(Constants.getString("wjVersion")))
		{
			out.println(".noperms-ver-pro { display: none !important; }");
		}
	}
	catch (Exception ex)
	{
		sk.iway.iwcm.Logger.error(ex);
	}
}

%>