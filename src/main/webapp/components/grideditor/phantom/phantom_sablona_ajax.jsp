<%sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.doc.ninja.*" %><%@ page import="sk.iway.iwcm.i18n.Prop" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%
String forward = request.getParameter("forward");
int docId = Tools.getDocId(request);
Prop prop = Prop.getInstance(true);
String htmlCode = prop.getText("phantomjs.html_data.template");
if (Tools.isEmpty(forward)) {
%>

    <body><style type="text/css">
        body {
            background-color: white;
            margin: 0px;
            padding: 0px;
        }
        </style>
    <div id="phantomAutoHeight"> <%
    out.print(htmlCode);
    %>
    </div>
    </body>
<% } else {
    request.setAttribute("doc_data", "<div id=\"phantomAutoHeight\">"+htmlCode+"</div>");
    request.setAttribute("group_htmlhead_recursive", "");
    request.setAttribute("doc_header", "");
    request.setAttribute("doc_footer", "");

    DocDB docDB = DocDB.getInstance();
    GroupsDB groupsDB = GroupsDB.getInstance();
    DocDetails doc = docDB.getDoc(docId);
    GroupDetails group = groupsDB.getGroup(doc.getGroupId());

    ShowDoc.setRequestData(doc, group, docDB, groupsDB, request);
    request.setAttribute("NO WJTOOLBAR", true);
    request.setAttribute("disableInlineEditing", true);

    Ninja.includeNinja(request);
    if (forward.endsWith(".jsp")) {
        pageContext.include(forward);
    } else {
        request.setAttribute("thymeleafTemplateFile", forward);
        request.getRequestDispatcher("/thymeleaf/showdoc").include(request, response);
    }
    %>
    <style type="text/css">
    header, footer, h1, .md-breadcrumb, .noprint, .ly-global-bar { display: none !important; }
    article, section div.container {
        margin-top: 0px !important;
        margin-bottom: 0px !important;
        padding-top: 0px !important;
        padding-bottom: 0px !important;
    }
    section div.container {
        margin-left: 0px;
        margin-right: 0px;
    }
    #phantomAutoHeight {
        padding-top: 12px;
        padding-bottom: 12px;
        width: 1200px !import;
        min-width: 1200px !import;
    }
    </style>
    <%
}
%>