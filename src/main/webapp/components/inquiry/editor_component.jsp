<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.util.*,sk.iway.iwcm.*, sk.iway.iwcm.inquiry.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<iwcm:checkLogon admin="true" perms="menuInquiry"/>
<%@page import="org.apache.struts.util.ResponseUtils"%>

<% request.setAttribute("cmpName", "inquiry"); %>
<jsp:include page="/components/top.jsp"/>

<%
	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
	//out.println(paramPageParams);
	//System.out.println(paramPageParams);
	if (Tools.isNotEmpty(paramPageParams))
	{
		request.setAttribute("includePageParams", paramPageParams);
	}
	PageParams pageParams = new PageParams(request);
	String style =  pageParams.getValue("style", "01");
	String color =  pageParams.getValue("color", "01");
%>
<script type='text/javascript'>
var error

function addGroup()
{
		if (document.textForm.cisGroup.value == "")
		{
				document.textForm.cisGroup.value = document.textForm.group.value;
		}
		else
		{
				document.textForm.cisGroup.value = document.textForm.cisGroup.value + "+" + document.textForm.group.value;
		}
}

function Ok()
{
		var random = false;
		var totalClicks = false;
		var style = $('input[name=style]:checked').val();
		var color = $('input[name=color]:checked').val();
		group = document.textForm.cisGroup.value
		if (group != "")
		{
				imagesLength = document.textForm.imagesLength.value
				percentageFormat = document.textForm.percentageFormat.value
	         orderBy = document.textForm.orderBy.value
	         order = document.textForm.order.value
	         width = document.textForm.width.value
				displayVoteResults = false;

	   		error = 0
	   		if (isNaN(imagesLength) || imagesLength < 0 || imagesLength=="")
	         {
	   				alert("<iwcm:text key="components.invalid_number"/>: <iwcm:text key="components.inquiry.imagesLength"/>");
	   				error = 1
	   				textForm.imagesLength.select()
	   				textForm.imagesLength.focus()
	   		}

	   		if (document.textForm.random.checked == true)
	   		{
	   				random = true;
	   		}

	   		if (document.textForm.totalClicks.checked == true)
	   		{
	   				totalClicks = true;
	   		}

				if (document.textForm.displayVoteResults.checked == true)
	   		{
	   				displayVoteResults = true;
	   		}

	   		if (error != 1)
	         {
	   			//console.log('oEditor: '+oEditor);
				oEditor.FCK.InsertHtml("!INCLUDE(/components/inquiry/inquiry.jsp, style="+style+", color="+color+", group="+group+", imagesLength="+imagesLength+", percentageFormat="+percentageFormat+", orderBy="+orderBy+", order="+order+", width="+width+", random="+random+", totalClicks="+totalClicks+", displayVoteResults="+displayVoteResults+")!");
	   		} // End if
	  }
	  else
	  {
	  		error = 1;
	  		window.alert("<iwcm:text key="components.inquiry.missing_group"/>");
	  }

	if (error != 0)
	{
		return false;
	}
	return true ;
} // End function


if (isFck)
{

}
else
{
	var leftPos = (screen.availWidth-450) / 2;
	var topPos = (screen.availHeight-340) / 2;
	window.moveTo(leftPos, topPos);
	resizeDialog(500, 450);
}

function loadComponentIframe()
{
	var url = "/components/inquiry/admin_inquiry_list.jsp";
	 $("#componentIframeWindowTab").attr("src", url);
}

</script>

<iwcm:menu name="menuInquiry">
	<link type="text/css" rel="stylesheet" media="screen" href="/admin/css/tabpane-luna.css" />
	<style type="text/css">
		ul.tab_menu { padding: 2px 0 0 10px; }
		td.main { padding: 0px; }
		/* UPRAVA JVY */
			.propertiesContent {
				padding-top: 20px;
			}
			.col-sm-4 {
				text-align: right;
				padding-top: 10px;
				margin-bottom: 5px;
			}
			.col-sm-8 {
				margin-bottom: 15px;
			}
		/* END UPRAVA JVY*/
	</style>
	<div class="box_tab box_tab_thin left">
		<ul class="tab_menu" id="Tabs">
			<li class="first openFirst"><a href="#" onclick="showHideTab('1');" id="tabLink1"><iwcm:text key="components.universalComponentDialog.title"/></a></li>
			<li><a href="#" onclick="loadComponentIframe();showHideTab('3');" id="tabLink3"><iwcm:text key="components.roots.new.style"/></a></li>
			<li class="last"><a href="#" onclick="loadComponentIframe();showHideTab('2');" id="tabLink2"><iwcm:text key="menu.inquiry"/></a></li>
		</ul>
	</div>
</iwcm:menu>

<%
List<LabelValueDetails> groups = InquiryDB.getQuestionGroupsByUser(request);
request.setAttribute("groups", groups);
%>

<div class="tab-pane toggle_content tab-pane-fullheight" style="o2verflow: auto; width:790px;">
	<div class="tab-page" id="tabMenu1" style="display: block;">
		<form name=textForm>
			<div class="col-sm-12 propertiesContent">
				<div class="form-group">
					<div class="col-sm-4">
						<iwcm:text key="components.tips.select_group"/>:
					</div>
					<div class="col-sm-8">
						 <select name="group">
				            <logic:iterate name="groups" id="lvb" type="sk.iway.iwcm.LabelValueDetails">
				               <option value="<jsp:getProperty name="lvb" property="label"/>"><jsp:getProperty name="lvb" property="label"/></option>
				            </logic:iterate>
				            <option value="fromTemplate"><iwcm:text key="components.inquiry.fromTemplate"/></option>
				         </select>
				         [<a href="javascript: addGroup();"><iwcm:text key="editor.spell.add"/></a>]
					</div>
				</div>
			 	<div class="form-group">
					<div class="col-sm-4">
						<iwcm:text key="components.tips.groups"/>:
					</div>
					<div class="col-sm-8">
						<input style="width:250px;" type=text name=cisGroup class="form-control" size=30 maxlength="100" value="<%=ResponseUtils.filter(pageParams.getValue("group", ""))%>">
					</div>
				</div>
				<div class="form-group">
					<div class="col-sm-4">
						<iwcm:text key="components.inquiry.imagesLength"/>:
					</div>
					<div class="col-sm-8">
						<input style="width:250px;" type="text" name="imagesLength" class="form-control" size="3" maxlength="3" value="<%=pageParams.getIntValue("imagesLength", 10)%>">
					</div>
				</div>
				<div class="form-group">
					<div class="col-sm-4">
						<iwcm:text key="components.inquiry.percentageFormat"/>:
					</div>
					<div class="col-sm-8">
						<select name="percentageFormat">
				            <option value="0">0</option>
				            <option value="0.0">0.0</option>
				         </select>
					</div>
				</div>
				<div class="form-group">
					<div class="col-sm-4">
						<iwcm:text key="components.inquiry.orderBy"/>:
					</div>
					<div class="col-sm-8">
						<select name="orderBy">
				            <option value="answer_id"><iwcm:text key="components.inquiry.order.answer_id"/></option>
				            <option value="answer_text"><iwcm:text key="components.inquiry.order.answer_text"/></option>
				            <option value="answer_clicks"><iwcm:text key="components.inquiry.order.answer_clicks"/></option>
				         </select>

				         <select name="order">
				            <option value="ascending"><iwcm:text key="components.inquiry.orderAsc"/></option>
				            <option value="descending"><iwcm:text key="components.inquiry.orderDesc"/></option>
				         </select>
					</div>
				</div>
				<div class="form-group">
					<div class="col-sm-4">
						<iwcm:text key="components.inquiry.width"/>:
					</div>
					<div class="col-sm-8">
						<input style="width:60px;" type="text" name="width" class="form-control" size="7" maxlength="7" value="<%=ResponseUtils.filter(pageParams.getValue("width", "100%"))%>">
					</div>
				</div>
				<div class="form-group">
					<div class="col-sm-4">
					</div>
					<div class="col-sm-8">
						<input type="checkbox" name="random" <%if (pageParams.getBooleanValue("random", true)) out.print("checked='checked'");%>>
						<iwcm:text key="components.inquiry.random"/>
					</div>
				</div>
				<div class="form-group">
					<div class="col-sm-4">
					</div>
					<div class="col-sm-8">
						<input type="checkbox" name="totalClicks" <%if (pageParams.getBooleanValue("totalClicks", true)) out.print("checked='checked'");%>>
						<iwcm:text key="components.inquiry.display_total_clicks"/>
					</div>
				</div>
				<div class="form-group">
					<div class="col-sm-4">
					</div>
					<div class="col-sm-8">
						<input type="checkbox" name="displayVoteResults" <%if (pageParams.getBooleanValue("displayVoteResults", true)) out.print("checked='checked'");%>>
						<iwcm:text key="components.inquiry.display_vote_results"/>
					</div>
				</div>
			</div>
		</form>
	</div>

	<div class="tab-page tab-page-iframe" id="tabMenu2">
		<iframe id="componentIframeWindowTab" frameborder="0" name="componentIframeWindowTab" width="100%" src="/admin/iframe_blank.jsp"></iframe>
	</div>

	<div class="tab-page" id="tabMenu3">
		<div id="styleSelectArea" style="width: 100%; overflow: auto;">

			<%
			int checkedInputPosition = 0;
			IwcmFile stylesDir = new IwcmFile(Tools.getRealPath("/components/inquiry/admin-styles/"));
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

					String styleValue = file.getName().substring(0, file.getName().lastIndexOf("."));

					if (styleValue.equals(style)) checkedInputPosition = counter;
					%>

						<div class="style_box styleBox">
							<label class="image" for="style-<%=styleValue%>">
								<img src="<%=file.getVirtualPath() %>" alt="<%=styleValue%>" />
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
		<div id="colorSelectArea" style="width: 100%; overflow: auto;">
			<h2>Farba ankety</h2>
			<%
			checkedInputPosition = 0;
			IwcmFile colorsDir = new IwcmFile(Tools.getRealPath("/components/inquiry/admin-colors/"));
			if (colorsDir.exists() && colorsDir.canRead())
			{
				IwcmFile colorFiles[] = colorsDir.listFiles();
				colorFiles = FileTools.sortFilesByName(colorFiles);
				int counter = 0;
				for (IwcmFile file : colorFiles)
				{
					if (file.getName().endsWith(".png")==false) continue;

					// tuto kontrolu pravdepodobne netreba kedze bude bootstrap vzdy
					//if (file.getName().contains("bootstrap") && sk.iway.iwcm.common.CloudToolsForCore.isBootstrap(request)==false) continue;

					String colorValue = file.getName().substring(0, file.getName().lastIndexOf("."));

					if (colorValue.equals(color)) checkedInputPosition = counter;
					%>

						<div class="style_box styleBoxColor">
							<label class="image" for="color-<%=colorValue%>">
								<img src="<%=file.getVirtualPath() %>" alt="<%=colorValue%>" />
								<div class="radioSelect">
  									<input type="radio" name="color" id="color-<%=colorValue%>" value="<%=colorValue%>" <%= colorValue.equals(color) ? " checked=\"checked\"" : "" %>/>
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

<script type="text/javascript">
<%	if (Tools.isNotEmpty(pageParams.getValue("percentageFormat", ""))) {%>
	document.textForm.percentageFormat.value = "<%=ResponseUtils.filter(pageParams.getValue("percentageFormat", ""))%>";
<%}
	if (Tools.isNotEmpty(pageParams.getValue("orderBy", ""))) {%>
	document.textForm.orderBy.value = "<%=ResponseUtils.filter(pageParams.getValue("orderBy", ""))%>";
<%}
	if (Tools.isNotEmpty(pageParams.getValue("order", ""))) {%>
	document.textForm.order.value = "<%=ResponseUtils.filter(pageParams.getValue("order", ""))%>";
<%}%>
	$(document).ready(function(){
		checkRadio();
		$('input[type=radio]').change(function() {
	    	$("span.checkedRadio").removeClass("showChecked");
	    	$("div.style_box").removeClass("checkedBox");
			checkRadio();
	    });
	});
	function checkRadio(){
		$("input:checked").next("span").addClass("showChecked");
		$("input:checked").parents("div.radio").next("span").addClass("showChecked");
		$("input:checked").parents("div.style_box").addClass("checkedBox");
		if ($('#style-01').is(':checked')){
			$('#colorSelectArea').hide();
		}else{
			$('#colorSelectArea').show();
	    }
	}
</script>

<style>
	#styleSelectArea, #colorSelectArea { padding: 10px 20px; }
	#colorSelectArea { display: none; }
	.styleBox, .styleBoxColor {
		margin: 0px 20px 20px 0px; padding: 10px; border: 1px solid #f5f5f5; -webkit-border-radius: 3px !important; -moz-border-radius: 3px !important; border-radius: 3px !important; float: left; position: relative;
		-webkit-transition: all 0.3s linear; -moz-transition: all 0.3s linear; -ms-transition: all 0.3s linear; -o-transition: all 0.3s linear; transition: all 0.3s linear;
	}
	.style_box.checkedBox { border: 1px solid #29c01a; }
	.styleBox { width: 140px; height: 180px; }
	.styleBoxColor { width: 140px; height: 180px; }
	.styleBox input, .styleBoxColor input { display: none !important; }
	.styleBox label, .styleBoxColor label { margin: 0px; padding: 0px; }
	span.checkedRadio {
		display: block; width: 45px; height: 45px; position: absolute; left: 50%; bottom: 0px; margin: 0 0 -20px -22px; background: url(style-selected.png);
		-webkit-transition: all 0.3s linear; -moz-transition: all 0.3s linear; -ms-transition: all 0.3s linear; -o-transition: all 0.3s linear; transition: all 0.3s linear;
		-ms-filter:"progid:DXImageTransform.Microsoft.Alpha(Opacity=0)"; filter: alpha(opacity=0); opacity: 0;
	}
	span.checkedRadio.showChecked { display:block; -ms-filter:"progid:DXImageTransform.Microsoft.Alpha(Opacity=1)"; filter: alpha(opacity=1); opacity: 1; }
</style>

<jsp:include page="/components/bottom.jsp"/>
