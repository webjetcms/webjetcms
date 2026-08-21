package sk.iway.iwcm.components.basket.rest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.BasketTools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicesRepository;
import sk.iway.iwcm.components.basket.jpa.InvoiceStatus;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.editor.rest.GetAllItemsDocOptions;
import sk.iway.iwcm.editor.service.GroupsService;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.json.LabelValueInteger;
import sk.iway.tags.CurrencyTag;

/**
 * Provides product-category administration and invoice calculation helpers for the basket module.
 */
public class ProductListService {

    private static final String BASKET_ADMIN_GROUP_IDS = "basketAdminGroupIds";

    private ProductListService() { /*private constructor to hide the implicit public one*/ }

    /**
     * Represents the outcome of creating a product category.
     */
    public enum AddingStatus {
        SUCCESS, ALREADY_EXIST, ERROR
    }

    private static Specification<DocDetails> hasGroupIdIn(List<Integer> groupIds) {
        return (Root<DocDetails> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) ->
            root.get("groupId").in(groupIds);

    }

    private static Specification<DocDetails> fieldStartsWithDigit(String fieldName) {
        return (Root<DocDetails> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) -> {
            Path<String> fieldPath = root.get(fieldName);
            // Create predicates for each digit using a loop
            List<Predicate> digitPredicates = new ArrayList<>();
            for (int i = 0; i <= 9; i++) {
                digitPredicates.add(criteriaBuilder.like(fieldPath, i + "%"));
            }

            // Combine all predicates with OR
            Predicate startsWithDigit = criteriaBuilder.or(digitPredicates.toArray(new Predicate[0]));
            return startsWithDigit;
        };
    }

    /**
     * Loads products from the selected category tree and prepares prices in the requested currency.
     *
     * @param options  repositories, request data, paging, and selected group information
     * @return prepared product page for the administration DataTable
     */
    public static DatatablePageImpl<DocDetails> getAllItems(GetAllItemsDocOptions options) {
        List<Integer> groupIds = getGroupTreeIds( options.getGroupId(), options.getDocDetailsRepository() );
        //So no error will be returned about wrong sql query
        if(groupIds.isEmpty()) return new DatatablePageImpl<>(new ArrayList<>());

        String priceField = Constants.getString("basketPriceField");

        Specification<DocDetails> spec = hasGroupIdIn(groupIds).and(fieldStartsWithDigit(priceField));
        Page<DocDetails> page = options.getDocDetailsRepository().findAll(spec, options.getPageable());

        String supportedCurrency = BasketTools.getNormalizedSupportedCurrency(options.getRequest().getParameter("currency"));
        String wantedCurrency = supportedCurrency == null ? BasketTools.getSystemCurrency() : supportedCurrency;
        DatatablePageImpl<DocDetails> pageImpl = WebpagesService.preparePage(page, options);
        pageImpl.get().forEach(doc -> {
            doc.setFieldH( doc.getLocalPriceVat(options.getRequest(), wantedCurrency).toString() );
            doc.setFieldK( doc.getLocalPrice(options.getRequest(), wantedCurrency).toString() );

        });

        return pageImpl;
    }

    /**
     * Resolves product group identifiers for a selected subtree or all configured product groups.
     *
     * @param rootGroupId  root group identifier, or a value below one to use all product groups
     * @param repo  document repository used to detect configured product groups
     * @return identifiers of groups included in the product listing
     */
    public static List<Integer> getGroupTreeIds(int rootGroupId, DocDetailsRepository repo) {
        if(rootGroupId < 1) {
            //Get default option
            List<LabelValueInteger> allOptions = getListOfProductsGroups(repo);
            List<Integer> groupTreeIds = new ArrayList<>();
            for(LabelValueInteger option : allOptions) {
                groupTreeIds.add(option.getValue());
            }
            return groupTreeIds;
        }

        List<Integer> groupIds = new ArrayList<>();
        List<GroupDetails> groupsTree = GroupsDB.getInstance().getGroupsTree(rootGroupId, true, true);
        for(GroupDetails group : groupsTree) { groupIds.add(group.getGroupId()); }
        return groupIds;
    }

    /**
     * Creates a product category and its default category page for an authorized user.
     *
     * @param currentUser  user requesting category creation
     * @param customData  JSON containing the parent group identifier and new group name
     * @param editorFacade  editor facade used to create the category page
     * @return status describing whether the category was created, already existed, or failed validation
     */
    public static AddingStatus addProductGroup(Identity currentUser, String customData, EditorFacade editorFacade) {
        if(!currentUser.isEnabledItem("cmp_basket")) return AddingStatus.ERROR;

        int groupId = -1;
        String newGroupName = "";
        if(customData != null && !customData.isEmpty()) {
            try {
                JSONObject jsonObject = new JSONObject(customData);
                groupId = Tools.getIntValue((String) jsonObject.get("groupId"), -1);
                newGroupName = (String) jsonObject.get("newGroupName");
            } catch (Exception ex) {
                sk.iway.iwcm.Logger.error(ex);
                return AddingStatus.ERROR;
            }
        }

        if(Tools.isEmpty(newGroupName) || groupId < 1) return AddingStatus.ERROR;

        //Check if this product category (for this parent) already exist - case insensitive
        GroupsDB groupsDB = GroupsDB.getInstance();
        List<GroupDetails> childGroups = groupsDB.getGroups(groupId);
        for(GroupDetails childGroup : childGroups)
            if(childGroup.getGroupName().equalsIgnoreCase(newGroupName)) return AddingStatus.ALREADY_EXIST;

        //All is ok, create group
        GroupDetails newGroup = createGroup(newGroupName, currentUser.getLogin(), GroupsDB.getInstance().getGroup(groupId));
        if (newGroup != null) {
            createGroupNewsDoc(editorFacade, newGroup.getGroupId(), newGroup.getGroupName(), currentUser.getUserId());
            return AddingStatus.SUCCESS;
        }
        return AddingStatus.ERROR;
    }

    /**
     * Creates a product group inheriting its template and domain from the parent group.
     *
     * @param groupName  name of the new group
     * @param userLogin  current user login used by the group lookup
     * @param parentGroup  parent group for the new category
     * @return created group, or {@code null} when the group already exists
     */
    private static GroupDetails createGroup(String groupName, String userLogin, GroupDetails parentGroup) {
        //Check if group already exist
        GroupDetails newGroup = GroupsDB.getInstance().getGroup(userLogin, parentGroup.getGroupId());
        if(newGroup != null) return null;

        //Create new root group
        newGroup = new GroupDetails();
        newGroup.setParentGroupId( parentGroup.getGroupId() );
        newGroup.setGroupName( groupName );
        newGroup.setTempId( parentGroup.getTempId() );
        newGroup.setDomainName( parentGroup.getDomainName() );
        GroupsDB.getInstance().setGroup(newGroup);
        return newGroup;
    }

    /**
     * Creates the default product-list page for a newly added category.
     *
     * @param editorFacade  editor facade used to persist the page
     * @param rootGroupId  identifier of the new category group
     * @param docTitle  title and navigation label of the page
     * @param authorId  identifier of the page author
     */
    private static void createGroupNewsDoc(EditorFacade editorFacade, int rootGroupId, String docTitle, int authorId) {
        DocDetails groupDoc = editorFacade.getDocForEditor(-1, -1, rootGroupId);

        //Set doc
        groupDoc.setTitle(docTitle);
        groupDoc.setNavbar(docTitle);
        groupDoc.setData("");
		groupDoc.setAuthorId(authorId);
		groupDoc.setAvailable(true);
		groupDoc.setSearchable(true);
		groupDoc.setCacheable(false);
        groupDoc.setPerexGroup(new String[]{"5"}); //5 - kategoria
        groupDoc.setData(Constants.getString("basketNewCategoryHtmlCode"));

        //Save doc
        editorFacade.save(groupDoc);
    }

    /**
     * Returns product groups from explicit configuration or automatic include detection.
     *
     * @param docDetailsRepository  repository used to detect pages containing product-list applications
     * @return configured product groups sorted as a tree
     */
    public static List<LabelValueInteger> getListOfProductsGroups(DocDetailsRepository docDetailsRepository) {
        String constantIds = Constants.getString(BASKET_ADMIN_GROUP_IDS);
        if(Tools.isEmpty(constantIds)) {
            return getListOfProductGroupsViaInclude(docDetailsRepository);
        } else {
            return getListOfProductGroupsViaIds(constantIds);
        }
    }

    /**
     * Finds product groups from pages containing a basket product-list application.
     *
     * Detected groups are expanded to their complete subtrees and sorted for selection.
     *
     * @param docDetailsRepository  repository used to search page content
     * @return detected product groups sorted as a tree
     */
    public static List<LabelValueInteger> getListOfProductGroupsViaInclude(DocDetailsRepository docDetailsRepository) {
        GroupsDB groupsDB = GroupsDB.getInstance();

        List<Integer> groupIds = docDetailsRepository.getDistGroupIdsByDataLike("%!INCLUDE(/components/eshop/%", "%!INCLUDE(/components/basket/%", "%product-list.jsp%", "%products.jsp%");

        if(groupIds == null || groupIds.isEmpty()) return new ArrayList<>();

        //Get groups by ids
        List<GroupDetails> groups = new ArrayList<>();
        for(Integer groupId : groupIds) groups.add(groupsDB.getGroup(groupId));

        //Filter groups out
        groups = filterProductGroups(groups);

        //
        Set<Integer> loadedIds = new HashSet<>();
        List<GroupDetails> expandedGroups = new ArrayList<>();
        for(GroupDetails group : groups) {
            for(GroupDetails treeGroup : groupsDB.getGroupsTree(group.getGroupId(), true, true)) {
                if(loadedIds.contains(treeGroup.getGroupId())) continue;

                expandedGroups.add(treeGroup);
                loadedIds.add(treeGroup.getGroupId());
            }
        }

        //Sort it, then push it into map
        List<LabelValueInteger> groupsList = new ArrayList<>();
        for(GroupDetails group : GroupsService.sortItIntoTree(expandedGroups)) groupsList.add( new LabelValueInteger(group.getFullPath(), group.getGroupId()) );
        return groupsList;
    }

    /**
     * Parses configured product group identifiers and validates them against the group cache.
     *
     * An identifier followed by {@code *} includes the complete subtree of that group.
     *
     * @param idsString  comma- or plus-separated group identifiers
     * @return valid configured product groups sorted as a tree
     */
    public static List<LabelValueInteger> getListOfProductGroupsViaIds(String idsString) {
        GroupsDB groupsDB = GroupsDB.getInstance();
        Set<Integer> loadWithSubfolders = new HashSet<>();
        Set<Integer> loadedIds = new HashSet<>();

        //Get parsed ids
        idsString = idsString.trim();
        String[] strIds = idsString.split("[,+]");

        List<GroupDetails> groups = new ArrayList<>();
        for(String strId : strIds) {
            //Remove "*" from end if needed
            boolean withSubfolders = false;
            if(strId.endsWith("*")) {
                withSubfolders = true;
                strId = strId.substring(0, strId.length() - 1);
            }

            //Parse string to int and check if group do exist
            int groupId = Tools.getIntValue(strId, -1);
            GroupDetails group = groupsDB.getGroup(groupId);
            if(group != null) {
                //Group do exist ...
                groups.add(group);
                if(withSubfolders) loadWithSubfolders.add(groupId);
            }
        }

        //Filter groups out
        groups = filterProductGroups(groups);

        //
        List<GroupDetails> expandedGroups = new ArrayList<>();
        for(GroupDetails group : groups) {
            int groupId = group.getGroupId();

            if(loadedIds.contains(groupId)) continue;

            if(loadWithSubfolders.contains(groupId) == false) {
                // Add without child tree
                expandedGroups.add(group);
                loadedIds.add(groupId);
            } else {
                for(GroupDetails treeGroup : groupsDB.getGroupsTree(groupId, true, true)) {
                    expandedGroups.add(treeGroup);
                    loadedIds.add(treeGroup.getGroupId());
                }
            }
        }

        //Sort it, then push it into map
        List<LabelValueInteger> groupsList = new ArrayList<>();
        for(GroupDetails group : GroupsService.sortItIntoTree(expandedGroups)) groupsList.add( new LabelValueInteger(group.getFullPath(), group.getGroupId()) );
        return groupsList;
    }

    /**
     * Removes missing, deleted, and foreign-domain groups from product category candidates.
     *
     * @param groups  candidate product groups
     * @return groups available in the current domain and outside the trash
     */
    private static List<GroupDetails> filterProductGroups(List<GroupDetails> groups) {
        List<GroupDetails> correctGroups = new ArrayList<>();

        GroupsDB groupsDB = GroupsDB.getInstance();
        String domainName = CloudToolsForCore.getDomainName();

        GroupDetails trashGroup = groupsDB.getTrashGroup();
        String trashGroupPath = null;
        if (trashGroup != null) {
            trashGroupPath = trashGroup.getFullPath();
        }

        for(GroupDetails group : groups) {
            if (group == null) continue;
            //remove groups in trash
            if (trashGroupPath != null && group.getFullPath().startsWith(trashGroupPath)) continue;
            //Only current domain and not deleted groups
            if(Tools.isNotEmpty(group.getDomainName()) && domainName.equals(group.getDomainName())==false) continue;
            //
            correctGroups.add(group);
        }

        return correctGroups;
    }

    public static void updateInvoiceStats(Long invoiceId, boolean updateStatus) {
        updateInvoiceStats(invoiceId, null, updateStatus);
    }

    /**
     * Recalculates invoice item count, totals, balance, and optionally payment status.
     *
     * @param invoiceId  invoice whose totals are updated
     * @param browserId  browser identifier used to select items, or {@code null} to use the invoice identifier
     * @param updateStatus  whether to derive the invoice status from invoice and payment totals
     */
    public static void updateInvoiceStats(Long invoiceId, Long browserId, boolean updateStatus) {
        if(invoiceId < 1) return;

        //Get repositories
        BasketInvoicesRepository bir = Tools.getSpringBean("basketInvoicesRepository", BasketInvoicesRepository.class);
        BasketInvoiceItemsRepository biir = Tools.getSpringBean("basketInvoiceItemsRepository", BasketInvoiceItemsRepository.class);
        BasketInvoicePaymentsRepository bipr = Tools.getSpringBean("basketInvoicePaymentsRepository", BasketInvoicePaymentsRepository.class);

        Integer domainId = CloudToolsForCore.getDomainId();
        BasketInvoiceEntity invoice = bir.findFirstByIdAndDomainId(invoiceId, domainId).orElse(null);
        if(invoice == null) return;

        //Get invoice items
        List<BasketInvoiceItemEntity> invoiceItems;
        if(browserId != null && browserId > 0)
            invoiceItems = biir.findAllByBrowserIdAndDomainId(browserId, domainId);
        else
            invoiceItems = biir.findAllByInvoiceIdAndDomainId(invoiceId, domainId);

        Integer itemsCount = 0;
        BigDecimal priceToPayNoVat = BigDecimal.ZERO; //NO VAT
        BigDecimal priceToPayVat = BigDecimal.ZERO; //WITH VAT

        for(BasketInvoiceItemEntity item : invoiceItems) {
            itemsCount += item.getItemQty();
            priceToPayNoVat = priceToPayNoVat.add( item.getItemPriceQty() );
            priceToPayVat = priceToPayVat.add( item.getItemPriceVatQty() );
        }

        //Set and save invoice
        invoice.setItemQty(itemsCount);
        invoice.setPriceToPayNoVat(priceToPayNoVat);
        invoice.setPriceToPayVat(priceToPayVat);
        invoice.setBalanceToPay( priceToPayVat.subtract( ProductListService.getPayedPrice(invoice.getId(), bipr) ) );

        if(updateStatus) {
            //SAME time, it can change the status of invoice
            BigDecimal totalPayedPrice = getPayedPrice(invoice.getId(), bipr);
            invoice.setStatusId( ProductListService.getInvoiceStatusByValues(priceToPayVat, totalPayedPrice) );
        }

        bir.save(invoice);
    }

    /**
     * Calculates the VAT-inclusive total price of all items in an invoice.
     *
     * @param invoiceId  invoice identifier
     * @param biir  invoice item repository
     * @return total price, zero for an empty invoice, or {@code -1} for an invalid identifier
     */
    public static BigDecimal getPriceToPay(Long invoiceId, BasketInvoiceItemsRepository biir) {
        if(invoiceId == null || invoiceId < 0) return new BigDecimal(-1);

        List<BasketInvoiceItemEntity> invoiceItems = biir.findAllByInvoiceIdAndDomainId(invoiceId, CloudToolsForCore.getDomainId());
        if(invoiceItems == null || invoiceItems.isEmpty()) return BigDecimal.ZERO;

        return invoiceItems.stream()
                           .map(item -> item.getItemPriceVatQty())
                           .reduce(BigDecimal.ZERO, BigDecimal::add)
                           .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the total amount of confirmed payments for an invoice.
     *
     * @param invoiceId  invoice identifier
     * @param bipr  invoice payment repository
     * @return confirmed payment total, zero when no payments exist, or {@code -1} for a null identifier
     */
    public static BigDecimal getPayedPrice(Long invoiceId, BasketInvoicePaymentsRepository bipr) {
        if(invoiceId == null) return new BigDecimal(-1);

        //ONLY confirmed payments
        List<BasketInvoicePaymentEntity> invoicePayments = bipr.findAllByInvoiceIdAndConfirmedTrue(invoiceId);
        if(invoicePayments == null || invoicePayments.isEmpty()) return BigDecimal.ZERO;
        return invoicePayments.stream()
                              .map(item -> item.getPayedPrice())
                              .reduce(BigDecimal.ZERO, BigDecimal::add)
                              .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Returns invoice totals and the status derived from its confirmed payments.
     *
     * @param invoiceId  invoice identifier
     * @param biir  invoice item repository
     * @param bipr  invoice payment repository
     * @return string values for price to pay, paid price, and derived status
     */
    public static Map<String, String> getPriceInfo(Long invoiceId, BasketInvoiceItemsRepository biir, BasketInvoicePaymentsRepository bipr) {
        BigDecimal priceToPay = getPriceToPay(invoiceId, biir);
        BigDecimal payedPrice = getPayedPrice(invoiceId, bipr);
        int status = getInvoiceStatusByValues(priceToPay, payedPrice);
        return Map.of(
            "priceToPay", priceToPay.toString(),
            "payedPrice", payedPrice.toString(),
            "status", String.valueOf(status)
        );
        }

    /**
     * Derives an invoice status by comparing its total price with received payments.
     *
     * @param priceToPayVat  VAT-inclusive invoice total
     * @param totalPayedPrice  total confirmed payments
     * @return paid, partially paid, or new invoice status identifier
     */
    public static final Integer getInvoiceStatusByValues(BigDecimal priceToPayVat, BigDecimal totalPayedPrice) {
        if(CurrencyTag.formatNumber(priceToPayVat).equals(CurrencyTag.formatNumber(totalPayedPrice)))
            return InvoiceStatus.INVOICE_STATUS_PAID.getValue();
        else if(totalPayedPrice.compareTo(BigDecimal.valueOf(0)) > 0)
            return InvoiceStatus.INVOICE_STATUS_PARTIALLY_PAID.getValue();
        else
            return InvoiceStatus.INVOICE_STATUS_NEW.getValue();
    }

    /**
     * Adds or increments selected products in an existing invoice.
     *
     * @param invoiceId  target invoice identifier
     * @param itemIdsToAdd  product document identifiers to add
     * @param biir  invoice item repository
     * @param userId  identifier of the user modifying the invoice
     * @param request  current request used to resolve product prices
     */
    public static void addItemToInvoice(Long invoiceId, List<Integer> itemIdsToAdd, BasketInvoiceItemsRepository biir, int userId, HttpServletRequest request) {
		DocDB docDB = DocDB.getInstance();
		int domainId = CloudToolsForCore.getDomainId();

		Long browserId = biir.getBrowserIdByInvoiceId(invoiceId, domainId).orElse(null);
		if(browserId == null) browserId = Long.valueOf( PkeyGenerator.getNextValue("basket_browser_id") );

		for(Integer itemId : itemIdsToAdd) {
			DocDetails itemDoc = docDB.getDoc(itemId);

			BasketInvoiceItemEntity item = biir.findByInvoiceIdAndItemIdAndDomainId(invoiceId, Long.valueOf(itemId), domainId).orElse(null);

			if(EshopService.canAddItem(itemDoc, item, 1)) {

				if(item != null) {
					item.setItemQty(item.getItemQty() + 1);
					biir.save(item);
				} else {
					//Prepare new item
					BasketInvoiceItemEntity newItem = new BasketInvoiceItemEntity();

					newItem.setBrowserId(browserId);
					newItem.setLoggedUserId(userId);
					newItem.setItemId(itemId);
					newItem.setItemTitle( itemDoc.getTitle() );
					newItem.setItemPartNo( itemDoc.getFieldA() );
					newItem.setItemPrice( itemDoc.getPrice(request) );
					newItem.setItemVat( itemDoc.getVat().intValue() );
					newItem.setItemQty(1);
					newItem.setDateInsert(new Date(Tools.getNow()));
					newItem.setInvoiceId(invoiceId.intValue());
					newItem.setDomainId(domainId);

					biir.save(newItem);
				}

			}
		}
	}
}
