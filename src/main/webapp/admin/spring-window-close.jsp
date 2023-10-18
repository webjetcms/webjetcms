<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld"%>
<script type="text/javascript">
    <c:if test="${not empty windowResponse.messages}">
        try {
            <c:forEach var="message" items="${windowResponse.messages}">
                window.opener.WJ.show${message.type}("<iwcm:text key="${message.textKey}" param1="${message.param1}" param2="${message.param2}" param3="${message.param3}" />");
            </c:forEach>
        } catch (e) {}
    </c:if>

    <c:if test="${windowResponse.reloadWebPagesTree}">
        try {
            window.opener.reloadWebpagesTree();
        } catch (e) {}
    </c:if>

    window.close();
</script>

<c:forEach var="message" items="${windowResponse.messages}">
    <p>${message.type}: <iwcm:text key="${message.textKey}" param1="${message.param1}" param2="${message.param2}" param3="${message.param3}"/></p>
</c:forEach>