<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.FileTools,sk.iway.iwcm.PageParams,sk.iway.iwcm.Tools,sk.iway.iwcm.doc.DocDB,sk.iway.iwcm.doc.DocDetails" %><%@ page import="sk.iway.spirit.MediaDB" %><%@ page import="sk.iway.spirit.model.Media" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"%><%@ 
taglib uri="/WEB-INF/iway.tld" prefix="iway"%><%@ 
taglib uri="/WEB-INF/displaytag.tld" prefix="display" %><%
PageParams pageParams = new PageParams(request);

String groups = pageParams.getValue("group", pageParams.getValue("groups", ""));

DocDetails docDetails = (DocDetails)request.getAttribute("docDetails");
if (docDetails == null) return;
int docId = docDetails.getDocId();

int docIdEx = pageParams.getIntValue("docid", -1);

if (docIdEx > 0) {
  docId = docIdEx;
  docDetails = DocDB.getInstance().getDoc(docId);
}

List files = null;
if(docId < 1)
{
	sk.iway.iwcm.Identity user = sk.iway.iwcm.users.UsersDB.getCurrentUser(request);
	if (user != null)	files = MediaDB.getMedia(session, "documents_temp", user.getUserId(), null, 0, false);
}
else
{
	files = MediaDB.getMedia(session, "documents", docId, groups, docDetails.getDateCreated(), false);
}
if (files == null || files.size()==0) return;

Iterator iter = files.iterator();
Media m;
String iconUrl, fileSize;

out.println("<ul>");
while (iter.hasNext())
{
   m = (Media)iter.next();
   iconUrl = FileTools.getFileIcon(m.getMediaLink());
   fileSize = FileTools.getFileLength(m.getMediaLink());
   
   if (Tools.isEmpty(fileSize) || "0 B".equals(fileSize))
   {
   	//asi to nie je subor / subor neexistuje
   	out.println("<li><a href='"+m.getMediaLink()+"' title='"+m.getMediaTitleSk()+"'>"+m.getMediaTitleSk()+"</a></li>");
   }
   else
   {
   	//je to subor, zobrazime aj ikonu aj velkost suboru
   	out.println("<li><img src='"+iconUrl+"' alt='media' /> <a href='"+m.getMediaLink()+"'>"+m.getMediaTitleSk()+"</a> ["+fileSize+"]</li>");
   }
}
out.println("</ul>");


%>