package sk.iway.iwcm.components.basket.rest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.json.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicesRepository;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.editor.rest.GetAllItemsDocOptions;
import sk.iway.iwcm.editor.service.GroupsService;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.system.datatable.json.LabelValueInteger;

public class ProductListService {

    private ProductListService() { /*private constructor to hide the implicit public one*/ }

    public enum AddingStatus {
        SUCCESS, ALREADY_EXIST, ERROR
    }

    private static Specification<DocDetails> hasGroupIdIn(List<Integer> groupIds) {
        return (Root<DocDetails> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) -> {
            return root.get("groupId").in(groupIds);
        };
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

    public static DatatablePageImpl<DocDetails> getAllItems(GetAllItemsDocOptions options) {
        List<Integer> groupIds = getGroupTreeIds( options.getGroupId(), options.getDocDetailsRepository() );
        //So no error will be returned about wrong sql query
        if(groupIds.isEmpty()) return new DatatablePageImpl<>(new ArrayList<>());

        String priceField = Constants.getString("basketPriceField");

        Specification<DocDetails> spec = Specification.where(hasGroupIdIn(groupIds))
                                                      .and(fieldStartsWithDigit(priceField));
        Page<DocDetails> page = options.getDocDetailsRepository().findAll(spec, options.getPageable());

        String currentCurrency =  Constants.getString("basketProductCurrency");

        String wantedCurrency;
        //Safety check
        if(isCurrencySupported(options.getRequest().getParameter("currency")) == false) wantedCurrency = currentCurrency;
        else wantedCurrency = options.getRequest().getParameter("currency");

        DatatablePageImpl<DocDetails> pageImpl = WebpagesService.preparePage(page, options);
        pageImpl.get().forEach(doc -> {
            //Set currency
            doc.setFieldI(currentCurrency);

            if(currentCurrency.equals(wantedCurrency)) {
                doc.setFieldH( doc.getPriceVat().toString() );
                doc.setFieldK( doc.getPrice().toString() );
            } else {
                doc.setFieldH( doc.getLocalPriceVat(options.getRequest(), wantedCurrency).toString() );
                doc.setFieldK( doc.getLocalPrice(options.getRequest(), wantedCurrency).toString() );
            }
        });

        return pageImpl;
    }

    public static List<Integer> getGroupTreeIds(int rootGroupId, DocDetailsRepository repo) {
        if(rootGroupId < 1) {
            //Get default option
            List<LabelValueInteger> allOptions = getListOfProductsGroups(repo);
            if(allOptions.isEmpty()) return new ArrayList<>();

            rootGroupId = allOptions.get(0).getValue();
        }

        List<Integer> groupIds = new ArrayList<>();
        List<GroupDetails> groupsTree = GroupsDB.getInstance().getGroupsTree(rootGroupId, true, true);
        for(GroupDetails group : groupsTree) { groupIds.add(group.getGroupId()); }
        return groupIds;
    }

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
        groupDoc.setData(
            "<section>\r\n" + //
            "!INCLUDE(/components/eshop/shop/modules/md-category-header.jsp)!\r\n" + //
            "\r\n" + //
            "!INCLUDE(/components/eshop/shop/modules/md-subcategory-selector.jsp)!\r\n" + //
            "</section>\r\n" + //
            "\r\n" + //
            "!INCLUDE(/components/eshop/shop/modules/md-product-list.jsp)!"
        );

        //Save doc
        editorFacade.save(groupDoc);
    }

    public static List<LabelValueInteger> getListOfProductsGroups(DocDetailsRepository docDetailsRepository) {
        List<LabelValueInteger> groupsList = new ArrayList<>();
        List<GroupDetails> correctGroups = new ArrayList<>();

        List<Integer> groupIds = docDetailsRepository.getDistGroupIdsByDataLike("%!INCLUDE(/components/eshop/%", "%!INCLUDE(/components/basket/%", "%product-list.jsp%", "%products.jsp%");
        if(groupIds == null || groupIds.isEmpty()) return groupsList;

        GroupsDB groupsDB = GroupsDB.getInstance();
        String domainName = CloudToolsForCore.getDomainName();

        GroupDetails trashGroup = groupsDB.getTrashGroup();
        String trashGroupPath = null;
        if (trashGroup != null) {
            trashGroupPath = trashGroup.getFullPath();
        }

        for(Integer groupId : groupIds) {
            GroupDetails group = groupsDB.getGroup(groupId);
            if (group == null) continue;
            //remove groups in trash
            if (trashGroupPath != null && group.getFullPath().startsWith(trashGroupPath)) continue;
            //Only current domain and not deleted groups
            if(Tools.isNotEmpty(group.getDomainName()) && domainName.equals(group.getDomainName())==false) continue;

            correctGroups.add(group);
        }

        if(correctGroups.isEmpty()) return groupsList;

        //Sort it, then push it into map
        for(GroupDetails group : GroupsService.sortItIntoTree(correctGroups)) groupsList.add( new LabelValueInteger(group.getFullPath(), group.getGroupId()) );

        return groupsList;
    }

    public static boolean isCurrencySupported(String currency) {
        if(Tools.isEmpty(currency) == true) return false;
        List<String> supportedCurrencies = Arrays.asList( Constants.getString("supportedCurrencies").split(",") );
        return supportedCurrencies.contains(currency);
    }

    public static List<LabelValue> getListOfSupportedCurrencies() {
        List<String> supportedCurrencies = Arrays.asList( Constants.getString("supportedCurrencies").split(",") );
        Collections.sort(supportedCurrencies);

        List<LabelValue> groupsList = new ArrayList<>();
        for (String curr: supportedCurrencies)
            groupsList.add( new LabelValue(curr, curr) );

        return groupsList;
    }

    /**
     * Update invoice stats (items count, total price, total price with VAT)
     * @param invoiceId
     */
    public static void updateInvoiceStats(int invoiceId) {
        if(invoiceId < 1) return;

        //Get repositories
        BasketInvoicesRepository bir = Tools.getSpringBean("basketInvoicesRepository", BasketInvoicesRepository.class);
        BasketInvoiceItemsRepository biir = Tools.getSpringBean("basketInvoiceItemsRepository", BasketInvoiceItemsRepository.class);

        Integer domainId = CloudToolsForCore.getDomainId();
        BasketInvoiceEntity invoice = bir.findFirstByIdAndDomainId(Long.valueOf(invoiceId), domainId).orElse(null);
        if(invoice == null) return;

        //Get invoice items
        List<BasketInvoiceItemEntity> invoiceItems = biir.findAllByInvoiceIdAndDomainId(Long.valueOf(invoiceId), domainId);

        Integer itemsCount = 0;
        BigDecimal totalPrice = BigDecimal.ZERO; //NO VAT
        BigDecimal totalPriceVat = BigDecimal.ZERO; //WITH VAT

        for(BasketInvoiceItemEntity item : invoiceItems) {
            itemsCount += item.getItemQty();
            totalPrice = totalPrice.add( item.getItemPriceQty() );
            totalPriceVat = totalPriceVat.add( item.getItemPriceVatQty() );
        }

        //Set and save invoice
        invoice.setItemQty(itemsCount);
        invoice.setPriceToPayNoVat(totalPrice);
        invoice.setPriceToPayVat(totalPriceVat);
        bir.save(invoice);
    }

    public static BigDecimal getPriceToPay(Long invoiceId, BasketInvoiceItemsRepository biir) {
        if(invoiceId == null || invoiceId < 0) return new BigDecimal(-1);

        List<BasketInvoiceItemEntity> invoiceItems = biir.findAllByInvoiceIdAndDomainId(invoiceId, CloudToolsForCore.getDomainId());
        if(invoiceItems == null || invoiceItems.isEmpty()) return BigDecimal.ZERO;

        return invoiceItems.stream()
                           .map(item -> item.getItemPriceVatQty())
                           .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal getPayedPrice(Long invoiceId, BasketInvoicePaymentsRepository bipr) {
        if(invoiceId == null) return new BigDecimal(-1);

        List<BasketInvoicePaymentEntity> invoicePayments = bipr.findAllByInvoiceId(invoiceId);
        if(invoicePayments == null || invoicePayments.isEmpty()) return BigDecimal.ZERO;
        return invoicePayments.stream()
                              .map(item -> item.getPayedPrice())
                              .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}