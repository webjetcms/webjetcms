<%@page import="sk.iway.iwcm.Tools"%><%@page import="java.awt.Color"%><%@page import="sk.iway.iwcm.components.reservation.ImageUserDay"%><%
    // avoid caching in browser
   response.setHeader ("Pragma", "no-cache");
   response.setHeader ("Cache-Control", "no-cache");
   response.setDateHeader ("Expires",0);
   response.setContentType("image/png");
String strDate = "";
int uid = 0;

if(Tools.getIntValue(request.getParameter("resId"),0) > 0)
{
   if(request.getParameter("datum") != null) strDate = (String) request.getParameter("datum");
   response.getOutputStream().flush();
   ImageUserDay iud = new ImageUserDay();
   iud.setBoundaryLineColor(new Color(100, 100, 150));
   iud.setHoursColor(new Color(100, 100, 200));
   iud.setImgDate(strDate);
   //iud.setMyHourPixelWidth(16);
   if(request.getParameter("time") != null) iud.setStartTime(((String)request.getParameter("time")));
   if(request.getParameter("stoptime") != null) iud.setStopTime(((String)request.getParameter("stoptime")));
   response.setContentType(iud.createImageFromReservation(response.getOutputStream(), Tools.getIntValue(request.getParameter("resId"),0)));
   response.getOutputStream().flush();
}
%>