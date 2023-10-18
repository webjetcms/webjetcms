package sk.iway.iwcm.system.stripes;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.validation.ValidationErrors;

/**
 *  AjaxPostFormResult.java
 *  Simple DTO containing information about form posting:
 *
 * - boolean state flag: ok / not ok
 * - String containing HTML snippet rendering Stripes messages / errors
 * - ValidationErrors object containing details on error fields
 * source: http://blog.novoj.net/2008/01/25/running-ajax-with-jquery-in-stripes-framework/
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1 $
 *@created      Date: 3.2.2008 15:20:13
 *@modified     $Date: 2008/02/06 10:01:36 $
 */
public class AjaxPostFormResult
{
	private boolean ok;
	private ValidationErrors validationErrors;
	private String okResult = "";
	private String errorResult = "";
	private int userId = -1;
	private String token = "";

	public AjaxPostFormResult(String okResult, String errorResult)
	{
		this.ok = (errorResult == null);
		this.okResult = okResult;
		this.errorResult = errorResult;
	}

	public AjaxPostFormResult(String okResult, String errorResult, int userId)
	{
		this.ok = (errorResult == null);
		this.okResult = okResult;
		this.errorResult = errorResult;
		this.userId = userId;
	}

	public void updateCsrfToken(HttpServletRequest request)
	{
		this.token = CSRF.getCsrfToken(request.getSession(), true);
	}
	
	public AjaxPostFormResult(ValidationErrors validationErrors, String errorResult)
	{
		this.ok = false;
		this.validationErrors = validationErrors;
		this.errorResult = errorResult;
	}

	public AjaxPostFormResult(ValidationErrors validationErrors, String errorResult, int userId)
	{
		this.ok = false;
		this.validationErrors = validationErrors;
		this.errorResult = errorResult;
		this.userId = userId;
	}
	
	public boolean isOk()
	{
		return ok;
	}

	public ValidationErrors getValidationErrors()
	{
		return validationErrors;
	}

	public String getOkResult()
	{
		return okResult;
	}

	public String getErrorResult()
	{
		return errorResult;
	}

	public int getUserId() {
		return userId;
	}

	public String getToken() {
		return token;
	}
}
