<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.io.*,sk.iway.iwcm.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@page import="org.apache.struts.util.ResponseUtils"%>

<% request.setAttribute("cmpName", "rating");
String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
String jspFileName = request.getParameter("jspFileName");
if(Tools.isNotEmpty(jspFileName)){
	int slash = jspFileName.lastIndexOf("/");
	int dot = jspFileName.lastIndexOf(".");

	if(slash > 0 && dot < jspFileName.length()) jspFileName = jspFileName.substring(slash+1, dot);	//ziskam len nazov komponenty bez koncovky
}
if (Tools.isNotEmpty(paramPageParams))
{
	request.setAttribute("includePageParams", paramPageParams);
}
PageParams pageParams = new PageParams(request);
%>
<jsp:include page="/components/top.jsp"/>

<script type="text/javascript" src="/components/form/check_form.js"></script>
<script type='text/javascript'>

function Ok()
{
	checkLogon = false;
	if (document.textForm.checkLogon.checked)
		checkLogon = true;
    ratingType = document.textForm.ratingType.value;
    ratingDocId = document.textForm.ratingDocId.value;
    range = document.textForm.range.value;
    usersLength = document.Form3.usersLength.value;
	docsLength = document.Form4.docsLength.value;
	period = document.Form4.period.value;

	if (ratingType == "rating_form")
	{
		htmlCode = "!INCLUDE(/components/rating/rating_form.jsp, checkLogon="+checkLogon+", ratingDocId="+ratingDocId+", range="+range+")!";
	} else if (ratingType == "rating_page")
	{
		htmlCode = "!INCLUDE(/components/rating/rating_page.jsp, ratingDocId="+ratingDocId+", range="+range+")!";
	} else if (ratingType == "rating_top_users")
	{
		htmlCode = "!INCLUDE(/components/rating/rating_top_users.jsp, usersLength="+usersLength+")!";
	} else if (ratingType == "rating_top_pages_new")
	{
		htmlCode = "!INCLUDE(/components/rating/rating_top_pages.jsp, range="+range+", docsLength="+docsLength+", period="+period+")!";
	}

	oEditor.FCK.InsertHtml(htmlCode);
	return true ;
} // End function

function showMenu(select)
{
		//el = document.getElementsByTagName("DIV");

		// if (el!=null)
		// {
		// 	for(var i=6; i<el.length; i++)
		// 	{
		// 		if (i == (select.value - 1 - 6))
		// 			el[i].style.display = "block";
		// 		else
		// 			el[i].style.display = "none";
		// 	}
		// }

		checkLogon = document.getElementById("checkLogon1");
		ratingDocId = document.getElementById("ratingDocId1");
		range = document.getElementById("range1");

		divId1 = document.getElementById("1");
		divId2 = document.getElementById("2");
		divId3 = document.getElementById("3");
		divId4 = document.getElementById("4");

		var browserName=navigator.appName;
		var display;
		if (browserName == "Microsoft Internet Explorer")
		  display = "table-row";
		else
		  display = "block";

		if(select.value == "rating_form")
		{
			checkLogon.style.display = display;
			ratingDocId.style.display = display;
			range.style.display = display;

			divId1.style.display = display;
			divId2.style.display = "none";
			divId3.style.display = "none";
			divId4.style.display = "none";
		}else if(select.value == "rating_page")
		{
			checkLogon.style.display = "none";
			ratingDocId.style.display = display;
			range.style.display = display;

			divId1.style.display = "none";
			divId2.style.display = display;
			divId3.style.display = "none";
			divId4.style.display = "none";
		}else if(select.value == "rating_top_users")
		{
			checkLogon.style.display = "none";
			ratingDocId.style.display = "none";
			range.style.display = "none";

			divId1.style.display = "none";
			divId2.style.display = "none";
			divId3.style.display = display;
			divId4.style.display = "none";
		}else if(select.value == "rating_top_pages_new")
		{
			checkLogon.style.display = "none";
			ratingDocId.style.display = "none";
			range.style.display = "none";

			divId1.style.display = "none";
			divId2.style.display = "none";
			divId3.style.display = "none";
			divId4.style.display = display;
		}
}

</script>

<div class="tab-pane toggle_content tab-pane-fullheight">
	<div class="tab-page" id="tabMenu1" style="display: block; width:100%; ">
		<div class="padding10">
			<form method=get name=textForm>
				<div class="col-xs-12">
					<div class="col-xs-8 form-group col-xs-offset-2">
						<div class="col-xs-8 leftCol">
							<iwcm:text key="components.rating.type"/>
						</div>
						<div class="col-xs-4">
							<select name="ratingType" onchange="javascript: showMenu(this);" >
									<option value="rating_form" selected><iwcm:text key="components.rating.rating_form"/></option>
									<option value="rating_page"><iwcm:text key="components.rating.show_rating"/></option>
									<option value="rating_top_users"><iwcm:text key="components.rating.top_users"/></option>
									<option value="rating_top_pages_new"><iwcm:text key="components.rating.top_docid"/></option>
							</select>
						</div>
					</div>
					<div id="checkLogon1" class="col-xs-8 form-group col-xs-offset-2">
						<div class="col-xs-8 leftCol">
							<iwcm:text key="components.rating.check_logon"/>
						</div>
						<div class="col-xs-4">
							<input type=checkbox name=checkLogon <%if (pageParams.getBooleanValue("checkLogon", true))
								out.print("checked='checked'");%>>
						</div>
					</div>
					<div id="ratingDocId1" class="col-xs-8 form-group col-xs-offset-2">
						<div class="col-xs-8 leftCol">
							<iwcm:text key="components.rating.rating_doc_id"/>
						</div>
						<div class="col-xs-4">
							<input type="text" name="ratingDocId" size="8" maxlength="10" value="<%=ResponseUtils.filter(pageParams.getValue("ratingDocId", ""))%>" class="numbers">
						</div>
					</div>
					<div id="range1" class="col-xs-8 form-group col-xs-offset-2">
						<div class="col-xs-8 leftCol">
							<iwcm:text key="components.rating.range"/>
						</div>
						<div class="col-xs-4">
							<input type=text name=range size=3 maxlength="2" value="<%=ResponseUtils.filter(pageParams.getValue("range", "10"))%>" class="numbers">
						</div>
					</div>
				</div>
			</form>
			<div class="col-xs-12">
				<div id="1" style="display:block">
				 <form name="Form1">
				 	<div class="col-xs-offset-1 col-xs-10 form-group text-center">
						<p><iwcm:text key="components.rating.rating_form_desc"/>.</p>
					</div>
				 </form>
				</div>

				<div id="2">
				 <form name="Form2">
				 	<div class="col-xs-offset-1 col-xs-10 form-group text-center">
				  		<p><iwcm:text key="components.rating.show_rating_desc"/>.</p>
				  	</div>
				 </form>
				</div>

				<div id="3">
				 <form name="Form3">
				 	<div class="col-xs-offset-2 col-xs-8 form-group">
						<div class="col-sm-8 leftCol">
					 		<iwcm:text key="components.rating.display_users"/>:
					 	</div>
					 	<div class="col-sm-4">
							<input type=text name=usersLength size=3 maxlength="2" value="<%=pageParams.getIntValue("usersLength", 10)%>" class="required numbers">
						</div>
				 	</div>
				 </form>
				</div>

				<div id="4">
				 <form name="Form4">
				 	<div class="col-xs-offset-2 col-xs-8 form-group">
						<div class="col-sm-8 leftCol">
				 			<iwcm:text key="components.rating.display_docs"/>:
				 		</div>
					 	<div class="col-sm-4">
							<input type=text name=docsLength size=3 maxlength="2" value="<%=pageParams.getIntValue("docsLength", 10)%>" class="required numbers">
						</div>
					</div>
					<div class="col-xs-offset-2 col-xs-8 form-group">
						<div class="col-sm-8 leftCol">
						<iwcm:text key="components.rating.period"/>:
					</div>
					<div class="col-sm-4">
						<input type=text name=period size=4 maxlength="3" value="<%=pageParams.getIntValue("period", 7)%>" class="required numbers">
				 	</div>
				 </form>
				 </div>
			</div>
		</div>
	</div>
</div>

<script type="text/javascript">
	//inicializacia poloziek

	<% if (Tools.isNotEmpty(jspFileName)) {%>
		document.textForm.ratingType.value = "<%=jspFileName%>";
		showMenu(document.textForm.ratingType);
	<%}%>
</script>

<jsp:include page="/components/bottom.jsp"/>
