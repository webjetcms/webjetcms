package sk.iway.iwcm;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *  Encoding.java - nastavenie response encodingu
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.4 $
 *@created      Date: 12.9.2004 17:39:32
 *@modified     $Date: 2005/10/25 06:48:03 $
 */
public class Encoding
{
	public static final String REQ_ENCODING = "sk.iway.iwcm.Encoding.encReq";
	
	/**
	 * Nastavi response objektu kodovanie znakov podla requestu
	 * @param request
	 * @param response
	 */
	public static void setResponseEnc(HttpServletRequest request, HttpServletResponse response, String contentType)
	{
		//zisti encoding
		String enc = SetCharacterEncodingFilter.selectEncoding(request);
		String encReq = (String)request.getAttribute(REQ_ENCODING);
		if (encReq != null)
		{
			enc = encReq;
		}
		
		//Logger.println(this,"CT: " + contentType + " enc: " + enc);
		response.setContentType(contentType + "; charset=" + enc);
		
		if (encReq == null)
		{
			//nastav hodnotu do requestu
			request.setAttribute(REQ_ENCODING, enc);
		}
	}
	
	/**
	 * Nastavi zvolene kodovanie response objektu a nastavi ho aj do requestu
	 * @param request
	 * @param response
	 * @param enc
	 */
	public static void setResponseEnc(HttpServletRequest request, HttpServletResponse response, String contentType, String enc)
	{
		request.setAttribute(REQ_ENCODING, enc);
		setResponseEnc(request, response, contentType);
	}
}
