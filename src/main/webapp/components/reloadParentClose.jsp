<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" %><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<script type="text/javascript">

<% if (request.getAttribute("customScript")!=null)
	{
		out.print((String)request.getAttribute("customScript"));
   }
	else if (request.getParameter("url")!=null)
	{ %>
		window.opener.location.href="<%=sk.iway.iwcm.tags.support.ResponseUtils.filter(request.getParameter("url"))%>";
<% } else { %>

		//test na IE dialog - musime spravit reload
		var isReloaded = false;
		try
		{
			var failsafe = 1;
			var parentWindow = window.opener.parent;
			var dialogWindow = null;
			while (failsafe++<10 && dialogWindow == null)
			{
				if (parentWindow.location.href.indexOf("dialogframe_inline.jsp")!=-1) dialogWindow = parentWindow;

				parentWindow = parentWindow.parent;
			}

			dialogWindow.LoadInnerDialog();

			isReloaded = true;
		}
		catch (e) {}

		try
		{
			if (isReloaded==false) window.opener.location.reload();
		}
		catch (e) {}
<% } %>
	try { window.close(); } catch (e) {}
</script>
<iwcm:text key="components.page_update_info.save_ok"/>