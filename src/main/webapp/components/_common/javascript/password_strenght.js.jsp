<%
    sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
        taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
        taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
        taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
        taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
        taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
        taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
        taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
        taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%@
        taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

function chceckPasswordStrenght(inputElement, style, prefix) {
    var strength = document.getElementById('strength');
    var strongRegex = new RegExp("^(?=.{8,})(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*\\W).*$", "g");
    var mediumRegex = new RegExp("^(?=.{7,})(((?=.*[A-Z])(?=.*[a-z]))|((?=.*[A-Z])(?=.*[0-9]))|((?=.*[a-z])(?=.*[0-9]))).*$", "g");
    var enoughRegex = new RegExp("(?=.{4,}).*", "g");

    if(inputElement == undefined){
        inputElement = $('input[name=password]');
    }
    if(style == undefined){
        style = 0;
    }
    if(prefix == undefined){prefix = '';}


    switch (style){
        case  1:
            var weak = 	 '<span style="position: relative;top: -15px;text-align: right; width: 100%;display: block;font-size: 11px;"><span style=" padding-right: 5px; margin-bottom: 6px; position: relative; bottom: 2px; ">'+prefix+'<iwcm:text key="user.admin.password.strength"/>: <iwcm:text key="user.admin.password.weak"/> </span> <span style="background-color:#d00e0e; width:100%; height:3px; display: block;"> </span></span>';
            var medium = '<span style="position: relative;top: -15px;text-align: right; width: 100%;display: block;font-size: 11px;"><span style=" padding-right: 5px; margin-bottom: 6px; position: relative; bottom: 2px; ">'+prefix+'<iwcm:text key="user.admin.password.strength"/>: <iwcm:text key="user.admin.password.medium"/> </span> <span style="background-color:orange; width:100%; height:3px; display: block;"> </span></span>';;
            var strong = '<span style="position: relative;top: -15px;text-align: right; width: 100%;display: block;font-size: 11px;"><span style=" padding-right: 5px; margin-bottom: 6px; position: relative; bottom: 2px; ">'+prefix+'<iwcm:text key="user.admin.password.strength"/>: <iwcm:text key="user.admin.password.strong"/> </span> <span style="background-color:#29c01a; width:100%; height:3px; display: block;"> </span></span>';
            break;

        default:
            var weak = prefix+'<iwcm:text key="user.admin.password.strength"/>: <span style="color:red"><iwcm:text key="user.admin.password.weak"/></span>';
            var medium = prefix+'<iwcm:text key="user.admin.password.strength"/>: <span style="color:orange"><iwcm:text key="user.admin.password.medium"/></span>';
            var strong = prefix+'<iwcm:text key="user.admin.password.strength"/>: <span style="color:green"><iwcm:text key="user.admin.password.strong"/></span>';
            break;

    }

    var val = inputElement.val();

    if (val.length==0) {
        strength.innerHTML = weak;
    } else if (false == enoughRegex.test(val)) {
        strength.innerHTML = weak;
    } else if (strongRegex.test(val)) {
        strength.innerHTML = strong;
    } else if (mediumRegex.test(val)) {
        strength.innerHTML = medium;
    } else {
        strength.innerHTML = weak;
    }
}