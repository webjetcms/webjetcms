<%@ page import="sk.iway.spirit.MediaDB" %>
<%@ page import="sk.iway.spirit.model.Media" %>
<%@ page import="java.util.List" %>
<%@ page import="sk.iway.iwcm.doc.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ page pageEncoding="utf-8" trimDirectiveWhitespaces="true" %>

<%
    DocDetails doc = (DocDetails)request.getAttribute("docDetails");
%>

<section class="md-product-detail">
    <div class="row">
        <div class="col-12 col-md-5 offset-md-1 order-2 mt-4 mt-md-0">
            <h1><iwcm:write name="doc_title"/></h1>
            <p>
                <%
                    if(doc.isInPerexGroup(1)) out.print("<span class='badge badge-danger'>Akcia</span> ");
                    if(doc.isInPerexGroup(2)) out.print("<span class='badge badge-warning'>Top</span> ");
                    if(doc.isInPerexGroup(3)) out.print("<span class='badge badge-primary'>Novinga</span> ");
                    if(doc.isInPerexGroup(4)) out.print("<span class='badge badge-success'>+ Darček</span> ");
                %>
            </p>
            <p class="perex">
                <iwcm:write name="perex_pre"/>
            </p>

            <form>
                <iwcm:write>!INCLUDE(/components/attributes/attribute-select-html.jsp, htmlName=&quot;size&quot;, htmlId=&quot;size&quot;, htmlClass=&quot;form-control&quot; )!</iwcm:write>
            </form>
            <p>
                <%
                    if(Integer.parseInt(doc.getFieldM()) > 0){
                %>
                <small class="badge badge-success">
                    <%=doc.getFieldM()%> kusov na sklade
                </small>
                <%
                } else{
                %>
                <small class="badge badge-warning">
                    Nie je skladom
                </small>
                <%
                    }
                %>
                <span class="badge badge-danger">Zľava 18%</span>
            </p>
            <iwcm:write>!INCLUDE(/components/eshop/basket/addbasket.jsp)!</iwcm:write>
        </div>
        <div class="col-12 col-md-6 order-1">
            <iwcm:write>!INCLUDE(/components/eshop/media/media-slider.jsp, group=&quot;2,&quot;, mainSlider=true)!</iwcm:write>
            <div class="sub-slider slider">
                <iwcm:write>!INCLUDE(/components/eshop/media/media-slider.jsp, group=&quot;2,&quot;)!</iwcm:write>
            </div>
            <p class="share">
                <span class="text">kód produktu 368920</span>
                <span class="d-inline-block float-right">
                    <span class="text">Zdielať</span>
                    <button class="btn btn-primary btn-circle"><i class="fab fa-facebook-f"></i></button>
                    <button class="btn btn-danger btn-circle ml-2"><i class="far fa-envelope"></i></button>
                </span>
            </p>
        </div>
    </div>
</section>