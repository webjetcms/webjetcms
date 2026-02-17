package sk.iway.iwcm.rest;

import jakarta.servlet.http.HttpServletRequest;

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
	/**
	 * Verify if IP address is available to call this REST service
	 * @param request
	 * @return
	 */
	public boolean isIpAddressAllowed(HttpServletRequest request)
	{
		String callerIpAddress = Tools.getRemoteIP(request);

		if(Tools.isNotEmpty(callerIpAddress))
		{
			String allowedIpsString = null;
			//specific version for className
			String className = this.getClass().getSimpleName();
			if(Tools.isNotEmpty(className)) {
				allowedIpsString = Constants.getString("restAllowedIpAddresses-" + className);
			}

			if(Tools.isEmpty(allowedIpsString)) allowedIpsString = Constants.getString("restAllowedIpAddresses");

			if ("*".equals(allowedIpsString)) return true;

			if(allowedIpsString !=null && Tools.isNotEmpty(allowedIpsString)) {
				String[] allowedIps = allowedIpsString.split(",");
				for(String ip : allowedIps) {
					if(callerIpAddress.trim().startsWith(ip.trim())) {
						return true;
					}
				}
			}
		}

		Logger.debug(RestController.class, "Not allowed access, ip: " + callerIpAddress);
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
	}
}
