package sk.iway.iwcm.ebanking.epayments;

import javax.servlet.http.HttpServletRequest;

import sk.iway.Password;
import sk.iway.iwcm.ebanking.Payment;

/**
 *  PaymentInformation.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: jraska $
 *@version      $Revision: 1.4 $
 *@created      Date: 26.8.2009 14:08:38
 *@modified     $Date: 2009/12/11 14:51:53 $
 */
public abstract class PaymentInformation
{

	protected String merchantId;

	protected String key;

	protected int constantSymbol;

	public String getMerchantId()
	{
		return this.merchantId;
	}

	public String getReturnEmail()
	{
		return "";
	}

	/**
	 * Returns a private key assigned to the merchant in order to run e-bussiness
	 */
	public String getKey()
	{
		return this.key;
	}

	public abstract String getUrlString();

	/**
	 * Returns a constant symbol part of the bank account number.
	 * It throws an exception in case no such symbol is set(Security issues)
	 */
	public int getConstantSymbol()
	{
		return constantSymbol;
	}

	public boolean hasOwnForm()
	{
		return false;
	}

	public String generateForm(Payment payment, HttpServletRequest request)
	{
		return "";
	}

	/**
	 * Validuje spravnost navratovej odpovede banky. Overi ci su pritomne povinne parametre a ci sedi podpisovy hash, ak nie vyhodi Exception.
	 * Ak je odpoved formalne v poriadku, vrati TRUE v pripade kladnej (platba bola uspesna), FALSE v pripade zapornej odpovede (platba nebola uspesna).
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public boolean validateBankResponce(HttpServletRequest request) throws Exception
	{
		return false;
	}

	@Override
	public String toString()
	{
		return this.getClass().getSimpleName()+" mid => "+getMerchantId()+" key => "+getKey();
	}

	public String generateEditorForm()
	{
		return "";
	}

	/**
	 * Vrati variabilny symbol, potrebny pre sparovanie platby, z navratoveho requestu banky. Vacsina bank ho vracia ako URL parameter `VS` alebo `vs`,
	 * vtedy staci pouzit metodu predka, ak banka vracia informaciu inak, je potrebny Override v implementacii.
	 * <br><br>
	 * Ak banka nevracia variabilny symbol tak bude velmi obtiazne ak nie nemozne platbu sparovat.
	 * @param request
	 * @return Variabilny symbol
	 */
	public String getResponceVS(HttpServletRequest request)
	{
		String vs=null;
		vs = request.getParameter("VS")!=null ? request.getParameter("VS") : request.getParameter("vs");
		return vs;
	}

	public String getDecrypredKey(String key)
	{
		try
		{
			Password password = new Password();
			return password.decrypt(key);
		}
		catch(Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return "";
	}

	/**
	 * vrati specialny symbol
	 * @param request
	 * @return
	 */
	public String getResponceSS(HttpServletRequest request)
	{
		String ss=null;
		ss = request.getParameter("SS")!=null ? request.getParameter("SS") : request.getParameter("ss");
		return ss;
	}
}
