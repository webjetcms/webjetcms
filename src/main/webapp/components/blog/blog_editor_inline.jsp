<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.doc.PerexGroupBean"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.components.blog.BlogAction"%>
<%@page import="sk.iway.iwcm.doc.DocDetails"%>
<%@page import="org.apache.struts.util.ResponseUtils"%>
<%@page import="java.io.File"%>
<%@page import="sk.iway.iwcm.doc.GroupDetails"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="cmp_blog"/><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

request.setAttribute("packagerEnableControljs", Boolean.FALSE);

int editorWidth = 600; //TODO: nejako rozumnejsie urcit

boolean canEdit = BlogAction.isEditable(request);
if (canEdit==false) return;

Identity user = (Identity) session.getAttribute(Constants.USER_KEY);

String myData = (String)request.getAttribute("doc_data_blog");
if ("1".equals(request.getParameter("new"))==false && myData != null && myData.indexOf("!INCLUDE(/components/blog/blog_")!=-1)
{
	out.println(myData);
	%><iwcmwrite name="doc_data_blog"/><%
	return;
}
%>

<LINK rel='stylesheet' href='/admin/css/editor.css'>
<script type="text/javascript">

/********** SCRIPTY POTREBNE PRE EDITOR (MULTILANG) *************/

var oFCKeditor = null;
var lastEditorField = null;
function setEditorValue(radioField, name)
{
   var lng = radioField.value;
   var field = document.getElementById(name+"_"+lng); // eval("radioField.form."+name+"_"+lng);

   //najskor uloz to co treba (ak treba ;-)
   saveEditorValue();

   //window.alert(field.value);
   var oEditor = FCKeditorAPI.GetInstance('data') ;

	// Set the editor contents (replace the actual one).
	oEditor.SetHTML(field.value) ;

	lastEditorField = field;
}

//nezabudni to volat pri submite formularu!!!
function saveEditorValue()
{
   if (lastEditorField == null)
   {
      return;
   }

   var oEditor = FCKeditorAPI.GetInstance('data') ;
   lastEditorField.value = oEditor.GetHTML(true);
}
function FCKeditor_OnComplete( editorInstance )
{
	//nastav default hodnotu

}
function setEditorMode(isVisual)
{
	var oEditor = FCKeditorAPI.GetInstance('data') ;
	oEditor.SwitchEditMode();
}

/************ SCRIPTY OSTATNE ************/
function openImageDialog(button, fieldName, requestedImageDir)
{
	var url = '/admin/FCKeditor/editor/dialog/fck_image.html?setfield='+button.form.name+'.elements["'+fieldName+'"]&requestedImageDir='+requestedImageDir;
   WJDialog.OpenDialog( 'WJDialog_Image' , "Image", url, 620, 500 ) ;
}
</script>

<%
if (request.getParameter("btnSave")!=null)
{
   pageContext.include("/sk/iway/iwcm/components/blog/Blog.action");
}

if (request.getParameter("isNew")==null)
{
	if (session.getAttribute("pageSavedToPublic")!=null)
	{
		request.setAttribute("pageSavedToPublic", "true");
		session.removeAttribute("pageSavedToPublic");
	}

	if (session.getAttribute("pageSaved")!=null)
	{
		request.setAttribute("pageSaved", "true");
		session.removeAttribute("pageSaved");
	}

	if (session.getAttribute("approveByUsers")!=null)
	{
		request.setAttribute("approveByUsers", (String)session.getAttribute("approveByUsers"));
		session.removeAttribute("approveByUsers");
	}

	if (session.getAttribute("pagePublishDate")!=null)
	{
		request.setAttribute("pagePublishDate", (String)session.getAttribute("pagePublishDate"));
		session.removeAttribute("pagePublishDate");
	}

	if (session.getAttribute("updatedDocs")!=null)
	{
		request.setAttribute("updatedDocs", session.getAttribute("updatedDocs"));
		session.removeAttribute("updatedDocs");
	}

	if (session.getAttribute("allreadyUsedVirtualPathDocId")!=null)
	{
		request.setAttribute("allreadyUsedVirtualPathDocId", session.getAttribute("allreadyUsedVirtualPathDocId"));
		session.removeAttribute("allreadyUsedVirtualPathDocId");
	}
}
%>

<logic:present name="approveByUsers">
	<div class="topNotify">
		<div class="notifyText">
			<b><iwcm:text key="editor.approveRequestGet"/></b>:
			<iway:request name="approveByUsers" />
		</div>
	</div>
</logic:present>

<logic:notPresent name="approveByUsers">
	<logic:present name="pageSavedToPublic">
		<div class="topNotify topNotifyOk">
			<div class="notifyText"><iwcm:text key="editor.pageSavedToPublic"/></div>
		</div>
	</logic:present>

	<logic:present name="pageSaved">
		<div class="topNotify topNotifyOk">
			<div class="notifyText"><iwcm:text key="editor.pageSaved"/></div>
		</div>
	</logic:present>

	<logic:present name="pagePublishDate">
		<div class="topNotify topNotifyOk">
			<div class="notifyText">
				<iwcm:text key="editor.publish.pagesaved" />
				<iway:request name="pagePublishDate"/>
			</div>
		</div>
	</logic:present>
</logic:notPresent>

<stripes:useActionBean var="actionBean" beanclass="sk.iway.iwcm.components.blog.BlogAction"/>
<%
DocDetails actualDoc = (DocDetails)request.getAttribute("docDetails");
if (actualDoc == null) return;

Prop prop = Prop.getInstance(request);

String data = "<p>&nbsp;</p>";
if ("1".equals(request.getParameter("new")))
{
	actionBean.getEf().setTitle(prop.getText("editor.title"));
	actionBean.getEf().setTempId(actualDoc.getTempId());
	actionBean.getEf().setAvailable(true);
}
else
{
	actionBean.getEf().setTitle(actualDoc.getTitle());
	actionBean.getEf().setTempId(actualDoc.getTempId());
	actionBean.getEf().setAvailable(actualDoc.isAvailable());
	actionBean.getEf().setPerexGroupString(actualDoc.getPerexGroupIdsString());
	actionBean.getEf().setFieldA(actualDoc.getFieldA());

	//kontrola ci tam nie je nejaky include aby nemohol editovat hlavnu stranku adresara
	data = BlogAction.checkData((String)request.getAttribute("doc_data_blog"));
}
%>
<iwcm:stripForm action="/components/blog/blog_save.jsp" beanclass="sk.iway.iwcm.components.blog.BlogAction" name="editorForm" method="post">

    <input type="hidden" name="docid" value="<%=Tools.getDocId(request)%>" />
    <input type="hidden" name="groupid" value="<%=((GroupDetails)request.getAttribute("pageGroupDetails")).getGroupId()%>" />

    <link rel='stylesheet' media='screen' type='text/css' href='/components/blog/blog.css'/>
	 <style type="text/css">
	 	div.blogEditorToolbar { width:<%=(editorWidth-2)%>px; }
	 </style>

	<!-- Toolbars -->
	<div class="blogEditorToolbar">
		<table cellspacing="0" cellpadding="0">
			<tr>
				<td id="wjTlacitka" class="wjTlacitka" style="width:22px;"></td>
				<td class="wjTlacitka" style="width: auto; margin: 0px;">
					<% if (user.isEnabledItem("addPage")){ %>
							<a href="?blogEdit=1&new=1" title="<iwcm:text key="components.wiki.newPage"/>" class="TB_Button_Off"><img src="/admin/FCKeditor/editor/skins/webjet/toolbar/newpage.gif" class="TB_Button_Image"></a>
					<%} %>
					<a href="<%=PathFilter.getOrigPath(request)%>" title="<iwcm:text key="components.wiki.showOriginalPage"/>" class="TB_Button_Off"><img src="/admin/FCKeditor/editor/skins/webjet/toolbar/preview.gif" class="TB_Button_Image"></a>
				</td>
				<td>
					<label>
						<iwcm:text key="editor.title"/>:
						<stripes:text name="ef.title" maxlength="256" size="35" style="width:180px;" />
					</label>

					<%
					List<PerexGroupBean> perexGroups = BlogAction.getPerexGroups(actualDoc.getGroupId());
					if (perexGroups.size()>0)
					{
						pageContext.setAttribute("perexGroups", perexGroups);
					%>
						<iwcm:text key="components.blog.articles.topic"/>:
						<stripes:select name="topicId">
							<stripes:options-collection collection="${perexGroups}" label="perexGroupName" value="perexGroupId" />
						</stripes:select>
					<%

					}
					%>

					<label>
						<iwcm:text key="editor.show"/>:
						<stripes:checkbox name="ef.available"/>
					</label>

					<label>
						<iwcm:text key="components.forum.title"/>:
						<stripes:checkbox name="discussion"/>
					</label>

				</td>
			</tr>
		</table>
	</div>

	<textarea name="data" style="width: 500px; height: 300px"><%=ResponseUtils.filter(data) %></textarea>
	 <script type="text/javascript">
		window.CKEDITOR_BASEPATH = "/admin/skins/webjet8/ckeditor/dist/";
	</script>
	<script type="text/javascript" src="/admin/skins/webjet8/ckeditor/dist/ckeditor.js?t=f3e1"></script>

	<script type="text/javascript">
	function docform_submit_ajax(publish, oForm, FCK)
	{
		oForm.data.value = FCK.GetXHTML(true);
		oForm.publish.value = publish;
		oForm.submit();
	}

	window.onload = function()
	{
	  oFCKeditor = new FCKeditor( 'data') ;
	  oFCKeditor.BasePath = "/admin/FCKeditor/";
	  oFCKeditor.Height = "400";
	  oFCKeditor.Width = "<%=editorWidth%>";

	  <%
	  if ("cz".equals(lng)) lng = "cs";
	  else if (lng.length()==0) lng = "sk";
	  System.out.println("---> LNG: " + lng);

	  String cssStyle = Constants.getString("editorPageCss");
	  File f = new File(sk.iway.iwcm.Tools.getRealPath("/css/editor.css"));
	  if (f.exists())
	  {
	     cssStyle +=",/css/editor.css";
	  }
	  %>
	  oFCKeditor.Config["EditorAreaCSS"] = "<%=cssStyle%>";
	  //TODO: uz neexistuje oFCKeditor.Config["StylesXmlPath"] = '/admin/FCKeditor/styles.do';
	  oFCKeditor.Config["DefaultLanguage"] = "<%=lng%>";
	  oFCKeditor.Config["EnableXHTML"] = false;
	  oFCKeditor.ToolbarSet = "Blog";
	  oFCKeditor.Config[""] = "";
	  oFCKeditor.ReplaceTextarea();
	}
	</script>
	<input type="hidden" name="publish" value=""/>
	<input type="hidden" name="btnSave" value="true"/>
	<% if ("1".equals(request.getParameter("new"))) { %><input type="hidden" name="isNew" value="true"/><% } %>
</iwcm:stripForm>

