package sk.iway.iwcm.components.basket.rest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.WriteTagToolsForCore;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicesRepository;
import sk.iway.iwcm.components.basket.jpa.InvoiceStatus;
import sk.iway.iwcm.components.basket.payment_methods.rest.PaymentMethodsService;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.ShowDoc;
import sk.iway.iwcm.helpers.BeanDiff;
import sk.iway.iwcm.helpers.BeanDiffPrinter;

@Service
public class EshopService {

	private static final String BROWSER_ID_SESSION_KEY = "BasketDB.browserIdSession";
	private static final String BASKET_ITEM_ID = "basketItemId";
	private static final String BASKET_QTY = "basketQty";

	private final BasketInvoicesRepository bir;
    private final BasketInvoiceItemsRepository biir;
	private final BasketInvoicePaymentsRepository bipr;

	public EshopService(BasketInvoicesRepository bir, BasketInvoiceItemsRepository biir, BasketInvoicePaymentsRepository bipr) {
		this.bir = bir;
		this.biir = biir;
		this.bipr = bipr;
	}

	/******************* INSTANCE METHOD **************************/


	public static EshopService getInstance() {
		javax.servlet.ServletContext servletContext = Constants.getServletContext();
		EshopService service = (EshopService) servletContext.getAttribute(EshopService.class.getName());
		if(service == null) {
			service = new EshopService(
				Tools.getSpringBean("basketInvoicesRepository", BasketInvoicesRepository.class),
				Tools.getSpringBean("basketInvoiceItemsRepository", BasketInvoiceItemsRepository.class),
				Tools.getSpringBean("basketInvoicePaymentsRepository", BasketInvoicePaymentsRepository.class)
			);
			servletContext.setAttribute(EshopService.class.getName(), service);
		}

		return service;
	}


	/******************* GETTER METHODS **************************/


	public BasketInvoiceEntity getInvoiceById(int invoiceId) {
		return bir.findById(Long.valueOf(invoiceId)).orElse(null);
	}

	public BasketInvoiceItemEntity getBasketItemById(int basketItemId) {
		return biir.findById(Long.valueOf(basketItemId)).orElse(null);
	}

	/**
	 * Vrati zoznam poloziek nakupneho kosika
	 * @param request
	 * @return
	 */
	public List<BasketInvoiceItemEntity> getBasketItems(HttpServletRequest request)
	{
		return biir.findAllByBrowserIdAndItemsBasketInvoiceNullAndDomainId(EshopService.getBrowserId(request), CloudToolsForCore.getDomainId());
	}

	/**
	 * ziska zaznamy pre objednavku (invoiceId)
	 * @param invoiceId
	 * @param typ -> true/false - vrati uspesne/neuspesne platby, null - vrati vsetky
	 * @return
	 */
	public List<BasketInvoicePaymentEntity> getBasketInvoicePaymentByInvoiceId(int invoiceId, Boolean typ)
	{
		if(typ == null) {
			return bipr.findAllByInvoiceId(Long.valueOf(invoiceId));
		} else {
			return bipr.findAllByInvoiceIdAndConfirmed(Long.valueOf(invoiceId), typ);
		}
	}

	/**
	 * vracia vyslednu sumu zaplatenu ciastkovymi platbami na zaklade invoiceId
	 * @param invoiceId
	 * @return
	 */
	public BigDecimal getPaymentsSum(int invoiceId)
	{
		return ProductListService.getPayedPrice(Long.valueOf(invoiceId), bipr);
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
	public BasketInvoiceItemEntity getBasketItem(long browserId, int invoiceId, int itemId)
	{
		List<BasketInvoiceItemEntity> basketItems = new ArrayList<>();
		int domId = CloudToolsForCore.getDomainId();

		if (invoiceId == -1)
			basketItems = biir.findBasketItemInvoiceNull(Long.valueOf(itemId), browserId, domId);
		else
			basketItems = biir.findBasketItem(Long.valueOf(itemId), browserId, domId, invoiceId);

		return basketItems.size() > 0 ? basketItems.get(0) : null;
	}

	/**
	 * Vrati polozku z kosika (ak existuje) pre zadane browserId, userId a itemId a itemNote alebo null
	 * @param browserId
	 * @param invoiceId
	 * @param itemId
	 * @param itemNote
	 * @return
	 */
	public BasketInvoiceItemEntity getBasketItem(long browserId, int invoiceId, int itemId, String itemNote)
	{
		List<BasketInvoiceItemEntity> basketItems = new ArrayList<>();
		int domId = CloudToolsForCore.getDomainId();


		if (invoiceId == -1)
			basketItems = biir.findBasketItemInvoiceNull(Long.valueOf(itemId), browserId, domId, itemNote);
		else
			basketItems = biir.findBasketItem(Long.valueOf(itemId), browserId, domId, invoiceId, itemNote);

		return basketItems.size() > 0 ? basketItems.get(0) : null;
	}

	private BasketInvoiceItemEntity getBasketItemBean(BasketInvoiceItemEntity basketItem, HttpServletRequest request, int qty, String userNote)
	{
		long browserId = getBrowserId(request);
		int invoiceId = -1;
		int itemId = Tools.getIntValue(Tools.getIntValue(request.getParameter(BASKET_ITEM_ID), -1), -1);
		if (itemId == -1)
			return null;

		if (Constants.getBoolean("basketCumulateItems"))
		{
			int basketId = Tools.getIntValue(request.getParameter("basketId"), -1);
			if (basketId > 0)
			{
				basketItem = getBasketItemById(basketId);
			}
			else
			{
				basketItem = getBasketItem(getBrowserId(request), invoiceId, itemId);
				if (basketItem!=null && qty != basketItem.getItemQty().intValue() && Tools.isNotEmpty(userNote))
				{
					//ak sa meni QTY, skus najst aj podla userNote (measureshop...)
					basketItem = getBasketItem(browserId, invoiceId, itemId, userNote);
				}
			}
		}
		else
		{
			int basketId = Tools.getIntValue(request.getParameter("basketId"), -1);
			if (basketId > 0)
			{
				basketItem = getBasketItemById(basketId);
			}
		}

		return basketItem;
	}


	/******************* SAVE METHODS **************************/

	/**
	 * Ulozi polozku kosika do tabulky basket_item
	 * @param basketItem	instancia objektu polozky v kosiku
	 * @return true, ak sa ulozenie vydarilo, inak false
	 */
	public boolean saveBasketItem(BasketInvoiceItemEntity basketItem)
	{
		biir.save(basketItem);
		return true;
	}

	public BasketInvoiceEntity saveInvoice(BasketInvoiceEntity basketInvoice) {
        //Just to be sure, set domainId
		basketInvoice.setDomainId(CloudToolsForCore.getDomainId());
		if (basketInvoice.getBasketInvoiceId() > 0) {
			String message = new BeanDiffPrinter(new BeanDiff().setNewLoadJpaOriginal(basketInvoice, basketInvoice.getBasketInvoiceId())).toString();
			Adminlog.add(Adminlog.TYPE_BASKET_UPDATE, String.format("Zmenená objednávka: %s", message), basketInvoice.getBasketInvoiceId(), -1);
		}

        return bir.save(basketInvoice);
    }

    public BasketInvoiceEntity saveOrder(HttpServletRequest request)
	{
		BasketInvoiceEntity invoice = null;

		try {
			invoice = new BasketInvoiceEntity();

			BeanWrapperImpl wrapper = new BeanWrapperImpl(invoice);
			wrapper.setPropertyValues(request.getParameterMap());

			long browserId = EshopService.getBrowserId(request);
			int userId = Tools.getUserId(request);

			invoice.setBrowserId(Long.valueOf(browserId));
			invoice.setLoggedUserId(userId);
			invoice.setCreateDate(new Date(Tools.getNow()));
			invoice.setStatusId(InvoiceStatus.INVOICE_STATUS_NEW.getValue());
			invoice.setCurrency(EshopService.getDisplayCurrency(request));
			invoice.setUserLng(PageLng.getUserLng(request));

			int deliveryMethod = Tools.getIntValue(request.getParameter("deliveryMethod"),-1);
			if(deliveryMethod > 0)
			{
				DocDetails deliveryDoc = DocDB.getInstance().getBasicDocDetails(deliveryMethod, false);
				if (deliveryDoc != null) invoice.setDeliveryMethod(deliveryDoc.getTitle());
			}

			//Get all items for adminlog
			List<BasketInvoiceItemEntity> basketItems = new ArrayList<>();

			//Add payment as basket item, if created add it to list
			int newItemId = PaymentMethodsService.createPaymentInvoiceItemAndReturnId(invoice, request);
			if(newItemId != -1) {
				BasketInvoiceItemEntity newBasketItem = getBasketItemById(newItemId);
				basketItems.add( newBasketItem );
			}

			//Save invoice
			invoice = saveInvoice(invoice);

			//Check and logs
			if(invoice.getBasketInvoiceId() > 1)
			{
				Adminlog.add(Adminlog.TYPE_BASKET_CREATE, "Vytvorena objednavka: " + StringUtils.join(basketItems.iterator(), ","), invoice.getBasketInvoiceId(), -1);
				Logger.println(EshopService.class, "Objednavka ulozena, id= " + invoice.getBasketInvoiceId());
				//zrus browserId
				request.getSession().removeAttribute(BROWSER_ID_SESSION_KEY);

				//Bind items to new invoice
				bindItemsToInvoice(invoice.getId(), invoice.getBrowserId());
			}
			else
			{
				Logger.error(EshopService.class, "Chyba pri ulozeni objednavky do DB invoice_id: " + invoice.getBasketInvoiceId());
				invoice = null;
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
			invoice = null;
		}

		return invoice;
	}


	/******************* DELETE METHODS **************************/


	/**
	 * Vymaze polozku v kosiku
	 * @param invoiceItemId	identifikator polozky v kosiku
	 */
	public void deleteBasketItem(int invoiceItemId)
	{
		biir.deleteById(Long.valueOf(invoiceItemId));
	}

	/**
	 * Vymazanie celeho nakupneho kosika - vsetkych poloziek ulozenych v session
	 * @param request
	 * @return
	 */
	public boolean deleteAll(HttpServletRequest request)
	{
		for (BasketInvoiceItemEntity item : getBasketItems(request))
		{
			deleteBasketItem(item.getBasketItemId());
		}

		request.getSession().removeAttribute(BROWSER_ID_SESSION_KEY);
		return true;
	}


	/******************* SUPPORT METHODS **************************/


	public void bindItemsToInvoice(Long invoiceId, Long browserId) {
		//Safety check
		if (invoiceId == null || invoiceId < 1 || browserId == null || browserId < 0) return;
		//Bind items using browserId
		biir.updateInvoiceId(invoiceId, browserId, CloudToolsForCore.getDomainId());
	}

	public boolean sendInvoiceEmail(HttpServletRequest request, int invoiceId, String fromEmail, String toEmail, String subject)
	{
		return sendInvoiceEmail(request, invoiceId, fromEmail, toEmail, subject, null);
	}

	public boolean sendInvoiceEmail(HttpServletRequest request, int invoiceId, String fromEmail, String toEmail, String subject, String attachements)
	{
		boolean sendOK = false;

		String compUrl = WriteTagToolsForCore.getCustomPage("/components/basket/invoice_email.jsp", request);

		Cookie [] cookies = request.getCookies();
		String data = Tools.downloadUrl(Tools.getBaseHrefLoopback(request) + compUrl + "?invoiceId=" + invoiceId + "&auth="+BasketInvoiceEntity.getAuthorizationToken(invoiceId),cookies);

		String senderName = null;

		try
		{
			//pouzi meno domeny ako odosielatela emailu
			String domain = Tools.getServerName(request);
			domain = Tools.replace(domain, "www.", "");

			String[] tokens = Tools.getTokens(domain, ".");
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

	public void decreaseCountOfProductFromStock(int basketInvoiceId)
	{
		BasketInvoiceEntity invoiceBean = getInvoiceById(basketInvoiceId);

		if(invoiceBean == null )
			return;

		List<BasketInvoiceItemEntity> listItmes = invoiceBean.getBasketItems();
		if(listItmes == null )
			return;

		for(BasketInvoiceItemEntity item: listItmes)
		{
			decreaseProductCount(item);
		}
	}

	private synchronized void decreaseProductCount(BasketInvoiceItemEntity item )
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
	public boolean setItemFromDoc(HttpServletRequest request)
	{
		int itemId = Tools.getIntValue(request.getParameter(BASKET_ITEM_ID), -1);
		if (itemId == -1)
			return(false);
		int qty = Tools.getIntValue(request.getParameter(BASKET_QTY), 1);
		String userNote = request.getParameter("basketUserNote");
		return setItemFromDoc(request, itemId, qty, userNote);
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
	public boolean setItemFromDoc(HttpServletRequest request, int basketItemId, int basketQty, String basketUserNote)
	{
		if (ShowDoc.getXssRedirectUrl(request) != null)
			return false;

		int itemId = Tools.getIntValue(basketItemId, -1);
		if (itemId == -1)
			return(false);

		boolean ok = false;

		long browserId = getBrowserId(request);
		int userId = Tools.getUserId(request);
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
			BasketInvoiceItemEntity basketItem = null;
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
						deleteBasketItem(basketItem.getBasketItemId());
						return true;
					}
					else
					{
						if(!canAddItem(doc, null, qty))
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
				basketItem = new BasketInvoiceItemEntity();

				basketItem.setBrowserId(Long.valueOf(browserId));
				basketItem.setLoggedUserId(userId);
				basketItem.setItemId(itemId);

				BigDecimal price = doc.getPrice(request);
				BigDecimal vat = doc.getVat();

				if(request.getAttribute("docPrice")!= null)
					price = Tools.getBigDecimalValue(request.getAttribute("docPrice"), "0");

				if(request.getAttribute("docVat")!= null)
					vat = Tools.getBigDecimalValue(request.getAttribute("docVat"), "0");

				basketItem.setItemPrice(price);
				basketItem.setItemVat( vat.intValue() );
				if(!canAddItem(doc, basketItem, qty))
					return false;
				basketItem.setItemQty(Integer.valueOf(qty));
				basketItem.setItemNote(userNote);
				basketItem.setDateInsert(new Date(Tools.getNow()));

				//nastavime domainId - v podstate mozeme vlozit cokolvek, setter je preimplementovanya vzdy sa vlozi aktualna domena.
				basketItem.setDomainId(CloudToolsForCore.getDomainId());

				basketItem.setItemsBasketInvoice( EshopService.getInstance().getInvoiceById(invoiceId) );

				//uloz aj nazov a PN, lebo sa moze stat, ze dokument sa vymaze
				basketItem.setItemTitle(doc.getTitle());
				if(request.getAttribute("itemTitle")!= null)
					basketItem.setItemTitle((String)request.getAttribute("docTitle"));
				basketItem.setItemPartNo(doc.getFieldA());
				if(request.getAttribute("itemPartNo")!= null)
					basketItem.setItemPartNo((String)request.getAttribute("itemPartNo"));
			}

			saveBasketItem(basketItem);
			ok = true;
		}

		return(ok);
	}

	/** Ak je dost produktov na sklade tak vrati true (mozeme vlozit do kosiku)
	 *
	 * @param req - HttpServletRequest
	 * @return
	 * @author		$Author: $(prau)
	 * @Ticket 		Number: #16989 WJ Cloud - Rozšírenie košíka o počet položiek na sklade
	 */
	public boolean canAddItem(HttpServletRequest req)
	{
		int qty = Tools.getIntValue(req.getParameter(BASKET_QTY), 1);
		String userNote = req.getParameter("basketUserNote");
		int itemId = Tools.getIntValue(Tools.getIntValue(req.getParameter(BASKET_ITEM_ID), -1), -1);
		if (itemId == -1)
			return(false);

		BasketInvoiceItemEntity basketItem = null;
		basketItem = getBasketItemBean(basketItem, req, qty, userNote);

		boolean result = canAddItem(DocDB.getInstance().getDoc(itemId), basketItem, Tools.getIntValue(req.getParameter(BASKET_QTY), 1));
		return result;
	}

	/**Vrati aktualne moznosti dopravy. Ak doprava neexistuje, vrati prazdny list
	 *
	 * MBO: moze sa poslat parameter lng, vtedy hlada [nazov][lng], ak nenajde, hlada [nazov]
	 *
	 * @return
	 * @author		$Author: $(prau)
	 * @Ticket 		Number: #15137
	 */
	public List<DocDetails> getModeOfTransports(String... lng)
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
	public List<DocDetails> getModeOfTransports(HttpServletRequest request, String... lng)
	{
		List<DocDetails> modeOfTransports = new ArrayList<>();
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
			List <BasketInvoiceItemEntity> basketItems = getBasketItems(request);
			boolean foundInStorePickup = false;
			if(basketItems != null && basketItems.size() > 0)
			{
				//zistim, ci je nieco zadane v danom poli. Ak je prazdne, beriem, ze je oznacene len pre odobny odber
				for(BasketInvoiceItemEntity item : basketItems)
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


	/******************* STATIC METHODS **************************/


	public static BigDecimal getTotalLocalPrice(List<BasketInvoiceItemEntity> items, HttpServletRequest request) {
		return items.stream()
				.map(item -> item.getItemLocalPriceQty(request))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public static int getTotalItems(List<BasketInvoiceItemEntity> items) {
		return items.stream()
				.mapToInt(BasketInvoiceItemEntity::getItemQty)
				.sum();
	}

	public static BigDecimal getTotalLocalPriceVat(List<BasketInvoiceItemEntity> items, HttpServletRequest request) {
		return items.stream()
				.map(item -> item.getItemLocalPriceVatQty(request))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public static long getBrowserId(HttpServletRequest request) {
		long browserId = 0;
		String cValue = (String)request.getSession().getAttribute(BROWSER_ID_SESSION_KEY);

		if (Tools.isEmpty(cValue)) {
			browserId = PkeyGenerator.getNextValue("basket_browser_id");
			request.getSession().setAttribute(BROWSER_ID_SESSION_KEY, Long.toString(browserId));
		} else {
			browserId = Integer.parseInt(cValue);
		}

		return browserId;
	}

	/**
     * Funkcia vrati z requestu zobrazovanu menu, ak sa v requeste nenachadza, vrati default z Constants.getString("basketDisplayCurrency")
     * v pripade cloudu kontroluje nastavenia root grupy fieldC az potom berie basketDisplayCurrency
     * @param request
     * @return
     */
    public static String getDisplayCurrency(HttpServletRequest request)
    {
        String curr = "";
        if("cloud".equals(Constants.getInstallName()))
        {
            int rootGroupId = CloudToolsForCore.getRootGroupId(request);
            GroupDetails rootGroup = GroupsDB.getInstance().getGroup(rootGroupId);

            if (rootGroup != null && Tools.isNotEmpty(rootGroup.getFieldC()))
            {
                curr = CloudToolsForCore.getValue(rootGroup.getFieldC(), "curr");
            }
            if (Tools.isEmpty(curr)) curr = Constants.getString("basketDisplayCurrency");
        }
        else
        {

            String reqCurr = (String)request.getAttribute("displayCurrency");
            if (Tools.isNotEmpty(reqCurr))
                return reqCurr;

            curr = Constants.getString("basketDisplayCurrency");
        }
        return curr;
    }

	/** Ak je dost produktov na sklade tak vrati true (mozeme vlozit do kosiku)
	 *
	 * @param doc
	 * @param basketInvoiceItemEntity
	 * @param newQty - pocet produktov ktore pridavame
	 * @return
	 * @author		$Author: $(prau)
	 * @Ticket 		Number: #16989 WJ Cloud - Rozšírenie košíka o počet položiek na sklade
	 */
	public static boolean canAddItem(DocDetails doc, BasketInvoiceItemEntity basketInvoiceItemEntity, int newQty)
	{
		Logger.debug(EshopService.class, "doc: "+doc+" basketInvoiceItemEntity: "+basketInvoiceItemEntity  +" newQty: "+newQty);
		if(doc == null)
		{
			return false;
		}

		int oldQty = 0;
		if(basketInvoiceItemEntity != null)
			oldQty = Tools.getIntValue(basketInvoiceItemEntity.getItemQty());

		//kvoli moznostiam dopravy ktore su v /system adresari
		if (doc.getDocLink().toLowerCase().startsWith("/system"))
		{
			return true;
		}

		//ak tam nie je ziadne cislo pocitanie kusov na sklade neriesime
		if (Tools.isEmpty(doc.getFieldB()) || Tools.getIntValue(doc.getFieldB(),-9999)==-9999) return true;

		return Tools.getIntValue(doc.getFieldB(),0) >= (newQty+oldQty);
	}
}