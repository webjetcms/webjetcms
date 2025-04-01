package sk.iway.iwcm.ebanking.epayments;


import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *  ElectronicPayments.java
 *
 *  Library class serving as a collection of methods dealing with
 *  various ways of electronic payments.
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: jraska $
 *@version      $Revision: 1.8 $
 *@created      Date: 2.7.2008 11:13:42
 *@modified     $Date: 2009/12/11 14:51:53 $
 */
public final class ElectronicPayments
{
	private ElectronicPayments(){}

	/**
	 * Returns the set of available electronic payment methods, which
	 * are correctly configured.
	 *
	 * @return {@link Set}<String>
	 */
	public static final Set<PaymentType> getSupportedPaymentMethods()
	{
		Set<PaymentType> paymentMethods = new HashSet<PaymentType>();

		for (PaymentType type : PaymentType.values())
			if (type.isConfigured())
				paymentMethods.add(type);
		paymentMethods.removeAll(getBlackListPaymentMethods());
		return Collections.unmodifiableSet(paymentMethods);
	}

	public static final Set<String> getSupportedPaymentMethodsToBasketString()
	{
		Set<String> paymentMethodsToBasketString = new HashSet<String>();
		for(PaymentType type:getSupportedPaymentMethods())
		{
			paymentMethodsToBasketString.add(type.toBasketString());
		}
		return Collections.unmodifiableSet(paymentMethodsToBasketString);
	}

	/**
	 * Factory method for producing parameter classes for
	 * providing configuration information about the payment type.
	 *
	 * It throws an exception in case such a payment type is not configured correctly.
	 *
	 * @param type String
	 * @return {@link PaymentInformation}
	 */
	public static PaymentInformation getPaymentInformation(PaymentType type)
	{
		if (!type.isConfigured())
			throw new IllegalStateException(type+" misses one of its configuration values. Required: "+type.requiredConstants);
		switch (type)
		{
			case TATRA_PAY :		return new TatraPayInformation();
			case CARD_PAY :		return new CardPayInformation();
			case VUB_E_PLATBY: 	return new VubEplatbyInformation();
			case POST_BANK: 		return new PostBankInformation();
			case SPORO_PAY: 		return new SporoPayInformation();
			case UNI_PLATBA: 		return new UniPlatbaInformation();
			case OTP_BANK:			return new OtpPaymentInformation();
			case DEXIA_PAY:		return new DexiaPayInformation();
			//case CSOB_TLACITKO:	return new CsobTlacitkoInformation();
			default:	throw new IllegalArgumentException("Uknown payment type");
		}
	}

	/**
	 * Returns all the electronic payment methods, which are currently implemented
	 * @return {@link Set}<String>
	 */
	public static Set<PaymentType> getKnownPaymentMethods()
	{
		Set<PaymentType> storage = new HashSet<PaymentType>(Arrays.asList(PaymentType.values()));
		storage.removeAll(getBlackListPaymentMethods());
		return storage;
	}

	public static final Set<String> getKnownPaymentMethodsToBasketString()
	{
		Set<String> paymentMethodsToBasketString = new HashSet<String>();
		for(PaymentType type:getKnownPaymentMethods())
		{
			paymentMethodsToBasketString.add(type.toBasketString());
		}
		return Collections.unmodifiableSet(paymentMethodsToBasketString);
	}


	public static boolean isPaymentMethodConfigured(PaymentType paymentType)
	{
		return paymentType.isConfigured();
	}

	/**
	 * Returns all electronic payment methods that are not working, either due to being in development/testing mode or that are not supported due to
	 * insufficient security or because they don't support online responce
	 * @return
	 */
	public static Set<PaymentType> getBlackListPaymentMethods()
	{
		Set<PaymentType> blackList = new HashSet<PaymentType>();
		blackList.add(PaymentType.CSOB_TLACITKO);
		blackList.add(PaymentType.OTP_BANK);
		blackList.add(PaymentType.POST_BANK);
		return blackList;
	}
}
