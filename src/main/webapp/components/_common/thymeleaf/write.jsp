<%@ page pageEncoding="utf-8" %><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%

//jsp pouzite pri vykonani iwcm:write v thymeleaf sablone

sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");

String KEY = "thymeleaf_write_name";
String objectName = (String)request.getAttribute(KEY);
String objectValue = "";
request.removeAttribute(KEY);

//System.out.println("----> WRITE1: name="+objectName+" value="+objectValue);

if (objectName == null) return;

if (request.getAttribute(objectName)==null) {
    //objekt neexistuje, prenesieme ho ako hodnotu
    objectValue = objectName;
    objectName = "";
}
//System.out.println("----> WRITE2: name="+objectName+" value="+objectValue);
%><iwcm:write name="<%=objectName%>"><%=objectValue%></iwcm:write>