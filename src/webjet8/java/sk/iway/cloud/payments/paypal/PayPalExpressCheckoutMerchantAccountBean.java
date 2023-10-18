package sk.iway.cloud.payments.paypal;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import sk.iway.iwcm.database.ActiveRecord;

/**Dokumentacia:
 * https://developer.paypal.com/docs/integration/direct/express-checkout/integration-jsv4/add-paypal-button/
 * 
 * @author prau
 *
 */

@Entity
@Table(name="paypal_ex_merchant_account")
public class PayPalExpressCheckoutMerchantAccountBean extends ActiveRecord implements Serializable
{
	private static final long serialVersionUID = -1;
	
	@Id
	@GeneratedValue(generator="WJGen_paypal_ex_ch_merchant_account")
	@TableGenerator(name="WJGen_paypal_ex_ch_merchant_account",pkColumnName="paypal_ex_ch_merchant_account")
	@Column
	private int id;
	@Column(name="client_id")
	private String clientId;
	@Column
	private String secret;
	@Column(name="domain_id")
	private int domainId;
	
	@Override
	public int getId()
	{
		return id;
	}

	@Override
	public void setId(int id)
	{
		this.id = id;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public int getDomainId() {
		return domainId;
	}

	public void setDomainId(int domainId) {
		this.domainId = domainId;
	}
}
