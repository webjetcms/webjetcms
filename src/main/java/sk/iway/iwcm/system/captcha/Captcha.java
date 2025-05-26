package sk.iway.iwcm.system.captcha;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.octo.captcha.service.CaptchaServiceException;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;

/**
 *  Captcha.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1 $
 *@created      Date: 2.2.2010 10:57:33
 *@modified     $Date: 2010/02/09 08:56:19 $
 */
public class Captcha
{
	private static String captchaDiv = "<div class=\"g-recaptcha\"id=\"wjReCaptcha\"></div><script src=\"https://www.google.com/recaptcha/api.js?onload=onloadCallback&render=explicit\"async defer></script><script type=\"text/javascript\">var reCaptchaWidgetId=-1;function isReCaptchaValid(){return serverRequest(false)}function serverRequest(setId){var isValid=false;var captchaId=$('#g-recaptcha-response').val();var url='/components/form/re_captcha_ajax.jsp';if(setId)url='/components/form/set_re_catpcha_ajax.jsp';$.ajax({type:'POST',url:url,data:{capchaId:captchaId},success:function(data){if(data.trim()=='OK')isValid=true;else grecaptcha.reset(reCaptchaWidgetId)},async:false});return isValid}var verifyCallback=function(response){serverRequest(true)};var onloadCallback=function(){reCaptchaWidgetId=grecaptcha.render('wjReCaptcha',{'sitekey':'"+Constants.getString("reCaptchaSiteKey")+"','callback':verifyCallback,'theme':'light'})};</script>";
	/**
	 * Vrati true ak je pre zadanu komponentu vyzadovana captcha (nastavuje sa konfiguracnou premennou captchaComponents ako zoznam oddeleny medzerami)
	 * @param component
	 * @return
	 */
	public static boolean isRequired(String component)
	{
		if (Tools.isEmpty(component)) return true;

		if ((" "+Constants.getString("captchaComponents")+" ").indexOf(" "+component+" ")!=-1 || (","+Constants.getString("captchaComponents")+",").indexOf(","+component+",")!=-1) return true;
		return false;
	}

	/**
	 * Zvaliduje a VYMAZE odpoved (malo by sa pouzit vo finalnej validacii na strane servera)
	 * @param httpServletRequest
	 * @param response
	 * @param component - meno komponenty, alebo null ak kontrolu na povolenie komponenty nie je potrebne vykonat
	 * @return
	 */
	public static boolean validateResponse(HttpServletRequest httpServletRequest, String response, String component)
	{
		try
		{
			if (component != null && isRequired(component)==false)
				return true;
			if("invisible".equals(Constants.getString("captchaType"))) {
				if (response == null) response = httpServletRequest.getParameter("g-recaptcha-response");
				return reCaptchaValidate(response);
			}
			else if ("reCaptcha".equals(Constants.getString("captchaType")))
			{
				return reCaptchaValidate(httpServletRequest.getSession().getAttribute("sessionId"));
			}
			else if ("reCaptchaV3".equals(Constants.getString("captchaType")))
			{
				return reCaptchaV3Validate(httpServletRequest);
			}
			//preber si priamo wjcaptcha parameter
            if (Tools.isEmpty(response)) response = httpServletRequest.getParameter("wjcaptcha");
			if (Tools.isEmpty(response)) return false;

			String captchaId = httpServletRequest.getSession().getId();
			return CaptchaServiceSingleton.getInstance().validateResponseForID(captchaId, response.toUpperCase());
		}
		catch (Exception e)
		{

		}
		return false;
	}

	/** Zisti, ci je reCaptcha spravna
	 *
	 * @return
	 */
	private static boolean reCaptchaValidate(Object capchaId)
	{
		if(capchaId == null || Tools.isEmpty(capchaId+""))
		{
			Logger.debug(Captcha.class, "ReCaptcha BAD id: "+capchaId);
			return false;
		}

		try
		{
			String url = "https://www.google.com/recaptcha/api/siteverify";
			URL obj = new URL(url);
			HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

			con.setRequestMethod("POST");
			con.setRequestProperty("User-Agent", null);
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

			String urlParameters = "secret="+Constants.getString("reCaptchaSecret")+"&response="+capchaId;

			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();

			int responseCode = con.getResponseCode();
			if(responseCode != 200)
			{
				Logger.debug(null, "Sending 'POST' request to URL : " + url);
				Logger.debug(null, "Post parameters : " + urlParameters);
				Logger.debug(null, "Response Code : " + responseCode);
			}

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer postResponse = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				postResponse.append(inputLine);
			}
			in.close();
			if(postResponse.toString().indexOf("\"success\": true") != -1)
			{
				Logger.debug(Captcha.class, "ReCaptcha OK id: "+capchaId);
				return true;
			}
			else if(postResponse.toString().indexOf("\"success\": false") != -1)
			{
				Logger.debug(Captcha.class, "ReCaptcha BAD id: "+capchaId);
				return false;
			}
			else
			{
				Logger.debug(Captcha.class, "ReCaptcha Response Error id: "+capchaId);
				Logger.debug(Captcha.class, postResponse.toString());
			}
		}
		catch(Exception ex)
		{
			Logger.debug(Captcha.class,"ReCaptcha Verify Error id: "+capchaId);
			sk.iway.iwcm.Logger.error(ex);
		}
		return false;
	}

	private static boolean reCaptchaV3Validate(HttpServletRequest request) {
		String token = Tools.getParameter(request, "g-recaptcha-response");
		if (Tools.isEmpty(token)) {
			Logger.debug(Captcha.class, "reCaptchaV3Validate - Google token empty");
			return false;
		}

		String ip = Tools.getRemoteIP(request);
		if (Tools.isEmpty(ip)) {
			Logger.debug("reCaptchaV3Validate - ip empty");
			return false;
		}

		RestTemplate restTemplate = new RestTemplate();

		ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
		restTemplate.setRequestFactory(factory);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("secret", Constants.getString("reCaptchaSecret"));
		map.add("response", token);
		map.add("remoteip", ip);

		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

		ResponseEntity<GoogleRecaptchaResponse> response =
				restTemplate.exchange("https://www.google.com/recaptcha/api/siteverify",
						HttpMethod.POST,
						entity,
						GoogleRecaptchaResponse.class);

		double minimalScore = Tools.getDoubleValue(Constants.getString("recaptchaMinScore"), 0.5);

		if (response.getStatusCode() != HttpStatus.OK) {
			Logger.debug("reCaptchaV3Validate - Google response status not ok: {}", response.getStatusCode().toString());
			return false;
		}

		GoogleRecaptchaResponse body = response.getBody();
		if (body == null) {
			Logger.debug("reCaptchaV3Validate - Google response body null");
			return false;
		}

		if (!body.isSuccess()) {
			Logger.debug("reCaptchaV3Validate - Google response success false, error codes: {}", Tools.join(body.getErrorCodes(), ", "));
			return false;
		}

		if (body.getScore() < minimalScore) {
			Logger.debug("reCaptchaV3Validate - Google response score: "+body.getScore()+", minimal score: "+minimalScore);
			return false;
		}

		return true;
	}

	/**
	 * Overi spravnost odpovede voci obrazku, pouziva sa opakovane cez AJAX volanie (viz check_form_impl.jsp)
	 * NEPOUZIVAT V JAVA KODE NA OVERENIE SPRAVNOSTI CAPTCHA, na to treba pouzit validateResponse!!!!
	 * @param httpServletRequest
	 * @param response
	 * @return
	 */
	public static boolean isReponseCorrect(HttpServletRequest httpServletRequest, String response)
	{
		if (Tools.isEmpty(response)) return false;

		Boolean isResponseCorrect = Boolean.FALSE;
		// remenber that we need an id to validate!
		String captchaId = httpServletRequest.getSession().getId();
		// retrieve the response
		// Call the Service method
		if("invisible".equals(Constants.getString("captchaType"))) {
			return reCaptchaValidate(response);
		}
		else if ("reCaptcha".equals(Constants.getString("captchaType")))
		{
			return reCaptchaValidate(httpServletRequest.getSession().getAttribute("sessionId"));
		}
		try
		{
			isResponseCorrect = CaptchaServiceSingleton.getInstance().testResponseForID(captchaId, response);
			//Logger.debug(Captcha.class, "ID="+captchaId+", response="+response+", correct="+isResponseCorrect);
		}
		catch (CaptchaServiceException e)
		{
			// should not happen, may be thrown if the id is not valid
			sk.iway.iwcm.Logger.error(e);
		}
		if (isResponseCorrect == null) isResponseCorrect = Boolean.FALSE;
		return isResponseCorrect.booleanValue();
	}

	/**
	 * Vygeneruje JPG obrazok captchy
	 * @param httpServletRequest
	 * @param httpServletResponse
	 * @throws ServletException
	 * @throws IOException
	 */
	public static void getImage(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException
	{
		if("invisible".equals(Constants.getString("captchaType")) || "reCaptcha".equals(Constants.getString("captchaType")))// pre reCaptcha nepotrebujeme
			return;

		// the output stream to render the captcha image as jpeg into
		try
		{
			// get the session id that will identify the generated captcha.
			// the same id must be used to validate the response, the session id is
			// a good candidate!
			String captchaId = httpServletRequest.getSession().getId();
			//Logger.debug(Captcha.class, "Generating CAPTCHA for "+captchaId);
			// call the ImageCaptchaService getChallenge method

			ByteArrayOutputStream jpegOutputStream = new ByteArrayOutputStream();
			BufferedImage challenge = CaptchaServiceSingleton.getInstance().getImageChallengeForID(captchaId, Locale.ENGLISH);
			ImageIO.write(challenge, "jpg", jpegOutputStream);
			byte[] captchaChallengeAsJpeg = null;
			captchaChallengeAsJpeg = jpegOutputStream.toByteArray();


			// flush it in the response
			httpServletResponse.setHeader("Cache-Control", "no-store");
			httpServletResponse.setHeader("Pragma", "no-cache");
			httpServletResponse.setDateHeader("Expires", 0);
			httpServletResponse.setContentType("image/jpeg");
			ServletOutputStream responseOutputStream = httpServletResponse.getOutputStream();


			responseOutputStream.write(captchaChallengeAsJpeg);

			//use imageio to write image
			//ImageIO.write(challenge, "jpeg", responseOutputStream);
			responseOutputStream.flush();
			responseOutputStream.close();
		}
		catch (CaptchaServiceException e)
		{
			httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
	}

	/**
	 * Vrati HTML kod captcha obrazku
	 * @param request
	 * @return
	 */
	public static String getImage(HttpServletRequest request)
	{
		Prop prop = Prop.getInstance(request);
		if("invisible".equals(Constants.getString("captchaType")) || "reCaptcha".equals(Constants.getString("captchaType")))
			return captchaDiv;
		return ("<img id=\"wjcaptchaImg1\" class=\"captchaImage\" src=\"/captcha.jpg\" onclick=\"this.src='/captcha.jpg?rnd='+(new Date().getTime());\" style=\"cursor: pointer;\" alt=\""+prop.getText("captcha.imageAlt")+"\" title=\""+prop.getText("captcha.imageAlt")+"\"/>");
	}
}