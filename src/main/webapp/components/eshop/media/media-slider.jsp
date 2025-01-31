<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="windows-1250" import="sk.iway.iwcm.FileTools,sk.iway.iwcm.PageParams,sk.iway.iwcm.Tools,sk.iway.iwcm.doc.DocDB,sk.iway.iwcm.doc.DocDetails" %><%@ page import="sk.iway.spirit.MediaDB" %><%@ page import="sk.iway.spirit.model.Media" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"%><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway"%><%@
taglib uri="/WEB-INF/struts-bean.tld" prefix="bean"%><%@
taglib uri="/WEB-INF/struts-html.tld" prefix="html"%><%@
taglib uri="/WEB-INF/struts-logic.tld" prefix="logic"%><%@
taglib uri="/WEB-INF/displaytag.tld" prefix="display" %><%
PageParams pageParams = new PageParams(request);

String groups = pageParams.getValue("group", pageParams.getValue("groups", ""));
boolean main = pageParams.getBooleanValue("mainSlider", false);

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

if(main){
    out.print("<div class='main-slider slider'>");
}

while (iter.hasNext())
{
   m = (Media)iter.next();

   if(main){
       out.print("<a class='thumb' data-dimensions='750x560' href='/thumb"+m.getMediaThumbLink()+ "?w=750&h=560&ip=4&c=ffffff'><img src='/thumb"+m.getMediaThumbLink()+ "?w=500&h=300&ip=4&c=ffffff' alt='"+m.getMediaTitleSk()+"'></a>");
   } else{
       out.print("<img src='/thumb"+m.getMediaThumbLink()+ "?w=250&h=150&ip=4&c=ffffff' alt='" +m.getMediaTitleSk()+"'>");
   }

}

if(main){
    out.print("</div>");
%>

<script type="text/javascript">
    $(document).ready(function()
    {
        $(".slick-track").addClass("thumbs").attr("id", "thumbs1");
        try
        {
            var head = document.getElementsByTagName('HEAD')[0];

            if ($("#photoswipe1").length <= 0) {
                var link = document.createElement("script");
                link.src = "/components/gallery/photoswipe/js/photoswipe.js";
                link.id = "photoswipe1";
                head.appendChild(link);
            }
            if ($("#photoswipe2").length <= 0) {
                var link = document.createElement("script");
                link.src = "/components/gallery/photoswipe/js/photoswipe-ui-default.js";
                link.id = "photoswipe2";
                head.appendChild(link);
            }
            if ($("#photoswipe3").length <= 0) {
                var link = document.createElement("script");
                link.src = "/components/gallery/photoswipe/photoswipe.jsp";
                link.id = "photoswipe3";
                head.appendChild(link);
            }

        } catch (e) {}
    });
</script>

<%}%>