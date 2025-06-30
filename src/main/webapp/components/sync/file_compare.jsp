<%@page import="sk.iway.iwcm.io.IwcmInputStream"%>
<%@page import="sk.iway.iwcm.tags.support.ResponseUtils"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%@page import="sk.iway.iwcm.io.IwcmFsDB"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%!
public String getFileContent(IwcmFile file)
{
	StringBuilder result=new StringBuilder();
	byte buffer[]=new byte[2*1024];
	int bytesRead=-1;
	try
	{
		if (file.exists())
		{
			IwcmInputStream reader =new IwcmInputStream(file);
			while ((bytesRead=reader.read(buffer))!=-1)
			{
				result.append(new String(buffer),0,bytesRead);
			}
			reader.close();
		}
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
%><%
String file1Data = null;
String file2Data = null;
String file1 = Tools.getRequestParameter(request, "firstFile");
String file2 = Tools.getRequestParameter(request, "secondFile");
boolean isImage = false;

if(file1!= null && file2!=null)
{
	if(file1.endsWith(".gif") || file1.endsWith(".jpg") || file1.endsWith(".bmp") || file1.endsWith(".png") || file1.endsWith(".jpeg"))
		isImage=true;
	else
	{
		file1Data=getFileContent(new IwcmFile(sk.iway.iwcm.Tools.getRealPath(Tools.getRequestParameter(request, "firstFile"))));
		file2Data=getFileContent(new IwcmFile(sk.iway.iwcm.Tools.getRealPath(Tools.getRequestParameter(request, "secondFile"))));
	}
}
else
	return;
%>

<table border="1">
	<tr>
		<td>Existujuci</td>
		<td>Novy</td>
	</tr>
	<tr>
		<td><%if(isImage){out.print("<img src=\""+file1+"\">");}else{out.print(diff(file1Data,file2Data));}%></td>
		<td><%if(isImage){out.print("<img src=\""+file2+"\">");}else{out.print(diff(file2Data,file1Data));}%></td>
	</tr>
</table>
