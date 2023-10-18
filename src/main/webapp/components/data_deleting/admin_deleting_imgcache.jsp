<%@page import="java.io.PrintWriter"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%@page import="sk.iway.iwcm.editor.ThumbServlet"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%!
public void delAllFiles(IwcmFile dir, JspWriter out, Prop prop)
{
	try
	{
		if(dir != null)
		{
			for(IwcmFile subor: dir.listFiles())
			{
				if(subor.isDirectory())	delAllFiles(subor, out, prop);
				else subor.delete();
			}

			if(!dir.getVirtualPath().equals(Constants.getString("thumbServletCacheDir").endsWith("/") ? Constants.getString("thumbServletCacheDir").substring(0, Constants.getString("thumbServletCacheDir").length()-1) : Constants.getString("thumbServletCacheDir")))
			{
				out.println(prop.getText("components.data.deleting.imgcache.zmazal_adresar", Tools.escapeHtml(dir.getName()))+"<br/>");
				dir.delete();
			}
			out.flush();
		}
	}
	catch(Exception e)
	{
		sk.iway.iwcm.Logger.error(e);
	}
}
%>
<%@ include file="/admin/layout_top.jsp" %>

<div class="row title">
    <h1 class="page-title"><i class="fa icon-size-actual"></i><iwcm:text key="components.data.deleting"/><i class="fa fa-angle-right"></i><iwcm:text key="components.data.deleting.imgcache.menu"/></h1>
</div>

<script type="text/javascript">
	function confirmDelAll(dir)
	{
		if(window.confirm('<iwcm:text key="components.data.deleting.imgcache.areYouSure" param1="'+dir+'"/>') == true)
	   {
			return true;
	   }
		return false;
	}
</script>

<div class="tabbable tabbable-custom tabbable-full-width">
	<ul class="nav nav-tabs">
		<li class="active">
			<a href="#tabMenu1" data-toggle="tab">
				<iwcm:text key="components.data_deleting.delete"/>
			</a>
		</li>
	</ul>

	<div class="tab-content">
	    <div id="tabMenu1" class="tab-pane active">

			<form action="<%=PathFilter.getOrigPath(request)%>" method="get">

				<div class="col-sm-2">
					<div class="form-group">
						<input type="hidden" value="true" name="delAll" />
						<input type="submit" value="<iwcm:text key="components.data_deleting.deleteAll"/>" class="btn green" onclick="return confirmDelAll('<%=Constants.getString("thumbServletCacheDir")%>');"/>
					</div>
				</div>

			</form>

		</div>
	</div>
</div>

<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
Prop prop = Prop.getInstance(lng);
PageParams pageParams = new PageParams(request);
if("true".equals(Tools.getStringValue(Tools.getRequestParameter(request, "delAll"), "false")))
{
	String dirStr = sk.iway.iwcm.Tools.getRealPath(Constants.getString("thumbServletCacheDir"));

	IwcmFile dir = new IwcmFile(dirStr);
	out.println(prop.getText("components.data.deleting.imgcache.zacinam_mazat")+"<br/>");
	delAllFiles(dir, out, prop);
	out.println(prop.getText("components.data.deleting.imgcache.mazanie_ukoncene"));

	Adminlog.add(Adminlog.TYPE_DATA_DELETING, "Deleting imgcache, dir= " + dir.getAbsolutePath(), -1, -1);
}
%>
<%@ include file="/admin/layout_bottom.jsp" %>
