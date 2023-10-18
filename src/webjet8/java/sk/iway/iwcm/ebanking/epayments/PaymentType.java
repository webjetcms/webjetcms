package sk.iway.iwcm.ebanking.epayments;

import static sk.iway.iwcm.ebanking.Bank.CSOB;
import static sk.iway.iwcm.ebanking.Bank.DEXIA;
import static sk.iway.iwcm.ebanking.Bank.OTP_BANKA;
import static sk.iway.iwcm.ebanking.Bank.POSTOVA_BANKA;
import static sk.iway.iwcm.ebanking.Bank.SLOVENSKA_SPORITELNA;
import static sk.iway.iwcm.ebanking.Bank.TATRA_BANKA;
import static sk.iway.iwcm.ebanking.Bank.UNI_CREDIT;
import static sk.iway.iwcm.ebanking.Bank.VUB;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import sk.iway.cloud.payments.pay24.Pay24MerchantAccountBean;
import sk.iway.cloud.payments.paypal.PayPalExpressCheckoutMerchantAccountBean;
import sk.iway.cloud.payments.paypal.PayPalMerchantAccountBean;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.ebanking.Bank;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.ConfDetails;

/**
 *  Banky.java
 *
 *@Title        webjet4_repair
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: jraska $
 *@version      $Revision: 1.6 $
 *@created      Date: 24.8.2009 13:03:51
 *@modified     $Date: 2009/12/11 14:51:53 $
 */
public enum PaymentType
{
	VUB_E_PLATBY("basketPaymentVubEplatbyMid", "basketPaymentVubEplatbyKey")
	{
		@Override
		public String toBasketString(){ return "vubEplatby"; }
		@Override
		public String toString(){ return "VUB E-Platby"; }
	},
	TATRA_PAY("basketPaymentTatraPayMid", "basketPaymentTatraPayKey","basketPaymentTatraPayAllowed")
	{
		@Override
		public String toBasketString(){	return "tatraPay"; }
		@Override
		public String toString(){	return "TatraPay"; }

		@Override
		public boolean isConfigured()
		{

			return super.isConfigured() && Constants.getBoolean("basketPaymentTatraPayAllowed");
		}
	},
	CARD_PAY("basketPaymentTatraPayMid","basketPaymentTatraPayKey","basketPaymentCardPayAllowed")
	{
		@Override
		public String toBasketString(){	return "cardPay";	}
		@Override
		public String toString(){	return "CardPay";	}

		@Override
		public boolean isConfigured()
		{

			return super.isConfigured() && Constants.getBoolean("basketPaymentCardPayAllowed");
		}

		@Override
		public String getEditorName()
		{
			return TATRA_PAY.getEditorName();
		}
	},
	POST_BANK("basketPaymentPostBankMid")
	{
		@Override
		public String toBasketString(){	return "postBank"; }
		@Override
		public String toString(){	return "Poštová banka"; }
	},
	SPORO_PAY("basketPaymentSporoPayAccount", "basketPaymentSporoPayAccountPrefix", "basketPaymentSporoPayKey")
	{
		@Override
		public String toBasketString(){	return "sporoPay"; }
		@Override
		public String toString(){	return "SporoPay"; }
	},
	UNI_PLATBA("basketPaymentUniPlatbaMid", "basketPaymentUniPlatbaKey")
	{
		@Override
		public String toBasketString(){	return "uniPlatba"; }
		@Override
		public String toString(){	return "UniPlatba"; }
	},
	DEXIA_PAY("basketPaymentDexiaPayMid", "basketPaymentDexiaPayKey")
	{
		@Override
		public String toBasketString(){	return "dexiaPay"; }
		@Override
		public String toString(){	return "DexiaPay"; }
	},
	OTP_BANK("basketPaymentOtpKey", "basketPaymentOtpMid")
	{
		@Override
		public String toBasketString(){	return "otpBank"; }
		@Override
		public String toString(){	return "OtpBank"; }
	},
	CSOB_TLACITKO("basketPaymentCSOBMid")
	{
		@Override
		public String toBasketString(){	return "csobTlacitko"; }
		@Override
		public String toString(){	return "ČSOB Platobné Tlačítko"; }
	},
	PAYPAL("payPalUser","payPalPwd","payPalSignature")
	{
		@Override
		public String toBasketString(){	return "paypal"; }
		@Override
		public String toString(){	return "PayPal"; }
	},
	PAYPAL_EXPRESS_CHECKOUT("PayPalExChClientId","PayPalExChSecret")
	{
		@Override
		public String toBasketString(){	return "PayPalExCh"; }
		@Override
		public String toString(){	return "PayPalExCh"; }
		@Override
		public String getEditorName()
		{
			return "paypal_express_checkout";
		}
	},
	PAY24("24pay_Key","24pay_IV")
	{
		@Override
		public String toBasketString(){	return "24pay"; }
		@Override
		public String toString(){	return "24pay"; }

		@Override
		public boolean isConfigured()
		{
			if("cloud".equals(Constants.getInstallName()))
			{
				Pay24MerchantAccountBean merchant = new JpaDB<Pay24MerchantAccountBean>(Pay24MerchantAccountBean.class).findFirst("domainId", CloudToolsForCore.getDomainId());
				if(merchant == null || Tools.isAnyEmpty(merchant.getEshopId(), merchant.getKey(), merchant.getMid()))
					return false;
				return true;
			}
			else
			{
				return Tools.isNotEmpty(Constants.getString("24payKey")) && Tools.isNotEmpty(Constants.getString("24payMid")) && Tools.isNotEmpty(Constants.getString("24payEshopId"));
			}
		}
	},
	GOPAY("gopay_ID","gopay_secret")
	{
		@Override
		public String toBasketString(){	return "gopay"; }
		@Override
		public String toString(){	return "gopay"; }

		@Override
		public boolean isConfigured()
		{
			return Tools.isNotEmpty(Constants.getString("gopayClientId")) && Tools.isNotEmpty(Constants.getString("gopayClientSecret")) && Tools.isNotEmpty(Constants.getString("gopayUrl")) && Tools.isNotEmpty(Constants.getString("gopayGoId"));
		}
	};

	List<String> requiredConstants;

	private PaymentType(String... requiredConstants)
	{
		this.requiredConstants = Collections.unmodifiableList(Arrays.asList(requiredConstants));
	}

	/**
	 * Vrati retazec, ktorym je typ platby oznacovany v parametroch komponenty Basket.
	 * @return
	 */
	public abstract String toBasketString();

	public static Bank getBanka(PaymentType type)
	{
		switch (type)
		{
			case SPORO_PAY : return SLOVENSKA_SPORITELNA;
			case VUB_E_PLATBY : return VUB;
			case TATRA_PAY :
			case CARD_PAY  : return TATRA_BANKA;
			case UNI_PLATBA : return UNI_CREDIT;
			case POST_BANK : return POSTOVA_BANKA;
			case DEXIA_PAY : return DEXIA;
			case OTP_BANK : return OTP_BANKA;
			case CSOB_TLACITKO : return CSOB;
			case GOPAY :
			default : return TATRA_BANKA;
		}
	}

	public Bank getBanka()
	{
		return getBanka(this);
	}

	public String getEditorName()
	{
		return toBasketString();
	}

	public boolean isConfigured()
	{
		if("cloud".equals(Constants.getInstallName())==false)
		{
			for (String key : requiredConstants)
				if (!Constants.containsKey(key) || Tools.isEmpty(Constants.getString(key)))
					return false;
		}
		else
		{
			if(this.equals(PAYPAL))
			{
				PayPalMerchantAccountBean merchant = new JpaDB<PayPalMerchantAccountBean>(PayPalMerchantAccountBean.class).findFirst("domainId", CloudToolsForCore.getDomainId());
				if(merchant == null) return false;
			}
			if(this.equals(PAY24))
			{
				Pay24MerchantAccountBean merchant = new JpaDB<Pay24MerchantAccountBean>(Pay24MerchantAccountBean.class).findFirst("domainId", CloudToolsForCore.getDomainId());
				if(merchant == null || Tools.isAnyEmpty(merchant.getEshopId(), merchant.getKey(), merchant.getMid()))
					return false;
			}
			if(this.equals(PAYPAL_EXPRESS_CHECKOUT))
			{
				if(InitServlet.isTypeCloud())
				{
					PayPalExpressCheckoutMerchantAccountBean merchant  = new JpaDB<PayPalExpressCheckoutMerchantAccountBean>(PayPalExpressCheckoutMerchantAccountBean.class).findFirst("domainId",  CloudToolsForCore.getDomainId());
					return merchant != null && Tools.isNotEmpty(merchant.getSecret()) && Tools.isNotEmpty(merchant.getClientId());
				}
				else
				{
					ConfDetails cd = ConfDB.getVariable("PayPalExChClientId");
					ConfDetails cd2 = ConfDB.getVariable("PayPalExChSecret");
					return cd != null && cd2 != null && !Tools.isAnyEmpty(cd.getValue(), cd2.getValue());
				}
			}
		}

		return true;
	}

	public static PaymentType getPaymentTypeFromBasketString(String basketString)
	{
		PaymentType[] paymentTypes = PaymentType.values();
		for(int i=0;i<paymentTypes.length;i++)
		{
			if(basketString.equals(paymentTypes[i].toBasketString()))
				return paymentTypes[i];
		}
		throw new IllegalArgumentException("Retazec '"+basketString+"' sa neda skonvertovat na PaymentType!!!");
	}
}