<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.io.*,sk.iway.iwcm.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page import="org.apache.struts.util.ResponseUtils"%>
<%
request.setAttribute("cmpName", "content-block");
%>
<%@page import="sk.iway.iwcm.system.ConfDB"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.Constants"%>
<%@page import="sk.iway.iwcm.Tools"%>
<jsp:include page="/components/top.jsp"/>
<script src="/components/form/check_form.js" type="text/javascript"></script>
<%=Tools.insertJQuery(request) %>
<%
	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
	//out.println(paramPageParams);
	//System.out.println(paramPageParams);
	if (Tools.isNotEmpty(paramPageParams))
	{
		request.setAttribute("includePageParams", paramPageParams);
	}
	PageParams pageParams = new PageParams(request);
%>
<script type='text/javascript'>
function Ok()
{
	var type = $('#type').val();
	var title = $('#title').val();
	var image1 = $('#image1').val();
	var image2 = $('#image2').val();
	var color = $('#color').val();
	var classes = $('#classes').val();

	oEditor.FCK.InsertHtml('!INCLUDE(/components/content-block/content-block.jsp, type=' + type + ', title=' + title + ', image1=' + image1 + ', image2=' + image2 + ', color=' + color + ', classes=' + classes + ')!');
	return true ;
} // End function

$(function(){

});

if (!isFck)
	resizeDialog(700, 700);

</script>

<div class="tab-pane toggle_content tab-pane-fullheight" style="width:680px;">
	<div class="tab-page" id="tabMenu1" style="display: block;">


		<div style="width: 530px">
		<form method="get" style="margin: 0px;" id="heatMapForm" name="contentBlockForm">

		<div class="row">
			<div class="form-group col-xs-12">
				<label for="type">
						Typ:
				</label>
				<select class="form-control" id="type">
					<option value="1" <%=pageParams.getValue("type", "").equals("1") ? "selected=\"selected\"" : ""%>>Typ 1</option>
					<option value="2" <%=pageParams.getValue("type", "").equals("2") ? "selected=\"selected\"" : ""%>>Typ 2</option>
					<option value="3" <%=pageParams.getValue("type", "").equals("3") ? "selected=\"selected\"" : ""%>>Typ 3</option>
					<option value="3" <%=pageParams.getValue("type", "").equals("4") ? "selected=\"selected\"" : ""%>>Typ 4</option>
					<option value="3" <%=pageParams.getValue("type", "").equals("5") ? "selected=\"selected\"" : ""%>>Typ 5</option>
				</select>
			</div>
		</div>

		<div class="row">
			<div class="form-group col-xs-12">
				<label for="title">
					<iwcm:text key="editor.title"/>
				</label>
				<input type="text" id="title" class="form-control" value="<%=ResponseUtils.filter(pageParams.getValue("title", ""))%>" placeholder="<iwcm:text key="editor.title"/>">
			</div>
		</div>

		<div class="row">
			<div class="form-group col-xs-12">
				<label for="image1">
					<iwcm:text key="editor.perex.image"/> 1
				</label>
				<div class="input-group">
					<input type="text" id="image1" name="image1" class="form-control" value="<%=ResponseUtils.filter(pageParams.getValue("image1", ""))%>" placeholder="<iwcm:text key="editor.perex.image"/> 1">
					<span class="input-group-addon btn green" onclick="openImageDialogWindow('contentBlockForm', 'image1', '')" >
                 		<i class="fa fa-picture-o"></i>
              	</span>
				</div>
			</div>
		</div>

		<div class="row">
			<div class="form-group col-xs-12">
				<label for="image2">
					<iwcm:text key="editor.perex.image"/> 2
				</label>

				<div class="input-group">
					<input type="text" id="image2" name="image2" class="form-control col-md-6 " value="<%=ResponseUtils.filter(pageParams.getValue("image2", ""))%>" placeholder="<iwcm:text key="editor.perex.image"/> 1">
					<span class="input-group-addon btn green" onclick="openImageDialogWindow('contentBlockForm', 'image2', '')" >
                 		<i class="fa fa-picture-o"></i>
              	</span>
				</div>
			</div>
		</div>

		<div class="row">
			<div class="form-group col-xs-12">
				<label for="color">
					<iwcm:text key="editor.div_properties.background_color" />
				</label>
				<input type="text" id="color" class="form-control colorpicker-rgba" value="<%=ResponseUtils.filter(pageParams.getValue("color", ""))%>" placeholder="<iwcm:text key="editor.div_properties.background_color" />">
			</div>
		</div>

		<div class="row">
			<div class="form-group col-xs-12">
		    	<label for="classes"><iwcm:text key="editor.div_properties.css_class" />:</label>
		    	<input type="text" class="form-control cssClass"  id="classes" value="<%=ResponseUtils.filter(pageParams.getValue("classes", ""))%>" placeholder="<iwcm:text key="editor.div_properties.css_class" />">
		  	</div>

		</div>


		</form>
		</div>

	</div>
</div>
<jsp:include page="/components/bottom.jsp"/>
