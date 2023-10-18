<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.util.*,sk.iway.iwcm.qa.*,sk.iway.iwcm.*"%>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/displaytag.tld" prefix="display" %>


<%
PageParams pageParams = new PageParams(request);
String style =  pageParams.getValue("style", "01");
int startPage = 1;

try
{
		if (Tools.getRequestParameter(request, "page")!=null)
	{
		startPage = Integer.parseInt(Tools.getRequestParameter(request, "page"));
	}
}
catch (Exception ex)
{
		sk.iway.iwcm.Logger.error(ex);
}

//pod touto skupinou to bude figurovat v admin casti
String groupName = pageParams.getValue("groupName", "default");
int pageSize = pageParams.getIntValue("pageSize", 10);
int sortBy = pageParams.getIntValue("sortBy", 1);
String sortOrder = pageParams.getValue("sortOrder", "asc");
boolean asc = sortOrder.equals("asc");
int displayType = pageParams.getIntValue("displayType", 1);

List<QABean> qaListAll = QADB.getQAList(groupName, true, sortBy, asc, request);

List<QABean> qaList = QADB.getQAList(groupName, true, sortBy, asc, startPage, pageSize, request);
//out.print("startPage: "+startPage+" pageSize: "+pageSize+" qaList.size():" +qaList.size());

request.setAttribute("qaList", qaList);

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
QABean qa;

if("01".equals(style)){
	request.setAttribute("qaList", qaListAll);
if(displayType==1){

%>
<display:table name="qaList" uid="qaRow" requestURI="<%=PathFilter.getOrigPath(request)%>"  pagesize="<%=pageSize %>" class="sort_table" >
<%qa = (QABean)qaRow;  %>

	<display:column titleKey="components.qa.dialog_title" class="sort_td" headerClass="sort_thead_td" sortable="true">
		<%=qa.getQuestion() %>

		<p style="margin-left: 100px;">
			<%=qa.getAnswer() %>
		</p>
	</display:column>

	<display:column property="questionDate" titleKey="qa.date" class="sort_td" headerClass="sort_thead_td" sortable="true" />

</display:table>
<%}
if(displayType==2)
{
	if (qaList != null && qaList.size()>0)
	{
	%>

		<%=Tools.insertJQuery(request)%>
		<%=Tools.insertJQueryUI(pageContext, "accordion")%>
		<script type="text/javascript">
		<!--
			$(function() {
				$(".qa").accordion({
					header: '.question'
				});
			});

			document.write('<link type="text/css" href="/components/qa/qa.css" rel="stylesheet"/>');
		-->
		</script>

		<ol class="qa">
		<logic:iterate id="q" name="qaList" type="sk.iway.iwcm.qa.QABean">
			<li>
				<span class="question"><jsp:getProperty name="q" property="question"/></span>
				<span class="answer"><jsp:getProperty name="q" property="answerWeb"/></span>
			</li>
		</logic:iterate>
		</ol>
	<% } else { %>
	<iwcm:text key="displaytag.basic.msg.empty_list"/>
	<% } %>
<%}%>

<%} else if("02".equals(style)){
	request.setAttribute("qaList", qaList);
	if(displayType==1){%>
	<div class="row">
		<div id="faq" class="col-md-12">
			<div class="panel-group">
				<%
					int count_numer = 0;
				%>
				<logic:iterate id="q" name="qaList" type="sk.iway.iwcm.qa.QABean">
				<%
					count_numer++;
				%>
				<div class="panel panel-default">
					<div class="panel-heading">
						<h4 class="panel-title" id="collapse-<%=count_numer%>">
							<jsp:getProperty name="q" property="question"/>
						</h4>
					</div>
					<div id="collapse-<%=count_numer%>" class="panel-collapse">
						<div class="panel-body">
							<jsp:getProperty name="q" property="answerWeb"/>
						</div>
					</div>
				</div>
				</logic:iterate>
			</div>
		</div>
	</div>

	<%
	int pagingSize = pageSize;
	//out.print("pagingSize: "+pagingSize+"<br>");
    int ind = 0;
    int pageInd = 1;
    if (Tools.isNotEmpty(Tools.getRequestParameter(request, "page")))
    {
      pageInd = Integer.valueOf(Tools.getRequestParameter(request, "page"));
    }
    //out.print("pageInd: "+pageInd);
    %>
    <div class="pagination">
    	<div class="btn-group forum-pagination pull-right">
	      <%
	      String paramsToUrl = "";

	      String tagParam = Tools.getParameter(request, "tag");

	      if (Tools.isNotEmpty(tagParam)) {
	        paramsToUrl = "&tag=" + tagParam;
	      }


	      if(pageInd != 1)
	      {
	        %><a class="btn btn-default btn-sm" href="?page=<%=(pageInd-1)+paramsToUrl%>"><iwcm:text key="components.search.back"/></a><%
	      }
	      else
	      {
	        %><span class="btn btn-default btn-sm disabled"><iwcm:text key="components.search.back"/></span><%
	      }
	      if (qaListAll.size() != 0)
	        ind = qaListAll.size();
	      else
	        ind = qaListAll.size()-1;
	     //out.print("ind: "+ind);
	      int prevCount = pageInd-pagingSize-1;
	      if((pageInd-pagingSize-1) < 1)
	        prevCount = 0;

	      if(prevCount != 0)
	      {
	        %><a class="btn btn-default btn-sm" href="?page=1<%=paramsToUrl%>">1</a><span class="btn btn-default btn-sm">...</span><%
	      }

	      for(int i=prevCount; i < pageInd-1; i++)
	      {
	        %><a class="btn btn-default btn-sm" href="?page=<%=(i+1)+paramsToUrl%>"><%=i+1%></a><%
	      }

	      //for(int i=(pageInd-1); i < (pageInd+pagingSize); i++)
	    	  //out.print("kv: "+(ind%pagingSize > 0 ?((ind/pagingSize)+1):(ind/pagingSize)));
	      //for(int i=(pageInd-1); i < (ind%pagingSize > 0 ?((ind/pagingSize)+1):(ind/pagingSize)); i++)
	    	 for(int i=(pageInd-1); i < ((pageInd+1) < (ind/pagingSize)? (pageInd+1):(ind/pagingSize)+1); i++)
	      {
	        if(i == ind)
	          break;
	        if (pageInd != 0 && pageInd == i+1)
	        {
	          %><strong class="btn btn-primary btn-sm disabled"><%=(i+1)%></strong><%
	        }else{
	          %><a class="btn btn-default btn-sm gg" href="?page=<%=(i+1)+paramsToUrl%>"><%=i+1%></a><%
	        }
	      }
	      if((pageInd*pagingSize+pagingSize+pagingSize) <= ind)
	      {
	    	  //zostava pocet stranok
	    	  int lastPageInd = ind%pagingSize != 0 ? (ind/pagingSize)+1:ind/pagingSize;
	        %><span class="btn btn-default btn-sm">...</span><a class="btn btn-default btn-sm" href="?page=<%=lastPageInd+paramsToUrl%>"><%=lastPageInd%></a><%
	      }
	      //out.print("xx");
	      if(pageInd*pagingSize < ind)
	      {
	        %><a class="btn btn-default btn-sm" href="?page=<%=(pageInd+1)+paramsToUrl%>"><iwcm:text key="components.search.next"/></a></li><%
	      }
	      else
	      {
	        %><span class="btn btn-default btn-sm disabled"><iwcm:text key="components.search.next"/></span><%
	      }
	      %>
    	</div>
    </div>
<% 	}
if(displayType==2){ %>
	<script>
		$('.carousel').carousel({
			interval: 3000
		});
	</script>
	<div class="row">
		<div id="faq" class="col-md-12">
			<div class="panel-group" id="accordion">
				<%
					int count_numer = 0;
				%>
				<logic:iterate id="q" name="qaList" type="sk.iway.iwcm.qa.QABean">
				<%
					count_numer++;
				%>
				<div class="panel panel-default">
					<div class="panel-heading">
						<h4 class="panel-title">
							<a class="accordion-toggle" data-toggle="collapse" data-parent="#accordion" href="#collapse-<%=count_numer%>">
								<jsp:getProperty name="q" property="question"/>
							</a>
						</h4>
					</div>
					<div id="collapse-<%=count_numer%>" class="panel-collapse collapse">
						<div class="panel-body">
							<jsp:getProperty name="q" property="answerWeb"/>
						</div>
					</div>
				</div>
				</logic:iterate>
			</div>
		</div>
	</div>

	<%
	int pagingSize = pageSize;
	//out.print("pagingSize: "+pagingSize+"<br>");
    int ind = 0;
    int pageInd = 1;
    if (Tools.isNotEmpty(Tools.getRequestParameter(request, "page")))
    {
      pageInd = Integer.valueOf(Tools.getRequestParameter(request, "page"));
    }
    //out.print("pageInd: "+pageInd);
    %>
    <div class="pagination">
    	<div class="btn-group forum-pagination pull-right">
	      <%
	      String paramsToUrl = "";

	      String tagParam = Tools.getParameter(request, "tag");

	      if (Tools.isNotEmpty(tagParam)) {
	        paramsToUrl = "&tag=" + tagParam;
	      }


	      if(pageInd != 1)
	      {
	        %><a class="btn btn-default btn-sm" href="?page=<%=(pageInd-1)+paramsToUrl%>"><iwcm:text key="components.search.back"/></a><%
	      }
	      else
	      {
	        %><span class="btn btn-default btn-sm disabled"><iwcm:text key="components.search.back"/></span><%
	      }
	      if (qaListAll.size() != 0)
	        ind = qaListAll.size();
	      else
	        ind = qaListAll.size()-1;
	     //out.print("ind: "+ind);
	      int prevCount = pageInd-pagingSize-1;
	      if((pageInd-pagingSize-1) < 1)
	        prevCount = 0;

	      if(prevCount != 0)
	      {
	        %><a class="btn btn-default btn-sm" href="?page=1<%=paramsToUrl%>">1</a><span class="btn btn-default btn-sm">...</span><%
	      }

	      for(int i=prevCount; i < pageInd-1; i++)
	      {
	        %><a class="btn btn-default btn-sm" href="?page=<%=(i+1)+paramsToUrl%>"><%=i+1%></a><%
	      }

	      //for(int i=(pageInd-1); i < (pageInd+pagingSize); i++)
	    	  //out.print("kv: "+(ind%pagingSize > 0 ?((ind/pagingSize)+1):(ind/pagingSize)));
	      //for(int i=(pageInd-1); i < (ind%pagingSize > 0 ?((ind/pagingSize)+1):(ind/pagingSize)); i++)
	    	 for(int i=(pageInd-1); i < ((pageInd+1) < (ind/pagingSize)? (pageInd+1):(ind/pagingSize)+1); i++)
	      {
	        if(i == ind)
	          break;
	        if (pageInd != 0 && pageInd == i+1)
	        {
	          %><strong class="btn btn-primary btn-sm disabled"><%=(i+1)%></strong><%
	        }else{
	          %><a class="btn btn-default btn-sm gg" href="?page=<%=(i+1)+paramsToUrl%>"><%=i+1%></a><%
	        }
	      }
	      if((pageInd*pagingSize+pagingSize+pagingSize) <= ind)
	      {
	    	  //zostava pocet stranok
	    	  int lastPageInd = ind%pagingSize != 0 ? (ind/pagingSize)+1:ind/pagingSize;
	        %><span class="btn btn-default btn-sm">...</span><a class="btn btn-default btn-sm" href="?page=<%=lastPageInd+paramsToUrl%>"><%=lastPageInd%></a><%
	      }
	      //out.print("xx");
	      if(pageInd*pagingSize < ind)
	      {
	        %><a class="btn btn-default btn-sm" href="?page=<%=(pageInd+1)+paramsToUrl%>"><iwcm:text key="components.search.next"/></a></li><%
	      }
	      else
	      {
	        %><span class="btn btn-default btn-sm disabled"><iwcm:text key="components.search.next"/></span><%
	      }
	      %>
    	</div>
    </div>
	<%}
}%>
