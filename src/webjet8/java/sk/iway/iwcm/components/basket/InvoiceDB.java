package sk.iway.iwcm.components.basket;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.util.RequestUtils;
import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.WriteTagToolsForCore;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.helpers.BeanDiff;
import sk.iway.iwcm.helpers.BeanDiffPrinter;
import sk.iway.iwcm.system.jpa.JpaTools;

/**
 *  InvoiceDB.java - Praca s fakturami (objednavkami)
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2005
 *@author       $Author: jeeff $
 *@version      $Revision: 1.15 $
 *@created      Date: 10.11.2005 23:41:49
 *@modified     $Date: 2010/02/09 08:37:24 $
 */
public class InvoiceDB
{

	/**
	 * Nacitanie objednavky podla jej identifikatora

	 * @param basketInvoiceId identifikator faktury(objednavky)
	 * @return objekt BasketInvoiceBean
	 */
	public static BasketInvoiceBean getInvoiceById(int basketInvoiceId)
	{
		//return JpaTools.getEclipseLinkEntityManager().find(BasketInvoiceBean.class, basketInvoiceId);

		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
	      ExpressionBuilder builder = new ExpressionBuilder();

	      //Filter by domainId
	      Expression expression = builder.get("domainId").equal(CloudToolsForCore.getDomainId());

	      expression = JpaDB.and(expression, builder.get("basketInvoiceId").equal(basketInvoiceId));

	      ReadAllQuery dbQuery = new ReadAllQuery(BasketInvoiceBean.class, builder);
	      dbQuery.addOrdering(builder.get("createDate").descending());
	      dbQuery.setSelectionCriteria(expression);

	      Query query = em.createQuery(dbQuery);
	      BasketInvoiceBean result = null;
	      try
	      {
	    	  result = (BasketInvoiceBean) query.getSingleResult();
	    	  return result;
	      }
	      catch (NoResultException ex)
	      {
	    	  return null;
	      }
	}

	/**
	 * Funkcia, ktora vrati vsetky faktury z tabulky basket_invoice
	 */
	public static List<BasketInvoiceBean> getAllInvoices()
	{
      JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
      ExpressionBuilder builder = new ExpressionBuilder();

      //vyber iba tie beany s aktualnou domenou
      Expression expression = builder.get("domainId").equal(CloudToolsForCore.getDomainId());

      ReadAllQuery dbQuery = new ReadAllQuery(BasketInvoiceBean.class, builder);
      dbQuery.addOrdering(builder.get("createDate").descending());
      dbQuery.setSelectionCriteria(expression);

      Query query = em.createQuery(dbQuery);
      return JpaDB.getResultList(query);
	}

	/**
	 * Funkcia, ktora vrati faktury vyfiltrovane podla internalInvoiceId
	 */
	public static BasketInvoiceBean getInvoiceByInvoiceId(String internalInvoiceId)
	{
			JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
	      ExpressionBuilder builder = new ExpressionBuilder();

	      //Filter by domainId
	      Expression expression = builder.get("domainId").equal(CloudToolsForCore.getDomainId());

	      expression = JpaDB.and(expression, builder.get("internalInvoiceId").equal(internalInvoiceId));

	      ReadAllQuery dbQuery = new ReadAllQuery(BasketInvoiceBean.class, builder);
	      dbQuery.addOrdering(builder.get("createDate").descending());
	      dbQuery.setSelectionCriteria(expression);

	      Query query = em.createQuery(dbQuery);
	      BasketInvoiceBean result = null;
	      try
	      {
	    	  result = (BasketInvoiceBean) query.getSingleResult();
	    	  return result;
	      }
	      catch (NoResultException ex)
	      {
	    	  return null;
	      }
	}

	/**
	 * Posle notifikacny e-mail.
	 *
	 * @param request
	 * @param invoiceId
	 * @param fromEmail
	 * @param toEmail
	 * @param subject
	 *
	 * @return
	 */
	public static boolean sendInvoiceEmail(HttpServletRequest request, int invoiceId, String fromEmail, String toEmail, String subject)
	{
		return sendInvoiceEmail(request, invoiceId, fromEmail, toEmail, subject, null);
	}

	public static boolean sendInvoiceEmail(HttpServletRequest request, int invoiceId, String fromEmail, String toEmail, String subject, String attachements)
	{
		boolean sendOK = false;

		String compUrl = WriteTagToolsForCore.getCustomPage("/components/basket/invoice_email.jsp", request);

		Cookie [] cookies = request.getCookies();
		String data = Tools.downloadUrl(Tools.getBaseHrefLoopback(request) + compUrl + "?invoiceId=" + invoiceId + "&auth="+BasketInvoiceBean.getAuthorizationToken(invoiceId),cookies);

		String senderName = null;

		try
		{
			//pouzi meno domeny ako odosielatela emailu
			String domain = Tools.getServerName(request);
			domain = Tools.replace(domain, "www.", "");

			String tokens[] = Tools.getTokens(domain, ".");
			if (tokens.length > 0) senderName = tokens[0];
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		if (Tools.isEmpty(senderName)) senderName = fromEmail;

		String basketInvoiceSenderName = Constants.getString("basketInvoiceSenderName");
		if ("@".equals(basketInvoiceSenderName)) senderName = fromEmail;
		else if (Tools.isNotEmpty(basketInvoiceSenderName)) senderName = basketInvoiceSenderName;

		if (Tools.isNotEmpty(data) && Tools.isNotEmpty(toEmail) && toEmail.indexOf('@')!=-1)
		{
			if(Tools.isEmpty(attachements))
				sendOK = SendMail.send(senderName, fromEmail, toEmail, subject, data, request);
			else
				sendOK = SendMail.send(senderName, fromEmail, toEmail, null, null, null, subject, data, Tools.getBaseHref(request), attachements);
		}
		return(sendOK);
	}

	/**
	 * Funkcia, ktora vrati zoznam objektov typu {@link BasketInvoiceBean},
	 * ktore maju flag basketInvoiceId = -1, majú atribut browserId > 0 usporiadanych podla datumu vlozenia zostupne.
	 */
	public static List<BasketInvoiceBean> getNoInvoiceItems()
	{
      JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();

      ExpressionBuilder builder = new ExpressionBuilder();
      ReadAllQuery dbQuery = new ReadAllQuery(BasketInvoiceBean.class, builder);

      Expression expr = builder.get("basketInvoiceId").equal(-1);
      expr = expr.and(builder.get("browserId").greaterThan(Long.valueOf(0)));

      //Filter by domainId
      expr = expr.and(builder.get("domainId").equal(CloudToolsForCore.getDomainId()));

	   dbQuery.setSelectionCriteria(expr);
	   dbQuery.addOrdering(builder.get("dateInsert").descending());

	   Query query = em.createQuery(dbQuery);
		return JpaDB.getResultList(query);
	}

	/**
	 * Vrati celkovu cenu poloziek vratane DPH
	 *
	 * @param items
	 * @return
	 */
	public static double getTotalPriceVat(List<BasketItemBean> items)
	{
		double totalPrice = 0;

		for (BasketItemBean b : items)
		{
			totalPrice += b.getItemPriceVatQty();
		}

		return(totalPrice);
	}

	/**
	 * Vrati celkovu cenu poloziek bez DPH
	 *
	 * @param items
	 * @return
	 */
	public static double getTotalPrice(List<BasketItemBean> items)
	{
		double totalPrice = 0;

		for (BasketItemBean b : items)
		{
			totalPrice += b.getItemPriceQty();
		}

		return(totalPrice);
	}

	/**
	 * Vrati celkovy pocet poloziek v kosiku (vratane QTY)
	 *
	 * @param items
	 * @return
	 */
	public static int getTotalItems(List<BasketItemBean> items)
	{
		int totalItems = 0;

		for (BasketItemBean b : items)
		{
			totalItems += b.getItemQty().intValue();
		}

		return(totalItems);
	}

	/**
	 *
	 *
	 * @param userId
	 * @param statusId
	 * @param name
	 * @param email
	 * @param basketInvoiceId
	 *
	 * @return
	 */
	public static List<BasketInvoiceBean> searchInvoices(int userId, int statusId, String name, String email, int basketInvoiceId)
	{
		List<BasketInvoiceBean> invoices = new ArrayList<BasketInvoiceBean>();

		if(basketInvoiceId != -999)
		{
			BasketInvoiceBean inv = InvoiceDB.getInvoiceById(basketInvoiceId);
			if(inv != null)
				invoices.add(inv);
			return invoices;
		}

		if (name != null)
			name = DB.internationalToEnglish(name).toLowerCase();
		if (email != null)
			email = email.toLowerCase();

		for (BasketInvoiceBean i : getAllInvoices())
		{
			try
			{
				if (userId > 0 && i.getLoggedUserId() != userId)
				{
					continue;
				}
				if (statusId > 0 && i.getStatusId() != null && i.getStatusId().intValue()!=statusId)
				{
					continue;
				}
				if (Tools.isNotEmpty(name) && i.getDeliveryName()!=null && DB.internationalToEnglish(i.getDeliveryName()).toLowerCase().indexOf(name)==-1)
				{
					continue;
				}
				if (Tools.isNotEmpty(email) && i.getContactEmail()!=null && i.getContactEmail().toLowerCase().indexOf(email)==-1)
				{
					continue;
				}

				invoices.add(i);
			}
			catch (RuntimeException ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
		}

		return(invoices);
	}

	/**
	 * Vrati zoznam objednavok pre daneho pouzivatela
	 *
	 * @param userId Identifikator pouzivatela
	 *
	 * @return 	Zoznam objektov typu {@link BasketInvoiceBean} - faktury resp. objednavok daneho pouzivatela. <br />
	 * 			Ak je parameter userId &lt; 1, vrati vsetky faktury zoradene podla datumu zostupne.
	 */
	public static List<BasketInvoiceBean> getInvoices(int userId)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();

      ExpressionBuilder builder = new ExpressionBuilder();
      ReadAllQuery dbQuery = new ReadAllQuery(BasketInvoiceBean.class, builder);

      if (userId > 0)
      {
      	Expression expr = builder.get("loggedUserId").equal(userId);

      	//Filter by domainId
      	expr = JpaDB.and(expr, builder.get("domainId").equal(CloudToolsForCore.getDomainId()));

      	dbQuery.setSelectionCriteria(expr);
      }

	   dbQuery.addOrdering(builder.get("createDate").descending());

	   Query query = em.createQuery(dbQuery);
		return JpaDB.getResultList(query);
	}

	/**
	 * Ulozenie objednavky do databazy
	 *
	 * @param request
	 * @return
	 */
	public static BasketInvoiceBean saveOrder(HttpServletRequest request)
	{
		BasketInvoiceBean invoice = null;

		try
		{
			invoice = new BasketInvoiceBean();
			RequestUtils.populate(invoice, request);

			long browserId = BasketDB.getBrowserId(request);
			int userId = BasketDB.getUserId(request);

			invoice.setBrowserId(Long.valueOf(browserId));
			invoice.setLoggedUserId(userId);
			invoice.setCreateDate(new Date(Tools.getNow()));
			invoice.setStatusId(BasketInvoiceBean.INVOICE_STATUS_NEW);
			invoice.setCurrency(BasketDB.getDisplayCurrency(request));
			invoice.setUserLng(PageLng.getUserLng(request));

			int deliveryMethod = Tools.getIntValue(request.getParameter("deliveryMethod"),-1);
			if(deliveryMethod > 0)
			{
				DocDetails deliveryDoc = DocDB.getInstance().getBasicDocDetails(deliveryMethod, false);
				if (deliveryDoc != null) invoice.setDeliveryMethod(deliveryDoc.getTitle());
			}

			//nastav jej polozky
			List<BasketItemBean> items = BasketDB.getBasketItems(request);

			List<BasketItemBean> basketItems = new ArrayList<BasketItemBean>();

			for (BasketItemBean b : items)
			{
				DocDetails itemDoc = b.getDoc();
				double price = itemDoc.calculateLocalPrice(itemDoc.getPrice(request), BasketDB.getDisplayCurrency(request)).doubleValue();
				if (price > 0)
				{
					b.setItemPrice(price);
				}

				basketItems.add(b);
				b.setBasketInvoice(invoice);
			}

			invoice.setBasketItems(basketItems);

			if(saveInvoice(invoice))
			{
				Adminlog.add(Adminlog.TYPE_BASKET_CREATE, "Vytvorena objednavka: " + StringUtils.join(items.iterator(), ","), invoice.getBasketInvoiceId(), -1);
				Logger.println(BasketDB.class, "Objednavka ulozena, id= " + invoice.getBasketInvoiceId());
				//zrus browserId
				request.getSession().removeAttribute(BasketDB.BROWSER_ID_SESSION_KEY);
			}
			else
			{
				Logger.error(BasketDB.class, "Chyba pri ulozeni objednavky do DB invoice_id: "+invoice.getBasketInvoiceId());
				invoice = null;
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			invoice = null;
		}

		return(invoice);
	}

	/**
	 * Ulozi objednavku do tabulky basket_invoice
	 *
	 * @param basketInvoice	objekt faktury (objednavky)
	 *
	 * @return true, ak sa ulozenie vydarilo, inak false
	 */
	public static boolean saveInvoice(BasketInvoiceBean basketInvoice)
	{
		//pre istotu setneme domainId
		basketInvoice.setDomainId(CloudToolsForCore.getDomainId());
		if (basketInvoice.getBasketInvoiceId() > 0)
		{
			String message = new BeanDiffPrinter(new BeanDiff().setNewLoadJpaOriginal(basketInvoice, basketInvoice.getBasketInvoiceId())).toString();
			Adminlog.add(Adminlog.TYPE_BASKET_UPDATE, String.format("Zmenená objednávka: %s", message), basketInvoice.getBasketInvoiceId(), -1);
		}
	    JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
	    try
	    {
	       em.getTransaction().begin();
	       if(basketInvoice.getBasketInvoiceId() < 1)
	       {
	    	   em.persist(basketInvoice);
	       }
	       else
	       {
	    	   em.merge(basketInvoice);
	       }

	       em.getTransaction().commit();

	       return true;
	    }
	    catch(Exception e)
	    {
	   	 sk.iway.iwcm.Logger.error(e);
	   	 return false;
	    }
	}


	/**
	 * Vymaze objednavku a vymazu sa aj vsetky jej polozky
	 *
	 * @param basketInvoiceId	identifikator faktury
	 */
	public static void deleteInvoice(int basketInvoiceId)
	{


	  JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();

	  //vytiahne objednavku, respektujuc domainID, v pripade vyhodenia vynimky (ina domena) nevymaze
	  BasketInvoiceBean basketInvoice = getInvoiceById(basketInvoiceId); //em.getReference(BasketInvoiceBean.class, basketInvoiceId);

	  em.getTransaction().begin();
	  em.remove(basketInvoice);
	  em.getTransaction().commit();
	  Adminlog.add(Adminlog.TYPE_BASKET_DELETE, "Zmazaná objednávka", basketInvoiceId, -1);

	}

	/** Znizi pocet produktov v sklade z objednavky
	 *
	 * @param basketInvoiceId
	 * @Ticket 		Number: #16989
	 */
	public static void decreaseCountOfProductFromStock(int basketInvoiceId)
	{
		BasketInvoiceBean invoiceBean = InvoiceDB.getInvoiceById(basketInvoiceId);

		if(invoiceBean == null )
			return;

		List<BasketItemBean> listItmes = invoiceBean.getBasketItems();
		if(listItmes == null )
			return;

		for(BasketItemBean item: listItmes)
		{
			decreaseProductCount(item);
		}
	}

	/** Odobere konkretnemu produktu pocet kusov na sklade
	 *
	 *
	 * @author		$Author: $(prau)
	 * @Ticket 		Number: #16989
	 */
	private static synchronized void decreaseProductCount(BasketItemBean item )
	{
		DocDetails docDetails = item.getDoc();
		if(docDetails != null )
		{
			int stockQty = Tools.getIntValue(docDetails.getFieldB(), -1);
			int productQty = Tools.getIntValue(item.getItemQty());

			if(stockQty > 0 && productQty > 0)
			{
				if(stockQty - productQty >= 0)
					docDetails.setFieldB(stockQty - productQty + "");
				else//objednali viac produktov ako je na sklade (nemalo by nastat)
					docDetails.setFieldB("0");

				DocDB.saveDoc(docDetails);
			}
		}

	}

}