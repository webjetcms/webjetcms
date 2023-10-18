<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><iwcm:checkLogon admin="true"/>

<%
//otestuj ci existuje nahrada za tuto stranku
String forward = "/admin/authorize_result_email-"+Constants.getInstallName()+".jsp";
java.io.File fForward = new java.io.File(sk.iway.iwcm.Tools.getRealPath(forward));
if (fForward.exists())
{
   pageContext.forward(forward);
   return;
}
%> 

<html:html >
<HEAD>
<TITLE>:: Web JET admin ::</TITLE>
<%
response.setHeader("Pragma","No-Cache");
response.setDateHeader("Expires",0);
response.setHeader("Cache-Control","no-Cache");
%>
<LINK rel="stylesheet" href="css/style.css"> 
<script language="JavaScript">
<!--
function m_over(x)
{
   if(x == null)
      x= window;
   x.event.srcElement.style.background="#ff9933";
   x.event.srcElement.style.color = "white";
   x.event.srcElement.style.cursor = "hand";
}

function m_out(x)
{
   if(x == null)
      x= window;
   x.event.srcElement.style.background="#CCCCFF";
   x.event.srcElement.style.color = "#3366CC";
   x.event.srcElement.style.cursor = "default";
}

function m_click(dest)
{
   top.location = dest;
}

function m_click_help()
{
   var options = "menubar=no,toolbar=no,scrollbars=yes,resizable=yes,width=600,height=400;"
   popWindow=window.open("help.jsp","_blank",options);
}

var t_server = 1040376789

var lokalny_start = new Date().getTime() // pocet ms od 1.1. 1970 (lokal)

function FormatCas(ms)
{
  var hours=ms.getHours() // hodin
  var minutes=ms.getMinutes() // minut
  var seconds=ms.getSeconds() // sekund
  var days=ms.getDate() // den
  var months=ms.getMonth()+1 // mesiac
  var years=ms.getYear() // rok
  if(years<1000) years+=1900 // odstranenie nekompatibility
  if(minutes<=9) minutes="0"+minutes
  if(seconds<=9) seconds="0"+seconds
  return days + "." + months + ". " + years + " " + hours + ":" + minutes + ":" + seconds + " "
}

function Cas()
{
  var lokalny = new Date()
  var posun = lokalny.getTime() - lokalny_start
  var server = new Date(t_server*1000 + posun)

  tdTime.innerText = FormatCas(server)
  setTimeout("Cas()",999)
}

function popup(url, width, height)
{
      var options = "toolbar=no,scrollbars=yes,resizable=yes,width="+width+",height="+height+";"
      popupWindow=window.open(url,"_blank",options);
}

function popup_no_scroll(url, width, height)
{
      var options = "toolbar=no,scrollbars=no,resizable=no,width="+width+",height="+height+";"
      popupWindow=window.open(url,"_blank",options);
}

function popup_with_menu(url, width, height)
{
      var options = "menubar=yes,toolbar=yes,scrollbars=yes,resizable=yes,width="+width+",height="+height+";"
      popupWindow=window.open(url,"_blank",options);
}

var domLT = (document.getElementsByTagName) ? true : false;
var ie5LT = (document.getElementsByTagName && document.all) ? true : false;

//added by jeeff
function highlight(obj,on)
{
   if (ie5LT || domLT)
   {
      if (on==1) obj.bgColor="#eeeeee";
      else obj.bgColor="white";
   }
}



//-->
</script>

<SCRIPT LANGUAGE="JavaScript" src="scripts/modalDialog.js"></SCRIPT>

</HEAD>

<BODY leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">

<h3><iwcm:text key="authorize_user.title"/>:</h3>

<logic:present name="emailSendFail">
   <b><iwcm:text key="authorize_user.email_send_fail"/>
</logic:present>

<logic:notPresent name="emailSendFail">
   <b><iwcm:text key="authorize_user.authorize_od"/>:</b><br><br>
   <hr>
   <iwcm:text key="authorize_user.sender"/>: <iway:request name="from"/><br>
   <iwcm:text key="authorize_user.for"/>: <iway:request name="to"/><br>
   <iwcm:text key="authorize_user.subject"/>: <iway:request name="subject"/><br>
   <hr>
   <iway:request name="body"/>
</logic:notPresent>

</html:html>