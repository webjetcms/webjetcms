package sk.iway.iwcm;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Umoznuje zaregistrovat forwarder do pathfiltra
 *  DynamicForward.java
 *  
 *
 *@Title        webjet8
 *@Company      Interway a.s. (www.interway.sk)
 *@Copyright    Interway a.s (c) 2001-2016
 *@author       Author: mbocko
 *@created      Date: 20.9.2016
 *@modified     Date: 20.9.2016
 */
public interface DynamicForward
{
	public boolean isValid(String path);
	public boolean forward(String path, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;
}
