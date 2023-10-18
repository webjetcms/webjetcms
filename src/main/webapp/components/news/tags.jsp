<%
    sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
    taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
    taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
    taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
    taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
    taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
    taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
    taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%@
    taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %><%
    pageContext.include("/sk/iway/iwcm/components/news/News.action?setTags=true");
%><stripes:useActionBean var="tagsActionBean" beanclass="sk.iway.iwcm.components.news.NewsActionBean" />
<c:forEach items="${tagsActionBean.tags}" var="tag">
    <a href="${tagsActionBean.tagClickLink}?tag=${tag.value}" rel="tag">${tag.label}</a>
</c:forEach>
