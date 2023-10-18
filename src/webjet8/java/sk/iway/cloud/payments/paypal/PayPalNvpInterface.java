package sk.iway.cloud.payments.paypal;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.basket.BasketInvoiceBean;
import sk.iway.iwcm.database.JpaDB;

/**
 *  PayPalNvpInterface.java
 *
 *  Trieda zabezpecuje http komunikaciu s name value pair rozhranim paypalu, zikanie tokenu a vykonanie sale
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2014
 *@author       $Author: jeeff mhalas $
 *@version      $Revision: 1.3 $
 *@created      Date: 2.10.2014 16:40:49
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class PayPalNvpInterface
{

	/**
	 * Iniciuje transakciu, ziska token na zaklade ktoreho sa da presmerovat na paypal
	 *
	 *

	https://api-3t.sandbox.paypal.com/nvp?
	USER=juan.merchant_api1.espana.es
	&PWD=2FSUU2PFK6PQQ26J
	&SIGNATURE=AiPC9BjkCyDFQXbSkoZcgqH3hpacAo95ErXCp2fAAa2UfXPKStHov0me
	&METHOD=SetExpressCheckout
	&PAYMENTREQUEST_0_PAYMENTACTION=SALE
	&PAYMENTREQUEST_0_AMT=19
	&VERSION=95
	&PAYMENTREQUEST_0_CURRENCYCODE=USD
	&cancelUrl=http://www.yourdomain.com/cancel.html
	&returnUrl=http://www.yourdomain.com/success.html

	 * @param invoice
	 * @param returnUrl
	 * @param cancelUrl
	 * @return
	 * @author mhalas
	 */
	public static String setExpressCheckout(BasketInvoiceBean invoice, String returnUrl, String cancelUrl)
	{

		if(Tools.isAnyEmpty(returnUrl, cancelUrl)) throw new IllegalArgumentException("returnUrl and cancelUrl can't be empty");
		if(invoice == null) throw new IllegalArgumentException("invoice can't be null");

		Logger.debug(PayPalNvpInterface.class,"setExpressCheckout returnUrl: " + returnUrl + " , cancelUrl: "+cancelUrl);

		String token = "";
		String url = Constants.getString("paypalNvpUrl");
		PayPalMerchantAccountBean merchant = new JpaDB<PayPalMerchantAccountBean>(PayPalMerchantAccountBean.class).findFirst("domainId", CloudToolsForCore.getDomainId());

		HttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost(url);

		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		urlParameters.add(new BasicNameValuePair("USER", merchant.getUser()));
		urlParameters.add(new BasicNameValuePair("PWD", merchant.getPwd()));
		urlParameters.add(new BasicNameValuePair("SIGNATURE", merchant.getSignature()));
		urlParameters.add(new BasicNameValuePair("METHOD", "SetExpressCheckout"));
		urlParameters.add(new BasicNameValuePair("PAYMENTREQUEST_0_PAYMENTACTION", "SALE"));

		//zaokruhli na 2 miesta
		double price = Math.round(invoice.getTotalPriceVat()*100) / 100d;

		urlParameters.add(new BasicNameValuePair("PAYMENTREQUEST_0_AMT", Tools.replace(Double.toString( price ), ",", ".")));
		urlParameters.add(new BasicNameValuePair("PAYMENTREQUEST_0_CURRENCYCODE", invoice.getCurrency().toUpperCase()));
		urlParameters.add(new BasicNameValuePair("VERSION", Constants.getString("paypalNvpVersion")));
		urlParameters.add(new BasicNameValuePair("cancelUrl", cancelUrl));
		urlParameters.add(new BasicNameValuePair("returnUrl", returnUrl));

		Logger.debug(PayPalNvpInterface.class, "PayPal url: "+url);
		for (NameValuePair p : urlParameters)
		{
			Logger.debug(PayPalNvpInterface.class, "PayPal params: "+p.getName()+"="+p.getValue());
		}

		try
		{
			post.setEntity(new UrlEncodedFormEntity(urlParameters,Charset.forName("UTF-8")));
			HttpResponse response = client.execute(post);
			Logger.debug(PayPalNvpInterface.class,"Response Code : " + response.getStatusLine().getStatusCode());
			if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
			{
				String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
				Logger.debug(PayPalNvpInterface.class, "setExpressCheckout response: "+responseBody);

				if(!responseBody.contains("ACK=Failure"))
				{
					//parse and urldecode token
					String [] responseBodyItems = responseBody.split("&");
					for(String pair : responseBodyItems)
					{
						if(pair.startsWith("TOKEN"))
						{
							token = Tools.URLDecode(pair.split("=")[1]);
							break;
						}
					}
					Logger.debug(PayPalNvpInterface.class, "recieved token: " + token);
				}
				else
				{
					Logger.debug(PayPalNvpInterface.class, "Failed to retrieve token!");
					token = "";
				}

			}
		}
		catch (ClientProtocolException e)
		{
			Logger.debug(PayPalNvpInterface.class,"Transaction init failed: " + e.getMessage());
		}
		catch (IOException e)
		{
			Logger.debug(PayPalNvpInterface.class,"Transaction init failed: " + e.getMessage());
		}
		return token;
	}

	/**
	 * Vykona samotnu transakciu po schvaleni klientom na strankach paypalu
	 *
	 * https://api-3t.sandbox.paypal.com/nvp?
		USER=juan.merchant_api1.espana.es
		&PWD=2FSUU2PFK6PQQ26J
		&SIGNATURE=AiPC9BjkCyDFQXbSkoZcgqH3hpacAo95ErXCp2fAAa2UfXPKStHov0me
		&METHOD=DoExpressCheckoutPayment
		&TOKEN=EC-5CC0287178897953R
		&PAYERID=H5VGM5PD7MMVQ
		&PAYMENTREQUEST_0_PAYMENTACTION=SALE
		&PAYMENTREQUEST_0_AMT=19
		&PAYMENTREQUEST_0_CURRENCYCODE=USD
	 *
	 * @param invoice
	 * @param token
	 * @param payerId
	 * @return
	 * @author mhalas
	 */
	public static boolean doExpressCheckoutPayment(BasketInvoiceBean invoice, String token, String payerId)
	{
		if(Tools.isAnyEmpty(token, payerId)) throw new IllegalArgumentException("token and payerId can't be empty");
		if(invoice == null) throw new IllegalArgumentException("invoice can't be null");

		Logger.debug(PayPalNvpInterface.class,"doExpressCheckoutPayment token: " + token + " , payerid: "+payerId);
		boolean transok = false;

		String url = Constants.getString("paypalNvpUrl");
		PayPalMerchantAccountBean merchant = new JpaDB<PayPalMerchantAccountBean>(PayPalMerchantAccountBean.class).findFirst("domainId", CloudToolsForCore.getDomainId());
		if(merchant == null)
		{
			Logger.debug(PayPalNvpInterface.class,"Failed to retrieve merchant info from DB");
			throw new NullPointerException("merchant can't be null");
		}

		HttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost(url);

		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		urlParameters.add(new BasicNameValuePair("USER", merchant.getUser()));
		urlParameters.add(new BasicNameValuePair("PWD", merchant.getPwd()));
		urlParameters.add(new BasicNameValuePair("SIGNATURE", merchant.getSignature()));
		urlParameters.add(new BasicNameValuePair("METHOD", "DoExpressCheckoutPayment"));
		urlParameters.add(new BasicNameValuePair("TOKEN", token));
		urlParameters.add(new BasicNameValuePair("PAYERID", payerId));
		urlParameters.add(new BasicNameValuePair("PAYMENTREQUEST_0_PAYMENTACTION", "SALE"));

		//zaokruhli na 2 miesta
		double price = Math.round(invoice.getTotalPriceVat()*100) / 100d;

		urlParameters.add(new BasicNameValuePair("PAYMENTREQUEST_0_AMT", Tools.replace(Double.toString( price ), ",", ".")));
		urlParameters.add(new BasicNameValuePair("PAYMENTREQUEST_0_CURRENCYCODE", invoice.getCurrency().toUpperCase()));
		urlParameters.add(new BasicNameValuePair("VERSION", Constants.getString("paypalNvpVersion")));

		try
		{
			post.setEntity(new UrlEncodedFormEntity(urlParameters,Charset.forName("UTF-8")));
			HttpResponse response = client.execute(post);
			Logger.debug(PayPalNvpInterface.class,"Response Code : " + response.getStatusLine().getStatusCode());
			if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
			{
				String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
				Logger.debug(PayPalNvpInterface.class, "setExpressCheckout response: "+responseBody);
				/**
				 * example succes response
				 *
				 * TOKEN=EC%2d6F106004G20718133
				 * &SUCCESSPAGEREDIRECTREQUESTED=false
				 * &TIMESTAMP=2014%2d10%2d03T10%3a32%3a58Z
				 * &CORRELATIONID=67f0526c2e0fc&ACK=Success
				 * &VERSION=95&BUILD=13154493
				 * &INSURANCEOPTIONSELECTED=false
				 * &SHIPPINGOPTIONISDEFAULT=false
				 * &PAYMENTINFO_0_TRANSACTIONID=0EL99900EU5413417
				 * &PAYMENTINFO_0_TRANSACTIONTYPE=expresscheckout
				 * &PAYMENTINFO_0_PAYMENTTYPE=instant
				 * &PAYMENTINFO_0_ORDERTIME=2014%2d10%2d03T10%3a32%3a58Z
				 * &PAYMENTINFO_0_AMT=19%2e00
				 * &PAYMENTINFO_0_FEEAMT=1%2e04
				 * &PAYMENTINFO_0_TAXAMT=0%2e00
				 * &PAYMENTINFO_0_CURRENCYCODE=USD
				 * &PAYMENTINFO_0_PAYMENTSTATUS=Completed
				 * &PAYMENTINFO_0_PENDINGREASON=None
				 * &PAYMENTINFO_0_REASONCODE=None
				 * &PAYMENTINFO_0_PROTECTIONELIGIBILITY=Eligible
				 * &PAYMENTINFO_0_PROTECTIONELIGIBILITYTYPE=ItemNotReceivedEligible%2cUnauthorizedPaymentEligible
				 * &PAYMENTINFO_0_SECUREMERCHANTACCOUNTID=PJQEECPYGMEDC
				 * &PAYMENTINFO_0_ERRORCODE=0
				 * &PAYMENTINFO_0_ACK=Success
				 *
				 * example error response
				 *
				 * TIMESTAMP=2014%2d10%2d03T10%3a31%3a28Z
				 * &CORRELATIONID=f5085e93f1f5
				 * &ACK=Failure
				 * &VERSION=0%2e000000
				 * &BUILD=13154493
				 * &L_ERRORCODE0=10006
				 * &L_SHORTMESSAGE0=Version%20error
				 * &L_LONGMESSAGE0=Version%20is%20not%20supported
				 * &L_SEVERITYCODE0=Error
				 */

				if(responseBody.contains("PAYMENTINFO_0_ACK=Success"))
				{
					transok = true;
				}
				else
				{
					Logger.debug(PayPalNvpInterface.class,"Transaction failed");
					transok =false;
				}
			}
		}
		catch (ClientProtocolException e)
		{
			Logger.debug(PayPalNvpInterface.class,"Transaction failed: " + e.getMessage());
		}
		catch (IOException e)
		{
			Logger.debug(PayPalNvpInterface.class,"Transaction failed: " + e.getMessage());
		}
		return transok;
	}
}
