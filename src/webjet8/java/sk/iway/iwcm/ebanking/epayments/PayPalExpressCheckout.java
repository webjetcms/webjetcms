/*https://developer.paypal.com/demo/checkout/#/pattern/client
 * https://developer.paypal.com/docs/integration/direct/make-your-first-call/
 *
 * detail platby:
 * https://developer.paypal.com/docs/api/payments/#payment_get
 *
 * */

package sk.iway.iwcm.ebanking.epayments;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import sk.iway.iwcm.Logger;

//podla tohoto implementovane
//https://developer.paypal.com/docs/api/payments/#payment_get

public class PayPalExpressCheckout {
	public static String getAccessToken()
	{
		try
		{
			JSONObject obj = new JSONObject(getAccessTokenResponse());

			if(obj.getInt("expires_in") <= 2)
			{
				Logger.debug(null, "oauth getAccessToken zaspavam na 2000");
				Thread.sleep(2000);
				obj = new JSONObject(getAccessTokenResponse());
			}
			return obj.getString("access_token");
		}
		catch(Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			return null;
		}

	}

	public static String paymentVerify(String paymentId)
	{
		String url = "https://api.sandbox.paypal.com/v1/payments/payment/"+paymentId;

		try
		{
			HttpURLConnection con = getCustomConnection(url);
			con.setRequestProperty("Authorization", "Bearer " + getAccessToken());
			con.setRequestMethod("GET");
			Logger.debug(PayPalExpressCheckout.class, "Connected method : "+con.getRequestMethod() + " Response Code : "+con.getResponseCode()+" Message : "+con.getResponseMessage());

			if(isResponseError(con))
				return null;

			return contentToString(con.getContent());

		}
		catch(Exception exc)
		{
			catchMethod(exc);
		}
		return null;
	}

	public static String getAccessTokenResponse()
	{
		String url = "https://api.sandbox.paypal.com/v1/oauth2/token";
		String login = "AVvjcXS5hPVL27SACutRO_6ilUCrtc9FJUxBnrPFb0fH2XcdD4Eitm8I6lsGid38K2LSs-L20GMLdmtX";
		String heslo = "EFDd3yp8_thTHwftcSR4IhH3rFBdnCz9g0zwot7FoTdfzDuQA4wlKHkGU_AjcFxOr_Wm713api3sAO2j";

		try
		{
			HttpURLConnection con = getCustomConnection(url);
			String userCredentials = login+":"+heslo;//"username:password";
			con.setRequestProperty("Authorization", "Basic " + new String(new Base64().encode(userCredentials.getBytes())));
			con.setRequestMethod("POST");
			String urlParameters  = "grant_type=client_credentials";	//"{\"grant_type\": \"client_credentials\"";
			byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);

			DataOutputStream  osw = new DataOutputStream(con.getOutputStream());
			osw.write(postData);
			osw.flush();
			osw.close();
			con.connect();

			Logger.debug(PayPalExpressCheckout.class, "Connected method : "+con.getRequestMethod() + " Response Code : "+con.getResponseCode()+" Message : "+con.getResponseMessage());

			if(isResponseError(con))
				return null;

			return contentToString(con.getContent());
			//typicky vystup:
			//{"scope":"https://api.paypal.com/v1/payments/.* https://uri.paypal.com/services/applications/webhooks openid","nonce":"2017-06-08T12:38:46Z2NIcwTJg_rMfbIN0nNdGeP4wqNed7eBM1TV9L12uhk4","access_token":"A21AAHSFookkTKeiAntt2TSDL80zKY1Fm0apgY9uOyNYpu0oSQU5t44IpAaCduZDz7jDZeDk25PQmhcFIuKYg8nNx-aCY288g","token_type":"Bearer","app_id":"APP-80W284485P519543T","expires_in":32246}
		}
		catch(Exception exc)
		{
			catchMethod(exc);
		}
		return null;
	}

	private static HttpURLConnection getCustomConnection(String url) throws Exception
	{
		Logger.debug(PayPalExpressCheckout.class, "Requested url: "+url);
		HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		con.setDoOutput(true);
		con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		con.setReadTimeout(60*1000);
		return con;
	}

	private static boolean isResponseError(HttpURLConnection con ) throws Exception
	{
		if(con.getErrorStream()  != null)
		{
			Logger.debug(PayPalExpressCheckout.class, "getErrorStream() != null Treba precitat! ");
			StringWriter writer = new StringWriter();
			IOUtils.copy(con.getInputStream(), writer, StandardCharsets.UTF_8);
			System.out.println(writer.toString());
		}

		//ak nieco nezbehlo v poriadku
		if(con.getResponseCode() >= 300)
		{
			Logger.debug(PayPalExpressCheckout.class, "!!! ResponseCode  =  "+con.getResponseCode());
			return true;
		}

		return false;
	}

	private static String contentToString(Object connectionContent) throws Exception
	{
		StringWriter writer = new StringWriter();
		IOUtils.copy((InputStream)connectionContent, writer, "UTF-8");
		return writer.toString();
	}

	private static void catchMethod(Exception exc)
	{
//		StringWriter sw = new StringWriter();
//		PrintWriter pw = new PrintWriter(sw);
//		exc.printStackTrace(pw);
		sk.iway.iwcm.Logger.error(exc);
	}
}
