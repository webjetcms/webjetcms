<%@page import="java.util.HashMap"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>

<%@page import="sk.iway.iwcm.components.basket.rest.EshopService"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.io.*,java.io.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%
DocDetails doc = (DocDetails)request.getAttribute("docDetails");

if (doc == null)
  return;

String perexImage = doc.getPerexImageNormal();
String price = doc.getFieldK();
String basketOrder = Tools.getStringValue(request.getParameter("basketAct"), "");
boolean isbasketOrder = basketOrder.equals("orderform") || "saveorder".equals(request.getParameter("act"));
PageParams pageParams = new PageParams(request);
%>

<iwcm:write name="basketSmall"/>

<style>

/*globals*/


img {
  max-width: 100%; }

.preview {
  display: -webkit-box;
  display: -webkit-flex;
  display: -ms-flexbox;
  display: flex;
  -webkit-box-orient: vertical;
  -webkit-box-direction: normal;
  -webkit-flex-direction: column;
      -ms-flex-direction: column;
          flex-direction: column; }
  @media screen and (max-width: 996px) {
    .preview {
      margin-bottom: 20px; } }

.preview-pic {
  -webkit-box-flex: 1;
  -webkit-flex-grow: 1;
      -ms-flex-positive: 1;
          flex-grow: 1; }

.preview-thumbnail.nav-tabs {
  border: none;
  margin-top: 15px; }
  .preview-thumbnail.nav-tabs li {
    width: 18%;
    margin-right: 2.5%; }
    .preview-thumbnail.nav-tabs li img {
      max-width: 100%;
      display: block; }
    .preview-thumbnail.nav-tabs li a {
      padding: 0;
      margin: 0; }
    .preview-thumbnail.nav-tabs li:last-of-type {
      margin-right: 0; }

.tab-content {
  overflow: hidden; }
  .tab-content img {
    width: 100%;
    -webkit-animation-name: opacity;
            animation-name: opacity;
    -webkit-animation-duration: .3s;
            animation-duration: .3s; }

.card {
  background: <%=pageParams.getValue("bgColor", "#eee") %>;
  padding: 3em;
  line-height: 1.5em; }

@media screen and (min-width: 997px) {
  .wrapper {
    display: -webkit-box;
    display: -webkit-flex;
    display: -ms-flexbox;
    display: flex; } }

.details {
  display: -webkit-box;
  display: -webkit-flex;
  display: -ms-flexbox;
  display: flex;
  -webkit-box-orient: vertical;
  -webkit-box-direction: normal;
  -webkit-flex-direction: column;
      -ms-flex-direction: column;
          flex-direction: column; }

.colors {
  -webkit-box-flex: 1;
  -webkit-flex-grow: 1;
      -ms-flex-positive: 1;
          flex-grow: 1; }

.product-title, .price, .sizes, .colors {
  text-transform: UPPERCASE;
  font-weight: bold; }

.checked, .price span {
  color: #ff9f1a; }

.product-title, .rating, .product-description, .price, .vote, .sizes {
  margin-bottom: 15px; }

.product-title {
  margin-top: 0; }

.size {
  margin-right: 10px; }
  .size:first-of-type {
    margin-left: 40px; }

.color {
  display: inline-block;
  vertical-align: middle;
  margin-right: 10px;
  height: 2em;
  width: 2em;
  border-radius: 2px; }
  .color:first-of-type {
    margin-left: 20px; }

.add-to-cart, .like {
  background: #ff9f1a;
  padding: 1.2em 1.5em;
  border: none;
  text-transform: UPPERCASE;
  font-weight: bold;
  color: #fff;
  -webkit-transition: background .3s ease;
          transition: background .3s ease; }
  .add-to-cart:hover, .like:hover {
    background: #b36800;
    color: #fff;
      font-weight: bold;
     }

.not-available {
  text-align: center;
  line-height: 2em; }
  .not-available:before {
    font-family: fontawesome;
    content: "\f00d";
    color: #fff; }

.orange {
  background: #ff9f1a; }

.green {
  background: #85ad00; }

.blue {
  background: #0076ad; }

.tooltip-inner {
  padding: 1.3em; }

@-webkit-keyframes opacity {
  0% {
    opacity: 0;
    -webkit-transform: scale(3);
            transform: scale(3); }
  100% {
    opacity: 1;
    -webkit-transform: scale(1);
            transform: scale(1); } }

@keyframes opacity {
  0% {
    opacity: 0;
    -webkit-transform: scale(3);
            transform: scale(3); }
  100% {
    opacity: 1;
    -webkit-transform: scale(1);
            transform: scale(1); } }


.ratingTable div{
	display:none;
	}
	.ratingTable div.star{
	display:inline-block;
	}

</style>

<%
if (isbasketOrder) {
  pageContext.include("/components/basket/order_form.jsp");
}
else if (Tools.isNotEmpty(price)) {
%>
<link rel="stylesheet" type="text/css" href="/components/basket/css/basket.css"/>

	<div class="row">
	<div class="">
		<div class="card">
			<div class="container-fliud">
				<div class="wrapper row">
					<div class="col-md-6">
				   <%
            String perexGalleryDir = perexImage.substring(0, perexImage.lastIndexOf("/"));
        		if (perexGalleryDir.indexOf("/images/gallery/")!=-1)
        		{
             String perexGallery = "!INCLUDE(/components/gallery/gallery-basket.jsp,perexImagePath=\""+perexImage+"\" ,dir="+ perexGalleryDir +", galleryStyle=photoSwipe, orderBy=title, orderDirection=asc)!";
             request.setAttribute("perexGallery", perexGallery);

             if (perexGalleryDir.length()<20) request.setAttribute("perexGalleryDontShow", "1");
        		}
        		else
        		{
        			request.setAttribute("perexGallery", "");
        		}


          %>
        <iwcm:notEmpty name="perexGallery">
          <iwcm:write name="perexGallery"/>
		</iwcm:notEmpty>
		<iwcm:empty name="perexGallery">
		<img src="/thumb<%=doc.getPerexImage() %>?w=390&ip=1"/>
		</iwcm:empty>
			</div>
					<div class="details col-md-6">
						<h3 class="product-title"><iwcm:write name="doc_title"/></h3>
						<%
				request.setAttribute("ratingForm", "!INCLUDE(/components/rating/rating_form.jsp, ratingDocId="+doc.getDocId()+", range=5)!");
			%>
				<iwcm:write name="ratingForm"/>
						<p class="product-description"> <%=doc.getPerex() %></p>
											<%
												//fieldP=velkost:S,M,L,XL|farba:modra,zelena,cervena|style:muzske,zenske

			String variantsString = doc.getFieldP();
          	Map<String, String[]> variants = new HashMap<String, String[]>();
			if (Tools.isNotEmpty(variantsString)) {
			 	for(String variant : Tools.getTokens(variantsString, "|")) {
			 		String[] variantParams = Tools.getTokens(variant, ":");
			 		if (variantParams.length>=2)
			 		{
			 			variants.put(variantParams[0], Tools.getTokens(variantParams[1], ","));
			 		}
			 	}
			}
			request.setAttribute("variants", variants);
			%>
				<h5 class="colors">

			<c:if test="${fn:length(variants) > 0 }">
				<div class="variantsBox">
					<table>
						<c:forEach items="${variants}" var="variant">

							<tr>
								<td>
									<label for="${variant.key}">${variant.key}:</label>
								</td>
								<td>
									<select name="${variant.key}" id="${variant.key}">
									<c:forEach items="${variant.value}" var="variantValue">
										<option value="${variantValue}">${variantValue}</option>
									</c:forEach>
									</select>
								</td>
							</tr>

						</c:forEach>
					</table>
				</div>
			</c:if>
						</h5>
			<h4 class="price">Cena: <span><iway:curr currency="<%=EshopService.getDisplayCurrency(request)%>"><%=doc.getLocalPriceVat(request) %></iway:curr></span></h4>

						<div class="action">
							 <a  class="addToBasket add-to-cart btn btn-default itemId_<%=doc.getDocId()%>" type="button"> <i class="fa fa-shopping-cart" aria-hidden="true"></i>
							 <iwcm:text key="components.basket.add_to_basket"/></a>

						</div>
					</div>
				</div>
			</div>
		</div>
	</div></div>
<%
}
%>

