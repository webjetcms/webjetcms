package sk.iway.cloud.payments.paypal;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import org.apache.commons.lang.StringUtils;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.stripes.WebJETActionBean;

public class PayPalExpressCheckoutMerchantAccountActionBean extends WebJETActionBean {
	private String accessTokenUrl = "https://api.sandbox.paypal.com/v1/oauth2/token?grant_type=client_credentials";
	private String paymentDetailUrl = "https://api.sandbox.paypal.com/v1/payments/payment/";
	String firstState = "";
    int indexOfState = -1;
	
	private PayPalExpressCheckoutMerchantAccountBean account = new PayPalExpressCheckoutMerchantAccountBean();
	
	public PayPalExpressCheckoutMerchantAccountBean getAccount()
	{
		//if( !Tools.isAnyEmpty(Constants.getString("PayPalExChClientId"), Constants.getString("PayPalExChSecret")))
		if(InitServlet.isTypeCloud())
		{	
			//ostra platba webjet cloud
			PayPalExpressCheckoutMerchantAccountBean byDomain = new JpaDB<PayPalExpressCheckoutMerchantAccountBean>(PayPalExpressCheckoutMerchantAccountBean.class).findFirst("domainId", CloudToolsForCore.getDomainId());
			if(byDomain != null) 
			{
				account.setId(byDomain.getId());
				account.setClientId(byDomain.getClientId());
				account.setSecret(byDomain.getSecret());
			}
		}
		else
		{
			//ostra platba webjet (nie cloud)
			account.setClientId(Constants.getString("PayPalExChClientId"));
			account.setSecret(Constants.getString("PayPalExChSecret"));
		}
		account.setDomainId(CloudToolsForCore.getDomainId());
		return account;
	}
	
	private static String httpPaypalRequest(String method,String url,String authorization_value)
	{
		try {
		    URL urll = new URL(url);
		    //String encoding = Base64.getEncoder().encodeToString((authentification).getBytes());

		    HttpURLConnection connection = (HttpURLConnection) urll.openConnection();
		    connection.setRequestMethod(method);//"POST" / GET
		    //connection.setDoOutput(true);
		    connection.setRequestProperty  ("Authorization", authorization_value); //"Basic " + encoding
		    
			//connection.connect();
		    
			
		    InputStream content = (InputStream)connection.getInputStream();
		    BufferedReader in =   new BufferedReader (new InputStreamReader(content));
		    
		    
		    String line = "";
		    String responseBody = "";
		    while ((line = in.readLine()) != null) {
		    	responseBody += line;
		    }
		    
		    return responseBody;
		} catch(Exception e) {
		    e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * 
	 * @param payId - example: "PAY-1H6008447G305014NLNII75Q"
	 * @return
	 */
	public LabelValueDetails getPaymentStatus(String payId)
	{
		//String encoding = "Bearer " +Base64.getEncoder().encodeToString((getAccount().getClientId()+":"+getAccount().getSecret()).getBytes());
		return parseResponse(httpPaypalRequest("GET", paymentDetailUrl+payId, "Bearer "+getAccessToken()));
		
	}
	
	/** Ak vsetko zbehne spravne mali by sme mat v labelValueDetails 
	 * 	label = approved
	 * 	value = completed
	 * 
	 * @param responseBodyJson
	 * @return
	 */
	private LabelValueDetails parseResponse(String responseBodyJson)
	{
		LabelValueDetails labelValueDetails = new LabelValueDetails(); 
    	if(responseBodyJson != null && responseBodyJson.indexOf("related_resources") != -1)
    	{
    		labelValueDetails.setLabel(getState(responseBodyJson));
    		indexOfState =  responseBodyJson.indexOf("state",responseBodyJson.indexOf("related_resources"));
    		labelValueDetails.setValue(getState(responseBodyJson.substring(indexOfState)));//StringUtils.substringBetween(responseBodyJson.substring(indexOfState), "state\":\"", "\"");
    		return labelValueDetails;
    	}
    	return null;
	}
	
	private static String getState(String responseBodyJson)
	{
		return StringUtils.substringBetween(responseBodyJson, "state\":\"", "\"");
	}
	
	public String getAccessToken()
	{
		String cacheObjectname = "PayPalExCh-"+CloudToolsForCore.getDomainId()+"-"+getAccount().getClientId();
		String accesToken = (String)Cache.getInstance().getObject(cacheObjectname);
		if(accesToken != null)
			return accesToken;
		
		String encoding = "Basic " +Base64.getEncoder().encodeToString((getAccount().getClientId()+":"+getAccount().getSecret()).getBytes());
		LabelValueDetails lvd = readResponseForAccessToken(httpPaypalRequest("POST", accessTokenUrl, encoding));
		if(lvd != null)
		{
			Cache.getInstance().setObject(cacheObjectname, lvd.getLabel(), lvd.getInt1()/60);
			return lvd.getLabel();
		}
		
		return null;
	}
	
	private LabelValueDetails readResponseForAccessToken(String responseBodyJson)
	{
		if(responseBodyJson == null)
			return null;
		 
		LabelValueDetails labelValueDetails = new LabelValueDetails();
		labelValueDetails.setLabel( StringUtils.substringBetween(responseBodyJson, "\"access_token\":\"", "\""));
		labelValueDetails.setInt1(Tools.getIntValue(StringUtils.substringBetween(responseBodyJson, "\"expires_in\":", "}"), -1));
		return labelValueDetails;
	   // out.print("<br> Time expired: "+sdf.format(cal.getTime()));
	   //SimpleDateFormat sdf = new SimpleDateFormat("HH:mm  dd.MM.yyyy");
	}
}
