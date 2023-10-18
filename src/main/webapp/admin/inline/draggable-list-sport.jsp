<%@ page import="sk.iway.iwcm.i18n.Prop" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><iwcm:checkLogon admin="true" perms="menuWebpages"/>
<%
Prop prop = Prop.getInstance(request);

%>
{
  "items": [
    {
      "name": "<iwcm:text key="components.inline.tabs.components.sport.rozpis"/>",
      "img": "/components/_common/admin/inline/toptoolbar/icon_sports_matches.png",
      "value": "!INCLUDE(/components/sportclubs/competition_table.jsp,detailUrl=/match-detail/, type=matches)!",
      "class": "component"
    },
    {
      "name": "<iwcm:text key="components.inline.tabs.components.sport.pavuk"/>",
      "img": "/components/_common/admin/inline/toptoolbar/icon_sports_bracket.png",
      "value": "!INCLUDE(/components/sportclubs/competition_table.jsp,detailUrl=/match-detail/ , type=bracket)!",
      "class": "component"
    }
    ,
    {
      "name": "<iwcm:text key="components.inline.tabs.components.sport.panel"/>",
      "img": "/components/_common/admin/inline/toptoolbar/icon_sports_panel.png",
      "value": "!INCLUDE(/components/sportclubs/competition_table.jsp,detailUrl=/match-detail/, type=panel)!",
      "class": "component"
    }
    ,   {
      "name": "<iwcm:text key="components.inline.tabs.components.sport.match_history"/>",
      "img": "/components/_common/admin/inline/toptoolbar/icon_sports_history.png",
      "value": "!INCLUDE(/components/sportclubs/competition_table.jsp,detailUrl=/match-detail/, type=matches_history)!",
      "class": "component"
    }, {
      "name": "<iwcm:text key="components.inline.tabs.components.sport.match_last"/>",
      "img": "/components/_common/admin/inline/toptoolbar/icon_sports_last_match.png",
      "value": "!INCLUDE(/components/sportclubs/match_info.jsp,detailUrl=/match-detail/ , localFirst=true, infoType=last)!",
      "class": "component"
    }, {
      "name": "<iwcm:text key="components.inline.tabs.components.sport.match_next"/>",
      "img": "/components/_common/admin/inline/toptoolbar/icon_sports_next_match.png",
      "value": "!INCLUDE(/components/sportclubs/match_info.jsp,detailUrl=/match-detail/ , localFirst=true, infoType=next)!",
      "class": "component"
    },  {
      "name": "<iwcm:text key="components.inline.tabs.components.sport.table"/>",
      "img": "/components/_common/admin/inline/toptoolbar/icon_sports_table.png",
      "value": "!INCLUDE(/components/sportclubs/competition_table.jsp,detailUrl=/match-detail/, type=short)!",
      "class": "component"
    },   {
      "name": "<iwcm:text key="components.inline.tabs.components.sport.table_full"/>",
      "img": "/components/_common/admin/inline/toptoolbar/icon_sports_table_full.png",
      "value": "!INCLUDE(/components/sportclubs/competition_table.jsp,detailUrl=/match-detail/, type=full)!",
      "class": "component"
    }


  ]
}