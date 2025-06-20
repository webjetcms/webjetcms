<%@ page pageEncoding="utf-8" import="java.util.List,sk.iway.iwcm.doc.*,sk.iway.iwcm.Tools" %>

<section class="md-subcategory-selector">
    <ul>
    <%
    DocDB docDB = DocDB.getInstance();
    List<GroupDetails> subGroups = GroupsDB.getInstance().getGroups(ninja.getDoc().getGroupId());
    for (GroupDetails subGroup : subGroups) {
        DocDetails doc = docDB.getDocAndAddToCacheIfNot(subGroup.getDefaultDocId());
        if (doc != null && doc.isAvailable()) {
            pageContext.setAttribute("doc", doc);
            String perexImage = doc.getPerexImage();
            if (Tools.isEmpty(perexImage)) {
                perexImage = ninja.getDoc().getPerexImage();
            }
            if (Tools.isEmpty(perexImage)) {
                //TODO: get perexImage recursively from current group
                perexImage = "";
            }
            pageContext.setAttribute("perexImage", perexImage);
            %>
                <li><a href="${doc.docLink}"><img src="/thumb${perexImage}?w=60&h=60&ip=4" alt=""> ${doc.title}</a></li>
            <%
        }
    }
    %>
    </ul>
</section>