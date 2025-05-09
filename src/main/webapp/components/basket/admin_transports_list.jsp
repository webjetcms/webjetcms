<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.tags.CurrencyTag"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="java.util.List"%>
<%@page import="org.apache.struts.util.ResponseUtils"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,java.io.*"%>
<%@page import="sk.iway.iwcm.components.basket.rest.EshopService"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<iwcm:checkLogon admin="true" perms="cmp_basket"/>
<%@ include file="/admin/layout_top.jsp" %>
<style type="text/css">
body { background-image: none !important; background-color: white !important; }
table.newsTable tr td  { border-bottom: 1px dotted #a5a6a8; padding-bottom: 16px; padding-top: 16px; padding-left: 8px; padding-right: 8px; }
table.newsTable input.textInput { width: 800px; }
table.newsTable input.textInputWithImage { width: 770px; }
table.newsTable textarea.textInput { width: 800px; height: 80px; color: #999999; font-size: 11px; }
div.imageDiv { cursor: pointer; background-repeat: no-repeat; background-position: 2px 2px; margin-bottom: 6px; }
</style>
<%
//ADMIN stranka pre zobrazenie zoznamu noviniek v inline editacii

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

String includeText = request.getParameter("include");
//out.println(includeText);
if (Tools.isEmpty(includeText))
{
	out.println("Error parsing params");
	return;
}

request.setAttribute("includePageParams", includeText);

PageParams pageParams = new PageParams(request);

String groupIds = pageParams.getValue("groupIds", "");

//mame to v takomto formate, takze to convertneme
groupIds = groupIds.replace('+', ',');

//ak je nastavene na true beru sa do uvahy aj podadresare
boolean expandGroupIds = false;pageParams.getBooleanValue("expandGroupIds", true);

String perexGroup = pageParams.getValue("perexGroup", "");
//mame to v takomto formate, takze to convertneme
perexGroup = perexGroup.replace('+', ',');




boolean paging = pageParams.getBooleanValue("paging", true);
int pageSize = 1000;
int maxCols = pageParams.getIntValue("cols", 1);
// image moznosti - none, top, bottom
String image = pageParams.getValue("image", "none");
String pagingStyle = pageParams.getValue("pagingStyle", "top");
if (pagingStyle.equals("no")) {
	paging = false;
}
String newsName = pageParams.getValue("name", "");
String newsStyle = pageParams.getValue("style", "");
boolean perex = pageParams.getBooleanValue("perex", true);
boolean date = pageParams.getBooleanValue("date", false);
boolean place = pageParams.getBooleanValue("place", false);

GroupDetails gd = new GroupDetails();
GroupsDB groupsDB = GroupsDB.getInstance();


List<DocDetails> docDetails = EshopService.getInstance().getModeOfTransports();
request.setAttribute("novinky", docDetails);

Prop prop = Prop.getInstance(request);
GroupDetails modeOfTransportGroup = groupsDB.getCreateGroup("/System/ModeOfTransport");
if (modeOfTransportGroup!=null)
{

	List<DocDetails> novinky = (List<DocDetails>)request.getAttribute("novinky");
	DocDetails newDoc = new DocDetails();
	newDoc.setDocId(0);
	newDoc.setGroupId(modeOfTransportGroup.getGroupId());
	newDoc.setTitle(prop.getText("groupslist.enter_page_name"));
	newDoc.setExternalLink("");
	novinky.add(newDoc);
}

%>
<script type="text/javascript" src="/components/basket/jscript.jsp"></script>
<script type="text/javascript">
	function selectImage(docId)
	{
		openImageDialogWindow("docform"+docId, "perexImage", null);
	}
	function perexImageBlur(input)
	{
		var div = $("#img"+input.form.name.substring(7));
		div.css("background-image", "url(<iwcm:cp/>/thumb"+input.value+"?w=100&h=100&ip=5)");
	}

	function deleteTransport(id)
	{
		//var data;
		$.ajax({
			url: "/components/basket/admin_delete_ajax.jsp",
			data: 'docId='+id,
			type: "POST",
			dataType: "html",

			success: function( html )
			{
				if (html.indexOf("true") == -1)
				{
					window.alert(html);
				}
				//$("#loader"+docId).hide();
				if (html.indexOf("reload")!=-1)
				{
					window.location.href="admin_transports_list.jsp?rnd=<%=Tools.getNow()%>&include=<%=Tools.URLEncode(includeText) %>";
				}
			},

			error: function( xhr, status ) {
		        alert( "<iwcm:text key="dmail.subscribe.db_error"/>");
		       // $("#loader"+docId).hide();
		    }
		});
	}

	function saveChanges(button)
	{
		var docId = button.form.docid.value;
		$("#loader"+docId).show();
		var myForm = $("#iddocform"+docId);
		var ajaxData = myForm.serialize();
		$.ajax({
			url: "/components/basket/admin_save.jsp",
			data: ajaxData,
			type: "POST",
			dataType: "html",

			success: function( html )
			{
				if (html.indexOf("OK")!=0)
				{
					window.alert(html);
				}
				$("#loader"+docId).hide();
				if (html.indexOf("reload")!=-1)
				{
					window.location.href="admin_transports_list.jsp?rnd=<%=Tools.getNow()%>&include=<%=Tools.URLEncode(includeText) %>";
				}
			},

			error: function( xhr, status ) {
		        alert( "<iwcm:text key="dmail.subscribe.db_error"/>");
		        $("#loader"+docId).hide();
		    }
		});

	}

	function addNewItemClick()
	{
		$("#rowAddNewItemButton").hide();
		$("#rowAddNewItem").show();
	}
</script>

<iwcm:notEmpty name="novinky">
	<table border="0" cellspacing="0" cellpadding="1" class="newsTable">
	<logic:iterate id="doc" name="novinky" type="sk.iway.iwcm.doc.DocDetails">
		<% if (doc.getDocId()<1) { %>
		<tr id="rowAddNewItemButton">
			<td colspan="2">
				<input type="button" class="button150" onclick="addNewItemClick()" value="<iwcm:text key="components.basket.mode_of_transport.add_new"/>"/>
			</td>
		</tr>
		<% } %>
		<form name="docform<%=doc.getDocId()%>" id="iddocform<%=doc.getDocId()%>">
		<tr<% if (doc.getDocId()<1) out.print(" id=\"rowAddNewItem\" style=\"display: none;\""); %>>
			<td valign="top" align="center"><% String perexImage = doc.getPerexImage();

				if (Tools.isEmpty(perexImage)) perexImage = "/components/news/admin_imgplaceholder.png";
				else perexImage = "/thumb"+perexImage;
			%>
			<!-- Obrazok -->
<%-- 				<div class="imageDiv" id="img<%=doc.getDocId()%>" style="background-image:url(<%=ResponseUtils.filter(perexImage) %>?w=100&h=100&ip=6)" onclick="selectImage(<%=doc.getDocId()%>)"> --%>
<!-- 					<img src="/components/news/admin_imgbg.png"/> -->
<!-- 				</div>												 -->
			</td>
			<td>
				<iwcm:text key="components.basket.mode_of_transport"/>:
				<br/>
				<input type="text" name="title" value="<%=(doc.getDocId()<1)? prop.getText("components.basket.mode_of_transport.doc_title") : ResponseUtils.filter(doc.getTitle())%>" class="textInput" size="100" maxlength="255"/>
				<br/>
				&nbsp;
				<br/>
				<iwcm:text key="components.catalog.price_without_vat"/> <input type="text" name="fieldK" value="<%=ResponseUtils.filter(doc.getFieldK())%>" class="poleMini" size="10" maxlength="10" onchange="calculateWithVat(this.form.name)" onkeyup="calculateWithVat(this.form.name)" onblur="calculateWithVat(this.form.name)"/>
				<iwcm:text key="components.basket.invoice.currency"/> <input type="text" name="fieldJ" value="<%=ResponseUtils.filter(doc.getFieldJ())%>" class="poleMini" style="width: 40px !important;" size="3" maxlength="3"/>
				<iwcm:text key="components.catalog.vat"/> <input type="text" name="fieldL" value="<%=ResponseUtils.filter(doc.getFieldL())%>" class="poleMini" style="width: 40px !important;" size="4" maxlength="4" onchange="calculateWithVat(this.form.name)" onkeyup="calculateWithVat(this.form.name)" onblur="calculateWithVat(this.form.name)"/>
				<iwcm:text key="components.catalog.price_with_vat"/> <input type="text" name="priceWithVat" value="<%=ResponseUtils.filter(CurrencyTag.formatNumber(doc.getPriceVat())) %>" class="poleMini" size="10" maxlength="10" onchange="calculateWithoutVat(this.form.name)" onkeyup="calculateWithoutVat(this.form.name)" onblur="calculateWithoutVat(this.form.name)"/>
				<iwcm:text key="components.basket.old_price"/> <input type="text" name="fieldM" value="<%=ResponseUtils.filter(doc.getFieldM())%>" class="poleMini" size="10" maxlength="10"/>

				<div style="padding-top: 12px;">
					<%--<input onclick="selectImage(<%=doc.getDocId()%>)" type="button" value="<iwcm:text key="components.news.selectImage"/>" class="button50 buttonNotImportant"/>--%>
					<input onclick="saveChanges(this)" type="button" value="<iwcm:text key="button.save"/>" class="button50"/>
					<input <%=(doc.getDocId() <= 0)? "type=\"hidden\"":"type=\"button\"" %> onclick="deleteTransport(<%=doc.getDocId()%>)"  value="<iwcm:text key="user.button.delete"/>" class="button50"/>

					<img src="/admin/images/ajax-loader.gif" id="loader<%=doc.getDocId()%>" style="display: none;"/>
					<input type="hidden" name="docid" value="<%=doc.getDocId()%>"/>
					<input type="hidden" name="groupid" value="<%=doc.getGroupId()%>"/>
					<input type="text" name="perexImage" value="<%=ResponseUtils.filter(doc.getPerexImage())%>" onblur="perexImageBlur(this)" style="display: none;"/>
				</div>
			</td>
		</tr>
		</form>
	</logic:iterate>
	</table>
</iwcm:notEmpty>
<iwcm:empty name="novinky">
	<div class="no_news"><iwcm:text key="components.news.nonews" /></div>
</iwcm:empty>

<%@ include file="/admin/layout_bottom.jsp" %>