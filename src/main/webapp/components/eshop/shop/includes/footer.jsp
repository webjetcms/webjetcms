<%@ page pageEncoding="utf-8" %>
<section class="md-footer">
    <iwcm:write name="doc_footer"/>
    <div class="copy">
        <div class="container">
            &copy; <iwcm:write>!YEAR!</iwcm:write> Interway, a. s. Všetky práva vyhradené
        </div>
    </div>
</section>

<%
    PageParams pageParams = new PageParams(request);
    int orderFormDocId = pageParams.getIntValue("orderFormDocId", -1);

%>
<iwcm:write>!INCLUDE(/components/basket/basket-popup.jsp, orderFormDocId=<%=orderFormDocId%>)!</iwcm:write>
<iwcm:write>!INCLUDE(/components/gdpr/cookie_bar.jsp)!</iwcm:write>
<%@ include file="debug-info.jsp" %>