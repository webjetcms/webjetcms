<%
    sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*"%>
<%@ page import="sk.iway.iwcm.doc.GroupsDB" %>
<%@ page import="sk.iway.iwcm.doc.PerexGroupBean" %>
<%@ page import="java.util.List" %>
<%@ page import="sk.iway.iwcm.doc.DocDB" %>
<%@ page import="java.util.Arrays" %>
<%@
        taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@
        taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@
        taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %><%@
        taglib uri="/WEB-INF/struts-html.tld" prefix="html" %><%@
        taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><%@
        taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %><%@
        taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%@
        taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %><%@
        taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
        taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>

<%
    String[] tmpPerexGroups = request.getParameterValues("perexGroups");
    List<String> requestedPerexGroups = null;
    if(tmpPerexGroups != null) {
        requestedPerexGroups = Arrays.asList(tmpPerexGroups);
    }


    int groupId = Integer.parseInt((String)request.getAttribute("group_id"));
    List<PerexGroupBean> perexGroups = DocDB.getInstance().getPerexGroups(groupId);
%>
<%--<% request.setAttribute("documents", "!INCLUDE(/components/media/media.jsp)!"); %>--%>
<%--<iwcm:write name="documents" />--%>

<div id="perexGroupSelectorBox">
    <label id="selectCategory">
        Vybrať kategóriu
    </label>
    <form id="perexGroupSelector" action="" method="get" style="display: none">
        <div class="list">
            <% for(PerexGroupBean perexGroup : perexGroups) { %>
                <label for="perexGroup-<%=perexGroup.getPerexGroupId()%>">
                    <input
                            id="perexGroup-<%=perexGroup.getPerexGroupId()%>"
                            type="checkbox"
                            name="perexGroups"
                            value="<%=perexGroup.getPerexGroupId()%>"
                            <% if(requestedPerexGroups != null && requestedPerexGroups.indexOf(perexGroup.getPerexGroupId() + "") > -1) { %>checked="checked"<% } %>
                    > <%=perexGroup.getPerexGroupName()%>
                </label>
            <% } %>
        </div>
        <button class="submit btn btn-danger">Zobraziť</button>
    </form>
</div>
<script>
    $(function() {
        $("#perexGroupSelectorBox .submit").on("click", function(e) {
            $('#perexGroupSelector').submit();
        })

        $("#selectCategory").on('click', function(){
            $('#perexGroupSelector').slideToggle('fast')
        })
    })
</script>
<style>
    #perexGroupSelectorBox {
        position: absolute;
        width: 200px;
        background: #FFF;
        right: 8px;
        top: -53px;
        border: 1px solid grey;
        padding: 6px 0;
        border-radius: 5px;
        z-index: 9;
    }
    label#selectCategory {
        margin-bottom: 0;
        cursor: pointer;
        display: block;
        height: 100%;
        width: 100%;
    }
    form#perexGroupSelector .list{
        max-height: 220px;
        overflow: auto;
        padding: 10px;
        text-align: left;
    }
    #perexGroupSelectorBox .list label {
        display: block;
        border-bottom: 1px solid #efefef;
        margin: 0;
        padding: 8px 3px;
        cursor: pointer;
    }
    #perexGroupSelector button {
        margin-top: 10px;
    }
    .sp #perexGroupSelectorBox {
        position: absolute;
        top: 0; right: 0; left: 210px;
        text-align: center;
        margin: 0 0 20px 0;
        float: right;
    }
</style>
