<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>

<!-- Search Form -->
<form method="post">
    <div class="form-group">
        <label for="name"><iwcm:text key="editor.search"/></label>
        <input type="text" id="name" name="name" class="form-control" placeholder="Enter name" value="ľščťžýá" />
    </div>
    <button type="submit" class="btn btn-primary"><iwcm:text key="button.submit"/></button>
</form>

<h2>JSP version</h2>

<p class="name-request">${param.name}</p>
<p class="name-model">${name}</p>
<p class="encoding">${encoding}</p>
<p class="userId">${userId}</p>