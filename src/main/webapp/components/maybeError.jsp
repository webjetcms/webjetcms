<%
String error = (String)request.getAttribute("errorText");
if (error != null && error.indexOf("<p")==-1) out.print("<div class='error alert alert-danger' style='color: red; font-weight: bold;'><p>"+error+"</p></div>");
else if (error != null) out.print("<div class='error alert alert-danger'>"+error+"</div>");

String success = (String)request.getAttribute("successText");
if (success != null) out.print("<div class='success alert alert-success'>"+success+"</div>");
%>