<% 
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript"); 
	sk.iway.iwcm.PathFilter.setStaticContentHeaders("/cache/common.js", null, request, response);
%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.editor.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>

<%@page import="sk.iway.iwcm.Tools"%>

// pridanie textov do text.properties
jQuery(function(a) 
{
	a.datepicker.regional.webjet = 
		{
			closeText:" <iwcm:text key='javascript.datepicker.closeText'/>",
			prevText:"&#x3c<iwcm:text key='javascript.datepicker.prevText'/>;",
			nextText:"<iwcm:text key='javascript.datepicker.nextText'/>&#x3e;",
			currentText:"<iwcm:text key='javascript.datepicker.currentText'/>",
			monthNames:[<iwcm:text key="javascript.datepicker.monthNames"/>],
			monthNamesShort:[<iwcm:text key='javascript.datepicker.monthNamesShort'/>],
			dayNames:[<iwcm:text key='javascript.datepicker.dayNames'/>],
			dayNamesShort:[<iwcm:text key='javascript.datepicker.dayNamesShort'/>],
			dayNamesMin:[<iwcm:text key='javascript.datepicker.dayNamesMin'/>],
			dateFormat:"dd.mm.yy",
			firstDay:0,
			isRTL:false
		};
	a.datepicker.setDefaults(a.datepicker.regional.webjet);
});