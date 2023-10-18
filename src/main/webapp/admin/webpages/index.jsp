<%
response.sendRedirect("/admin/v9/webpages/web-pages-list/" + (request.getQueryString()!=null ? "?"+request.getQueryString() : "" ));
%>