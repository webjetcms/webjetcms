<%@ page pageEncoding="windows-1250" %>
<c:if test="${ninja.debug}">
<div class="md-debug-info">
    <div class="debug-table">
        <div class="debug-table-row"><div class="debug-table-cell">${ninja.temp.lngIso} ${ninja.temp.charset} ${ninja.webjet.generatedTime}</div></div>
        <div class="debug-table-row"><div class="debug-table-cell">${ninja.page.urlDomain}</div></div>
        <div class="debug-table-row"><div class="debug-table-cell">${ninja.page.urlPath}</div></div>
    </div>
    <div class="debug-table">
        <c:forEach items="${ninja.page.urlParameters}" var="parameter">
        <div class="debug-table-row">
            <div class="debug-table-cell">${parameter.key}</div>
            <div class="debug-table-cell">
                <c:forEach items="${parameter.value}" var="value">
                    <span>${value}</span>
                </c:forEach>
            </div>
        </div>
        </c:forEach>
    </div>
    <div class="debug-table">
        <div class="debug-table-row">
            <div class="debug-table-cell">
                ${ninja.userAgent.browserName} ${ninja.userAgent.browserVersion}<br>
                    ${ninja.userAgent.deviceType}<br>
                ${ninja.userAgent.deviceOS}
            </div>
        </div>
        <div class="debug-table-row">
            <div class="debug-table-cell html-class-list">no-js</div>
        </div>
        <div class="debug-table-row">
            <div class="debug-table-cell window-info">
                <strong class="device-type-from-css"></strong>&nbsp;
                <strong class="device-resize-w-info">false</strong>&nbsp;/&nbsp;<strong class="device-resize-h-info">false</strong>&nbsp;
                <strong class="device-size"></strong><strong class="device-resize-new-size"></strong>
            </div>
        </div>
    </div>
</div>
</c:if>