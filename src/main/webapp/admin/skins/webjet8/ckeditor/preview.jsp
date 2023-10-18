<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><iwcm:checkLogon admin="true" perms="menuWebpages"/>

<html>
	<body>
	
		<form name="pf" action="<iwcm:cp/>/preview.do" method="post">
		   <input type="hidden" name="docId">
		   <input type="hidden" name="docid">
		   <input type="hidden" name="title">
		   <textarea name="data" wrap="off" cols="70" rows="6" style="visibility:hidden"></textarea>
		   <input type="hidden" name="externalLink">
		   <input type="hidden" name="navbar">
		   <input type="hidden" name="publishStart">
		   <input type="hidden" name="publishStartTime">
		   <input type="hidden" name="publishEnd">
		   <input type="hidden" name="publishEndTime">
		   <input type="hidden" name="groupId">
		   <input type="hidden" name="tempId">
		   <input type="hidden" name="available"/>
		   <input type="hidden" name="sortPriority">
		   <input type="hidden" name="headerDocId">
		   <input type="hidden" name="menuDocId">
		   <input type="hidden" name="footerDocId">
		   <select name="passwordProtected" multiple="multiple" style="display: none;"></select>
		   <input type="hidden" name="htmlHead">
		   <input type="hidden" name="htmlData">
		   <input type="hidden" name="perexPlace">
		   <input type="hidden" name="perexImage">
		   <select name="perexGroup" multiple="multiple" style="display: none;"></select>
		   <input type="hidden" name="eventDate">
		   <input type="hidden" name="eventTime">   
		   <input type="hidden" name="virtualPath">
		   <input type="hidden" name="rightMenuDocId">
		   
		   <input type="hidden" name="fieldA">
		   <input type="hidden" name="fieldB">
		   <input type="hidden" name="fieldC">
		   <input type="hidden" name="fieldD">
		   <input type="hidden" name="fieldE">
		   <input type="hidden" name="fieldF">
		   <input type="hidden" name="fieldG">
		   <input type="hidden" name="fieldH">
		   <input type="hidden" name="fieldI">
		   <input type="hidden" name="fieldJ">
		   <input type="hidden" name="fieldK">
		   <input type="hidden" name="fieldL">
		   <input type="hidden" name="fieldM">
		   <input type="hidden" name="fieldN">
		   <input type="hidden" name="fieldO">
		   <input type="hidden" name="fieldP">
		   <input type="hidden" name="fieldQ">
		   <input type="hidden" name="fieldR">
		   <input type="hidden" name="fieldS">
		   <input type="hidden" name="fieldT">
		   
		   <input type="hidden" name="domainName">
		   
		   <input type="submit" name="odoslat" style="visibility:hidden" />
		</form>
	
		<center>
		   <font color="black" face="Arial"><b><iwcm:text key="editor.openingPreview"/></b></font>
		</center>
	
		<script type="text/javascript">
		
			document.pf.docId.value = window.opener.document.editorForm.docId.value;
			document.pf.docid.value = window.opener.document.editorForm.docId.value;
			document.pf.title.value = window.opener.document.editorForm.title.value;
			document.pf.data.value = window.opener.document.editorForm.data.value;
			document.pf.externalLink.value = window.opener.document.editorForm.externalLink.value;	
			document.pf.navbar.value = window.opener.document.editorForm.navbar.value;
			document.pf.publishStart.value = window.opener.document.editorForm.publishStart.value;
			document.pf.publishStartTime.value = window.opener.document.editorForm.publishStartTime.value;
			document.pf.publishEnd.value = window.opener.document.editorForm.publishEnd.value;
			document.pf.publishEndTime.value = window.opener.document.editorForm.publishEndTime.value;
			document.pf.groupId.value = window.opener.document.editorForm.groupIdString.value;
			document.pf.tempId.value = window.opener.document.editorForm.tempId.value;
			document.pf.available.value = window.opener.document.editorForm.available.checked ? "true" : "false";
			document.pf.sortPriority.value = window.opener.document.editorForm.sortPriority.value;
			document.pf.headerDocId.value = window.opener.document.editorForm.headerDocId.value;
			document.pf.menuDocId.value = window.opener.document.editorForm.menuDocId.value;
			document.pf.footerDocId.value = window.opener.document.editorForm.footerDocId.value;
			try
			{
				//skopiruj optiony do selectu
				var passProtected = window.opener.document.editorForm.passwordProtected;
				for (i=0; i<passProtected.options.length; i++)
				{
					if (passProtected.options[i].selected)
					{
						var o = new Option(passProtected.options[i].text, passProtected.options[i].value, false, true);
						document.pf.passwordProtected.options[document.pf.passwordProtected.options.length] = o;
					}
				}
			} catch (ex) {}
			document.pf.htmlHead.value = window.opener.document.editorForm.htmlHead.value;
			document.pf.htmlData.value = window.opener.document.editorForm.htmlData.value;
			document.pf.perexPlace.value = window.opener.document.editorForm.perexPlace.value;
			document.pf.perexImage.value = window.opener.document.editorForm.perexImage.value;
			try
			{
				//skopiruj optiony do selectu
				var passProtected = window.opener.document.editorForm.perexGroup;
				for (i=0; i<passProtected.options.length; i++)
				{
					if (passProtected.options[i].selected)
					{
						var o = new Option(passProtected.options[i].text, passProtected.options[i].value, false, true);
						document.pf.perexGroup.options[document.pf.perexGroup.options.length] = o;
					}
				}
			} catch (ex) {}
			document.pf.eventDate.value = window.opener.document.editorForm.eventDate.value;
			document.pf.eventTime.value = window.opener.document.editorForm.eventTime.value;	
			document.pf.virtualPath.value = window.opener.document.editorForm.virtualPath.value;
			document.pf.rightMenuDocId.value = window.opener.document.editorForm.rightMenuDocId.value;
	
			try
			{
				document.pf.fieldA.value = window.opener.document.editorForm.fieldA.value;
				document.pf.fieldB.value = window.opener.document.editorForm.fieldB.value;
				document.pf.fieldC.value = window.opener.document.editorForm.fieldC.value;
				document.pf.fieldD.value = window.opener.document.editorForm.fieldD.value;
				document.pf.fieldE.value = window.opener.document.editorForm.fieldE.value;
				document.pf.fieldF.value = window.opener.document.editorForm.fieldF.value;
				document.pf.fieldG.value = window.opener.document.editorForm.fieldG.value;
				document.pf.fieldH.value = window.opener.document.editorForm.fieldH.value;
				document.pf.fieldI.value = window.opener.document.editorForm.fieldI.value;
				document.pf.fieldJ.value = window.opener.document.editorForm.fieldJ.value;
				document.pf.fieldK.value = window.opener.document.editorForm.fieldK.value;
				document.pf.fieldL.value = window.opener.document.editorForm.fieldL.value;
				document.pf.fieldM.value = window.opener.document.editorForm.fieldM.value;
				document.pf.fieldN.value = window.opener.document.editorForm.fieldN.value;
				document.pf.fieldO.value = window.opener.document.editorForm.fieldO.value;
				document.pf.fieldP.value = window.opener.document.editorForm.fieldP.value;
				document.pf.fieldQ.value = window.opener.document.editorForm.fieldQ.value;
				document.pf.fieldR.value = window.opener.document.editorForm.fieldR.value;
				document.pf.fieldS.value = window.opener.document.editorForm.fieldS.value;
				document.pf.fieldT.value = window.opener.document.editorForm.fieldT.value;
			} catch (ex) {}
			
			document.pf.domainName.value = window.opener.document.editorForm.domainName.value;
			
			document.pf.submit();
		
		</script>
	</body>
</html>