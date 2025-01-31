<%@ page pageEncoding="utf-8" trimDirectiveWhitespaces="true" import="sk.iway.iwcm.Tools"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<script>
    function setParam(name) {
        if("sort" === name) {
            var selectedValue = $('#sort').find(":selected").val();
            if(selectedValue == undefined || selectedValue == null) return;

            //Is value allready set in URL?
            const urlParams = new URLSearchParams(window.location.search);
            var sortValue = urlParams.get('sort');

            if(sortValue === selectedValue) return;

            //There is option, that nothing is set in URL, but default value is set in select
            if(sortValue === null && selectedValue === "date_asc") return;

            //They are not same, change parm and reload page
            urlParams.set(name, selectedValue);
            window.location.search = urlParams;
        } else if("display" === name) {
            var selectedValue = $('#display').val();
            if(selectedValue == undefined || selectedValue == null || selectedValue.length < 1) return;

            if(selectedValue.length > 1 && selectedValue.includes("-1")) {
                selectedValue.splice(selectedValue.indexOf("-1"), 1);
            }

            let joinValues = selectedValue.join(',');

            //Is value allready set in URL?
            const urlParams = new URLSearchParams(window.location.search);
            var sortValue = urlParams.get('display');

            if(sortValue == joinValues) return;

            if(sortValue === null && joinValues === "-1") return;

            //They are not same, change parm and reload page
            urlParams.set(name, selectedValue);
            window.location.search = urlParams;
        }
    }
</script>

<section class="md-product-list" style="display: grid;">
    <div class="toolbar">
        <div class="form-inline float-right">
            <div class="form-group">
                <label for="sort"><iwcm:text key="apps.eshop.shop.sort_by"/></label>
                <select name="sort" id="sort" class="form-control" onchange="setParam('sort')">
                    <option value="price_asc"><iwcm:text key="apps.eshop.shop.sort_option_1"/></option>
                    <option value="price_desc"><iwcm:text key="apps.eshop.shop.sort_option_2"/></option>
                    <option value="date_asc"><iwcm:text key="apps.eshop.shop.sort_option_3"/></option>
                    <option value="date_desc"><iwcm:text key="apps.eshop.shop.sort_option_4"/></option>
                </select>
            </div>
            <div class="form-group">
                <label for="display"><iwcm:text key="apps.eshop.shop.display"/></label>
                <div class="d-inline-block">
                    <iwcm:write>!INCLUDE(/components/eshop/shop/modules/md-perex-select.jsp, htmlName=&quot;sort&quot;, htmlId=&quot;display&quot;, htmlClass=&quot;form-control&quot;, htmlOnchange=&quot;setParam('display')&quot;,)!</iwcm:write>
                </div>
            </div>
            
        </div>
    </div>
    <div class="clearfix"></div>

    <%
        String orderValue = request.getParameter("sort");
        System.out.println("orderValue: " + orderValue);
        String ascendingValue = "true";
        if(Tools.isEmpty(orderValue)) {
            orderValue = "save_date";
        } else if("price_asc".equals(orderValue)) {
            orderValue = "price";
            ascendingValue = "true";
        } else if("price_desc".equals(orderValue)) {
            orderValue = "price";
            ascendingValue = "false";
        } else if("date_asc".equals(orderValue)) {
            orderValue = "save_date";
            ascendingValue = "true";
        } else if("date_desc".equals(orderValue)) {
            orderValue = "save_date";
            ascendingValue = "false";
        }

        String perexValue = request.getParameter("display");
        if(Tools.isEmpty(perexValue)) {
            perexValue = "";
        } else {
            perexValue = perexValue.replace(",", "+");
        }
    %>

    <iwcm:write>!INCLUDE(/components/news/news-velocity.jsp, groupIds=${ninja.doc.group.groupId}, alsoSubGroups=&quot;true&quot;, docMode=&quot;2&quot;, publishType=&quot;new&quot;, order=<%=orderValue%>, ascending=<%=ascendingValue%>, paging=&quot;true&quot;, pageSize=&quot;20&quot;, offset=&quot;0&quot;, perexNotRequired=&quot;false&quot;, loadData=&quot;false&quot;, checkDuplicity=&quot;false&quot;, contextClasses=&quot;sk.iway.tags.CurrencyTag&quot;, cacheMinutes=&quot;10&quot;, template=&quot;news.template.Product list&quot;, perexGroup=<%=perexValue%>, perexGroupNot=&quot;&quot;)!
    </iwcm:write>
</section>

<script>
    const urlParams = new URLSearchParams(window.location.search);
    
    var sortValue = urlParams.get('sort');
    if(sort !== null) {
        if(sortValue !== null) {
            $("#sort").val(sortValue).change(); 
        } else {
            //Default value
            $("#sort").val("date_asc").change();
        }
    }

    var displayValue = urlParams.get('display');
    if(display !== null) { 
        if(displayValue !== null) {
            $("#display").val(displayValue.split(",")).change();
        } else {
            $("#display").val(["-1"]).change();
        }
    }
</script>