<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.doc.*, java.util.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<iwcm:checkLogon admin="true" perms="menuWebpages"/>
<% 
	request.setAttribute("cmpName", "htmlbox"); 

   GroupsDB groupsDB = GroupsDB.getInstance();
   GroupDetails grpDet = groupsDB.getGroup("htmlbox");
   DocDB docDB = DocDB.getInstance();            
   List docList = null; 
   if(grpDet != null)
   {            	
   	docList = docDB.getDocByGroup(grpDet.getGroupId());
   	if(docList.size() > 0)
   		request.setAttribute("docList", docList);            	
   }
%>
<jsp:include page="/components/top.jsp"/>

<script type='text/javascript'>

function doOK()
{
	var sel = window.opener.ObjEditoriwcm.selection;
	if (sel!=null)
   {
		var rng = sel.createRange();
	   if (rng!=null)
      {
         var htmlCode = previewWindow.document.body.innerHTML;

      	if (htmlCode != "") rng.pasteHTML(htmlCode);

		} // End if
	} // End If


   self.close();
} // End function

function Ok()
{		
	var docId = document.textForm.field.value
	if (document.textForm.docid.value != "") docId = document.textForm.docid.value;
	var htmlCode = ""; 
	if (document.textForm.insertType[1].checked)
	{
		htmlCode = "!INCLUDE(/components/htmlbox/showdoc.jsp, docid="+docId+")!";
	}
	else
	{
		htmlCode = previewWindow.document.getElementById("_iframeHtmlData").value;
	}
	
	oEditor.FCK.InsertHtml(htmlCode);
	oEditor.FCK.WJToogleBordersCommand_thisRef.RefreshBorders();
	return true ;
} // End function

if (isFck)
{
	
}
else
{
	resizeDialog(500, 500);
}


function preview(select)
{
	previewWindow.location.href="/components/htmlbox/iframe.jsp?docid="+select.value;
}

function setStandardDoc(doc_id)
{
   document.textForm.docid.value = doc_id;
   previewDocId();	
}
function previewDocId()
{
	previewWindow.location.href="/components/htmlbox/iframe.jsp?docid="+document.textForm.docid.value;
}

</script>
<form name=textForm>
	<table border="0" cellspacing="0" cellpadding="5">
	   <tr>
	      <td valign="top">
	      	<iwcm:text key="components.htmlbox.select_object"/>:<br>
	      	<select name="field" onchange="preview(this);this.form.docid.value='';" size="15" style="width: 250px; background-color: Window;">
	      	  <iwcm:present name="docList">	
	          	 <logic:iterate id="dl" name="docList" type="sk.iway.iwcm.doc.DocDetails">
	          		<option value='<bean:write name="dl" property="docId"/>'><bean:write name="dl" property="title"/></option>
	          	 </logic:iterate>  
	          </iwcm:present>  
	         </select>
	         
	         	<iwcm:text key="components.htmlbox.otherWebPage"/>:
	         	<br/>
	         	<iwcm:text key="components.popup.docid"/>: <input type="text" name="docid" value="" size="5" onblur="previewDocId();"/> <input type="button" value="<iwcm:text key="groupedit.select"/>" name="bSelDoc" onClick='popupFromDialog("/admin/user_adddoc.jsp", 450, 340);' class="button50" />
					<br/>
					<iwcm:text key="components.htmlbox.insertType"/>:
	         	<br/>
	         	<label><input type="radio" name="insertType" value="static" checked="checked"/> <iwcm:text key="components.htmlbox.insertType.static"/></label>
	         	<br/>
	         	<label><input type="radio" name="insertType" value="dynamic" id="insertType"/> <iwcm:text key="components.htmlbox.insertType.dynamic"/></label>
	         
	      </td>
		   <td valign="top">
		      <br>
				<iframe ID="previewWindow" name="previewWindow" align="top" style="width:340px; height:291px; bgcolor: silver" src="iframe.jsp"></iframe>				
	      </td>
	   </tr>
	</table>
</form>

<jsp:include page="/components/bottom.jsp"/>