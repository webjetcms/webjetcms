<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%@page import="sk.iway.iwcm.users.SettingsAdminDB"%>
<%@page import="sk.iway.iwcm.qa.QADB"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<iwcm:checkLogon admin="true" perms="menuQa"/>
<%@page import="org.apache.struts.util.ResponseUtils"%>
<%
request.setAttribute("cmpName", "qa");

String loggedUserEmail = "";

Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
if (user!=null)
{
	loggedUserEmail = user.getEmail();
}

String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");

String jspFileName = Tools.getRequestParameter(request, "jspFileName");
if(Tools.isNotEmpty(jspFileName)){
	int slash = jspFileName.lastIndexOf("/");
	int dot = jspFileName.lastIndexOf(".");

	if(slash > 0 && dot < jspFileName.length()) jspFileName = jspFileName.substring(slash+1, dot);	//ziskam len nazov komponenty bez koncovky
}
jspFileName = Tools.replace(jspFileName, "_bootstrap", "");

if (Tools.isNotEmpty(paramPageParams))
{
	request.setAttribute("includePageParams", paramPageParams);
}
PageParams pageParams = new PageParams(request);
String style =  pageParams.getValue("style", "01");
%>
<jsp:include page="/components/top.jsp"/>

<script type='text/javascript'>

function Ok()
{

	var htmlCode = "";
	var groupName;
	var toEmail;
	var style = $('input[name=style]:checked').val();
	if (document.textForm.field.value=="qa-ask")
	{
		groupName = document.textForm1.groupName.value;
		toEmail = document.textForm1.toEmail.value;
		htmlCode = "!INCLUDE(/components/qa/qa-ask.jsp, style="+style+addIncludeParameter("groupName", groupName)+addIncludeParameter("toEmail", toEmail)+", show="+document.textForm1.show.value+", required="+document.textForm1.required.value+")!";
	}
	else
		if (document.textForm.field.value=="qa")
		{
			groupName = document.textForm0.groupName.value;
			pageSize = document.textForm0.pageSize.value;
			sort = document.textForm0.sort.value;
			sortOrder = document.textForm0.sortOrder.value;
			displayType = document.textForm2.displayType.value;
			htmlCode = "!INCLUDE(/components/qa/qa.jsp, style="+style+addIncludeParameter("groupName", groupName)+", pageSize="+pageSize+", sortBy="+sort+", sortOrder="+sortOrder+", displayType="+displayType+")!";
		}
		oEditor.FCK.InsertHtml(htmlCode);

	return true ;
} // End function


function showHelp(select)
{
	size = select.options.length;

	for (i=0; i<size; i++)
	{
		el = document.getElementById("help_"+i);
		elHeader = document.getElementById("selectQA");
		if (el!=null && elHeader != null)
		{
			if (i==select.selectedIndex)
			{
				el.style.display = "block";
				// elHeader.style.marginLeft = "104px";
			}
			else
			{
				el.style.display = "none";
				// elHeader.style.marginLeft = "90px";
			}
		}
	}
}

if (isFck)
{

}
else
{
	resizeDialog(400, 270);
}
function loadListIframe()
{
	var url = "/components/qa/admin_list.jsp";
	 $("#componentIframeWindowTabList").attr("src", url);
}
</script>

<iwcm:menu name="menuQa">
	<link type="text/css" rel="stylesheet" media="screen" href="/admin/css/tabpane-luna.css" />
	<style type="text/css">
		ul.tab_menu { padding: 2px 0 0 10px; }
		td.main { padding: 0px; }
	</style>
	<div class="box_tab box_tab_thin left">
		<ul class="tab_menu" id="Tabs">
			<li class="first openFirst"><a href="#" onclick="showHideTab('1');" id="tabLink1"><iwcm:text key="components.universalComponentDialog.title"/></a></li>
			<li><a href="#" onclick="loadListIframe();showHideTab('3');" id="tabLink3"><iwcm:text key="components.roots.new.style"/></a></li>
			<li class="last"><a href="#" onclick="loadListIframe();showHideTab('2');" id="tabLink2"><iwcm:text key="qa.roots.new"/></a></li>
		</ul>
	</div>
</iwcm:menu>

<style type="text/css">
	.leftCol {
		text-align: right;
		padding-top: 5px;
	}
	#tabMenu1 {
		padding-top: 15px;
	}
</style>

<div class="tab-pane toggle_content tab-pane-fullheight">
	<div class="tab-page" id="tabMenu1" style="display: block;">
		<%
		String field = ResponseUtils.filter(pageParams.getValue("field","qa"));
		%>
		<form name=textForm>
			<div class="col-sm-10 form-group">
				<div class="col-sm-4 leftCol">
					<iwcm:text key="components.qa.insert"/>
				</div>
				<div class="col-sm-8">
					<select id="selectQA" name="field" onChange="showHelp(this)">
						<option value="qa" <%=field.equals("qa")?"selected":""%>><iwcm:text key="components.qa.questions_answers2"/></option>
						<option value="qa-ask" <%=field.equals("qa-ask")?"selected":""%>><iwcm:text key="components.qa.question_form"/></option>
					</select>
				</div>
			</div>
		</form>
		<div id="help_0" style="display:block">
			<form name="textForm0">
				<div class="col-sm-10 form-group">
					<div class="col-sm-4 leftCol">
						<iwcm:text key="components.qa.group_name"/>
					</div>
					<div class="col-sm-8">
						<!-- 			sqlQuery="SELECT DISTINCT group_name FROM questions_answers ORDER BY group_name" -->
						<%pageContext.setAttribute("selectSqlQueryOptions", SettingsAdminDB.filterBeansByUserAllowedCategories(QADB.getQARoots(request),"label",user,"menuQa"));%>
						<iwcm:select property="groupName" name="groupName" enableNewTextKey="qa.admin_answer.new_group" style="max-width: 500px;">
			       			<iwcm:options collection="selectSqlQueryOptions" property="label" labelProperty="label"/>
			    		</iwcm:select>
			    	</div>
				</div>
				<div class="col-sm-10 form-group">
					<div class="col-sm-4 leftCol">
						<iwcm:text key="components.qa.page_size"/>
					</div>
					<div class="col-sm-8">
						<input type="text" name="pageSize" value="<%=pageParams.getIntValue("pageSize", 10)%>" size="4">
					</div>
				</div>

		<%
		String sort = ResponseUtils.filter(pageParams.getValue("sortBy","1"));
		%>

				<div class="col-sm-10 form-group">
					<div class="col-sm-4 leftCol">
						<iwcm:text key="components.qa.sortBy"/>
					</div>
					<div class="col-sm-8">
						<select id="sortBy" name="sort" >
							<option value="1" <%=sort.equals("1")?"selected":""%>><iwcm:text key="components.qa.sort.byDate"/></option>
							<option value="2" <%=sort.equals("2")?"selected":""%>><iwcm:text key="components.qa.sort.byPriority"/></option>
						</select>
					</div>
				</div>

		<%
		String sortOrder = ResponseUtils.filter(pageParams.getValue("sortOrder","1"));
		%>
				<div class="col-sm-10 form-group">
					<div class="col-sm-4 leftCol">
						<iwcm:text key="components.qa.sortOrder"/>
					</div>
					<div class="col-sm-8">
						<select id="sortOrder" name="sortOrder" >
							<option value="asc" <%=sortOrder.equals("asc")?"selected":""%>><iwcm:text key="components.qa.sort.asc"/></option>
							<option value="desc" <%=sortOrder.equals("desc")?"selected":""%>><iwcm:text key="components.qa.sort.desc"/></option>
						</select>
					</div>
				</div>
			</form>
		</div>
		<div id="help_1" style="display:none">
			<form name="textForm1" style="padding: 0px">
				<div class="col-sm-10 form-group">
					<div class="col-sm-4 leftCol">
						<iwcm:text key="components.qa.group_name"/>
					</div>
					<div class="col-sm-8">
						<iwcm:select property="groupName" name="groupName" sqlQuery="SELECT DISTINCT group_name FROM questions_answers ORDER BY group_name" enableNewTextKey="qa.admin_answer.new_group">
			       			<iwcm:options collection="selectSqlQueryOptions" property="label" labelProperty="label"/>
			    		</iwcm:select>
				    </div>
				</div>
				<div class="col-sm-10 form-group">
					<div class="col-sm-4 leftCol">
						<iwcm:text key="components.qa.to_email"/>
					</div>
					<div class="col-sm-8">
						<input type="text" name="toEmail" value="<%=ResponseUtils.filter(pageParams.getValue("toEmail", loggedUserEmail))%>" size="25">
					</div>
				</div>
				<div class="col-sm-10 form-group">
					<div class="col-sm-4 leftCol">
						<iwcm:text key="components.qa.show_fields"/>
					</div>
					<div class="col-sm-8">
						<input type="text" name="show" value="<%=ResponseUtils.filter(pageParams.getValue("show", "name+company+phone+email"))%>" size="40">
						<br/>
						<iwcm:text key="components.qa.options"/>: name+company+phone+email
					</div>
				</div>
				<div class="col-sm-10 form-group">
					<div class="col-sm-4 leftCol">
						<iwcm:text key="components.qa.required_fields"/>
					</div>
					<div class="col-sm-8">
						<input type="text" name="required" value="<%=ResponseUtils.filter(pageParams.getValue("required", "name+email"))%>" size="40">
						<br/>
						<iwcm:text key="components.qa.options"/>: name+company+phone+email
					</div>
				</div>
			</form>
		</div>

	</div>

	<div class="tab-page tab-page-iframe" id="tabMenu2">
		<iframe id="componentIframeWindowTabList" frameborder="0" name="componentIframeWindowTabList" width="100%" src="/admin/iframe_blank.jsp"></iframe>
	</div>

	<div class="tab-page" id="tabMenu3">
		<div id="styleSelectArea" staayle="height: 470px; width: 100%; overflow: auto;">
			<form name="textForm2" style="padding: 0px" class="row typeForm">
				<%
				String displayType = ResponseUtils.filter(pageParams.getValue("displayType","1"));
				%>
				<div class="form-group">
					<p class="heading">
						<iwcm:text key="components.qa.displayType"/>
					</p>

					<div class="style_box styleBox">
						<label for="type1">
							<img src="/components/qa/admin-display-type/01.png" alt="<iwcm:text key="components.qa.display.old"/>" title="<iwcm:text key="components.qa.display.old"/>" />
							<div class="radioSelect">
								<input id="type1" type="radio" name="displayType" value="1">
								<span class="checkedRadio"> </span>
							</div>
						</label>
					</div>

					<div class="style_box styleBox">
						<label for="type2">
							<img src="/components/qa/admin-display-type/02.png" alt="<iwcm:text key="components.qa.display.olList"/>" title="<iwcm:text key="components.qa.display.olList"/>" />
							<div class="radioSelect">
								<input id="type2" type="radio" name="displayType" value="2">
								<span class="checkedRadio"> </span>
							</div>
						</label>
					</div>
				</div>
			</form>
			<p class="heading">
				<iwcm:text key="components.qa.displayStyle"/>
			</p>
			<%
			int checkedInputPosition = 0;
			IwcmFile stylesDir = new IwcmFile(Tools.getRealPath("/components/qa/admin-styles/"));
			if (stylesDir.exists() && stylesDir.canRead())
			{
				IwcmFile styleFiles[] = stylesDir.listFiles();
				styleFiles = FileTools.sortFilesByName(styleFiles);
				int counter = 0;
				for (IwcmFile file : styleFiles)
				{
					if (file.getName().endsWith(".png")==false) continue;

					// tuto kontrolu pravdepodobne netreba kedze bude bootstrap vzdy
					//if (file.getName().contains("bootstrap") && sk.iway.iwcm.common.CloudToolsForCore.isBootstrap(request)==false) continue;

					String styleValue = Tools.escapeHtml(file.getName().substring(0, file.getName().lastIndexOf(".")));

					if (styleValue.equals(style)) checkedInputPosition = counter;
					%>

						<div class="style_box styleBox">
							<label class="image" for="style-<%=styleValue%>">
								<img src="<%=Tools.escapeHtml(file.getVirtualPath()) %>" alt="<%=styleValue%>" />
								<div class="radioSelect">
  									<input type="radio" name="style" id="style-<%=styleValue%>" value="<%=styleValue%>" <%= styleValue.equals(style) ? " checked=\"checked\"" : "" %>/>
  									<% //if ("iwcm.interway.sk".equals(request.getServerName())) out.print(styleValue); %>
  									<span class="checkedRadio"> </span>
  								</div>
							</label>
						</div>
					<%
					counter++;
				}
			}
			%>
		</div>
	</div>
</div>
<script type='text/javascript'>
<% if (Tools.isNotEmpty(jspFileName)) {%>
	document.textForm.field.value = "<%=jspFileName%>";
	showHelp(document.textForm.field)
<%}
	if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("groupName", "")))) {%>
	document.textForm0.groupName.value = "<%=ResponseUtils.filter(pageParams.getValue("groupName", ""))%>";
<%}
	if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("groupName", "")))) {%>
	document.textForm1.groupName.value = "<%=ResponseUtils.filter(pageParams.getValue("groupName", ""))%>";
<%}%>
$(document).ready(function(){

	$("input[name=displayType][value=<%=displayType %>]").prop("checked", "checked");

	checkRadio();
	$('input[type=radio]').change(function() {
    	$("span.checkedRadio").removeClass("showChecked");
    	$("div.style_box").removeClass("checkedBox");
		checkRadio();
    });

	checkSelect();
	$('select#selectQA').change(function(){
		checkSelect();
	});
});
function checkRadio(){
	$("input:checked").next("span").addClass("showChecked");
	$("input:checked").parents("div.radio").next("span").addClass("showChecked");
	$("input:checked").parents("div.style_box").addClass("checkedBox");
}
function checkSelect(){
	var target = $('#selectQA option:selected').val();
	if(target == "qa-ask"){
		$("form.typeForm").hide();
	}
	else{
		$("form.typeForm").show();
	}
}
</script>

<style>
	p.heading { font-weight: bold; margin: 5px 0; pading: 0px; }
	#styleSelectArea { padding: 10px 20px; }
	.styleBox {
		margin: 0px 20px 20px 0px; padding: 10px; border: 1px solid #f5f5f5; -webkit-border-radius: 3px !important; -moz-border-radius: 3px !important; border-radius: 3px !important; float: left; position: relative;
		-webkit-transition: all 0.3s linear; -moz-transition: all 0.3s linear; -ms-transition: all 0.3s linear; -o-transition: all 0.3s linear; transition: all 0.3s linear;
	}
	.style_box.checkedBox { border: 1px solid #29c01a; }
	.styleBox { width: 140px; height: 180px; }
	.styleBox input { display: none !important; }
	.styleBox label { margin: 0px; padding: 0px; }
	span.checkedRadio {
		display: block; width: 45px; height: 45px; position: absolute; left: 50%; bottom: 0px; margin: 0 0 -20px -22px; background: url(style-selected.png);
		-webkit-transition: all 0.3s linear; -moz-transition: all 0.3s linear; -ms-transition: all 0.3s linear; -o-transition: all 0.3s linear; transition: all 0.3s linear;
		-ms-filter:"progid:DXImageTransform.Microsoft.Alpha(Opacity=0)"; filter: alpha(opacity=0); opacity: 0;
	}
	span.checkedRadio.showChecked { display:block; -ms-filter:"progid:DXImageTransform.Microsoft.Alpha(Opacity=1)"; filter: alpha(opacity=1); opacity: 1; }
</style>

<jsp:include page="/components/bottom.jsp"/>
