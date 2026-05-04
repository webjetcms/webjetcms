<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,jakarta.servlet.http.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%
String lng = PageLng.getUserLng(request);
Cookie cookies[] = request.getCookies();
            int i;
            int len = cookies.length;
            Cookie c;
            for (i=0; i<len; i++)
            {
               c = cookies[i];
               if ("lng".equals(c.getName()))
               {
                  lng = c.getValue();
                  break;
               }
            }

pageContext.setAttribute("lng", lng);
//out.println("// lng:"+lng);
%>

$.dpText = {
  TEXT_PREV_YEAR    :  '<iwcm:text key="components.calendar_news.text_prev_year"/>',
  TEXT_PREV_MONTH    :  '<iwcm:text key="components.calendar_news.text_prev_month"/>',
  TEXT_NEXT_YEAR    :  '<iwcm:text key="components.calendar_news.text_next_year"/>',
  TEXT_NEXT_MONTH    :  '<iwcm:text key="components.calendar_news.text_next_month"/>',
  TEXT_CLOSE    :  '<iwcm:text key="components.calendar_news.text_close"/>',
  TEXT_CHOOSE_DATE  :  '<iwcm:text key="components.calendar_news.text_choose_date"/>'
}

Date.dayNames = [<iwcm:text key="components.calendar_news.dayNames"/>];
Date.abbrDayNames = [<iwcm:text key="components.calendar_news.abbrDayNames"/>];
Date.monthNames = [<iwcm:text key="components.calendar_news.monthsNames"/>];
Date.abbrMonthNames = [<iwcm:text key="components.calendar_news.abbrMonthNames"/>];
