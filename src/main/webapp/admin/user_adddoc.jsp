<%@page import="sk.iway.iwcm.Tools"%>
<%@page import="sk.iway.iwcm.Constants"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.doc.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<iwcm:checkLogon admin="true"/>
<%@page import="sk.iway.iwcm.tags.support_logic.ResponseUtils"%>
<iwcm:present parameter="docId">
<%
int docId = -1;
try
{
   docId = Integer.parseInt(Tools.getRequestParameter(request, "docId"));
}
catch (Exception ex)
{
   sk.iway.iwcm.Logger.error(ex);
}

String name = "";

if (docId > 0)
{
   DocDB docDB = DocDB.getInstance();
   GroupsDB groupsDB = GroupsDB.getInstance();
   DocDetails doc = docDB.getDoc(docId);
   if (doc!=null)
   {
      name = groupsDB.getPath(doc.getGroupId()) + "/" + doc.getTitle();
   }
}
%>

<html><head></head><body>
<script language="JavaScript">
window.parent.doOKDocument(<%=docId%>, "<%=ResponseUtils.filter(name)%>");
</script>
<style>
body { overflow: auto }
</style>
</body></html>


</iwcm:present>
<iwcm:notPresent parameter="docId">
<html>
<head>
   <title><iwcm:text key="editor.link.insert_link"/></title>

   <script type="text/javascript" src="<%=request.getContextPath()%>/admin/scripts/common.jsp"></script>
<%
//nafejkujeme ze mame jquery aj ui
Tools.insertJQueryUI(pageContext, "autocomplete");
//fajkujeme ajax_support.js pre autocomplete tag
request.setAttribute("ajaxSupportInserted", true);
%>
<iwcm:combine type="js" set="adminJqueryJs" />
<script type="text/javascript" src="<%=request.getContextPath()%>/admin/scripts/modalDialog.js"></script>

   <script language="JavaScript" src="scripts/common.jsp"></script>
   <script language="JavaScript1.1">
   <!--

   function resizeDialog(width, height)
   {
	   	resizeToInner(width, height);

	   	//nastav vysku centralneho riadku (len pre FF)
	   	var centralRow = document.getElementById("dialogCentralRow");
	   	if (centralRow != null && navigator.userAgent.indexOf("Gecko")!=-1)
	   	{
	   		centralRow.style.height = (height - 125) + "px";
	   	}
   }

   function selectDoc(docLink)
   {
      //window.alert("doclink="+docLink);
      index = docLink.indexOf("docid=")
      if (index>0)
      {
         document.uploadFileForm.docid.value=docLink.substring(index+6);
      }
   }

   function selectDocId(docId)
   {
      document.uploadFileForm.docid.value=docId;
   }

   function selectLinkFile(link)
   {

   }

   function selectLink(link)
   {

   }

   function doOK()
   {
   	if (window.opener && window.opener.setStandardDoc)
		{
			window.opener.setStandardDoc(document.uploadFileForm.docid.value);
			window.close();
			return;
		}
   		console.log("user_adddoc.jsp?docId="+document.uploadFileForm.docid.value);
      	docLink.location.href = "user_adddoc.jsp?docId="+document.uploadFileForm.docid.value;
   }

   function doOKDocument(docid, name)
   {
      var ret = new Array(2);
      ret[0] = docid;
      ret[1] = name;
      window.returnValue = ret;

      if(window.opener && window.opener.setPage)
	   {
	   	window.opener.setPage(ret);
	   }
      window.close();
   }

   function doCancel(docid, name)
   {
      var ret = new Array(1);
      ret[0] = -1;
      window.returnValue = ret;
      window.close();
   }
   function GetE( elementId )
	{
		return document.getElementById( elementId )  ;
	}
   function setPathSelector(pathSelector)
	{
		pathSelectorSelect = GetE("pathSelectorSelect");
		var spacer = "";
		var path = "";
		for (i=0; i<pathSelector.length; i++)
		{
		   //window.alert(pathSelector[i][0]+"->"+pathSelector[i][1]);
		   pathSelectorSelect.options[0] = new Option(pathSelector[i][1], pathSelector[i][0], true);
		   pathSelectorSelect.options[i+1] = new Option(spacer+pathSelector[i][1], pathSelector[i][0]);
		   spacer = spacer + "\u00a0\u00a0";

		   if ("/" != pathSelector[i][1])
		   {
		   	path = path + "/" + pathSelector[i][1];
		   }
		}
		i++;
		for (j=i; j<pathSelectorSelect.options.length; j++)
		{
			pathSelectorSelect.options[j] = null;
		}
		//window.alert(path);
		if (path.length > 50)
		{
		   path = "..." + path.substring(path.length - 45);
		}
		pathSelectorSelect.options[0].text = path;
		pathSelectorSelect.selectedIndex = 0;
	}
	function setIframeContent(url)
	{
		var iframeElement = document.getElementById("docLink");
		if (iframeElement != null)
		{
			iframeElement.src = url;
		}
	}
	function pathSelectorChange(select)
	{
		var url = select.options[select.selectedIndex].value;
		//window.alert(url);
		//set to default state
		setIframeContent("<iwcm:cp/>/admin/"+url);
	}
	function pathSelectorUp()
	{
		pathSelectorSelect = GetE("pathSelectorSelect");
		if (pathSelectorSelect.options.length > 2)
		{
			pathSelectorSelect.selectedIndex = pathSelectorSelect.options.length - 2;
			pathSelectorChange(pathSelectorSelect);
		}
	}
	function addItemToSelect(mySelect)
	{
		 if (mySelect.value=="popup")
		 {
			 el = document.getElementById("popupDim");
			 if (el!=null)
			 {
				 el.style.display = "block";
			 }
		 }
		 else
		 {
			 el = document.getElementById("popupDim");
			 if (el!=null)
			 {
				 el.style.display = "none";
			 }
		 }

		 if (mySelect.options[mySelect.selectedIndex].value=="-2")
		 {
			 myValue = window.prompt("<iwcm:text key="editor.link.enter_target"/>", "")
			 if (myValue!="" && myValue!="undefined")
			 {
				 var optionName = new Option(myValue, myValue, true, true)
				 mySelect.options[mySelect.length] = optionName;
			 }
		 }
	}
	resizeDialog(660, 480);

   //-->
   </script>
   <link type="text/css" rel="stylesheet" href="/components/cmp.css" media="all">
   <!-- mho edit - výmena za webjet8/css/fck_dialog.css
   <link type="text/css" rel="stylesheet" href="/admin/FCKeditor/editor/skins/webjet/fck_dialog.css" media="all">
   -->
   <link rel="stylesheet" href="/admin/skins/webjet8/css/fck_dialog.css" />

   <link type="text/css" rel="stylesheet" media="screen" href="/admin/css/tabpane-luna.css">
</head>
<body bgcolor="#FFFFFF">


<form name="uploadFileForm" style="margin-bottom:0;">
<table class="wjDialogHeaderTable" border="0" cellspacing="0" cellpadding="0" width="100%" height="100%" style="height: 100%;">
	<tbody>
		<tr id="headerTopRow" height="65">
			<td class="header">
				<h1><iwcm:text key="editor.link.select_page_on_server"/></h1>
			</td>
			<td class="header headerImage" nowrap="nowrap" valign="middle" align="right">
				<div style="background-image: url(/components/user/editoricon.gif);">&nbsp;</div>
			</td>
		</tr>
		<tr height="*">
			<td class="main mainTab" valign="top" colspan="2">
				<div class="tab-pane toggle_content">
				   <table>
				      <tr>
				         <td width="660" height="360" valign="top">
				         	<iframe name="docLink" id="docLink" width="0" height="0" align="left" marginwidth="0" marginheight="0" scrolling="no">
                 				Tvoj browser nepodporuje IFRAME
         					</iframe>
							<script type="text/javascript">
								var wjCkMapping = [
									{wj: 'txtUrl', tab:'info', ck:'url'},
								];
							</script>

							<script type="text/javascript">
									<!--
									function selectLink(link)
									{
										$('.PopupButtons input[name=docid]').val(link);
									}
									//-->
									</script>

									<%
									//je to tu takto, pretoze inak to spomalovalo pracu v editore
									request.setAttribute("uploadIcon", "wjIconBig-uploadDoc");
									pageContext.include("/admin/elFinder/jsinclude.jsp");
									%>

												         <%
							String systemPagesMyPages = Constants.getString("systemPagesMyPages");
							%>
								<script>
									var lastDocId = -1;
									var lastGroupId = -1;

									function initializeElfinder()
									{
										var docId = -1;
										var groupId = <%=systemPagesMyPages %>;

										$('#finder').elfinder({
											url : '<iwcm:cp/>/admin/elfinder-connector/',
											width: '100%',
											height: 350,
											resizable: false,
											rememberLastDir: false,
											requestType: 'post',
											tree: {
												openRootOnLoad : true
											},
											customData : {volumes : "pages", docId: docId, groupId: groupId},
											getFileCallback : function(files, fm) {
												 if(files.mime != 'directory') {
												 	selectLink(files.url.split(":").pop());
												 }
											},
											handlers : {
							                    select : function(event, elfinderInstance) {
							                        var selected = event.data.selected;

							                        var files = elfinderInstance.selectedFiles();
							                        if (files != null && files.length == 1) {
							                        	if(files[0].mime != 'directory') {
							                        		console.log(files[0]);
							                            	selectLink(files[0].url.split(":").pop());
							                        	}
							                        }
							                    }
							                },
											lang : '<%=sk.iway.iwcm.i18n.Prop.getLngForJavascript(request)%>',
											ui: ['tree', 'path', 'stat'],
										});

										elFinderInitialized = true;

										/*
										LPA zakomentovane, pokial je len jeden node WEB stranky tak ho pri nacitani zavrie

										//zvol prvy element v "Aktualna stranka"
										setTimeout(function() {
											$("span.elfinder-navbar-root").each(function(i, v)
											{
												if ($(this).attr("id").indexOf("nav-iwcm_doc_group_volume")==0)
												{
													$(this).children("span.elfinder-navbar-arrow").trigger("click");
												}
												$("span.elfinder-navbar-dir.ui-droppable:eq(1)").trigger("click");
											});
										}, 200);
										*/
									}
								</script>

								<div id="finder"><iwcm:text key="divpopup-blank.wait_please"/></div>

				         </td>
				      </tr>
				   </table>
			   </div>
			</td>
		</tr>
		<tr id="buttonsBottomRow">
	       <td class="PopupButtons" colspan="2" align="right">
	       <table>
		       	<tr>
			       	<td align="right">
			  			docid: <input type="text" name="docid" size=5 value=''>
				        <input type="button" name="bOK" value="<iwcm:text key="button.ok"/>" class="button" onClick="doOK()">
				        <input type="button" name="bCANCEL" value="<iwcm:text key="button.cancel"/>" onClick="doCancel();" class="button" id="btnCancel">
			       	</td>
		       	</tr>
	       </table>

	       </td>
	    </tr>
	</tbody>
</table>
</form>
</body>
</html>
</iwcm:notPresent>
