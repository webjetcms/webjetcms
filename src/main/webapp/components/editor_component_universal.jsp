<%@page import="org.apache.struts.util.ResponseUtils"%>
<%@page import="sk.iway.iwcm.Constants"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="windows-1250" import="sk.iway.iwcm.Identity,sk.iway.iwcm.PageParams,sk.iway.iwcm.PathFilter,sk.iway.iwcm.FileTools" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@page import="sk.iway.iwcm.Tools"%>
<%@ page import="sk.iway.iwcm.i18n.Prop" %>
<%@ page import="sk.iway.iwcm.stat.Column" %>
<%@ page import="sk.iway.iwcm.users.UserGroupDetails" %>
<%@ page import="sk.iway.iwcm.users.UserGroupsDB" %>
<%@ page import="java.util.List" %>
<%
Prop prop = Prop.getInstance(request);

String jspFileName = ResponseUtils.filter(request.getParameter("jspFileName"));
String jspFileNameKeyPrefix = "";
if (Tools.isNotEmpty(jspFileName))
{
	if (jspFileName.startsWith("!INCLUDE(")) jspFileName = jspFileName.substring(9);
	if (jspFileName.endsWith(")!")) jspFileName = jspFileName.substring(0, jspFileName.length()-2);

	if (jspFileName.indexOf(".")!=-1) jspFileNameKeyPrefix = Tools.replace(jspFileName.substring(0, jspFileName.lastIndexOf(".")+1), "/", ".").substring(1);
}

request.setAttribute("cmpName", "universalComponentDialog");
request.setAttribute("descKey", jspFileName);
Identity user = (Identity)session.getAttribute(Constants.USER_KEY);

String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
//System.out.println("paramPageParams="+paramPageParams);
//System.out.println("jspFileNameKeyPrefix="+jspFileNameKeyPrefix);
//System.out.println("path="+PathFilter.getOrigPath(request));

if (Tools.isNotEmpty(paramPageParams))
{
	request.setAttribute("includePageParams", paramPageParams);
}
PageParams pageParams = new PageParams(request);

//ak je nastaveny parameter alebo kluc typu jspFileName.conf.propSearchKey tak sa zobrazia 2 taby a v druhom bude mozne editovat properties zacinajuce na dany text
//napr.: components.raiffeisen.kalkulacky.kasicka.conf.propSearchKey=kalkulacka.kasicka
String propSearchKey = pageParams.getValue("conf.propSearchKey", null);
if (propSearchKey == null)
{
	String tmpValue = prop.getText(jspFileNameKeyPrefix+"conf.propSearchKey");
	if (tmpValue.equals(jspFileNameKeyPrefix+"conf.propSearchKey")==false)
	{
		propSearchKey = tmpValue;
	}
}
if (propSearchKey == null && prop.getTextStartingWith(jspFileNameKeyPrefix).isEmpty()==false)
{
    propSearchKey = jspFileNameKeyPrefix;
}
%>
<jsp:include page="/components/top.jsp"/>

<%=Tools.insertJQuery(request) %>
<script type="text/javascript" src="/admin/scripts/dateTime.jsp"></script>
<script type="text/javascript" src="/admin/scripts/modalDialog.js"></script>
<script type="text/javascript" src="/components/form/check_form.js"></script>
<link type="text/css" rel="stylesheet" media="screen" href="/admin/css/tabpane-luna.css" />
<style type="text/css">
.required {
	background: url(/components/form/required.gif) top right no-repeat;
	background-color: white;
}

.invalid {
	background-color: #FFF2BF;
}

.inputradio, .inputcheckbox {
	border: 0px;
}

.requiredradio, .requiredcheckbox {
	background-image: none !important;
	background-color: transparent;
}

.invalidradio, .invalidcheckbox {
	background-image: none !important;
	background-color: #FFF2BF !important;
}

.requiredLabel { background-color: #FFF2BF; }
.invalidLabel { color: red; }

ul.formMailValidationErrors { font-weight: normal; }

#ajaxFormResultContainer { border: 1px dashed #B72222; padding: 5px; }
#ajaxFormResultContainer ul.messages { font-weight: bold; }
#ajaxFormResultContainer div.ajaxError { color: #B72222; font-weight: bold; }
#ajaxFormResultContainer div.ajaxError li { font-weight: normal; }

<% if (Tools.isNotEmpty(propSearchKey)) { %>
ul.tab_menu { padding: 2px 0 0 10px; }
td.main { padding: 0px; }
<% } %>
</style>

<%
String url = PathFilter.getOrigPath(request);
if (url.startsWith("/components/")) {
	int i = url.indexOf("/", 14);
	if (i > 0) {
		String componentName = url.substring(12, i);
		System.out.println("componentName="+componentName+" dir="+url.substring(0, i));
		if (componentName.contains(".") && FileTools.isDirectory(url.substring(0, i))==false) {
			//TODO: OVER CI MA ANOTACIU

			//je to spring komponenta, prepni sa na zobrazenie cez datatabulku
			%>
				<script type="text/javascript">
					var src = '/admin/v9/webpages/component?id=1';
					var iframe = window.parent.$('#editorComponent');
					//console.log("iframe=", iframe);
					iframe
						.after($('<input type="hidden" id="className" />').val("<%=ResponseUtils.filter(componentName) %>"))
						.after($('<input type="hidden" id="parameters" />').val(""))
						.after($('<input type="hidden" id="docId" />').val("<%=ResponseUtils.filter(request.getParameter("docId")) %>"))
						.after($('<input type="hidden" id="groupId" />').val("<%=ResponseUtils.filter(request.getParameter("groupId")) %>"))
						.after($('<input type="hidden" id="title" />').val("<%=ResponseUtils.filter(request.getParameter("title")) %>"))
						.after($('<input type="hidden" id="originalComponentName" />').val(""))
                    	.after($('<input type="hidden" id="originalJspFileName" />').val(""));

					iframe.attr('src', src);
				</script>
			<%
			return;
		}
	}
}
%>

<%!
public static int TYPE_TEXT = 1;
public static int TYPE_DATE = 2;
public static int TYPE_INT = 3;
public static int TYPE_FLOAT = 4;
public static int TYPE_URL = 5;
public static int TYPE_IMAGE = 6;
public static int TYPE_DYNAMIC_NAME_VALUE_PAIR = 7;
public static int TYPE_SELECT = 8;
public static int TYPE_GROUP_ID_SELECT = 9;
public static int TYPE_SELECT_USERGROUPS_NAMES = 10;

public static String DEFAULT_NAME_KEY_PREFIX = "components.universalComponentDialog.";

public static String getText(String name, String jspFileNameKeyPrefix, Prop prop)
{
	String text = prop.getText(jspFileNameKeyPrefix+name);
	//System.out.println(jspFileNameKeyPrefix+name+"="+text);
	if (text.startsWith(jspFileNameKeyPrefix)==false) return text;

	text = prop.getText(DEFAULT_NAME_KEY_PREFIX+name);
	//System.out.println(DEFAULT_NAME_KEY_PREFIX+name+"="+text);
	if (text.startsWith(DEFAULT_NAME_KEY_PREFIX)==false) return text;

	//uprav text PonizenieODan na Ponizenie o dan
	if (name.indexOf('.')==-1)
	{
		StringBuilder fixedText = new StringBuilder();
		for (char ch : name.toCharArray())
		{
			if (ch=='_') ch=' ';
			if (fixedText.length()==0)
			{
				fixedText.append(ch);
				continue;
			}
			if (Character.isUpperCase(ch)) fixedText.append(' ');
			fixedText.append(Character.toLowerCase(ch));
		}
		name = fixedText.toString();
		name = Tools.replace(name, "U r l ", "URL");
		name = Tools.replace(name, "J s ", "JS");
	}

	return name;
}

public static Column getConfigData(String name, String value, String jspFileNameKeyPrefix, Prop prop)
{
	String config = getText(name+".conf", jspFileNameKeyPrefix, prop);
	Column data = new Column();

	//inicialne hodnoty
	data.setIntColumn1(TYPE_TEXT); //TYPE
	data.setColumn2(""); //CSS_CLASSES
	data.setIntColumn3(30); //INPUT_SIZE
	data.setIntColumn4(255); //INPUT_MAX_LENGTH
	data.setColumn5(""); //FREE_TEXT

	if (config.equals(name+".conf"))
	{
		//nebola zadana hodnota, skus vydedukovat s nazvu parametra
		if (name.indexOf("URL")!=-1 || name.indexOf("odkaz")!=-1) config = "5:url";

		if (name.indexOf("Vyska")!=-1 || name.indexOf("Pocet")!=-1 || name.indexOf("_doba_splacania")!=-1 || name.indexOf("_urok")!=-1 || name.indexOf("_pozicka")!=-1 || name.indexOf("poplatok")!=-1 ) config = "4";
	}
	System.out.println("name="+name+" config="+config);

	if (config.equals(name+".conf")==false)
	{
		//v konfigu mozu byt zapisane hodnoty a typy pre rendering daneho pola
		//format=TYPE:CSS_CLASSES:INPUT_SIZE:INPUT_MAX_LENGTH
		String[] configData = Tools.getTokens(config, ":", true);
		if (configData.length>=1) data.setIntColumn1(Tools.getIntValue(configData[0], TYPE_TEXT));
		if (configData.length>=2) data.setColumn2(configData[1]);
		if (configData.length>=3) data.setIntColumn3(Tools.getIntValue(configData[2], data.getIntColumn3()));
		else
		{
			if (data.getIntColumn1()==TYPE_DATE) data.setIntColumn3(10);
			if (data.getIntColumn1()==TYPE_INT) data.setIntColumn3(10);
			if (data.getIntColumn1()==TYPE_FLOAT) data.setIntColumn3(10);
			if (data.getIntColumn1()==TYPE_URL || data.getIntColumn1()==TYPE_IMAGE) data.setIntColumn3(64);
		}
		if (configData.length>=4) data.setIntColumn4(Tools.getIntValue(configData[3], data.getIntColumn4()));
		else
		{
			if (data.getIntColumn1()==TYPE_DATE) data.setIntColumn4(10);
			if (data.getIntColumn1()==TYPE_URL || data.getIntColumn1()==TYPE_IMAGE) data.setIntColumn4(1024);
		}
		if (configData.length>=5) data.setColumn5(configData[4]);
	}

	if (data.getIntColumn1()==TYPE_DATE && data.getColumn2().indexOf("datepicker")==-1) data.setColumn2(data.getColumn2()+" datepicker");
	if (data.getIntColumn1()==TYPE_INT && data.getColumn2().indexOf("integer")==-1) data.setColumn2(data.getColumn2()+" integer");
	if (data.getIntColumn1()==TYPE_FLOAT && data.getColumn2().indexOf("numbers")==-1) data.setColumn2(data.getColumn2()+" numbers");
	//url nenastavim, lebo sa neda potom nastavit len relativna cesta if (data.getIntColumn1()==TYPE_URL && data.getColumn2().indexOf("url")==-1) data.setColumn2(data.getColumn2()+" url");

	data.setColumn2(data.getColumn2().trim());

	return data;
}
%><%
//vypocitaj rozumny height
int height = (24 * pageParams.getParamNames().size()) + 10;
if (Tools.isNotEmpty(propSearchKey)) height = 500;
if (height < 200) height = 200;
if (height > 500) height = 500;
%>

<% if (Tools.isNotEmpty(propSearchKey)) { %>
<div class="box_tab box_tab_thin left">
	<ul class="tab_menu" id="Tabs">
		<li class="first openFirst"><a href="#" onclick="showHideTab('1');" id="tabLink1"><iwcm:text key="components.universalComponentDialog.title"/></a></li>
		<li class="last"><a href="#" onclick="showHideTab('2');" id="tabLink2"><iwcm:text key="admin.conf_editor.edit_text"/></a></li>
	</ul>
</div>
<% } %>

<div class="tab-pane toggle_content" style="height: <%=height%>px; overflow: auto; min-width:700px;">

	<div class="tab-page" id="tabMenu1" style="display: block; min-height: 400px;">
		<table border="0" cellspacing="0" cellpadding="1">
			<form method="get" name="textForm" action="editor_component_universal.jsp">

			<%
			int index = 0;
			for (String paramName : pageParams.getParamNames())
			{
				String nameText = getText(paramName, jspFileNameKeyPrefix, prop);
				String value = pageParams.getValue(paramName, "");

				Column configData = getConfigData(paramName, value, jspFileNameKeyPrefix, prop);
			%>
			<tr>
				<td nowrap="nowrap" valign="top" style="padding-top:10px;"><%=nameText %>:</td>
				<td nowrap="nowrap">
					<%
					if (configData.getIntColumn1()==TYPE_DYNAMIC_NAME_VALUE_PAIR)
					{
						//toto je dynamicka tabulka s moznostou pridavania riadkov, format je
						//NAME:VALUE;700:20.00;1751:18.00;2501:16.00;4501:14.00;
						String nazov1 = null;
						String nazov2 = null;
						String[] nazvy = Tools.getTokens(configData.getColumn5(), ";");
						if (nazvy.length==2)
						{
							nazov1 = nazvy[0];
							nazov2 = nazvy[1];
						}
						out.println("<table id='dynamicTable"+index+"' style='dynamicNameValuePairTable'>");

						if (nazov1 != null && nazov2!=null) out.println("<tr><th>"+nazov1+"</th><th>"+nazov2+"</th></tr>");

						String[] rows = Tools.getTokens(value, ";");
						int rowIdCounter = 0;
						for (String row : rows)
						{
							String data[] = Tools.getTokens(row, ":");
							if (data.length==2)
							{
								%>
								<tr id="dynamicTable<%=index%>Row<%=rowIdCounter%>">
									<td><input type="text" class="<%=configData.getColumn2() %>" name="dynamicTable<%=index%>Row<%=rowIdCounter %>Input1" value="<%=ResponseUtils.filter(data[0])%>" size="<%=configData.getIntColumn3() %>" maxlength="<%=configData.getIntColumn4() %>"/></td>
									<td><input type="text" class="<%=configData.getColumn2() %>" name="dynamicTable<%=index%>Row<%=rowIdCounter %>Input2" value="<%=ResponseUtils.filter(data[1])%>" size="<%=configData.getIntColumn3() %>" maxlength="<%=configData.getIntColumn4() %>"/></td>
									<td><a href="javascript:dynamicTableDelRow(<%=index%>, <%=rowIdCounter%>)"><img src="/admin/skins/webjet6/images/icon/icon-delete.png"/></a></td>
								</tr>
								<%
								rowIdCounter++;
							}
						}

						%>
						<input type="hidden" name="dynamicTable<%=index %>FreeRow" value="<%=rowIdCounter%>">
						<%
						//dogeneruj dalsich 30 riadkov na mozne doplnenie
						for (int i=0; i<30; i++)
						{
							%>
							<tr id="dynamicTable<%=index%>Row<%=rowIdCounter%>" style="display: none;">
									<td><input type="text" class="<%=configData.getColumn2() %>" name="dynamicTable<%=index%>Row<%=rowIdCounter %>Input1" value="" size="<%=configData.getIntColumn3() %>" maxlength="<%=configData.getIntColumn4() %>"/></td>
									<td><input type="text" class="<%=configData.getColumn2() %>" name="dynamicTable<%=index%>Row<%=rowIdCounter %>Input2" value="" size="<%=configData.getIntColumn3() %>" maxlength="<%=configData.getIntColumn4() %>"/></td>
									<td><a href="javascript:dynamicTableDelRow(<%=index%>, <%=rowIdCounter%>)"><img src="/admin/skins/webjet6/images/icon/icon-delete.png"/></a></td>
								</tr>
							<%
							rowIdCounter++;
						}

						out.println("</table>");
						%>
						<a href="javascript:dynamicTableAddRow(<%=index%>)"><img src="/admin/skins/webjet6/images/icon/icon-plus.png"/></a>
						<input type="hidden" name="param<%=index %>" value="TYPE_DYNAMIC_NAME_VALUE_PAIR">
						<input type="hidden" name="dynamicTable<%=index %>TotalRows" value="<%=rowIdCounter%>">
						<%
					}
					else if (configData.getIntColumn1()==TYPE_SELECT)
					{
						%>
						<select class="<%=configData.getColumn2() %>" name="param<%=index %>">
							<%
							String[] selectValues = Tools.getTokens(configData.getColumn5(), ";");
							for (String selectValue : selectValues )
							{
								out.print("<option value=\""+ ResponseUtils.filter(selectValue)+"\"");
								if (selectValue.equals(value)) out.print(" selected='selected'");
								out.println(">"+ ResponseUtils.filter(selectValue)+"</option>");
							}
							%>
						</select>
						<%
					}
					else if (configData.getIntColumn1()==TYPE_GROUP_ID_SELECT)
					{
						%>
						<input type="text" id="param<%=index%>id" class="<%=configData.getColumn2() %>" name="param<%=index %>" size="<%=configData.getIntColumn3() %>" maxlength="<%=configData.getIntColumn4() %>"  value="<%=value%>">
						<input type="button" class="button50" name="groupSelect<%=index %>" value="<iwcm:text key="groupedit.change"/>" onClick='popupFromDialog("<iwcm:cp/>/admin/grouptree.jsp?inputId=param<%=index%>id", 500, 500);'>
						<%
					}
					else if (configData.getIntColumn1()==TYPE_SELECT_USERGROUPS_NAMES)
					{
						%>
						<select class="<%=configData.getColumn2() %>" name="param<%=index %>">
							<%
								List<UserGroupDetails> groups = UserGroupsDB.getInstance().getUserGroups();

								for (UserGroupDetails selectValue : groups )
								{
								    if (Tools.isNotEmpty(configData.getColumn5()) && !selectValue.getUserGroupName().startsWith(configData.getColumn5())) continue;
									out.print("<option value=\""+ ResponseUtils.filter(selectValue.getUserGroupName())+"\"");
									if (selectValue.equals(value)) out.print(" selected='selected'");
									out.println(">"+ ResponseUtils.filter(selectValue.getUserGroupName())+"</option>");
								}
							%>
						</select>
				<%
				}
					else
					{
						%>
						<input type="text" class="<%=configData.getColumn2() %>" name="param<%=index %>" size="<%=configData.getIntColumn3() %>" maxlength="<%=configData.getIntColumn4() %>" value="<%=value%>">
						<%
						//zrendruj pripadne ikonky
						if (configData.getIntColumn1()==TYPE_URL) {%><input type="button" class="button" value="<iwcm:text key="button.select"/>" onclick="openLinkDialogWindow('textForm', 'param<%=index %>', null, null)" /><%}
						if (configData.getIntColumn1()==TYPE_IMAGE) {%><input type="button" class="button" value="<iwcm:text key="button.select"/>" onclick="openImageDialogWindow('textForm', 'param<%=index %>', null)" /><%}
					}
					%>
				</td>
			</tr>
			<%
				index++;
			}
			%>


			</form>
		</table>
		<% if (Tools.isNotEmpty(propSearchKey)) { %>
			<div style="width:800px"></div>
		<%} %>
	</div>

	<% if (Tools.isNotEmpty(propSearchKey)) { %>
	<div class="tab-page tab-page-iframe" id="tabMenu2">
		<iframe src="/admin/prop_search.jsp?search=yes&text=<%=propSearchKey %>" width="100%" height="380<%--=height-20--%>" frameborder="0"></iframe>
	</div>
	<% } %>

<script type='text/javascript'>

	var form = document.forms["textForm"];

	function dynamicTableAddRow(index)
	{
		var totalRows = parseInt(form.elements["dynamicTable"+index+"TotalRows"].value);
		var freeRow = parseInt(form.elements["dynamicTable"+index+"FreeRow"].value);
		if (freeRow >= totalRows)
		{
			window.alert("<iwcm:text key="components.universalComponentDialog.dynamicTable.noFreeRows"/>");
			return;
		}
		$("#dynamicTable"+index+"Row"+freeRow).show();
		form.elements["dynamicTable"+index+"FreeRow"].value = freeRow+1;
	}

	function dynamicTableDelRow(index, row)
	{
		form.elements["dynamicTable"+index+"Row"+row+"Input1"].value = "";
		form.elements["dynamicTable"+index+"Row"+row+"Input2"].value = "";
		$("#dynamicTable"+index+"Row"+row).hide();
	}

	function getParamValueDynamicValuePair(index)
	{
		var value = "";
		var totalRows = form.elements["dynamicTable"+index+"TotalRows"].value;
		//window.alert("totalRows="+totalRows);

		var rowCounter = 0;
		for (rowCounter = 0; rowCounter<totalRows; rowCounter++)
		{
		   var input1 = form.elements["dynamicTable"+index+"Row"+rowCounter+"Input1"].value;
		   var input2 = form.elements["dynamicTable"+index+"Row"+rowCounter+"Input2"].value;

		   if (input1!="" && input2!="")
			{
			   value += input1+":"+input2+";";
			}
		}
		//window.alert(value);
		return value;
	}

	function getParamValue(index)
	{
		var value = form.elements["param"+index].value;
		if ("TYPE_DYNAMIC_NAME_VALUE_PAIR"==value) value = getParamValueDynamicValuePair(index);

		if (value.indexOf(",")!=-1) value = "\""+value+"\"";

		return value;
	}



	function Ok()
	{
		<%
		out.print("oEditor.FCK.InsertHtml(\"!INCLUDE(");
		out.print(jspFileName);

		index = 0;
		for (String paramName : pageParams.getParamNames())
		{
			out.print(", "+paramName+"=\"+getParamValue("+index+")+\"");
			index++;
		}

		out.println(")!\");");
		%>

		return true ;
	}

	<% if (index==0) { %>
	//nemame ziadne parametre
    showHideTab('2');
    $("#tabLink1").hide();
	<% } %>

</script>

<jsp:include page="/components/bottom.jsp"/>
