<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
    boolean hideBasketNavbar = Tools.getBooleanValue(pageParams.getValue("hideBasketNavbar", ""), false);
    if(hideBasketNavbar == false) {
        GroupsDB groupsDB = GroupsDB.getInstance();
        StringBuilder navbar = new StringBuilder();
        GroupDetails pageGroupDetails = (GroupDetails)request.getAttribute("pageGroupDetails");

        if (pageGroupDetails != null)
        {
            int domainId = sk.iway.iwcm.common.CloudToolsForCore.getDomainId();

            int failsafe = 0;
            GroupDetails actualGroup = pageGroupDetails;
            int insertedItems = 0;
            while (failsafe-- < 10)
            {
                if (actualGroup == null || actualGroup.getGroupId()==domainId || actualGroup.getParentGroupId()<1) break;
                if (actualGroup.getDefaultDocId() > 1)
                {
                    String docLink = docDB.getDocLink(actualGroup.getDefaultDocId(), request);
                    if (Tools.isNotEmpty(docLink) && docLink.indexOf("showdoc.do")==-1)
                    {
                        StringBuilder item = new StringBuilder();
                        item.append("<li><a href='").append(docLink).append("'>").append(actualGroup.getNavbarNameNoAparam()).append("</a></li>\n");

                        navbar.insert(0, item);
                        insertedItems++;
                    }
                }

                actualGroup = groupsDB.getGroup(actualGroup.getParentGroupId());
            }

            if (insertedItems>1)
            {
                %>
                <div class="productsNavbar">
                    <ul>
                        <%=navbar.toString()	%>
                    </ul>
                </div>
                <%
            }
        }
    }
%>

<style>
    div.productsNavbar {
        border: 1px solid #d4d4d4;
        border-radius: 5px;
        height: 33px;
        margin-bottom: 15px;
    }

    div.productsNavbar ul {
        height: 33px;
        margin: 0px;
        padding: 0px;
        padding-left: 10px;
    }

    div.productsNavbar li {
        float: left;
        margin-right: 10px;
        list-style: none;
        background: none;
        line-height: 33px;
        height: 33px;
        background: url(/components/basket/img/bg-breadcrumg-li.png) right center no-repeat;
        padding: 0px;
    }

    div.productsNavbar li a {
        padding: 0 30px 0 10px;
        display: block;
    }

    div.productsNavbar li:before {
        display: none;
    }
</style>