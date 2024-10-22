<%@page import="sk.iway.iwcm.io.IwcmFsDB"%>
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
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/admin/layout_top.jsp" %>
<%!
public boolean isTextFile(String file1) {
	String[] textFiles = {"txt","html","htm","xml","jsp","java","css","js","properties","sql","log"};
	for (String textFile : textFiles) {
		if (file1.contains("."+textFile)) return true;
	}
	return false;
}

public boolean isImageFile(String file1) {
	String[] imageFiles = {"jpg","jpeg","gif","png","bmp"};
	for (String imageFile : imageFiles) {
		if (file1.contains("."+imageFile)) return true;
	}
	return false;
}

public String diff(String file1,String file2)
{
	System.out.println("file1="+file1+" file2="+file2);

	if (isTextFile(file1) && isTextFile(file2))
	{
		String input1=FileTools.readFileContent(file1);
		String input2=FileTools.readFileContent(file2);

		input1=ResponseUtils.filter(input1).replace("\r\n","\n").replace("\r","\n").replace("\n","<br>");
		input2=ResponseUtils.filter(input2).replace("\r\n","\n").replace("\r","\n").replace("\n","<br>");
		char a1[]=input1.toCharArray();
		char a2[]=input2.toCharArray();
		boolean inDifference=false;

		for (int i=0; i < a1.length && i<a2.length;i++)
		{
			if (a1[i]!=a2[i])
			{
				if (!inDifference)
				{
					input1=input1.substring(0,i)+"<font color=\"red\">"+input1.substring(i);
					inDifference=true;
				}
			}
		}
		if (a1.length<a2.length && !inDifference)
		{
			input1+="<font color=\"red\">"+input2.substring(input1.length())+"</font>";
		}

		return input1;
	} else if (isImageFile(file1) && isImageFile(file2)) {
		return "<img src='"+file1+"' />";
	} else {
		return "<font color=\"red\">Nemozem zobrazit diff pre tento typ suboru</font>";
	}
}
%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="org.apache.struts.util.ResponseUtils"%>
<%@page import="sk.iway.iwcm.io.IwcmInputStream"%>
<script language="JavaScript">
if (window.name && window.name=="componentIframe")
{
	document.write("<LINK rel='stylesheet' href='/components/iframe.css'>");
}
else
{
	document.write("<LINK rel='stylesheet' href='/admin/css/style.css'>");
}
var helpLink = "";
</script>
<%
Prop prop = Prop.getInstance(getServletContext(), request);

String file1=Tools.getRequestParameter(request, "firstFile");
String file2=Tools.getRequestParameter(request, "secondFile");
file1 = Tools.replace(file1, "?fHistoryId ", "?fHistoryId=");
file2 = Tools.replace(file2, "?fHistoryId ", "?fHistoryId=");
%>

<table width="100%" height="100%" >
	<tr>
		<td valign="top" width="50%" style="border: 1px solid black;"><frameset style="width: 100%;"><%=diff(file1,file2)%></frameset></td>
		<td valign="top" width="50%" style="border: 1px solid black;"><frameset style="width: 100%;"><%=diff(file2,file1)%></frameset></td>
	</tr>
</table>

<%@ include file="/admin/layout_bottom.jsp" %>
