package sk.iway.iwcm.components.basket.rest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.BasketTools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.WriteTagToolsForCore;
import sk.iway.iwcm.components.basket.delivery_methods.jpa.DeliveryMethodEntity;
import sk.iway.iwcm.components.basket.delivery_methods.rest.DeliveryMethodsService;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicesRepository;
import sk.iway.iwcm.components.basket.jpa.InvoiceStatus;
import sk.iway.iwcm.components.basket.payment_methods.rest.PaymentMethodsService;
import sk.iway.iwcm.components.basket.support.MethodDto;

import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.ShowDoc;
import sk.iway.iwcm.helpers.BeanDiff;
import sk.iway.iwcm.helpers.BeanDiffPrinter;
import sk.iway.iwcm.i18n.Prop;

@Service
public class EshopService {

	private static final String BROWSER_ID_SESSION_KEY = "BasketDB.browserIdSession";
	private static final String BASKET_ITEM_ID = "basketItemId";
	private static final String BASKET_QTY = "basketQty";
	private static final String PRICE_OVERRIDES = "basket.priceOverrides";

	private final BasketInvoicesRepository bir;
    private final BasketInvoiceItemsRepository biir;
	private final BasketInvoicePaymentsRepository bipr;

	private final DeliveryMethodsService dms;

	public EshopService(BasketInvoicesRepository bir, BasketInvoiceItemsRepository biir, BasketInvoicePaymentsRepository bipr, DeliveryMethodsService dms) {
		this.bir = bir;
		this.biir = biir;
		this.bipr = bipr;
		this.dms = dms;
	}

	/******************* INSTANCE METHOD **************************/


	public static EshopService getInstance() {
		jakarta.servlet.ServletContext servletContext = Constants.getServletContext();
		EshopService service = (EshopService) servletContext.getAttribute(EshopService.class.getName());
		if(service == null) {
			service = new EshopService(
				Tools.getSpringBean("basketInvoicesRepository", BasketInvoicesRepository.class),
				Tools.getSpringBean("basketInvoiceItemsRepository", BasketInvoiceItemsRepository.class),
				Tools.getSpringBean("basketInvoicePaymentsRepository", BasketInvoicePaymentsRepository.class),
				Tools.getSpringBean("deliveryMethodsService", DeliveryMethodsService.class)
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
		List<BasketInvoiceItemEntity> originals = biir.findAllByBrowserIdAndItemsBasketInvoiceNullAndDomainId(getBrowserId(request), CloudToolsForCore.getDomainId());
		if (!BasketPricingService.isEnabled()) return originals;
		String currency = getDisplayCurrency(request);
		int scale = BasketPricingService.createPriceFormat().getMaximumFractionDigits();
		List<BasketInvoiceItemEntity> items = new ArrayList<>();
		for (BasketInvoiceItemEntity original : originals) {
			BasketInvoiceItemEntity item = new BasketInvoiceItemEntity();
			BeanUtils.copyProperties(original, item, "doc", "editorFields", "itemsBasketInvoice");
			if (original.getItemIdInt() > 0) {
				DocDetails doc = original.getDoc();
				if (doc == null) throw new IllegalStateException("A basket product is no longer available.");
				item.setDoc(doc);
				item.setItemTitle(doc.getTitle());
				PriceOverride override = getPriceOverrides(request).get(original.getId());
				BigDecimal net = override != null && override.net() != null ? override.net() : doc.getPrice(request);
				BigDecimal vat = override != null && override.vat() != null ? override.vat() : doc.getVat();
				item.setItemPrice(BasketTools.convertCurrency(net, doc.getCurrency(), currency));
				item.setItemVat(vat.intValueExact());
			}
			BasketPricingService.recalculateLinePrice(item, scale);
			items.add(item);
		}
		BasketPricingService.allocateVat(items);
		return items;
	}

	private record PriceOverride(BigDecimal net, BigDecimal vat) implements Serializable { }

	@SuppressWarnings("unchecked")
	private Map<Long, PriceOverride> getPriceOverrides(HttpServletRequest request) {
		Object stored = request.getSession().getAttribute(PRICE_OVERRIDES);
		return stored instanceof Map ? (Map<Long, PriceOverride>) stored : Map.of();
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

    public BasketInvoiceEntity saveOrder(HttpServletRequest request) {
		if (!BasketPricingService.isEnabled()) return createOrder(request);
		synchronized (request.getSession()) {
			try {
				TransactionTemplate transaction = new TransactionTemplate(Tools.getSpringBean("webjet2022TransactionManager", PlatformTransactionManager.class));
				BasketInvoiceEntity invoice = transaction.execute(status -> createOrder(request));
				if (invoice != null) {
					request.getSession().removeAttribute(BROWSER_ID_SESSION_KEY);
					request.getSession().removeAttribute(PRICE_OVERRIDES);
				}
				return invoice;
			} catch (Exception ex) {
				Logger.error(EshopService.class, ex);
				return null;
			}
		}
	}

    private BasketInvoiceEntity createOrder(HttpServletRequest request)
	{
		BasketInvoiceEntity invoice = null;

		try {
			invoice = new BasketInvoiceEntity();

			BeanWrapperImpl wrapper = new BeanWrapperImpl(invoice);
			Map<String, String[]> fields = new HashMap<>(request.getParameterMap());
			if (BasketPricingService.isEnabled()) fields.keySet().removeIf(key ->
				!key.startsWith("contact") && !key.startsWith("delivery") && !"userNote".equals(key) && !"paymentMethod".equals(key));
			wrapper.setPropertyValues(new MutablePropertyValues(fields), true, true);
			invoice.setPriceToPayVat(null);

			long browserId = EshopService.getBrowserId(request);
			int userId = Tools.getUserId(request);

			invoice.setBrowserId(Long.valueOf(browserId));
			invoice.setLoggedUserId(userId);
			invoice.setCreateDate(new Date(Tools.getNow()));
			invoice.setStatusId(InvoiceStatus.INVOICE_STATUS_NEW.getValue());
			invoice.setCurrency(BasketTools.getSystemCurrency());
			invoice.setUserLng(PageLng.getUserLng(request));

			int deliveryMethodId = Tools.getIntValue(request.getParameter("deliveryMethod"),-1);
			String country = Tools.isEmpty(request.getParameter("deliveryCountry")) ? request.getParameter("contactCountry") : request.getParameter("deliveryCountry");
			if(deliveryMethodId > 0) {
				try {
					DeliveryMethodEntity dme = dms.getDeliveryMethod(deliveryMethodId, null, Prop.getInstance(request));
					if (BasketPricingService.isEnabled() && !dme.getSupportedCountriesList().contains(country))
						throw new IllegalArgumentException("Delivery is unavailable.");
					String title = dme.getTitle();
					if (Tools.isEmpty(title)) title = dme.getDeliveryMethodName();
					invoice.setDeliveryMethod(title);
					if (BasketPricingService.isEnabled()) saveDeliveryMethod(request, dme, Prop.getInstance(request));
				} catch(Exception e) {
					if (BasketPricingService.isEnabled()) throw e;
					Logger.error(e);
				}
			}

			if (BasketPricingService.isEnabled()) {
				if (!List.of("eur", "czk").contains(invoice.getCurrency())) throw new IllegalStateException("Rounded checkout supports EUR and CZK.");
				if (deliveryMethodId <= 0 && !dms.getAllDeliveryMethods(request, Prop.getInstance(request), country).isEmpty())
					throw new IllegalArgumentException("Select a delivery method.");
				if (!PaymentMethodsService.isPaymentMethodConfigured(invoice.getPaymentMethod(), request, Prop.getInstance(request)))
					throw new IllegalArgumentException("Select a payment method.");
			}

			//Get all items for adminlog
			List<BasketInvoiceItemEntity> basketItems = new ArrayList<>();

			//Add payment as basket item, if created add it to list
			int newItemId = PaymentMethodsService.createPaymentInvoiceItemAndReturnId(invoice, request, biir);
			if (BasketPricingService.isEnabled() && newItemId < 0) throw new IllegalStateException("Payment fee could not be resolved.");
			if(newItemId != -1) {
				BasketInvoiceItemEntity newBasketItem = getBasketItemById(newItemId);
				basketItems.add( newBasketItem );
			}

			if (BasketPricingService.isEnabled()) {
				basketItems = getBasketItems(request);
				if (basketItems.stream().noneMatch(item -> item.getItemIdInt() > 0)) throw new IllegalStateException("The basket is empty.");
				for (BasketInvoiceItemEntity item : basketItems) BasketPricingService.prepareForSave(item);
				invoice.setPriceToPayVat(getTotalLocalPriceVat(basketItems, request));
				invoice.setPriceToPayNoVat(getTotalLocalPrice(basketItems, request));
				invoice.setBalanceToPay(invoice.getPriceToPayVat());
				invoice.setItemQty(basketItems.stream().mapToInt(BasketInvoiceItemEntity::getItemQty).sum());
				if (invoice.getPriceToPayVat().signum() <= 0) throw new IllegalStateException("The order total must be positive.");
			}

			//Save invoice
			invoice = saveInvoice(invoice);

			//Check and logs
			if(invoice.getBasketInvoiceId() > 1)
			{
				if (BasketPricingService.isEnabled()) {
					for (BasketInvoiceItemEntity item : basketItems) item.setInvoiceId(invoice.getId().intValue());
					biir.saveAll(basketItems);
				} else bindItemsToInvoice(invoice.getId(), invoice.getBrowserId());
				Adminlog.add(Adminlog.TYPE_BASKET_CREATE, "Vytvorena objednavka: " + StringUtils.join(basketItems.iterator(), ","), invoice.getBasketInvoiceId(), -1);
				Logger.println(EshopService.class, "Objednavka ulozena, id= " + invoice.getBasketInvoiceId());
				//zrus browserId
				if (!BasketPricingService.isEnabled()) request.getSession().removeAttribute(BROWSER_ID_SESSION_KEY);

			}
			else
			{
				Logger.error(EshopService.class, "Chyba pri ulozeni objednavky do DB invoice_id: " + invoice.getBasketInvoiceId());
				invoice = null;
			}
		}
		catch (Exception e) {
			if (BasketPricingService.isEnabled()) throw new IllegalStateException("Could not save the rounded order.", e);
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

		String data = Tools.downloadUrl(Tools.getBaseHrefLoopback(request) + compUrl + "?invoiceId=" + invoiceId + "&auth="+BasketInvoiceEntity.getAuthorizationToken(invoiceId));

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

		if(invoiceBean == null) return;

		List<BasketInvoiceItemEntity> listItmes = biir.findAllByBrowserIdAndDomainId(invoiceBean.getBrowserId(), invoiceBean.getDomainId());
		if(listItmes == null )
			return;

		for(BasketInvoiceItemEntity item: listItmes)
			decreaseProductCount(item);
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

	public boolean addDeliveryMethod(HttpServletRequest request, int deliveryMethodId, Prop prop) {
		if (BasketPricingService.isEnabled()) return deliveryMethodId > 0;
		if (deliveryMethodId == -1) return false;
		return saveDeliveryMethod(request, dms.getDeliveryMethod(deliveryMethodId, null, prop), prop);
	}

	private boolean saveDeliveryMethod(HttpServletRequest request, DeliveryMethodEntity dme, Prop prop) {
		long browserId = getBrowserId(request);
		int userId = Tools.getUserId(request);

		BasketInvoiceItemEntity basketItem = new BasketInvoiceItemEntity();
		basketItem.setBrowserId(Long.valueOf(browserId));
		basketItem.setLoggedUserId(userId);

		basketItem.setItemId(0); // We do not reference any DOC
		basketItem.setItemQty(1); // Always one delivery

		basketItem.setItemPrice( dme.getPrice() );
		basketItem.setItemVat( dme.getVat() );

		basketItem.setItemNote( prop.getText("components.basket.invoice_email.delivery_method") );
		basketItem.setDateInsert(new Date(Tools.getNow()));

		//nastavime domainId - v podstate mozeme vlozit cokolvek, setter je preimplementovany a vzdy sa vlozi aktualna domena.
		basketItem.setDomainId(CloudToolsForCore.getDomainId());

		basketItem.setItemsBasketInvoice( getInvoiceById(-1) );

		basketItem.setItemTitle( DeliveryMethodsService.getDeliveryMethodLabel(dme.getDeliveryMethodName(), dme.getTitle(), request) );
		basketItem.setItemPartNo(null);

		saveBasketItem(basketItem);

		return true;
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

				//nastavime domainId - v podstate mozeme vlozit cokolvek, setter je preimplementovany a vzdy sa vlozi aktualna domena.
				basketItem.setDomainId(CloudToolsForCore.getDomainId());

				basketItem.setItemsBasketInvoice( null );

				//uloz aj nazov a PN, lebo sa moze stat, ze dokument sa vymaze
				basketItem.setItemTitle(doc.getTitle());
				if(request.getAttribute("itemTitle")!= null)
					basketItem.setItemTitle((String)request.getAttribute("docTitle"));
				basketItem.setItemPartNo(doc.getFieldA());
				if(request.getAttribute("itemPartNo")!= null)
					basketItem.setItemPartNo((String)request.getAttribute("itemPartNo"));
			}

			saveBasketItem(basketItem);
			if (BasketPricingService.isEnabled() && (request.getAttribute("docPrice") != null || request.getAttribute("docVat") != null)) {
				Map<Long, PriceOverride> overrides = new HashMap<>(getPriceOverrides(request));
				overrides.put(basketItem.getId(), new PriceOverride(
					request.getAttribute("docPrice") == null ? null : Tools.getBigDecimalValue(request.getAttribute("docPrice"), "0"),
					request.getAttribute("docVat") == null ? null : Tools.getBigDecimalValue(request.getAttribute("docVat"), "0")));
				request.getSession().setAttribute(PRICE_OVERRIDES, overrides);
			}
			ok = true;
		}

		return(ok);
	}

	/**
	 * Checks whether the requested product quantity can be added to the basket from available stock.
	 *
	 * @param req request containing the product and requested quantity
	 * @return {@code true} if the requested quantity is available
	 * @Ticket Number: #16989 WJ Cloud - Extend the basket with product stock quantities
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

	public List<MethodDto> getModeOfTransports(HttpServletRequest request, String country) {
		return getModeOfTransports(request, country, null);
	}

	public List<MethodDto> getModeOfTransports(HttpServletRequest request, String country, String lng) {
		Prop prop;
		if(Tools.isNotEmpty(lng)) prop = Prop.getInstance(lng);
		else if(request != null) prop = Prop.getInstance(request);
		else prop = Prop.getInstance();

		return dms.getAllDeliveryMethods(request, prop, country);
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
		if (BasketPricingService.isEnabled()) return BasketTools.getSystemCurrency();
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

			if(request != null) {
				String reqCurr = (String)request.getAttribute("displayCurrency");
				if (Tools.isNotEmpty(reqCurr)) return reqCurr;
			}

            curr = Constants.getString("basketDisplayCurrency");
        }
        return curr;
    }

	/**
	 * Checks whether the requested product quantity can be added to the basket from available stock.
	 *
	 * @param doc product document containing stock information
	 * @param basketInvoiceItemEntity existing basket item, or {@code null} for a new item
	 * @param newQty quantity to add
	 * @return {@code true} if the requested quantity is available
	 * @Ticket Number: #16989 WJ Cloud - Extend the basket with product stock quantities
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
