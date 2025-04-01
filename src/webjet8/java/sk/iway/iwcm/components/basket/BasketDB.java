package sk.iway.iwcm.components.basket;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.beanutils.BeanUtils;
import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.BasketDBTools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.ShowDoc;
import sk.iway.iwcm.system.jpa.JpaTools;

/**
 *  BasketDB.java - Praca s nakupnym kosikom, funkcie na pracu s databazou s tabulkou basket_item
 *  MBO: upravene aby sa dalo pracovat len s polozkami s domain_id aktualnej domeny
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: jeeff $
 *@version      $Revision: 1.16 $
 *@created      Date: 9.11.2005 15:08:15
 *@modified     $Date: 2010/01/20 10:12:27 $
 */
public class BasketDB
{
	private static final String BASKET_INVOICE_ID = "basketInvoiceId";
	private static final String BASKET_INVOICE = "basketInvoice";
	protected static final String BROWSER_ID_SESSION_KEY = "BasketDB.browserIdSession";

	/**
	 * Vrati instanciu objektu BasketItemBean podla zadaneho identifikatora
	 *
	 * @param basketItemId
	 * @return
	 */
	public static BasketItemBean getBasketItemById(int basketItemId)
	{
		//return JpaTools.getEclipseLinkEntityManager().find(BasketItemBean.class, basketItemId);

		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		ExpressionBuilder builder = new ExpressionBuilder();

		//Filter by domainId
		Expression expression = builder.get("domainId").equal(CloudToolsForCore.getDomainId());

		expression = JpaDB.and(expression, builder.get("basketItemId").equal(basketItemId));

		ReadAllQuery dbQuery = new ReadAllQuery(BasketItemBean.class, builder);

		dbQuery.setSelectionCriteria(expression);


		Query query = em.createQuery(dbQuery);
		BasketItemBean result = null;
		try
		{
			result = (BasketItemBean) query.getSingleResult();
			return result;
		}
		catch (NoResultException ex)
		{
			return null;
		}
	}

	/**
	 * Toto bolo povodne zalozene na statBrowserId, ale po zvazeni som to preimplementoval
	 * tak, aby to bolo zavisle na session (inak by zostavali neobjednane produkty v kosiku)
	 *
	 * @param request
	 *
	 * @return
	 */
	public static long getBrowserId(HttpServletRequest request)
	{
		long browserId = 0;

		String cValue = (String)request.getSession().getAttribute(BROWSER_ID_SESSION_KEY);

		if (Tools.isEmpty(cValue))
		{
			browserId = PkeyGenerator.getNextValue("basket_browser_id");
			request.getSession().setAttribute(BROWSER_ID_SESSION_KEY, Long.toString(browserId));
		}
		else
		{
			browserId = Integer.parseInt(cValue);
		}

		return(browserId);
	}

	/**
	 * Vrati userId z requestu
	 *
	 * @param request
	 * @return
	 */
	public static int getUserId(HttpServletRequest request)
	{
		int userId = -1;
		HttpSession session = request.getSession();
		Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
		if (user != null)
			userId = user.getUserId();

		return(userId);
	}

	/**
	 * Prida (nastavi QTY) polozku do nakupneho kosika
	 *  @param basketItemId = docId stranky s polozkou
	 *  @param basketQty = pocet kusov (ak je null pouzije sa 1)
	 *  @param basketUserNote = poznamka k polozke
	 *  @param request
	 *
	 * @return
	 */
	public static boolean setItemFromDoc(HttpServletRequest request, int basketItemId, int basketQty, String basketUserNote)
	{
		if (ShowDoc.getXssRedirectUrl(request) != null)
			return false;

		int itemId = Tools.getIntValue(basketItemId, -1);
		if (itemId == -1)
			return(false);

		boolean ok = false;

		long browserId = getBrowserId(request);
		int userId = BasketDB.getUserId(request);
		int qty = basketQty;
		String userNote = basketUserNote;

		if (userNote == null)
			userNote = "";

		//MBO: zabranenie ziskania DocDetails dokumentu pre inu domenu musi byt osetrene na urovni DocDB
		DocDB docDB = DocDB.getInstance();
		DocDetails doc = docDB.getDoc(itemId);

		int invoiceId = -1;

		if (doc != null)
		{
			BasketItemBean basketItem = null;
			basketItem = getBasketItemBean(basketItem, request, qty, userNote);

			if (basketItem != null)
			{
				if (userId > 0)
					basketItem.setLoggedUserId(userId);
				if (Tools.isNotEmpty(userNote))
					basketItem.setItemNote(userNote);
				if ("set".equals(request.getParameter("act")))
				{
					if (qty < 1)
					{
						BasketDB.deleteBasketItem(basketItem.getBasketItemId());
						return true;
					}
					else
					{
						if(!canAddItem(doc, null, Integer.valueOf(qty)))
							return false;
						basketItem.setItemQty(Integer.valueOf(qty));
					}
				}
				else
				{
					if(!canAddItem(doc, basketItem, qty))
						return false;
					basketItem.setItemQty(Integer.valueOf(basketItem.getItemQty().intValue() + qty));
				}
				ok = true;
			}
			else
			{
				basketItem = new BasketItemBean();

				basketItem.setBrowserId(Long.valueOf(browserId));
				basketItem.setLoggedUserId(userId);
				basketItem.setItemId(itemId);

				Double price = Double.valueOf(doc.getPriceDouble(request));
				Double vat = Double.valueOf(doc.getVatDouble());

				if(request.getAttribute("docPrice")!= null)
					price = Tools.getDoubleValue(request.getAttribute("docPrice"), 0);

				if(request.getAttribute("docVat")!= null)
					vat = Tools.getDoubleValue(request.getAttribute("docVat"), 0);

				basketItem.setItemPrice(price);
				basketItem.setItemVat(vat);
				if(!canAddItem(doc, basketItem, qty))
					return false;
				basketItem.setItemQty(Integer.valueOf(qty));
				basketItem.setItemNote(userNote);
				basketItem.setDateInsert(new Date(Tools.getNow()));

				//nastavime domainId - v podstate mozeme vlozit cokolvek, setter je preimplementovanya vzdy sa vlozi aktualna domena.
				basketItem.setDomainId(CloudToolsForCore.getDomainId());

				basketItem.setBasketInvoice(InvoiceDB.getInvoiceById(invoiceId));

				//uloz aj nazov a PN, lebo sa moze stat, ze dokument sa vymaze
				basketItem.setItemTitle(doc.getTitle());
				if(request.getAttribute("itemTitle")!= null)
					basketItem.setItemTitle((String)request.getAttribute("docTitle"));
				basketItem.setItemPartNo(doc.getFieldA());
				if(request.getAttribute("itemPartNo")!= null)
					basketItem.setItemPartNo((String)request.getAttribute("itemPartNo"));
			}

			BasketDB.saveBasketItem(basketItem);
			ok = true;
		}

		return(ok);
	}

	private static BasketItemBean getBasketItemBean(BasketItemBean basketItem, HttpServletRequest request, int qty, String userNote)
	{
		long browserId = getBrowserId(request);
		int invoiceId = -1;
		int itemId = Tools.getIntValue(Tools.getIntValue(request.getParameter("basketItemId"), -1), -1);
		if (itemId == -1)
			return null;

		if (Constants.getBoolean("basketCumulateItems"))
		{
			int basketId = Tools.getIntValue(request.getParameter("basketId"), -1);
			if (basketId > 0)
			{
				//basketItem = getBasketItem(session, browserId, invoiceId, itemId);
				basketItem = BasketDB.getBasketItemById(basketId);
			}
			else
			{
				basketItem = BasketDB.getBasketItem(getBrowserId(request), invoiceId, itemId);
				if (basketItem!=null && qty != basketItem.getItemQty().intValue() && Tools.isNotEmpty(userNote))
				{
					//ak sa meni QTY, skus najst aj podla userNote (measureshop...)
					basketItem = BasketDB.getBasketItem(browserId, invoiceId, itemId, userNote);
				}
			}
		}
		else
		{
			int basketId = Tools.getIntValue(request.getParameter("basketId"), -1);
			if (basketId > 0)
			{
				//basketItem = getBasketItem(session, browserId, invoiceId, itemId);
				basketItem = BasketDB.getBasketItemById(basketId);
			}
		}

		return basketItem;
	}

	/** Ak je dost produktov na sklade tak vrati true (mozeme vlozit do kosiku)
	 *
	 * @param doc
	 * @param basketItemBean
	 * @param newQty - pocet produktov ktore pridavame
	 * @return
	 * @author		$Author: $(prau)
	 * @Ticket 		Number: #16989 WJ Cloud - Rozšírenie košíka o počet položiek na sklade
	 */
	private static boolean canAddItem(DocDetails doc, BasketItemBean basketItemBean,int newQty)
	{
		Logger.debug(BasketDB.class, "doc: "+doc+" basketItemBean: "+basketItemBean  +" newQty: "+newQty);
		if(doc == null)
		{
			return false;
		}

		int oldQty = 0;
		if(basketItemBean != null)
			oldQty = Tools.getIntValue(basketItemBean.getItemQty());

		//kvoli moznostiam dopravy ktore su v /system adresari
		if (doc.getDocLink().toLowerCase().startsWith("/system"))
		{
			return true;
		}

		//ak tam nie je ziadne cislo pocitanie kusov na sklade neriesime
		if (Tools.isEmpty(doc.getFieldB()) || Tools.getIntValue(doc.getFieldB(),-9999)==-9999) return true;

		return Tools.getIntValue(doc.getFieldB(),0) >= (newQty+oldQty);
	}

	/** Ak je dost produktov na sklade tak vrati true (mozeme vlozit do kosiku)
	 *
	 * @param req - HttpServletRequest
	 * @return
	 * @Ticket 		Number: #16989 WJ Cloud - Rozšírenie košíka o počet položiek na sklade
	 */
	public static boolean canAddItem(HttpServletRequest req)
	{
		int qty = Tools.getIntValue(req.getParameter("basketQty"), 1);
		String userNote = req.getParameter("basketUserNote");
		int itemId = Tools.getIntValue(Tools.getIntValue(req.getParameter("basketItemId"), -1), -1);
		if (itemId == -1)
			return(false);

		BasketItemBean basketItem = null;
		basketItem = getBasketItemBean(basketItem, req, qty, userNote);

		boolean result = canAddItem(DocDB.getInstance().getDoc(itemId), basketItem, Tools.getIntValue(req.getParameter("basketQty"), 1));
		return result;
	}

	/**
	 * Prida (nastavi QTY) polozku do nakupneho kosika, v requeste ocakava nasledovne parametre:<br />
	 * - basketItemId = docId stranky s polozkou<br />
	 * - basketQty = pocet kusov (ak je null pouzije sa 1)<br />
	 * - basketUserNote = poznamka k polozke<br />
	 *
	 * @param request
	 *
	 * @return
	 */
	public static boolean setItemFromDoc(HttpServletRequest request)
	{
		int itemId = Tools.getIntValue(request.getParameter("basketItemId"), -1);
		if (itemId == -1)
			return(false);
		int qty = Tools.getIntValue(request.getParameter("basketQty"), 1);
		String userNote = request.getParameter("basketUserNote");
		return setItemFromDoc(request, itemId, qty, userNote);
	}

	/**
	 * Vrati polozku z kosika (ak existuje) pre zadane browserId, invoiceId a itemId. Ak neexistuje, vrati null
	 *
	 * @param browserId	identifikator podla browsera
	 * @param invoiceId	identifikator faktury
	 * @param itemId		identifikator polozky v kosiku
	 *
	 * @return
	 */
	public static BasketItemBean getBasketItem(long browserId, int invoiceId, int itemId)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();

		ExpressionBuilder builder = new ExpressionBuilder();
		ReadAllQuery dbQuery = new ReadAllQuery(BasketItemBean.class, builder);

		Expression expr = builder.get("browserId").equal(Long.valueOf(browserId));
		expr = JpaDB.and(expr, builder.get("itemId").equal(itemId));

		//kontroluje aj id domeny
		int domId = CloudToolsForCore.getDomainId();
		expr = JpaDB.and(expr, builder.get("domainId").equal(domId));

		if (invoiceId == -1)
			expr = expr.and(builder.get(BASKET_INVOICE).isNull());
		else
			expr = expr.and(builder.get(BASKET_INVOICE).get(BASKET_INVOICE_ID).equal(invoiceId));

		dbQuery.setSelectionCriteria(expr);

		Query query = em.createQuery(dbQuery);
		List<BasketItemBean> basketItems = JpaDB.getResultList(query);

		//Logger.println(BasketDB.class, "getBasketItem(long browserId, int invoiceId, int itemId) : exp: " + dbQuery.toString() + "\n " + expr.toString());

		if (basketItems.size()>0)
			return(basketItems.get(0));

		return(null);
	}

	/**
	 * Vrati polozku z kosika (ak existuje) pre zadane browserId, userId a itemId a itemNote alebo null
	 * @param browserId
	 * @param invoiceId
	 * @param itemId
	 * @param itemNote
	 * @return
	 */
	public static BasketItemBean getBasketItem(long browserId, int invoiceId, int itemId, String itemNote)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();

		ExpressionBuilder builder = new ExpressionBuilder();
		ReadAllQuery dbQuery = new ReadAllQuery(BasketItemBean.class, builder);

		Expression expr = builder.get("browserId").equal(Long.valueOf(browserId));
		expr = expr.and(builder.get("itemId").equal(itemId));

		//kontroluje aj id domeny
		expr = expr.and(builder.get("domainId").equal(CloudToolsForCore.getDomainId()));

		if (invoiceId == -1)
			expr = expr.and(builder.get(BASKET_INVOICE).isNull());
		else
			expr = expr.and(builder.get(BASKET_INVOICE).get(BASKET_INVOICE_ID).equal(invoiceId));

		expr = expr.and(builder.get("itemNote").equal(itemNote));

		//Logger.println(BasketDB.class, "exp: " + expr.toString());

		dbQuery.setSelectionCriteria(expr);

		Query query = em.createQuery(dbQuery);
		List<BasketItemBean> items = JpaDB.getResultList(query);

		if (items.size() > 0)
			return(items.get(0));

		return(null);
	}

	/**
	 * Vrati zoznam poloziek nakupneho kosika
	 * @param request
	 * @return
	 */
	public static List<BasketItemBean> getBasketItems(HttpServletRequest request)
	{
		long browserId = getBrowserId(request);

		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();

		ExpressionBuilder builder = new ExpressionBuilder();
		ReadAllQuery dbQuery = new ReadAllQuery(BasketItemBean.class, builder);

		Expression expr = builder.get("browserId").equal(Long.valueOf(browserId));
		expr = expr.and(builder.get(BASKET_INVOICE).isNull());

		//kontroluje aj id domeny
		expr = JpaDB.and(expr, builder.get("domainId").equal(CloudToolsForCore.getDomainId()));

		dbQuery.setSelectionCriteria(expr);
		Query query = em.createQuery(dbQuery);

		return JpaDB.getResultList(query);
	}

	/**
	 * Vymaze polozku v kosiku
	 *
	 * @param basketItemBeanId	identifikator polozky v kosiku
	 */
	public static void deleteBasketItem(int basketItemBeanId)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		try
		{
			BasketItemBean basketItem = getBasketItemById(basketItemBeanId);//em.getReference(BasketItemBean.class, basketItemBeanId);

			em.getTransaction().begin();
			em.remove(basketItem);
			em.getTransaction().commit();
		}
		catch (NoResultException ex)
		{
			Logger.debug(BasketDB.class, "Položku košíka nieje možné vymazať");
		}
	}

	/**
	 * Vymazanie celeho nakupneho kosika - vsetkych poloziek ulozenych v session
	 *
	 * @param request
	 * @return
	 */
	public static boolean deleteAll(HttpServletRequest request)
	{
		for (BasketItemBean item : getBasketItems(request))
		{
			BasketDB.deleteBasketItem(item.getBasketItemId());
		}

		request.getSession().removeAttribute(BROWSER_ID_SESSION_KEY);

		return true;
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
	 * Vrati celkovy pocet poloziek v kosiku (vratane QTY)
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
	 * Ulozi polozku kosika do tabulky basket_item
	 *
	 * @param basketItem	instancia objektu polozky v kosiku
	 *
	 * @return true, ak sa ulozenie vydarilo, inak false
	 */
	public static boolean saveBasketItem(BasketItemBean basketItem)
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		try
		{
			em.getTransaction().begin();
			if(basketItem.getBasketItemId() > 0)
			{
				//BasketItemBean originalBasketItem = em.find(BasketItemBean.class, basketItem.getBasketItemId());
				//em.merge(originalBasketItem);

				//pri zmene udajov kosiku musime mergnut k zmenenej entite co prisla, nie k originalu, lebo sa nam neulozia udaje (ziadane WebActive)
				em.merge(basketItem);
			}
			else
				em.persist(basketItem);

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
	 * Funkcia vrati z requestu zobrazovanu menu, ak sa v requeste nenachadza, vrati default z Constants.getString("basketDisplayCurrency")
	 * v pripade cloudu kontroluje nastavenia root grupy fieldC az potom berie basketDisplayCurrency
	 * @param request
	 * @return
	 */
	public static String getDisplayCurrency(HttpServletRequest request)
	{
		return BasketDBTools.getDisplayCurrency(request);
	}

	/**
	 *
	 *
	 * @param items
	 * @param request
	 * @return
	 */
	public static double getTotalLocalPrice(List<BasketItemBean> items, HttpServletRequest request)
	{
		double totalPrice = 0;

		for (BasketItemBean b : items)
		{
			totalPrice += b.getItemLocalPriceQty(request);
		}

		return(totalPrice);
	}

	/**
	 *
	 * @param items
	 * @param request
	 * @return
	 */
	public static double getTotalLocalPriceVat(List<BasketItemBean> items, HttpServletRequest request)
	{
		double totalPrice = 0;

		for (BasketItemBean b : items)
		{
			totalPrice += b.getItemLocalPriceVatQty(request);
		}

		return(totalPrice);
	}


	/**Vrati aktualne moznosti dopravy. Ak doprava neexistuje, vrati prazdny list
	 *
	 * MBO: moze sa poslat parameter lng, vtedy hlada [nazov][lng], ak nenajde, hlada [nazov]
	 *
	 * @return
	 * @Ticket 		Number: #15137
	 */
	public static List<DocDetails> getModeOfTransports(String... lng)
	{
		return getModeOfTransports(null, lng);
	}

	/**
	 * Vrati moznosti len osobneho vyzdvihnutia tovaru, ak je v kosiku polozka, ktora sa da vyzdvihnut len osobne.
	 * Definovanie cez konf. premennu basketTransportInStorePickupFieldName, pokial nie je zadana, vrati vsetky moznosti dopravy
	 *
	 * @param request
	 * @param lng
	 * @return
	 */
	public static List<DocDetails> getModeOfTransports(HttpServletRequest request, String... lng)
	{
		List<DocDetails> modeOfTransports = new ArrayList<DocDetails>();
		GroupsDB groupsDB = GroupsDB.getInstance();
		String language = "";
		String basketTransportsGroupName = Constants.getString("basketTransportGroupName");
		if(Tools.isEmpty(basketTransportsGroupName)) basketTransportsGroupName = "ModeOfTransport"; //vychodzi nazov
		if (lng!=null&&lng.length>0)
		{
			language = lng[0];
		}
		// ModeOfTransport je zapisany aj v /components/cloud/basket/admin_transports_list.jsp
		GroupDetails groupDetails = groupsDB.getLocalSystemGroup();
		if (groupDetails==null)
		{
			//asi to neni cloud alebo multidomain WJ, alebo bezi na localhost (iwcm.interway.sk)
			groupDetails = groupsDB.getGroupByPath("/System");
		}
		boolean modeOfTransportsFound = false;
		if(groupDetails != null)
		{
			List<GroupDetails> subgroups = groupsDB.getGroups(groupDetails.getGroupId());
			if (subgroups!=null)
			{
				//hladaj najprv jazykovu mutaciu
				for (GroupDetails group:subgroups)
				{
					if(group.getGroupName().equals(basketTransportsGroupName+language))
					{
						modeOfTransports.addAll(DocDB.getInstance().getDocByGroup(group.getGroupId()));
						modeOfTransportsFound = true;
					}
				}
				//ak nic nenasiel, hladaj bez pripony jazyka
				if(modeOfTransportsFound == false)
				{
					for (GroupDetails group:subgroups)
					{
						if(group.getGroupName().equals(basketTransportsGroupName))
						{
							modeOfTransports.addAll(DocDB.getInstance().getDocByGroup(group.getGroupId()));
							return modeOfTransports;
						}
					}
				}
			}
		}

		/**
		 * [LESYSR-81]: ak niektory z produktov ma nastavene len osobny odber na predajni, tak cely nakupny kosik musi mat obmedzenie len na osobný odber
		 * zistim, ci mam polozka v kosiku ktora sa da vyzdvihnut len osobne a vratim moznost len osobneho odberu celej objednavky
		 * basketTransportInStorePickupFieldName - tu mam definovane pole kde je priznak osobneho odberu napr. fieldM
		*/
		String inStorePickupFieldName = Constants.getString("basketTransportInStorePickupFieldName");
		if(request != null && Tools.isNotEmpty(inStorePickupFieldName) && modeOfTransportsFound && modeOfTransports.size() > 0)
		{
			List <BasketItemBean> basketItems = BasketDB.getBasketItems(request);
			boolean foundInStorePickup = false;
			if(basketItems != null && basketItems.size() > 0)
			{
				//zistim, ci je nieco zadane v danom poli. Ak je prazdne, beriem, ze je oznacene len pre odobny odber
				for(BasketItemBean item : basketItems)
				{
					try
					{
						if(Tools.isNotEmpty(BeanUtils.getProperty(item.getDoc(), inStorePickupFieldName)))
						{
							foundInStorePickup = true;
							break;
						}
					}
					catch(Exception e)
					{}
				}
				//ak ano, vymazem ine sposoby dorucenia. Osobny odber musi mat tiez zadane v danom volnom poli, ze sa jedna o osobny odber
				if(foundInStorePickup)
				{
					Iterator<DocDetails> transportIterator = modeOfTransports.iterator();
					while (transportIterator.hasNext())
					{
						DocDetails transport = transportIterator.next();
						try
						{
							if(Tools.isEmpty(BeanUtils.getProperty(transport, inStorePickupFieldName)))
								transportIterator.remove();
						}
						catch(Exception e)
						{}
					}
				}
			}
		}

		return modeOfTransports;
	}

	/** Vrati pocet poloziek na sklade
	 *
	 * @param doc
	 * @return
	 */
	public static synchronized int getCountInRepository(DocDetails doc)
	{
		if(doc == null)
			return 0;

		return Tools.getIntValue(doc.getFieldB(), 0);
	}

}















