<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@page import="org.apache.struts.util.ResponseUtils"%>
<iwcm:checkLogon admin="true" perms="menuWebpages"/>
<% request.setAttribute("cmpName", "search");
String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");

if (Tools.isNotEmpty(paramPageParams))
{
	request.setAttribute("includePageParams", paramPageParams);
}
PageParams pageParams = new PageParams(request);

%>
<jsp:include page="/components/top.jsp"/>

<script type="text/javascript" src="/admin/scripts/common.jsp"></script>
<script type="text/javascript">
<!--
function Ok()
{
	perpage = document.textForm.perpage.value;
	rootGroup = document.textForm.rootGroup.value;
	orderType = document.textForm.orderType.value;
	checkDuplicity = document.textForm.checkDuplicity.checked;
	if (document.textForm.order[0].checked)	order = "asc";
	else	order = "desc";

	htmlCode = "!INCLUDE(/components/search/search.jsp, perpage="+perpage+", rootGroup="+rootGroup+", orderType="+orderType+", order="+order+(checkDuplicity ? ", checkDuplicity=true" : "");


	if (document.textForm.sForm[0].checked)
	{
		sForm = document.textForm.sForm[0].value;
		resultsDocId = document.textForm.resultsDocId.value;
		htmlCode += ", resultsDocId="+resultsDocId;
	}
	else if (document.textForm.sForm[1].checked)
				sForm = document.textForm.sForm[1].value;
		  else if (document.textForm.sForm[2].checked)
					  sForm = document.textForm.sForm[2].value;

	htmlCode += ", sForm="+sForm;
	htmlCode += ")!";

	oEditor.FCK.InsertHtml(htmlCode);
	return true ;
} // End function


function setParentGroupId(returnValue)
{
	if (returnValue.length > 15)
	{
		var groupid = returnValue.substr(0,15);
		var groupname = returnValue.substr(15);
		groupid = groupid.replace(/^[ \t]+|[ \t]+$/gi, "");
		if (document.textForm.rootGroup.value=="")
		{
			document.textForm.rootGroup.value = groupid;
		}
		else
		{
			document.textForm.rootGroup.value = document.textForm.rootGroup.value + "+"+groupid;
		}
	}
}

function disableDocIdField()
{
	var obj = document.getElementById("separMenu1");
	if (obj != null)
	{
		if (document.textForm.sForm[0].checked)
			obj.style.visibility = "visible";
		else
			obj.style.visibility = "hidden";
	}
}

function setStandardDoc(doc_id)
{
   document.textForm.resultsDocId.value = doc_id;
}
if (isFck)
{

}
else
{
	resizeDialog(400, 400);
}
-->
</script>
<%

%>
<style type="text/css">
	.leftCol {
		text-align: right;
		padding-top: 5px;
	}
	#tabMenu1 {
		margin-top: 15px;
	}
	#separMenu1 {
		visibility:hidden;
		float:center;

	}
</style>
<div class="tab-pane toggle_content tab-pane-fullheight">
	<div class="tab-page" id="tabMenu1" style="display: block; width:800px; ">


		<form method="get" name="textForm">
			<div class="col-sm-10 col-sm-offset-1">
				<div class="col-sm-12 form-group">
					<div class="col-sm-6 leftCol">
						<iwcm:text key="components.news.groupids"/>
					</div>
					<div class="col-sm-6">
						<input type="text" name="rootGroup" size="20" maxlength="255" value="<%=ResponseUtils.filter(pageParams.getValue("rootGroup", "1"))%>"/>
						<input type="button" class="btn green" name="groupSelect" value="<iwcm:text key="components.news.addgroup"/>" onClick='popupFromDialog("<iwcm:cp/>/admin/grouptree.jsp", 500, 500);' />
					</div>
				</div>
				<div class="col-sm-12 form-group">
					<div class="col-sm-6 leftCol">
						<iwcm:text key="components.search.results_per_page"/>
					</div>
					<div class="col-sm-6">
						<input type="text" name="perpage" size="2" value="<%=pageParams.getIntValue("perpage", 10)%>"/>
					</div>
				</div>
				<div class="col-sm-12 form-group">
					<div class="col-sm-6 leftCol">
						<iwcm:text key="components.search.check_duplicty"/>
					</div>
					<div class="col-sm-6">
						<input type="checkbox" id="checkDuplicity" name="checkDuplicity" value="true" <%if (pageParams.getBooleanValue("checkDuplicity", false)) out.print("checked='checked'");%> onclick="alertDuplicity(this);" />
					</div>
				</div>
				<div class="col-sm-12 form-group">
					<div class="col-sm-6 leftCol">
						<iwcm:text key="components.search.order_by"/>
					</div>
					<div class="col-sm-6">
						<select name="orderType">
							<option value="sortPriority"><iwcm:text key="components.search.relevance"/></option>
							<option value="title"><iwcm:text key="components.search.file_name"/></option>
							<option value="lastUpdate"><iwcm:text key="components.search.file_change"/></option>
						</select>
					</div>
				</div>
				<div class="col-sm-12 form-group">
					<div class="col-sm-6 col-sm-offset-6">
						<input type="radio" name="order" value="asc" <%if ("asc".equals(pageParams.getValue("order", "asc")))
						out.print("checked='checked'");%> />
						<iwcm:text key="components.inquiry.orderAsc"/><br>
						<input type="radio" name="order" value="desc" <%if ("desc".equals(pageParams.getValue("order", "asc")))
						out.print("checked='checked'");%>/>
						<iwcm:text key="components.inquiry.orderDesc"/>
					</div>
				</div>
				<div class="col-sm-8 col-sm-offset-2 form-group">
						<iwcm:text key="components.search.search_type"/>
				</div>
				<div class="col-sm-12 form-group">
					<div class="col-sm-6 leftCol">
						<iwcm:text key="editor.paste"/>
					</div>
					<div class="col-sm-6">
						<input type="radio" id="radio1" name="sForm" size="textField" value="form" onclick="disableDocIdField();" />
						<iwcm:text key="components.search.text_field"/><br>
						<input type="radio" id="radio2" name="sForm" size="result" value="results" onclick="disableDocIdField();" />
						<iwcm:text key="components.search.results"/><br>
						<input type="radio" id="radio3" name="sForm" size="result" value="complete" onclick="disableDocIdField();" />
						<iwcm:text key="components.search.search_complete"/>
					</div>
				</div>
				<div id="separMenu1" class="col-sm-12 form-group">
					<div class="col-sm-6 leftCol">
						<iwcm:text key="components.search.results_docid"/>
					</div>
					<div class="col-sm-6">
						<input type="text" name="resultsDocId" size="5" value="<%=ResponseUtils.filter(pageParams.getValue("resultsDocId", ""))%>"/>
						<input type="button" value="<iwcm:text key="groupedit.select"/>" name="bSelDoc" onClick='popupFromDialog("/admin/user_adddoc.jsp", 450, 340);' class="button50" />
					</div>
				</div>
			</div>
		</form>
	</div>
</div>

<script type="text/javascript">
<!--
	//inicializacia poloziek
	function alertDuplicity(chck)
	{
		if(chck.checked) alert('<iwcm:text key="components.search.check_duplicty_alert"/>');
	}
	<% if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("orderType", "")))) {%>
		document.textForm.orderType.value = "<%=ResponseUtils.filter(pageParams.getValue("orderType", ""))%>";
	<%}%>
	<% if(ResponseUtils.filter(pageParams.getValue("sForm", "")).compareTo("form") == 0)
	{ %>
		$("#radio1").attr("checked", "checked");
		disableDocIdField();
<%	}
	else if(ResponseUtils.filter(pageParams.getValue("sForm", "")).compareTo("results") == 0)
	{ %>
		$("#radio2").attr("checked", "checked");
<%	}
	else
	{ %>
		$("#radio3").attr("checked", "checked");
<%	} %>
-->
</script>

<jsp:include page="/components/bottom.jsp"/>
