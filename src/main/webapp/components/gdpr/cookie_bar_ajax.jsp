<%@page import="java.util.List" %>
<%@page import="sk.iway.iwcm.i18n.Prop" %>
<%@page import="java.util.Iterator" %>
<%@page import="java.util.HashSet" %>
<%@page import="java.util.HashMap" %>
<%@page import="sk.iway.iwcm.components.gdpr.CookieManagerBean" %>
<%@page import="java.util.ArrayList" %>
<%@page import="java.util.LinkedList" %>
<%@page import="sk.iway.iwcm.components.gdpr.CookieManagerDB" %>
<%@page import="sk.iway.iwcm.users.UsersDB" %>
<%
    sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ page import="sk.iway.iwcm.Tools" %>
<%@ page import="sk.iway.iwcm.doc.DocDetails" %>
<%@ page import="sk.iway.iwcm.doc.GroupDetails" %>
<%@ page import="sk.iway.iwcm.doc.GroupsDB" %>
<%@ page import="sk.iway.iwcm.doc.DocDB" %>
<%@
        taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@
        taglib prefix="iway" uri="/WEB-INF/iway.tld" %>



<%@
        taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@
        taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@
        taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%

    //    Identity user = UsersDB.getCurrentUser(request);
//    if(!Constants.getBoolean("cookieBannerAllowed") && (user == null ||  user != null && !user.isAdmin()) )
//    {
//        return;
//    }

    String lng = PageLng.getUserLng(request);
    String extLng = Tools.getStringValue(request.getParameter("lng"), lng);
    pageContext.setAttribute("lng", extLng);
	Prop prop = Prop.getInstance(extLng);

    int docId = Tools.getDocId(request);
    String fieldA = "";
    if (docId > -1) {
        DocDetails doc = DocDB.getInstance().getDoc(docId);
        if (doc != null) {
            GroupDetails group = doc.getGroup();
            if (group != null) {
                GroupDetails rootGroup = GroupsDB.getInstance().getGroup(GroupsDB.getInstance().getRoot(group.getGroupId()));
                if (rootGroup != null) {
                    fieldA = rootGroup.getFieldA();
                }
            }
        }
    }

    CookieManagerDB cookieManagerDB = new CookieManagerDB();
    List<CookieManagerBean> cookieList = new ArrayList<CookieManagerBean>();
    cookieList.addAll(cookieManagerDB.getAll());
    HashSet<String> set = new HashSet<String>();
    for (CookieManagerBean cmb : cookieList) {
        set.add(cmb.getClassification());
    }

    Iterator<String> itr = set.iterator();
    String className = "";
    String key = "";
    String classActive = "active";
    String textKey = "";

    String cookeCategories = "_"+Tools.getCookieValue(request.getCookies(), "enableCookieCategory", "")+"_";

    String tabClasses = "container-inner tab-pane fade show in active";
    if ("3".equals(Constants.getString("bootstrapVersion"))) {
        tabClasses = "container-inner tab-pane fade in active";
    }
%>

<div class="in-page-nav">
    <ul class="nav nav-tabs">
        <li class="nav-item active">
            <a class="nav-link active" id="prohlaseni-tab" href="#prohlaseni-o-cookies" data-toggle="tab" data-bs-toggle="tab" data-bs-target="#prohlaseni-o-cookies"
               role="tab" aria-controls="prohlaseni-o-cookies" aria-selected="true">
                <iwcm:text key="cookies.bar.prohlaseni"/>
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link" id="o-cookies-tab" href="#o-cookies" role="tab"  data-toggle="tab" data-bs-toggle="tab" data-bs-target="#o-cookies"
               aria-controls="o-cookies" aria-selected="false">
                <iwcm:text key="cookies.bar.ocookies"/>
            </a>
        </li>
    </ul>
    <span class="close"><iwcm:text key="cookies.bar.close"/></span>
    <div class="container-fluid">
        <div class="tab-content">
            <div class="<%=tabClasses%>" id="prohlaseni-o-cookies">
                <ul class="cookies-list-menu">
                    <% while (itr.hasNext()) {
                        className = itr.next();
                        key = "cookies.bar." + className;%>
                    <li class="<%=classActive%>">
                        <span class="checkboxAccept">
                            <input type="checkbox" value="Alowed" data-rel="<%=className%>"
                                   id="checkboxAccept-<%=className%>" name="check" <%
                                if ("nutne".equals(className))
                                    out.print("disabled=\"disabled\" checked=\"checked\"");
                                else if (cookeCategories.contains("_"+className+"_"))
                                    out.print("checked=\"checked\"");
                            %> />
                            <label class="chck-<%=className%>" for="checkboxAccept-<%=className%>"><%=prop.getText(key) %></label>
                        </span>
                        <a href="javascript:void(0)" data-href="#cookies-<%=className%>" role="tab" aria-controls="cookies-<%=className%>"
                            <% if ("nutne".equals(className)) {
                                %>aria-selected="true"<%
                            } else {
                                %>aria-selected="false"<%
                            } %> >
                            <%=prop.getText(key) %>
                        </a>
                    </li>
                    <%
                            classActive = "";
                        }
                    %>
                </ul>
                <%
                    itr = set.iterator();
                    while (itr.hasNext()) {
                        className = itr.next();
                        List<CookieManagerBean> listCmb = cookieManagerDB.findByClassification(className);%>
                <div id="<%="cookies-"+className%>" class="cookies-list-content">
                    <div>
                        <%--div style="float:left;">
                            <form action="" class="gdprForm">
                                <div class="checkboxAccept">
                                    <input type="checkbox" value="Alowed" id="checkboxAccept-<%=className%>" name="check" <%if("nutne".equals(className)) out.print("disabled=\"disabled\""); %> checked="checked" />
                                    <label for="checkboxAccept-<%=className%>"></label>
                                </div>
                            </form>
                        </div--%>

                        <%
                            key = "cookies.bar." + className;
                            String keyText = "cookies.bar." + className + ".text";
                        %>

                        <h2>
                            <%=prop.getText(key)%>
                        </h2>

                        <p>
                            <%=prop.getText(keyText)%>
                        </p>

                    </div>
                    <div class="table-responsive">
                        <table class="table cookies-table">
                            <thead>
                            <tr>
                                <th><iwcm:text key="cookies.bar.table.meno"/></th>
                                <th><iwcm:text key="cookies.bar.table.poskytovatel"/></th>
                                <th><iwcm:text key="cookies.bar.table.ucel"/></th>
                                <th><iwcm:text key="cookies.bar.table.platnost"/></th>
                                <th><iwcm:text key="cookies.bar.table.typ"/></th>
                            </tr>
                            </thead>
                            <tbody>
                            <%for (CookieManagerBean cmb : listCmb) {%>
                            <tr>
                                <td title="<%=cmb.getCookieName()%>"><%=cmb.getCookieName()%>
                                </td>
                                <td><%
                                    textKey = "components.gdpr.cookies." + cmb.getCookieName() + ".provider";
                                    if (prop.getText(textKey).contains("!ACTUAL_DOMAIN!")) {
                                        textKey = Tools.getBaseHref(request);
                                        textKey = Tools.replace(textKey, "http://", "");
                                        textKey = Tools.replace(textKey, "https://", "");
                                    }
                                    out.print(prop.getText(textKey));
                                %>
                                </td>
                                <td>
                                    <%
                                        textKey = "components.gdpr.cookies." + cmb.getCookieName() + ".purpouse";
                                        out.print(prop.getText(textKey));
                                    %>
                                </td>
                                <%textKey = "components.gdpr.cookies." + cmb.getCookieName() + ".validity"; %>
                                <td title="<%=prop.getText(textKey) %>">
                                    <%out.print(prop.getText(textKey));%>
                                </td>
                                <td title="<%=cmb.getType() %>"><%=cmb.getType() %>
                                </td>
                            </tr>
                            <%} %>
                            </tbody>
                        </table>
                    </div>
                </div>
                <%
                    }
                %>
                <button class="btn btn-akcept btn-primary">
                   <iwcm:text key="cookies.bar.akceptovat"/>
                </button>
            </div>
            <div class="container-inner tab-pane fade" id="o-cookies">
                <div class="container-padding">
                    <iwcm:text key='cookies.bar.o_cookies'/>
                    <ul>
                        <%
                            itr = set.iterator();
                            while (itr.hasNext()) {
                                className = itr.next();
                                key = "cookies.bar." + className;%>
                        <li>
                            <%=prop.getText(key) %>
                        </li>
                        <%
                            }
                        %>
                    </ul>
                    <button class="btn btn-akcept btn-primary">
                        <iwcm:text key="cookies.bar.akceptovat"/>
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>
