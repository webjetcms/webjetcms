<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"
    import="sk.iway.iwcm.PageLng,sk.iway.iwcm.i18n.Prop,org.springframework.web.util.HtmlUtils" %><%
    Prop prop = Prop.getInstance(request);
    String title = prop.getText("branches.section.production.js");
    String backUrl = request.getContextPath() + "/admin/v9/webpages/web-pages-list/";
%><!doctype html>
<html lang="<%= HtmlUtils.htmlEscape(PageLng.getUserLng(request)) %>">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= HtmlUtils.htmlEscape(title) %> | WebJET CMS</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/admin/v9/dist/css/main.style.css">
    <style>
        dialog { position: fixed; inset: 0; margin: auto; width: min(30rem, calc(100vw - 2rem)); color: var(--bs-body-color); }
        dialog::backdrop { background: rgba(19, 21, 27, .45); }
    </style>
</head>
<body>
    <dialog open id="branch-section-dialog" class="p-4 border rounded" aria-labelledby="branch-section-heading" aria-describedby="branch-section-message">
        <h1 id="branch-section-heading" class="h4"><%= HtmlUtils.htmlEscape(title) %></h1>
        <p id="branch-section-message"><%= HtmlUtils.htmlEscape(prop.getText("branches.section.confirm.js", (String) request.getAttribute("productionBranchName"))) %></p>
        <div class="d-flex flex-wrap gap-2">
            <a class="btn btn-outline-secondary" href="<%= backUrl %>" autofocus><%= HtmlUtils.htmlEscape(prop.getText("branches.section.back.js")) %></a>
            <button type="button" class="btn btn-primary" data-continue="<%= HtmlUtils.htmlEscape((String) request.getAttribute("productionContinuation")) %>"><%= HtmlUtils.htmlEscape(prop.getText("branches.section.continue.js")) %></button>
        </div>
    </dialog>
    <script>
        const dialog = document.querySelector('dialog');
        dialog.close();
        dialog.showModal();
        const proceed = dialog.querySelector('[data-continue]');
        proceed.onclick = () => location.assign(proceed.dataset.continue + location.hash);
        dialog.addEventListener('cancel', () => location.assign(dialog.querySelector('.btn-outline-secondary').href));
    </script>
</body>
</html>
