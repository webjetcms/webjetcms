<%@ page pageEncoding="utf-8" trimDirectiveWhitespaces="true" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<section class="md-subcategory-selector">
    <iwcm:write>!INCLUDE(/components/news/news-velocity.jsp, alsoSubGroups=&quot;true&quot;, subGroupsDepth=&quot;1&quot;, docMode=&quot;1&quot;, onlyDefaultDoc=&quot;true&quot;, publishType=&quot;new&quot;, order=&quot;save_date&quot;, ascending=&quot;true&quot;, paging=&quot;false&quot;, pageSize=&quot;10&quot;, offset=&quot;0&quot;, perexNotRequired=&quot;true&quot;, loadData=&quot;false&quot;, checkDuplicity=&quot;false&quot;, contextClasses=&quot;&quot;, cacheMinutes=&quot;10&quot;, template=&quot;news.template.Category selector&quot;, perexGroup=&quot;&quot;, perexGroupNot=&quot;&quot;)!
    </iwcm:write>
</section>