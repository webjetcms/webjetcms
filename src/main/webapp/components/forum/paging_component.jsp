<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%>
<%@ page pageEncoding="utf-8" import="org.apache.struts.util.ResponseUtils"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%

Prop prop = Prop.getInstance( request.getParameter("language") );

int pageSize = 10;
if(request.getAttribute("pageSize") != null)
	pageSize = sk.iway.iwcm.Tools.getIntValue((String)request.getAttribute("pageSize"), 10);
if(request.getParameter("pageSize") != null)
	pageSize = sk.iway.iwcm.Tools.getIntValue((String)request.getParameter("pageSize"), 10);

int pageLinksNum = 5;
if(request.getAttribute("pageLinksNum") != null)
	pageLinksNum = sk.iway.iwcm.Tools.getIntValue(request.getParameter("pageLinksNum"), 5);

java.util.List pagingList = null;
java.util.Map pagingParams = null;
String paramsStr = "";
String pageLink = PathFilter.getOrigPath(request); // "/showdoc.do";

String offset = "0";
String end = ""+(pageSize);
int bazarOffset = 0;
int next = pageSize;
int back = -1;
int totalResults = 0;
int totalPages = 0;
int pageFrom = 0;
int pageTo = 0;
int actualPageNum = sk.iway.iwcm.Tools.getIntValue(request.getParameter("pageNum"), 1);
int intOffset = 0;
boolean fromAdmin = request.getAttribute("fromAdmin") == null ? false : (Boolean)request.getAttribute("fromAdmin");



int interval;
int length;
int pos;
int startIndex;
int endIndex;
String outStr;
int sOd;
int sDo;
int jumpIndex;

try
{
	if(request.getAttribute("pagingList") != null);
		pagingList = (java.util.ArrayList) request.getAttribute("pagingList");

	if(request.getAttribute("pagingLink") != null)
		pageLink = request.getAttribute("pagingLink").toString();

	if(request.getParameterMap() != null)
	{
		java.util.Map reqParams = (java.util.Map) request.getParameterMap();
		java.util.Set keySet = (java.util.Set) reqParams.keySet();
		if(keySet != null && keySet.size() > 0)
		{
			String paramKey = null;
			Object paramValue = null;
			java.util.Iterator it = keySet.iterator();
			while(it.hasNext())
			{
				paramKey = it.next().toString();
				if(request.getParameter(paramKey) != null && !"pageNum".equalsIgnoreCase(paramKey) && !"act".equalsIgnoreCase(paramKey))
				{
					if (Constants.getInt("linkType")==Constants.LINK_TYPE_HTML && "docid".equals(paramKey) && !fromAdmin) continue;
					paramsStr += "&" +paramKey+ "=" + ResponseUtils.filter(request.getParameter(paramKey));
				}
			}

		}
		//pagingParams = (java.util.Hashtable) request.getAttribute("pagingParams");

	/*	if(pagingParams != null && pagingParams.size() > 0)
			{
				String pagingParamsKey = null;
				Object pagingParamsValue = null;
				for(java.util.Enumeration paramsEnum = pagingParams.keys(); paramsEnum.hasMoreElements();)
				{
					pagingParamsKey = paramsEnum.nextElement().toString();
					pagingParamsValue = pagingParams.get(pagingParamsKey);
					if(pagingParamsValue != null)
						pagingParamsStr += "&" +pagingParamsKey+ "=" +pagingParamsValue.toString();
				}
			}
	*/

	}

	intOffset = pageLinksNum/2;

		if (pagingList != null && pagingList.size() > 0)
		{
			totalResults = pagingList.size();

			//strankovanie
			totalPages = totalResults / pageSize;
			if((totalResults % pageSize) > 0)
				totalPages++;

			if (actualPageNum < 1)
				actualPageNum = 1;

			if (actualPageNum > totalPages)
				actualPageNum = totalPages;

			bazarOffset = actualPageNum*pageSize - pageSize;
			if (bazarOffset >= 0 && bazarOffset < totalResults)
			{
				offset = ""+bazarOffset;
			}
			next = bazarOffset + pageSize;
			back = bazarOffset - pageSize;
			if((bazarOffset + pageSize) < totalResults)
				pageTo = bazarOffset + pageSize;
			else
				pageTo = totalResults;

			request.setAttribute("displayPaging", "");
			request.setAttribute("offset", ""+offset);
			request.setAttribute("end", ""+end);
		}
}
catch(Exception e)
{
	sk.iway.iwcm.Logger.error(e);
}

if (pageLink.startsWith("/components")) pageLink = ""; //kvoli AJAX nacitaniu, aby fungovalo spravne strankovanie (bez zakladneho URL budu adresy len na ?pageNum=xxx a budu funkcne)

%>
<%@page import="sk.iway.iwcm.PathFilter"%>
<%@page import="sk.iway.iwcm.Constants"%>
<logic:present name="displayPaging">
<div class="col-md-12 col-xs-12">
	<div class="btn-group float-left pull-left">
		<span class="count-total">
			<%
				String key = "components.forum.number_of_results";
				if (request.getAttribute("pagingListKey")!=null) key = (String)request.getAttribute("pagingListKey");
			%>
			<%=prop.getText(key) %> :&nbsp;<%=totalResults%>,&nbsp;
		</span>
		<span class="paging-info">
			<span> <%=prop.getText("components.forum.displaying") %> : <strong><%=(bazarOffset+1)%></strong> - <strong><%=pageTo%></strong></span>
		</span>
	</div>
	<div class="btn-group forum-pagination float-right pull-right">
		<%
					sOd = 0;
					sDo = 0;

					if (totalPages > 1){ %>
						<a class="btn btn-primary btn-default btn-sm" href='<%=pageLink%>?pageNum=1<%=paramsStr%>' ><i class="fa fa-chevron-left"></i> &lt;</a>
				<%
						if((actualPageNum - intOffset) > 1){ %>
									<a class="btn btn-primary btn-default btn-sm" href='<%=pageLink%>?pageNum=<%=(actualPageNum-(intOffset+1))%><%=paramsStr%>'>&lt;&lt;</a>
							<%	}
						if (totalPages < pageLinksNum)
						{
							for(int i=1; i<=totalPages; i++)
							{
								if(i != actualPageNum){ %>
									<a class="btn btn-primary btn-default btn-sm" href='<%=pageLink%>?pageNum=<%=i%><%=paramsStr%>' ><%=i%></a>
							<%	}
								else
									out.println("<strong class=\"btn btn-primary btn-sm disabled\">"+i+"</strong>");
							}
						}
						else
						{
							sOd = actualPageNum - intOffset;
							sDo = actualPageNum + intOffset;

							if (sDo > totalPages)
							{
								sOd = totalPages - pageLinksNum + 1;
								sDo = totalPages;
							}
							else if (sOd < 1)
							{
								sOd = 1;
								sDo = pageLinksNum;
							}

							for(int i=sOd; i<=sDo; i++)
							{
								if(i != actualPageNum){ %>
									<a class="btn btn-primary btn-default btn-sm" href='<%=pageLink%>?pageNum=<%=i%><%=paramsStr%>' ><%=i%></a>
							<%	}
								else
									out.println("<strong class=\"btn btn-primary btn-sm disabled\">"+i+"</strong>");
							}
							if((actualPageNum + intOffset) < totalPages){ %>
								<a class="btn btn-primary btn-default btn-sm" href='<%=pageLink%>?pageNum=<%=(actualPageNum+intOffset+1)%><%=paramsStr%>' >&gt;&gt;</a>
							<%	}
						}%>
							<a class="btn btn-primary btn-default btn-sm" href='<%=pageLink%>?pageNum=<%=totalPages%><%=paramsStr%>' ><i class="fa fa-chevron-right"></i> &gt;</a>
				<%		if (totalPages > 100)
						{ %>
							<form>
								<select name="pageNum" onchange="form.submit();" >
									<%	jumpIndex = totalPages/10;
									   //ked toto mame 0 tak sa nam to zacykli....
									   if (jumpIndex < 1) jumpIndex = 1;
										for(int j=jumpIndex; j<totalPages; j+=jumpIndex){%>
									<option value="<%=j%>" <%if(actualPageNum==j)out.println("selected");%>><%=j%></option>
									<% } %>
								</select>
								<input type="hidden" name="forumDocId" value="<%=org.apache.struts.util.ResponseUtils.filter(request.getParameter("forumDocId"))%>" >
								<input type="hidden" name="words" value="<%=org.apache.struts.util.ResponseUtils.filter(request.getParameter("words"))%>" >
							</form>
					<%	}
					} %>
	</div>
</div>
</logic:present>
<%request.removeAttribute("displayPaging");%>