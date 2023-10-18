<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*, java.util.*, sk.iway.iwcm.components.file_archiv.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%=Tools.insertJQuery(request)%>
<%
try
{
	PageParams pageParams = new PageParams(request);
	List<FileArchivatorBean> fabListCahce = FileArchivatorDB.getByConditions(null, FileArchivatorDB.createCollection("product", request),
			FileArchivatorDB.createCollection("category", request), FileArchivatorDB.createCollection("productCode", request), pageParams.getValue("dir", FileArchivatorKit.getArchivPath()),
	      pageParams.getBooleanValue("subDirsInclude", false),pageParams.getBooleanValue("asc", false),true,true);
	%>
	<ul class="download">
		<%
		List<FileArchivatorBean> fabList = new ArrayList<>(fabListCahce);
		if(fabList.size() > 0)
		{
			for(FileArchivatorBean fab: fabList)
			{
				if(fab.getReferenceId() == -1)
				{
					%>
					<li>
						<a target="blank" href="<%=Tools.getBaseHref(request)+"/"+fab.getFilePath()+fab.getFileName() %>">
							<%=fab.getVirtualFileName() %>
						</a>
					</li>
					<%
				}
			}
		}
		%>
	</ul>
	<%
}
catch(Exception ex)
{
	sk.iway.iwcm.Logger.error(ex);
}%>