<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,java.util.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%@page import="sk.iway.iwcm.tags.support_logic.ResponseUtils"%><%@
page import="sk.iway.iwcm.i18n.Prop"%><%

/*--------------------------------------------------------------------------

	Vlozi standartny formularik vyhladavania

	parametre:
		action - na aku stranku sa formularik odosle (default /sk/vyhladavanie.html)
		buttonText - text, ktory sa zobrazi na tlacitku odoslania (default components.search.search)
		inputText - text, ktory sa zobrazi, ked je vyhladavacie policko prazdne (default components.search.title)

--------------------------------------------------------------------------**/


String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

String formAction = pageParams.getValue("action", "/sk/vyhladavanie.html" );
String buttonText = pageParams.getValue("buttonText", Prop.getInstance(request).getText("components.search.search") );
String inputText = pageParams.getValue("inputText", Prop.getInstance(request).getText("components.search.title") );
String defaultInputText = inputText;

if (Tools.getRequestParameter(request, "words") != null)
	inputText = Tools.getRequestParameter(request, "words");

%>


<form action="<%=formAction %>" id="headerSearchForm" name="headerSearchForm">
    <div class="headerSearchFormDiv">
		<input type="text" value="<%=inputText %>" name="words"  id="headerSearchWords" class="headerSearchText" />
		<input type="submit" value="<%=buttonText %>" class="headerSearchSubmit" />
    </div>
</form>

<%=Tools.insertJQuery(request) %>

<script type="text/javascript">
//<![CDATA[
$(document).ready(function(){
	var inputText = '<%=inputText%>'
	var defaultInputText = '<%=defaultInputText%>'

    $("#headerSearchWords").focus(function () {
        var text = $(this).val();
        if(text == defaultInputText){
            $(this).val("");
        }else{
            $(this).val(text);
        }
    });
    $("#headerSearchWords").blur(function () {
        var text = $(this).val();
        if(text == ""){
            $(this).val(defaultInputText);
        }else{
            $(this).val(text);
        }
    });
});
//]]>
</script>
