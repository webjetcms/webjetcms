<%@ page pageEncoding="utf-8" trimDirectiveWhitespaces="true" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<section class="md-category-header">
    <h1><iwcm:write name="doc_title"/></h1>
    <c:if test="${(not empty ninja.doc.perexImage) || (not empty ninja.doc.perexPre)}">
        <div class="row">
            <c:if test="${not empty ninja.doc.perexPre}">
                <div class="col-12 col-md-6 col-lg-7 align-self-center">
                    <p>${ninja.doc.perexPre}</p>
                </div>
            </c:if>
            <c:if test="${not empty ninja.doc.perexImage}">
                <c:choose>
                    <c:when test="${not empty ninja.doc.perexPre}">
                        <div class="col-12 col-md-6 col-lg-5 align-self-center">
                            <img src="/thumb${ninja.doc.perexImage}?w=350&h=250&ip=4" alt="${ninja.doc.title}" class="w-100">
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="col-12 col-md-6 col-lg-5 offset-md-6 offset-lg-7 align-self-center">
                            <img src="/thumb${ninja.doc.perexImage}?w=350&h=250&ip=4" alt="${ninja.doc.title}" class="w-100">
                        </div>
                    </c:otherwise>
                </c:choose>
            </c:if>
        </div>
    </c:if>
</section>