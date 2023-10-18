<%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.io.*,sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %><iwcm:checkLogon admin="true" perms="cmp_menu"/>
<%@page import="org.apache.struts.util.ResponseUtils"%>
<%
	request.setAttribute("cmpName", "menu");
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
<script type="text/javascript" src="/admin/scripts/common.jsp"></script>
<script type='text/javascript'>

    function setRootGroup(returnValue)
    {
        if (returnValue.length > 15)
        {
            var groupid = returnValue.substr(0,15);
            var groupname = returnValue.substr(15);
            groupid = groupid.replace(/^[ \t]+|[ \t]+$/gi, "");

            document.MenuULForm.rootGroupId.value = groupid;
        }
    }

    function Ok()
    {
        var data = {
                rootGroupId: document.MenuULForm.rootGroupId.value,
                startOffset: document.MenuULForm.startOffset.value,
                maxLevel: document.MenuULForm.maxLevel.value,
                menuIncludePerex: document.MenuULForm.menuIncludePerex.value,
            	classes: document.MenuULForm.classes.value,
                generateEmptySpan: document.MenuULForm.generateEmptySpan.value,
                openAllItems: document.MenuULForm.openAllItems.value,
                onlySetVariables: document.MenuULForm.onlySetVariables.value,
                rootUlId:  document.MenuULForm.rootUlId.value,
            	menuInfoDirName: document.MenuULForm.menuInfoDirName.value
            };

        if (data.menuIncludePerex == "true") {
            data.menuIncludePerexLevel = document.MenuULForm.menuIncludePerexLevel.value;
        }

        var html = "!INCLUDE(/components/menu/menu_ul_li.jsp, {{params}})!",
            params = [];

        $.each(data, function(k, v) {
            params.push(k + '=' + v);
        });

        oEditor.FCK.InsertHtml(html.replace('{{params}}', params.join(", ")));
        return true;
    } // End function

    if (isFck)
    {

    }
    else
    {
        resizeDialog(570, 750);
    }

</script>
<style type="text/css">
	/* UPRAVA JVY */
	.col-sm-4 {
		text-align: right;
		padding-top: 5px;
	}
	.col-sm-4, .col-sm-8 {
		margin-bottom: 8px;
	}

	/* END UPRAVA JVY*/
</style>
<div class="tab-pane toggle_content" style="width:450px;">
	<div class="tab-page" id="tabMenu1" style="display: block;">

		<div id="help_0" style="display:block">
			<form name="MenuULForm" >

				<div class="col-sm-10 col-sm-offset-1">
					<div>
						<iwcm:text key="components.menu.menu_help_menu"/><br><br>
					</div>
					<div class="row">
						<div class="col-sm-4">
							<iwcm:text key="stat_settings.group_id"/>:
						</div>
						<div class="col-sm-8">
							<input type="text" name="rootGroupId" size="5" value="<%=pageParams.getIntValue("rootGroupId", 1) %>">
							<input type="button" class="btn green" name="groupSelect" value="<iwcm:text key="groupedit.change"/>" onClick='popupFromDialog("/admin/grouptree.jsp?fcnName=setRootGroup", 500, 500);'>
						</div>
					</div>
					<div class="row">
						<div class="col-sm-4">
							<iwcm:text key="components.menu.start_offset"/>:
						</div>
						<div class="col-sm-8">
							<input type="text" name="startOffset" size="5" value="<%=pageParams.getIntValue("startOffset", 1) %>">
							<br>
							<small>
								<iwcm:text key="components.menu.start_offset_help"/>
							</small>
						</div>
					</div>
					<div class="row">
						<div class="col-sm-4">
							<iwcm:text key="components.menu.max_level"/>:
						</div>
						<div class="col-sm-8">
							<input type="text" name="maxLevel" size="5" value="<%=pageParams.getIntValue("maxLevel", -1) %>">
						</div>
					</div>
					<div class="row">
						<div class="col-sm-4">
							<iwcm:text key="components.menu.class_type"/>:
						</div>
						<div class="col-sm-8">
							<select name="classes">
								<option value="none"><iwcm:text key="components.menu.class_type.none"/></option>
								<option value="basic" selected="selected"><iwcm:text key="components.menu.class_type.basic"/></option>
								<option value="full"><iwcm:text key="components.menu.class_type.full"/></option>
								<option value="bootstrap"><iwcm:text key="components.menu.class_type.bootstrap"/></option>
							</select>
						</div>
					</div>


					<div class="row">
						<div class="col-sm-4">
							<iwcm:text key="components.menu.generate_empty_span"/>:
						</div>
						<div class="col-sm-8">
							<select name="generateEmptySpan">
								<option value="false"><iwcm:text key="components.menu.generate_empty_span.false"/></option>
								<option value="true"><iwcm:text key="components.menu.generate_empty_span.true"/></option>
							</select>
						</div>
					</div>

					<div class="row">
						<div class="col-sm-4">
							<iwcm:text key="components.menu.open_all_items"/>:
						</div>
						<div class="col-sm-8">
							<select name="openAllItems">
								<option value="false"><iwcm:text key="components.menu.open_all_items.false"/></option>
								<option value="true"><iwcm:text key="components.menu.open_all_items.true"/></option>
							</select>
						</div>
					</div>

					<div class="row">
						<div class="col-sm-4">
							<iwcm:text key="components.menu.only_set_variables"/>:
						</div>
						<div class="col-sm-8">
							<select name="onlySetVariables">
								<option value="false"><iwcm:text key="components.menu.only_set_variables.false"/></option>
								<option value="true"><iwcm:text key="components.menu.only_set_variables.true"/></option>
							</select>
						</div>
					</div>

					<div class="row">
						<div class="col-sm-4">
							<iwcm:text key="components.menu.root_ul_id"/>:
						</div>
						<div class="col-sm-8">
							<input type="text" name="rootUlId" size="22" value="<%=ResponseUtils.filter(pageParams.getValue("rootUlId", "menu")) %>">
						</div>
					</div>

					<div class="row">
						<div class="col-sm-4">
							<iwcm:text key="components.menu.menu_info_dir_name"/>:
						</div>
						<div class="col-sm-8">
							<input type="text" name="menuInfoDirName" size="22" value="<%=ResponseUtils.filter(pageParams.getValue("menuInfoDirName", "")) %>">
						</div>
					</div>


					<div class="row">
						<div class="col-sm-4">
							<iwcm:text key="components.menu.menu_include_perex"/>:
						</div>
						<div class="col-sm-8">
							<select name="menuIncludePerex" class="menuIncludePerex">
								<option value="false"><iwcm:text key="components.menu.only_set_variables.false"/></option>
								<option value="true"><iwcm:text key="components.menu.only_set_variables.true"/></option>
							</select>
						</div>
					</div>
					<div class="perex_level clearfix">
						<div class="row">
							<div class="col-sm-4">
								<iwcm:text key="components.menu.menu_include_perex_level"/>:
							</div>
							<div class="col-sm-8">
								<input type="text" name="menuIncludePerexLevel"  size="5" value="<%= pageParams.getIntValue("menuIncludePerexLevel", 1) %>">
							</div>
						</div>
					</div>

					<div class="clearer"></div>

				</div>
			</form>
		</div>
	</div>
</div>

<script type="text/javascript">
    <%	if (Tools.isNotEmpty(pageParams.getValue("generateEmptySpan", ""))) {%>
    document.MenuULForm.generateEmptySpan.value = "<%=ResponseUtils.filter(pageParams.getValue("generateEmptySpan", ""))%>";
    <%}
    if (Tools.isNotEmpty(pageParams.getValue("openAllItems", ""))) {%>
    document.MenuULForm.openAllItems.value = "<%=ResponseUtils.filter(pageParams.getValue("openAllItems", ""))%>";
    <%}
    if (Tools.isNotEmpty(pageParams.getValue("onlySetVariables", ""))) {%>
    document.MenuULForm.onlySetVariables.value = "<%=ResponseUtils.filter(pageParams.getValue("onlySetVariables", ""))%>";
    <%}
    if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("classes", "")))) {%>
    document.MenuULForm.classes.value = "<%=ResponseUtils.filter(pageParams.getValue("classes", ""))%>";
    <%}
    if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("menuInfoDirName", "")))) {%>
    document.MenuULForm.menuInfoDirName.value = "<%=ResponseUtils.filter(pageParams.getValue("menuInfoDirName", ""))%>";
    <%}%>

    $(function(){
        var menuIncludePerex = "<%= pageParams.getBooleanValue("menuIncludePerex", false) %>";
        $('.menuIncludePerex').on('change', function(){
            $(this).val() == "true" ?   $('.perex_level').fadeIn() : $('.perex_level').fadeOut();
        });
        setTimeout(function() {
            $('.menuIncludePerex').val(menuIncludePerex).trigger('change');
        });
    });
</script>

<jsp:include page="/components/bottom.jsp"/>
