<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<iwcm:checkLogon admin="true" perms="cmp_form"/>
<%
String loggedUserEmail = "";

Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
if (user!=null)
{
   loggedUserEmail = user.getEmail();
}
%>

<html>
<head>
   <title><iwcm:text key="editor.form.properties"/></title>
   <script type="text/javascript" src="<%=request.getContextPath()%>/admin/scripts/common.jsp"></script>
   <style type="text/css">
   BUTTON {width:5em;}
   P {text-align : center;}
   BODY {background: #ECEEF0; margin : 1px 5px 1px 5px;}
    TD {FONT-SIZE: 11px; FONT-FAMILY: tahoma, verdana, Arial, Helvetica, sans-serif}
   SELECT {FONT-SIZE: 11px; FONT-FAMILY: tahoma, verdana, Arial, Helvetica, sans-serif}
   INPUT {FONT-SIZE: 11px; FONT-FAMILY: tahoma, verdana, Arial, Helvetica, sans-serif}
   .tbColor {cursor:hand;}
   .controls {width:100%;text-align:right;margin-top:5px;margin-right:3px}
   fieldset{background: #ECEEF0; font: 11px/11px Tahoma;margin:0px;}
   .button {width: 65px}
   </style>
</head>
<body>

<script type='text/javascript'>

<iwcm:notPresent parameter="modify">
window.onload = this.focus

function doForm()
{
	error = 0
	var sel = window.opener.ObjEditoriwcm.selection;
	if (sel!=null)
   {
		var rng = sel.createRange();
      if (rng!=null)
      {
			name = document.formForm.form_name.value
			action = document.formForm.form_action.value
			method = document.formForm.form_method[formForm.form_method.selectedIndex].value

         if (error != 1)
         {

				if (method != "None")
            {
					method = ' method="' + method + '"'
				}
            else
            {
					method = ""
				}

				if (action != "")
            {
               if (document.formForm.send_by_email.checked)
               {
                  action = "formmail.do?recipients="+document.formForm.recipients.value;
                  if (name != "")
                  {
                     name = encodeValue(name);
                     action = action + "&savedb=" + name;
                  }
               }
					action = ' action="' + action + '"'
				}
            else
            {
					action = ""
				}

            HTMLForm = "<html><body><scr"+"ipt type='text/javascript' src='<iwcm:cp/>/components/form/check_form.js'></scr"+"ipt><form name='formMailForm' id=ewp_element_to_style" + action + method +">&nbsp;</form></body></html>"
            rng.pasteHTML(HTMLForm)

            oForm = window.opener.ObjEditoriwcm.getElementById("ewp_element_to_style");
            oForm.runtimeStyle.border = "1px dotted #FF0000";
            oForm.removeAttribute("id");
         }
		}

	}

	if (error != 1)
   {
		self.close();
	}
}

function setValues()
{
   //nastavenie default hodnot...
   document.formForm.send_by_email.checked=true;
   sendByEmailClick();
   document.formForm.form_name.value = window.opener.editorForm.title.value;
}

</iwcm:notPresent>

<iwcm:present parameter="modify">
var myPage = window.opener;

var formName = myPage.selectedForm.name;
var formAction = myPage.selectedForm.action;
var formMethod = myPage.selectedForm.method;

function setValues()
{
   document.formForm.form_name.value = window.opener.editorForm.title.value;

   if (formAction.indexOf("formmail.do")!=-1)
   {

      document.formForm.send_by_email.checked = true;
      index = formAction.indexOf("?");
      if (index>0)
      {
         params = formAction.substring(index+1);
         paramsArray = params.split("&");
         for (i=0; i < paramsArray.length; i++)
         {
            index = paramsArray[i].indexOf("=");
            if (index!=-1)
            {
               name = paramsArray[i].substring(0, index);
               value = paramsArray[i].substring(index+1);
               value = unencodeValue(value);

               if (name=="recipients")
               {
                  document.formForm.recipients.value=value;
               }
               if (name=="savedb")
               {
                  document.formForm.form_name.value=value;
               }
            }
         }
      }
      formAction = "formmail.do";
   }

	formForm.form_action.value = formAction;
	var opt = document.formForm.form_method.options;
   var size = opt.length;
   for (i=0; i<size; i++)
   {
      value = opt[i].value;
      if (value == formMethod)
      {
         document.formForm.form_method.selectedIndex = i;
      }
   }

	this.focus();
}

function doForm()
{

	if (formForm.form_action.value != "")
   {
      var action = formForm.form_action.value;
      if (document.formForm.send_by_email.checked)
      {
         action = "formmail.do?recipients="+document.formForm.recipients.value;
         name = document.formForm.form_name.value;
         if (name != "")
         {
            name = encodeValue(name);
            action = action + "&savedb=" + name;
         }
      }

		myPage.selectedForm.action = action;
	}
   else
   {
		myPage.selectedForm.removeAttribute('action',0)
	}

	if (formForm.form_method[formForm.form_method.selectedIndex].value != "") {
    	myPage.selectedForm.method = formForm.form_method[formForm.form_method.selectedIndex].value
    } else {
		myPage.selectedForm.removeAttribute('method',0)
    }

    window.close()
}

</iwcm:present>

function sendByEmailClick()
{
   if (document.formForm.send_by_email.checked==true)
   {
      document.formForm.recipients.disabled=false;
      document.formForm.form_action.disabled=true;
      document.formForm.form_action.value="formmail.do";
   }
   else
   {
      document.formForm.recipients.disabled=true;
      document.formForm.form_action.disabled=false;
   }
}

document.onkeydown = function () {
			if (event.keyCode == 13) {	// ENTER
				InsertForm()
			}
};

document.onkeypress = onkeyup = function () {
	if (event.keyCode == 13) {	// ENTER
	event.cancelBubble = true;
	event.returnValue = false;
	return false;
	}
};

</script>

<fieldset style="padding:3px;"><legend style="height: 15px;"><b><iwcm:text key="editor.form.properties"/></b></legend>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
<form name=formForm>
  <tr>
	<td>&nbsp;</td>
	<td class="body">

	    <table border="0" cellspacing="0" cellpadding="5" width="98%" class="bevel2">


		  <tr>
		    <td class="body" width="80"><iwcm:text key="editor.form.name"/>:</td>
			 <td class="body">
			  <input type="text" name="form_name" size="40" class="Text90" maxlength="50" value="webform">
		    </td>
		  </tr>
        <tr>
		    <td class="body" width="80"><iwcm:text key="editor.form.send_by_email"/>:</td>
			 <td class="body">
			  <input type="checkbox" name="send_by_email" value="yes" onclick="sendByEmailClick()">
		    </td>
		  </tr>
        <tr>
		    <td class="body" width="80">
			   <iwcm:text key="editor.form.recipients"/>:
          </td>
			 <td class="body">
			  <input type="text" name="recipients" size="40" class="Text250" value="<%=loggedUserEmail%>">
		    </td>
		  </tr>
		  <tr>
		    <td class="body" width="80">
			   <iwcm:text key="editor.form.action"/>:
          </td>
			 <td class="body">
			  <input type="text" name="form_action" size="40" class="Text250">
		    </td>
		  </tr>
		  <tr>
		    <td class="body" width="80"><iwcm:text key="editor.form.method"/>:</td>
			 <td class="body">
			  <select class=text70 name=form_method>
				<option value=""><iwcm:text key="editor.form.none"/></option>
				<option value="post" selected><iwcm:text key="editor.form.post"/></option>
				<option value="get"><iwcm:text key="editor.form.get"/></option>
				</select>
			 </td>
		  </tr>
	    </table>
	</td>
  </tr>
  <tr>
	<td colspan="2"><img src="images/pixel.gif" width="1" height="10"></td>
  </tr>
  <tr>
	<td>&nbsp;</td>
	<td align="right">
	    <input type="button" name="insertForm" value="<iwcm:text key="button.ok"/>" class="button" onClick="javascript:doForm();">
	    <input type="button" name="Submit" value="<iwcm:text key="button.cancel"/>" class="button" onClick="javascript:window.close()">
	</td>
  </tr>
</form>
</table>

</fieldset>

<script type="text/javascript">
setValues();
sendByEmailClick();
</script>

</body>
</html>