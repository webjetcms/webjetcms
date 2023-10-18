<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<iwcm:checkLogon admin="true" perms="cmp_form"/>
  <td class="editorMiniEdit"><div class="tbSeparator"></div></td>

  <td style="position: relative;" class="tbButton editorMiniEdit" ID="DECMD_InsertFormiwcm" TITLE="<iwcm:text key="editor.insert_form"/>" TBTYPE="toggle" onclick="InsertFormMenu(ObjEditoriwcm)">
	 <img class="tbIcon" src="images/editor/form_insert.gif" WIDTH="23" HEIGHT="22" border="0" align="absmiddle">
	 <DIV id="formMenu" style="position: absolute; top: 22px; left: 0px; display: none; width: 187px; z-index: 0;">
		<!-- form menu -->
		<table class="popupMenu" border="0" cellspacing="0" cellpadding="0" width=187 style="BORDER-LEFT: buttonhighlight 1px solid; BORDER-RIGHT: buttonshadow 2px solid; BORDER-TOP: buttonhighlight 1px solid; BORDER-BOTTOM: buttonshadow 1px solid;" bgcolor="threedface">
		  <tr onClick="insertForm();hideMenus();">
			<td onMouseOver="contextHilite(this);" onMouseOut="contextDelite(this);">
				&nbsp;&nbsp;<img width="21" height="20" src="images/editor/button_form.gif" border=0 align="absmiddle"> <iwcm:text key="editor.form.menu.insert_form"/>
			</td>
		  </tr>
		  <tr onClick="modifyForm();hideMenus();" id="modifyForm1">
			 <td onMouseOver="contextHilite(this);" onMouseOut="contextDelite(this);">
				&nbsp;&nbsp;<img id="f2" width="21" height="20" src="images/editor/button_modify_form.gif" border=0 align="absmiddle"> <iwcm:text key="editor.form.menu.modify_form"/>
			 </td>
		  </tr>
		  <tr height=10>
			 <td align=center><img src="images/editor/vertical_spacer.gif" width="140" height="2" tabindex=1 HIDEFOCUS></td>
		  </tr>
		  <tr onClick="doTextField();hideMenus();">
			 <td onMouseOver="contextHilite(this);" onMouseOut="contextDelite(this);">
				&nbsp;&nbsp;<img width="21" height="20" src="images/editor/button_textfield.gif" border=0 align="absmiddle"> <iwcm:text key="editor.form.menu.tf"/>
			 </td>
		  </tr>
		  <tr onClick="doTextArea();hideMenus();">
			 <td onMouseOver="contextHilite(this);" onMouseOut="contextDelite(this);">
				&nbsp;&nbsp;<img width="21" height="20" src="images/editor/button_textarea.gif" border=0 align="absmiddle"> <iwcm:text key="editor.form.menu.ta"/>
			 </td>
		  </tr>
		  <tr onClick="doHidden();hideMenus();">
			 <td onMouseOver="contextHilite(this);" onMouseOut="contextDelite(this);">
				&nbsp;&nbsp;<img width="21" height="20" src="images/editor/button_hidden.gif" border=0 align="absmiddle"> <iwcm:text key="editor.form.menu.hf"/>
			 </td>
		  </tr>
		  <tr onClick="doButton();hideMenus();">
			 <td onMouseOver="contextHilite(this);" onMouseOut="contextDelite(this);">
				&nbsp;&nbsp;<img width="21" height="20" src="images/editor/button_button.gif" border=0 align="absmiddle"> <iwcm:text key="editor.form.menu.btn"/>
			 </td>
		  </tr>
		  <tr onClick="doCheckbox();hideMenus();">
			 <td onMouseOver="contextHilite(this);" onMouseOut="contextDelite(this);">
				&nbsp;&nbsp;<img width="21" height="20" src="images/editor/button_checkbox.gif" border=0 align="absmiddle"> <iwcm:text key="editor.form.menu.cb"/>
			 </td>
		  </tr>
		  <tr onClick="doSelect();hideMenus();">
			 <td onMouseOver="contextHilite(this);" onMouseOut="contextDelite(this);">
				&nbsp;&nbsp;<img width="21" height="20" src="images/editor/button_select.gif" border=0 align="absmiddle"> <iwcm:text key="editor.form.menu.sl"/>
			 </td>
		  </tr>
		  <tr onClick="doRadio();hideMenus();">
			 <td onMouseOver="contextHilite(this);" onMouseOut="contextDelite(this);">
				&nbsp;&nbsp;<img width="21" height="20" src="images/editor/button_radio.gif" border=0 align="absmiddle"> <iwcm:text key="editor.form.menu.rb"/>
			 </td>
		  </tr>
		</table>
		<!-- End formMenu -->

	 </DIV>
  </td>