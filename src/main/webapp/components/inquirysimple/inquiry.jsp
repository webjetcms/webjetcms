<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ page import="java.net.URLDecoder" %>
<%@ page import="org.apache.commons.codec.binary.StringUtils" %>
<%@ page import="org.apache.commons.codec.binary.Base64" %>
<%@ page import="com.fasterxml.jackson.databind.ObjectMapper" %>
<%@ page import="com.fasterxml.jackson.databind.JsonNode" %>
<%@ page import="java.util.UUID" %>
<%@ page import="java.util.Map" %>
<%@ page import="sk.iway.iwcm.components.inquirySimple.InquiryService" %>
<%@ page import="sk.iway.iwcm.components.inquirySimple.InquiryResultBean" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

    String lng = PageLng.getUserLng(request);
    pageContext.setAttribute("lng", lng);
    PageParams pageParams = new PageParams(request);
    InquiryService inqs = new InquiryService();

    String name = pageParams.getValue("name", "");
    String formId = pageParams.getValue("formId", "");
    String randomId = "form_" + UUID.randomUUID().toString().substring(0, 7);
    String active = pageParams.getValue("active", "true");
    String multiAnswer = pageParams.getValue("multiAnswer", "false");
    String json = pageParams.getValue("editorData", "W10=");
    Map<String, InquiryResultBean> results = inqs.getResultsForForm(formId);

    ObjectMapper om = new ObjectMapper();
    JsonNode items = om.readTree(URLDecoder.decode(StringUtils.newStringUtf8(Base64.decodeBase64(json)), "UTF-8"));
%>
<style>
    .inquiryBoxDefault .bar {
        height:100%;
        max-height: 20px;
        background:red;
    }
</style>
<form class="inquiryBoxDefault inquiryBox" id="<%=randomId%>">
    <h3><%=name%></h3>
    <span class="errorMsg"></span>
    <% for(int i = 0; i < items.size(); i++) { %>
    <% JsonNode item = items.get(i);
       String itemId = item.get("id").asText();
    %>
    <div>
        <label for="item_<%=itemId%>">
            <% if("true".equalsIgnoreCase(active)) { %>
                <input type="<%="true".equalsIgnoreCase(multiAnswer) ? "checkbox" : "radio"%>" name="answer" id="item_<%=itemId%>" value="<%=itemId%>">
            <% } %>
            <%=item.get("question").asText()%>
        </label>

        <span class="pull-right" id="result_<%=itemId%>">
            <span class="count"><%=results.get(itemId) != null ? results.get(itemId).getCount() : 0%></span> hlasov /
            <span class="percent"><%=results.get(itemId) != null? results.get(itemId).getPercent() : 0%></span> %
        </span>
        <div class="progress progress-info active" id="progress_<%=itemId%>">
            <div class="bar" style="width: <%=results.get(itemId) != null ? (int)results.get(itemId).getPercent() : 0%>%;"></div>
        </div>
    </div>
    <% } %>
    <%  if("true".equalsIgnoreCase(active)) { %>
        <button type="button" class="btn btn-primary" ><iwcm:text key="components.inquirysimple.sendInquiry" /></button>
    <% } %>
</form>

<iwcm:script type="text/javascript">
    $(function() {
        $('#<%=randomId%> button.btn-primary').on('click', function() {
            var formId = '<%=formId%>';
            var answers = Array.from($('#<%=randomId%> input').map(function() { return $(this).is(':checked') ? $(this).val() : null }));

            console.log('answers:');
            console.log(answers);

            $.ajax({
                type: "POST",
                url: '/rest/inquirySimple/saveAnswer/',
                data: JSON.stringify({formId: formId, answers: answers}),
                contentType: 'application/json',
                dataType: 'json',
                processData: false,
                success: function(data) {
                    console.log('success');
                    console.log(data);

                    if(data.error) {
                        console.log('error');
                        $('#<%=randomId%> .errorMsg').text('<iwcm:text key="components.inquirysimple.error"/>');
                    } else {
                        console.log('data success');
                        for(key in data) {
                            console.log("key=" + key);
                            console.log("value=" + data[key]);
                            console.log("obj=");
                            console.log($('#result_' + key + ' span'));

                            $('#result_' + key + ' .count').text(data[key].count);
                            $('#result_' + key + ' .percent').text(data[key].percent);
                            $('#progress_' + key + ' .bar').css("width", data[key].percent + '%');
                        }
                    }
                },
                error: function(jqXhr, textStatus, errorThrown) {
                    console.log("error");
                    console.log(errorThrown);
                }
            });
        });
    });

</iwcm:script>
