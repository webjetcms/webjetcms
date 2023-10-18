<%@page import="sk.iway.cloud.payments.paypal.PayPalExpressCheckoutMerchantAccountActionBean"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.components.basket.BasketInvoiceBean"%>
<%@page import="sk.iway.cloud.payments.paypal.PayPalExpressCheckoutMerchantAccountBean"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@ 
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@ 
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@ 
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@ 
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@ 
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@ 
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@ 
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@ 
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);
Prop prop = Prop.getInstance(request);



BasketInvoiceBean invoice = (BasketInvoiceBean)session.getAttribute("invoice");
if(invoice == null || invoice.getTotalPriceVat() <= 0d )
{
	out.print(prop.getText("components.basket.nastala_chyba_ziskania_objednavky"));
	return;
}

%>
<h1><iwcm:text key="basket.payment.PayPalExCh"/></h1>
<div id="paypal-button"></div>

<script src="https://www.paypalobjects.com/api/checkout.js"></script>
<%//out.print(new PayPalExpressCheckoutMerchantAccountActionBean().getAccount().getClientId()); 
//out.print(invoice.getTotalPriceVat());%>
<%session.setAttribute("invoice_id", invoice.getBasketInvoiceId()); %>
<script>
    paypal.Button.render({

        env: 'sandbox', // Or 'sandbox'	production

        client: {
            //sandbox:    'AVvjcXS5hPVL27SACutRO_6ilUCrtc9FJUxBnrPFb0fH2XcdD4Eitm8I6lsGid38K2LSs-L20GMLdmtX',
            //production: 'AX8OAig3ILQuXo54eb_Hk3Rf8SCeBr1inxo6CT5K4B8aekQkfCoiW8ECJNjqgAfDYh8QMpCZKRLkwOiD'
        	sandbox: '<%=new PayPalExpressCheckoutMerchantAccountActionBean().getAccount().getClientId()%>'
        },

        commit: true, // Show a 'Pay Now' button

        payment: function(data, actions) {
            return actions.payment.create({
            	payment: {
	                transactions: [
	                    {
	                        amount: { total: '<%=invoice.getTotalPriceVat()%>', currency: '<%=invoice.getCurrency().toUpperCase()%>' }//0.01, EUR
	                    }
	                ]
            	},
                experience: {
                    input_fields: {
                        no_shipping: 1
                    }
                }
            });
        },

        onAuthorize: function(data, actions) {
            return actions.payment.execute().then(function(payment) {
// console.log(data);
// alert(data);

			$.ajax({
			    type: "POST",
			    url: "/components/basket/process_papypal_response_json_ajax.jsp",
			    data: "paymentId="+data.paymentID,//datastring
			    success: function(data) {
			        var obj = jQuery.parseJSON(data);
			        if(obj.result != "ok")
			        	alert('<iwcm:text key="components.form.archive.saveFail"/>');
			    },
			    error: function() {
			        alert('<iwcm:text key="components.form.archive.saveFail"/>');
			    }
			});
                // The payment is complete!
                // You can now show a confirmation message to the customer
            });
        }

    }, '#paypal-button');
</script>