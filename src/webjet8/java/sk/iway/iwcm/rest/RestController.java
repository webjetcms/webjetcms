package sk.iway.iwcm.rest;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

/**
 *  RestController.java
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2017
 *@author       $Author: jeeff rzapach $
 *@version      $Revision: 1.3 $
 *@created      Date: 25.1.2017 10:15:22
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class RestController
{
	public static boolean isIpAddressAllowed(HttpServletRequest request)
	{
		String callerIpAddress = Tools.getRemoteIP(request);

		if(Tools.isNotEmpty(callerIpAddress))
		{
			String allowedIpsString = Constants.getString("restAllowedIpAddresses");
			if ("*".equals(allowedIpsString)) return true;
			if(Tools.isNotEmpty(allowedIpsString))
			{
				String[] allowedIps = allowedIpsString.split(",");
				for(String ip : allowedIps)
				{
					if(callerIpAddress.trim().startsWith(ip.trim()))
						return true;
				}
			}
		}

		Logger.debug(RestController.class, "Not allowed access!");
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
	}
}
