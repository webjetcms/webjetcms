<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="org.apache.struts.util.ResponseUtils,sk.iway.iwcm.Constants" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib prefix="bean" uri="http://struts.apache.org/tags-bean" %>
<%@page import="sk.iway.iwcm.Identity"%>
<%@ page import="sk.iway.iwcm.PageParams" %>
<%@ page import="sk.iway.iwcm.Tools" %>
<%@ page import="sk.iway.iwcm.doc.DocDB" %>
<%@ page import="sk.iway.iwcm.doc.DocDetails, sk.iway.spirit.MediaDB, sk.iway.spirit.model.MediaGroupBean, java.util.ArrayList, java.util.List" %>
<iwcm:checkLogon admin="true" perms="menuWebpages"/>
<%
request.setAttribute("cmpName", "media");
Identity user = (Identity)session.getAttribute(Constants.USER_KEY);

int docId =  Tools.getIntValue(Tools.getRequestParameter(request, "docId"), -1);
DocDetails doc = DocDB.getInstance().getDoc(docId);

String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
//out.println(paramPageParams);
//System.out.println(paramPageParams);
if (Tools.isNotEmpty(paramPageParams))
{
	request.setAttribute("includePageParams", paramPageParams);
}
PageParams pageParams = new PageParams(request);

List<MediaGroupBean> mediaGroups =  new ArrayList<>();
if(doc != null){
    mediaGroups = MediaDB.getGroups(doc.getGroupId());
}else{
    mediaGroups = MediaDB.getGroups();
}
List<MediaGroupBean> selectedGroups = new ArrayList<>();
String groupsParam = pageParams.getValue("group", pageParams.getValue("groups", ""));

	if(Tools.isNotEmpty(groupsParam)){
		String[] groupsArr = groupsParam.split(",");
		for(String g : groupsArr){
			MediaGroupBean group = null;
			if(Tools.getIntValue(g, -1) > -1){
				group = MediaDB.getGroup(Tools.getIntValue(g, -1));
			}
			else{
				group = MediaDB.getGroup(g);
			}
			if(group!=null){
				selectedGroups.add(group);
			}
		}
	}

request.setAttribute("mediaGroups", mediaGroups);
request.setAttribute("selectedGroups", selectedGroups);
request.setAttribute("docId", docId);
%>
<jsp:include page="/components/top.jsp"/>

<script type='text/javascript'>
    $(document).ready(function () {
        initializeMediaGroups();
    });

function initializeMediaGroups(){
	$('#selectedGroups option').each(function () {
		$('#disabledItemsLeft option[value='+$(this).val()+']').remove();
	});
	$('#disabledItemsLeft option, #selectedGroups option').click(function () {
		clickedMediaGroup = $(this).val();
	});
}

function removeFromData(data, key) {
    $.each(data, function(i, v){
        if (key == v['name']) {
            data.splice(i, 1);
		}
	})
}

function getData() {

    var data = $('#editor-component').serializeArray();

    removeFromData(data, 'disabledItemsLeft');
    removeFromData(data, 'disabledItemsRight');

    var map = $.map(data, function(v, i){
        return v.name + "=" + v.value;
	});

    var groups = [];
    $('#selectedGroups option').each(function(){
        groups.push($(this).val());
    });

    map.push("groups=\"" + groups.join(",") + "\"");

    if (map.length > 0) {
        return ", " + map.join(", ");
	}

    return "";
}

function Ok()
{
	oEditor.FCK.InsertHtml("!INCLUDE(/components/media/media.jsp" + getData() + ")!");
	return true ;
}

</script>
<script type="text/javascript" src="/admin/scripts/modalDialog.js"></script>
<script type="text/javascript" src="/components/form/check_form.js"></script>
<style type="text/css">
	.leftCol {
		text-align: right;
		padding-top: 5px;
	}
	#tabMenu1 {
		margin-top: 15px;
	}
</style>
<div class="tab-pane toggle_content">
	<div class="tab-page" id="tabMenu1" style="display: block; width: 99%">
		<form method="get" name="textForm" id="editor-component" action="editor_component.jsp">
			<div class="row">
				<div class="col-sm-12">
					<div class="form-group">
						<div class="row">
							<div class="col-sm-12">
								<h2><iwcm:text key="components.media.group"/></h2>
							</div>
							<div class="col-sm-5">
								<p><iwcm:text key="components.media.editor_component.dostupne_skupiny"/></p>
								<select name="disabledItemsLeft" id="disabledItemsLeft" multiple="true" size="7" class="disItems form-control">
									<logic:iterate id="mediaGroup" name="mediaGroups" type="sk.iway.spirit.model.MediaGroupBean">
										<option value="<bean:write name="mediaGroup" property="mediaGroupId"/>"><bean:write name="mediaGroup" property="mediaGroupName"/></option>
									</logic:iterate>
								</select>
							</div>

							<div class="col-sm-1" style="    padding-top: 60px;">
								<button type="button" onclick="moveLeft(this.form, 'mediaGroup', 'disabledItems');" title="Zrušiť" class="btn green">
									<i class="ti ti-circle-arrow-left"></i>
								</button>

								<button type="button" onclick="moveRight(this.form, 'mediaGroup', 'disabledItems');" title="Zvoliť" class="btn green" style="margin-top: 8px;">
									<i class="ti ti-circle-arrow-right"></i>
								</button>

							</div>
							<div class="col-sm-5">
								<p><iwcm:text key="editor.perex_group.selected_items" /></p>
								<select name="disabledItemsRight" multiple="true" size="7" class="disItems form-control" id="selectedGroups">
									<logic:iterate id="selectedGroup" name="selectedGroups" type="sk.iway.spirit.model.MediaGroupBean">
										<option value="<bean:write name="selectedGroup" property="mediaGroupId"/>"><bean:write name="selectedGroup" property="mediaGroupName"/></option>
									</logic:iterate>
								</select>
							</div>
						</div>
					</div>
				</div>
			</div>
			<div class="row">
				<div class="col-md-12">
					<div class="form-group">
						<label for="docid"><iwcm:text key="components.popup.docid" />:</label>

						<div class="input-group">
							<input id="docid" name="docid" class="form-control" value="${docId}">
							<span class="input-group-btn">
								<span onclick="popup('/admin/user_adddoc.jsp', 450, 340);" class="input-group-addon btn green">
									<i class="ti ti-focus-2"></i>
								</span>
							</span>
						</div>

					</div>
				</div>
			</div>
		</form>
		<script type="text/javascript">
		<% if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("group", "")))) {%>
			document.textForm.group.value = "<%=ResponseUtils.filter(pageParams.getValue("group", ""))%>";
		<%}%>

			function setStandardDoc (docId) {
			    $('#docid').val(docId);
			}
		</script>

	</div>
</div>

<jsp:include page="/components/bottom.jsp"/>
