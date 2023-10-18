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
public String getFileContent(File file)
{
	StringBuilder result=new StringBuilder();
	char buffer[]=new char[2048];
	int bytesRead=-1;
	try
	{

	if (file.exists())
	{
		FileReader reader =new FileReader(file);
		while ((bytesRead=reader.read(buffer))!=-1)
		{
			result.append(buffer,0,bytesRead);
		}
		reader.close();
	} else{
		return getFileContentFromHistory(IwcmFsDB.getVirtualPath(file.getAbsolutePath()),-1);
	}
	}
	catch (Exception e)
	{
		sk.iway.iwcm.Logger.error(e);
	}
	return result.toString();
}

public String getFileContentFromHistory(String virtualPath,int fatId)
{
	StringBuilder result=new StringBuilder();
	byte buffer[]=new byte[2048];
	int bytesRead=-1;
	try
	{
		InputStream in = null;

		if (fatId > 0)
		{
			in = new IwcmInputStream(virtualPath,fatId);
		}
		else
		{
			in = new IwcmInputStream(virtualPath,true);
		}
		while ((bytesRead=in.read(buffer))!=-1)
		{
			result.append(new String(buffer,0,bytesRead));
		}
		in.close();
	}
	catch (Exception e)
	{
		sk.iway.iwcm.Logger.error(e);
	}
	return result.toString();

}
public String diff(String input1,String input2)
{

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
}
%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="java.io.File"%>
<%@page import="java.io.FileReader"%>
<%@page import="org.apache.struts.util.ResponseUtils"%>
<%@page import="sk.iway.iwcm.io.IwcmInputStream"%>
<%@page import="java.io.InputStream"%><script language="JavaScript">
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

String dir=sk.iway.iwcm.Tools.getRealPath(Tools.getRequestParameter(request, "dir"));

String file1="";
String file2="";

if (Tools.getRequestParameter(request, "firstFile")!=null)
{
	file1=getFileContent(new File(sk.iway.iwcm.Tools.getRealPath(Tools.getRequestParameter(request, "firstFile"))));
}
else
{
	file1=getFileContent(new File(dir+File.separatorChar+Tools.getRequestParameter(request, "cFile")));
}

if (Tools.getRequestParameter(request, "secondFile")!=null)
{
	file2=getFileContent(new File(sk.iway.iwcm.Tools.getRealPath(Tools.getRequestParameter(request, "secondFile"))));
}
else
{
	if (Tools.getRequestParameter(request, "fatId")!=null)
	{
		file2=getFileContentFromHistory(Tools.getRequestParameter(request, "firstFile"),Tools.getIntValue(Tools.getRequestParameter(request, "fatId"),-1));
	}
	else
	{
		file2=getFileContent(new File(sk.iway.iwcm.Tools.getRealPath("WEB-INF/update/")+File.separatorChar+Tools.getRequestParameter(request, "zipDirectory")+File.separatorChar+Tools.getRequestParameter(request, "cFile")));
	}
}

%>

<table width="100%" height="100%" >
<tr>
<td valign="top" width="50%" style="border: 1px solid black;"><frameset  style="width: 100%;" ><%=diff(file1,file2)%></frameset></td>
<td valign="top" width="50%" style="border: 1px solid black;"><frameset style="width: 100%;"><%=diff(file2,file1)%></frameset></td>
</tr>
</table>
<%@ include file="/admin/layout_bottom.jsp" %>
