package sk.iway.iwcm.components.basket.rest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.BasketTools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.basket.delivery_methods.rest.DeliveryMethodsService;
import sk.iway.iwcm.components.basket.jpa.BasketFeeStatsProjection;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceStatsProjection;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicesRepository;
import sk.iway.iwcm.components.basket.jpa.BasketProductStatsProjection;
import sk.iway.iwcm.components.basket.jpa.InvoiceStatus;
import sk.iway.iwcm.components.basket.payment_methods.rest.PaymentMethodsService;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stat.rest.StatService;
import sk.iway.iwcm.system.datatable.json.LabelValueInteger;

/**
 * Builds domain-scoped basket dashboard statistics from invoices, products, and fee items.
 */
@Service
public class BasketStatsService {

    private static final int TOP_ITEMS_LIMIT = 10;
    private static final String UNKNOWN_KEY = "apps.eshop.stats.unknown";
    private static final String CATEGORY_ROOT_KEY = "apps.eshop.stats.categories_root";
    private static final String CATEGORY_DIRECT_KEY = "apps.eshop.stats.category_direct";
    private static final String DELIVERY_FEE_NOTE_KEY = "components.basket.invoice_email.delivery_method";
    private static final String PAYMENT_FEE_NOTE_KEY = "components.basket.invoice_email.payment_method";
    private static final String LEGACY_DELIVERY_GROUP_PATH_KEY = "basketStatsLegacyDeliveryGroupPath";
    private static final String LEGACY_PAYMENT_GROUP_PATH_KEY = "basketStatsLegacyPaymentGroupPath";

    private final BasketInvoicesRepository invoicesRepository;
    private final BasketInvoiceItemsRepository invoiceItemsRepository;
    private final DocDetailsRepository docDetailsRepository;

    public BasketStatsService(
        BasketInvoicesRepository invoicesRepository,
        BasketInvoiceItemsRepository invoiceItemsRepository,
        DocDetailsRepository docDetailsRepository
    ) {
        this.invoicesRepository = invoicesRepository;
        this.invoiceItemsRepository = invoiceItemsRepository;
        this.docDetailsRepository = docDetailsRepository;
    }

    /**
     * Loads and aggregates basket statistics for a reporting interval.
     *
     * Invalid or unsupported currency values fall back to the configured basket currency.
     * An empty status selection includes every invoice status.
     *
     * @param dayDate  encoded date range accepted by the statistics module
     * @param currency  requested reporting currency
     * @param statusIds  optional invoice statuses to include
     * @param request  current request used to resolve localized labels
     * @return aggregated basket statistics for the current domain
     */
    public BasketStatsDTO getStats(
        String dayDate,
        String currency,
        List<Integer> statusIds,
        HttpServletRequest request
    ) {
        Date[] dateRange = getDateRange(dayDate);
        int domainId = CloudToolsForCore.getDomainId();
        Prop prop = Prop.getInstance(request);
        List<Integer> selectedStatusIds = statusIds == null
            ? Collections.emptyList()
            : statusIds.stream().distinct().toList();
        boolean filterByStatus = selectedStatusIds.isEmpty() == false;
        List<Integer> queryStatusIds = filterByStatus
            ? selectedStatusIds
            : Collections.singletonList(-1);
        String supportedCurrency = BasketTools.getNormalizedSupportedCurrency(currency);
        String targetCurrency = supportedCurrency == null
            ? BasketTools.getSystemCurrency()
            : supportedCurrency;
        GroupsDB groupsDB = GroupsDB.getInstance();
        Set<Integer> legacyDeliveryItemIds = getLegacyFeeDocumentIds(
            groupsDB,
            Constants.getString(LEGACY_DELIVERY_GROUP_PATH_KEY)
        );
        Set<Integer> legacyPaymentItemIds = getLegacyFeeDocumentIds(
            groupsDB,
            Constants.getString(LEGACY_PAYMENT_GROUP_PATH_KEY)
        );
        Set<Integer> legacyFeeItemIds = new HashSet<>(legacyDeliveryItemIds);
        legacyFeeItemIds.addAll(legacyPaymentItemIds);

        List<BasketInvoiceStatsProjection> invoices = invoicesRepository.findAllForStatistics(
            domainId,
            dateRange[0],
            dateRange[1],
            filterByStatus,
            queryStatusIds
        );

        List<BasketProductStatsProjection> products = invoiceItemsRepository.findProductsForStatistics(
            domainId,
            dateRange[0],
            dateRange[1],
            filterByStatus,
            queryStatusIds,
            InvoiceStatus.INVOICE_STATUS_CANCELLED.getValue()
        );

        List<BasketFeeStatsProjection> fees = invoiceItemsRepository.findFeesForStatistics(
            domainId,
            dateRange[0],
            dateRange[1],
            filterByStatus,
            queryStatusIds,
            InvoiceStatus.INVOICE_STATUS_CANCELLED.getValue(),
            toQueryItemIds(legacyFeeItemIds)
        );

        if (invoices == null) invoices = Collections.emptyList();
        if (products == null) products = Collections.emptyList();
        if (fees == null) fees = Collections.emptyList();

        return createStats(
            invoices,
            products,
            fees,
            targetCurrency,
            request,
            prop,
            legacyDeliveryItemIds,
            legacyPaymentItemIds
        );
    }

    /**
     * Resolves document identifiers used by a legacy delivery or payment group.
     *
     * @param groupsDB  group cache used to resolve the configured path
     * @param groupPath  full path of the legacy fee group
     * @return document identifiers in the group, or an empty set when the group is unavailable
     */
    Set<Integer> getLegacyFeeDocumentIds(GroupsDB groupsDB, String groupPath) {
        if (Tools.isEmpty(groupPath)) return Collections.emptySet();

        GroupDetails group = groupsDB.getGroupByPath(groupPath);
        if (group == null) return Collections.emptySet();

        return docDetailsRepository.findAllByGroupId(group.getGroupId()).stream()
            .map(DocDetails::getDocId)
            .filter(docId -> docId > 0)
            .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Converts item identifiers into a deterministic, non-empty query parameter.
     *
     * @param itemIds  item identifiers to include
     * @return sorted identifiers, or a non-matching sentinel when the set is empty
     */
    static List<Integer> toQueryItemIds(Set<Integer> itemIds) {
        if (itemIds.isEmpty()) return Collections.singletonList(-1);
        return itemIds.stream().sorted().toList();
    }

    /**
     * Parses the requested reporting interval and falls back to the default range when invalid.
     *
     * @param dayDate  encoded reporting interval
     * @return inclusive start and end dates
     */
    private Date[] getDateRange(String dayDate) {
        boolean defaultRange = Tools.isEmpty(dayDate);
        String rangeValue = defaultRange ? getDefaultDateRangeString() : dayDate;
        Date[] dateRange;
        try {
            dateRange = StatService.processDateRangeString(rangeValue);
        } catch (RuntimeException ex) {
            defaultRange = true;
            rangeValue = getDefaultDateRangeString();
            dateRange = StatService.processDateRangeString(rangeValue);
        }

        String normalizedRange = rangeValue.replaceFirst("^daterange:", "");
        if (!defaultRange && normalizedRange.contains("-") && dateRange[1] != null) {
            dateRange[1] = getEndOfDay(dateRange[1]);
        }
        return dateRange;
    }

    private String getDefaultDateRangeString() {
        Calendar calendar = Calendar.getInstance();
        long dateTo = calendar.getTimeInMillis();
        calendar.add(Calendar.DAY_OF_MONTH, -30);
        return "daterange:" + calendar.getTimeInMillis() + "-" + dateTo;
    }

    /**
     * Aggregates loaded invoice, product, and fee records into dashboard statistics.
     *
     * @param invoices  invoice values in the reporting interval
     * @param products  aggregated sold-product quantities
     * @param fees  modern and legacy fee items
     * @param currency  target currency for monetary totals
     * @param request  current request used to resolve payment labels
     * @param prop  localization provider for the current request
     * @param legacyDeliveryItemIds  legacy delivery-fee document identifiers
     * @param legacyPaymentItemIds  legacy payment-fee document identifiers
     * @return fully aggregated basket statistics
     */
    BasketStatsDTO createStats(
        List<BasketInvoiceStatsProjection> invoices,
        List<BasketProductStatsProjection> products,
        List<BasketFeeStatsProjection> fees,
        String currency,
        HttpServletRequest request,
        Prop prop,
        Set<Integer> legacyDeliveryItemIds,
        Set<Integer> legacyPaymentItemIds
    ) {
        BasketStatsDTO stats = new BasketStatsDTO();
        stats.setCurrency(currency);
        stats.setInvoiceCount(invoices.size());

        Map<Date, TimelineAccumulator> timeline = new TreeMap<>();
        Map<String, Long> deliveryMethods = new HashMap<>();
        Map<String, Long> paymentMethods = new HashMap<>();
        Map<String, Long> statuses = new HashMap<>();

        BigDecimal revenue = BigDecimal.ZERO;
        long revenueInvoiceCount = 0;

        for (BasketInvoiceStatsProjection invoice : invoices) {
            mergeCount(deliveryMethods, getDeliveryMethodLabel(invoice.getDeliveryMethod(), prop));
            mergeCount(paymentMethods, getPaymentMethodLabel(invoice.getPaymentMethod(), request, prop));
            mergeCount(statuses, getStatusLabel(invoice.getStatusId(), prop));

            if (isCancelled(invoice.getStatusId())) continue;

            BigDecimal invoicePriceWithVat = convertPrice(
                invoice.getPriceToPayVat(),
                invoice.getCurrency(),
                currency
            );
            BigDecimal invoicePriceWithoutVat = convertPrice(
                invoice.getPriceToPayNoVat(),
                invoice.getCurrency(),
                currency
            );
            revenue = revenue.add(invoicePriceWithVat);
            revenueInvoiceCount++;

            if (invoice.getCreateDate() != null) {
                Date day = getStartOfDay(invoice.getCreateDate());
                TimelineAccumulator accumulator = timeline.computeIfAbsent(day, ignored -> new TimelineAccumulator());
                accumulator.revenueWithVat = accumulator.revenueWithVat.add(invoicePriceWithVat);
                accumulator.revenueWithoutVat = accumulator.revenueWithoutVat.add(invoicePriceWithoutVat);
            }
        }

        stats.setRevenue(scalePrice(revenue));
        stats.setAverageInvoiceValue(
            revenueInvoiceCount > 0
                ? revenue.divide(BigDecimal.valueOf(revenueInvoiceCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2)
        );
        stats.setSalesTimeline(toTimeline(timeline));
        stats.setDeliveryMethods(toTopList(deliveryMethods, Integer.MAX_VALUE));
        stats.setPaymentMethods(toTopList(paymentMethods, Integer.MAX_VALUE));
        stats.setInvoiceStatuses(toTopList(statuses, Integer.MAX_VALUE));

        fillFeeStats(
            stats,
            fees,
            revenue,
            currency,
            prop,
            legacyDeliveryItemIds,
            legacyPaymentItemIds
        );

        Set<Integer> legacyFeeItemIds = new HashSet<>(legacyDeliveryItemIds);
        legacyFeeItemIds.addAll(legacyPaymentItemIds);
        fillProductStats(stats, products, prop, legacyFeeItemIds);
        stats.setAverageItemsPerInvoice(
            revenueInvoiceCount > 0
                ? BigDecimal.valueOf(stats.getSoldItemCount())
                    .divide(BigDecimal.valueOf(revenueInvoiceCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2)
        );
        return stats;
    }

    /**
     * Calculates delivery, payment, and net-revenue totals from fee items.
     *
     * @param stats  statistics object to update
     * @param fees  fee items to classify and aggregate
     * @param revenue  gross revenue before fee subtraction
     * @param currency  target currency for fee totals
     * @param defaultProp  fallback localization provider
     * @param legacyDeliveryItemIds  legacy delivery-fee document identifiers
     * @param legacyPaymentItemIds  legacy payment-fee document identifiers
     */
    private void fillFeeStats(
        BasketStatsDTO stats,
        List<BasketFeeStatsProjection> fees,
        BigDecimal revenue,
        String currency,
        Prop defaultProp,
        Set<Integer> legacyDeliveryItemIds,
        Set<Integer> legacyPaymentItemIds
    ) {
        BigDecimal deliveryFees = BigDecimal.ZERO;
        BigDecimal paymentFees = BigDecimal.ZERO;

        for (BasketFeeStatsProjection fee : fees) {
            BigDecimal feePrice = getFeePriceWithVat(fee, currency);
            Integer itemId = fee.getItemId();

            if (legacyDeliveryItemIds.contains(itemId)) {
                deliveryFees = deliveryFees.add(feePrice);
            } else if (legacyPaymentItemIds.contains(itemId)) {
                paymentFees = paymentFees.add(feePrice);
            } else if (itemId != null && itemId == 0) {
                Prop invoiceProp = Tools.isEmpty(fee.getUserLng())
                    ? defaultProp
                    : Prop.getInstance(fee.getUserLng());

                if (isFeeType(fee.getItemNote(), invoiceProp, DELIVERY_FEE_NOTE_KEY)) {
                    deliveryFees = deliveryFees.add(feePrice);
                } else if (isFeeType(fee.getItemNote(), invoiceProp, PAYMENT_FEE_NOTE_KEY)) {
                    paymentFees = paymentFees.add(feePrice);
                }
            }
        }

        stats.setDeliveryFees(scalePrice(deliveryFees));
        stats.setPaymentFees(scalePrice(paymentFees));
        stats.setNetRevenue(scalePrice(revenue.subtract(deliveryFees).subtract(paymentFees)));
    }

    private BigDecimal getFeePriceWithVat(BasketFeeStatsProjection fee, String currency) {
        if (fee.getItemPrice() == null || fee.getItemQty() == null) return BigDecimal.ZERO;

        int vat = fee.getItemVat() == null ? 0 : fee.getItemVat();
        BigDecimal vatMultiplier = BigDecimal.ONE.add(BigDecimal.valueOf(vat).movePointLeft(2));
        BigDecimal feePrice = fee.getItemPrice()
            .multiply(BigDecimal.valueOf(fee.getItemQty()))
            .multiply(vatMultiplier);
        return convertPrice(feePrice, fee.getCurrency(), currency);
    }

    private boolean isFeeType(String itemNote, Prop prop, String noteKey) {
        return Tools.isNotEmpty(itemNote) && itemNote.equalsIgnoreCase(prop.getText(noteKey));
    }

    /**
     * Aggregates sold products and builds product and category rankings.
     *
     * @param stats  statistics object to update
     * @param products  aggregated product quantities
     * @param prop  localization provider for fallback labels
     * @param legacyFeeItemIds  fee document identifiers excluded from product totals
     */
    private void fillProductStats(
        BasketStatsDTO stats,
        List<BasketProductStatsProjection> products,
        Prop prop,
        Set<Integer> legacyFeeItemIds
    ) {
        List<Long> docIds = products.stream()
            .map(BasketProductStatsProjection::getItemId)
            .filter(itemId -> itemId != null && itemId > 0 && legacyFeeItemIds.contains(itemId) == false)
            .map(Long::valueOf)
            .distinct()
            .toList();

        Map<Long, DocDetails> docsById = docDetailsRepository.findAllById(docIds).stream()
            .collect(Collectors.toMap(DocDetails::getId, Function.identity()));

        Map<String, Long> productCounts = new HashMap<>();
        Map<String, Long> categoryCounts = new HashMap<>();
        long soldItemCount = 0;
        GroupsDB groupsDB = GroupsDB.getInstance();
        List<LabelValueInteger> productGroups = ProductListService.getListOfProductsGroups(docDetailsRepository);
        Map<Integer, String> productCategoryPaths = getProductCategoryPaths(productGroups, groupsDB);

        for (BasketProductStatsProjection product : products) {
            long quantity = product.getQuantity() == null ? 0 : product.getQuantity();
            if (quantity < 1) continue;
            if (legacyFeeItemIds.contains(product.getItemId())) continue;

            DocDetails doc = product.getItemId() == null ? null : docsById.get(product.getItemId().longValue());

            soldItemCount += quantity;

            String productName = doc == null ? product.getItemTitle() : doc.getTitle();
            if (Tools.isEmpty(productName)) {
                productName = prop.getText(UNKNOWN_KEY);
                if (product.getItemId() != null) productName += " #" + product.getItemId();
            }
            productCounts.merge(productName, quantity, Long::sum);

            String categoryPath = getCategoryPath(doc, productCategoryPaths, prop);
            categoryCounts.merge(categoryPath, quantity, Long::sum);
        }

        stats.setSoldItemCount(soldItemCount);
        stats.setTopProducts(toTopList(productCounts, TOP_ITEMS_LIMIT));
        productCategoryPaths.values().forEach(categoryPath -> categoryCounts.putIfAbsent(categoryPath, 0L));
        stats.setCategoryTree(createCategoryTree(categoryCounts, prop));
    }

    /**
     * Builds relative category paths for all configured product groups.
     *
     * @param productGroups  groups configured to contain products
     * @param groupsDB  group cache used to traverse parent groups
     * @return category paths keyed by product group identifier
     */
    private Map<Integer, String> getProductCategoryPaths(
        List<LabelValueInteger> productGroups,
        GroupsDB groupsDB
    ) {
        Set<Integer> productGroupIds = productGroups.stream()
            .map(LabelValueInteger::getValue)
            .collect(Collectors.toCollection(HashSet::new));
        Map<Integer, String> categoryPaths = new HashMap<>();

        for (Integer groupId : productGroupIds) {
            GroupDetails group = groupsDB.getGroup(groupId);
            List<String> pathSegments = new ArrayList<>();

            while (group != null && productGroupIds.contains(group.getGroupId())) {
                if (Tools.isNotEmpty(group.getGroupName())) pathSegments.add(group.getGroupName());
                group = groupsDB.getGroup(group.getParentGroupId());
            }

            if (pathSegments.isEmpty()) continue;
            Collections.reverse(pathSegments);
            categoryPaths.put(groupId, String.join("/", pathSegments));
        }
        return categoryPaths;
    }

    private String getCategoryPath(DocDetails doc, Map<Integer, String> productCategoryPaths, Prop prop) {
        if (doc == null) return prop.getText(UNKNOWN_KEY);

        String categoryName = productCategoryPaths.get(doc.getGroupId());
        return Tools.isEmpty(categoryName) ? prop.getText(UNKNOWN_KEY) : categoryName;
    }

    private String getDeliveryMethodLabel(String deliveryMethod, Prop prop) {
        if (Tools.isEmpty(deliveryMethod)) return prop.getText(UNKNOWN_KEY);

        String label = DeliveryMethodsService.getDeliveryMethodLabel(deliveryMethod, null, prop);
        return Tools.isEmpty(label) ? deliveryMethod : label;
    }

    private String getPaymentMethodLabel(String paymentMethod, HttpServletRequest request, Prop prop) {
        if (Tools.isEmpty(paymentMethod)) return prop.getText(UNKNOWN_KEY);

        String label = PaymentMethodsService.getPaymentMethodLabel(paymentMethod, request);
        if (Tools.isEmpty(label)) label = prop.getText(paymentMethod);
        return Tools.isEmpty(label) ? paymentMethod : label;
    }

    private String getStatusLabel(Integer statusId, Prop prop) {
        if (statusId == null) return prop.getText(UNKNOWN_KEY);

        Map<String, String> customStatuses = Constants.getHashtable("basketInvoiceBonusStatuses");
        String customStatusKey = customStatuses.get(String.valueOf(statusId));
        if (Tools.isNotEmpty(customStatusKey)) return prop.getText(customStatusKey);

        return prop.getText(InvoiceStatus.STATUS_KEY_PREFIX + statusId);
    }

    private boolean isCancelled(Integer statusId) {
        return statusId != null && statusId == InvoiceStatus.INVOICE_STATUS_CANCELLED.getValue();
    }

    private BigDecimal convertPrice(BigDecimal price, String fromCurrency, String toCurrency) {
        if (price == null) return BigDecimal.ZERO;

        String sourceCurrency = Tools.isEmpty(fromCurrency) ? toCurrency : fromCurrency;
        return BasketTools.convertCurrency(price, sourceCurrency, toCurrency);
    }

    private BigDecimal scalePrice(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private Date getStartOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date getEndOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    /**
     * Converts accumulated daily revenue into ordered timeline points.
     *
     * @param timeline  revenue totals keyed by calendar day
     * @return ordered daily timeline points with scaled monetary values
     */
    private List<BasketStatsDTO.SalesTimelinePoint> toTimeline(Map<Date, TimelineAccumulator> timeline) {
        List<BasketStatsDTO.SalesTimelinePoint> result = new ArrayList<>();
        timeline.forEach((day, value) -> result.add(
            new BasketStatsDTO.SalesTimelinePoint(
                day,
                scalePrice(value.revenueWithVat),
                scalePrice(value.revenueWithoutVat)
            )
        ));
        return result;
    }

    private void mergeCount(Map<String, Long> values, String name) {
        values.merge(name, 1L, Long::sum);
    }

    /**
     * Sorts named counts by descending value and then by name.
     *
     * @param values  aggregated counts keyed by display name
     * @param limit  maximum number of entries to return
     * @return sorted count entries
     */
    private List<BasketStatsDTO.NameCount> toTopList(Map<String, Long> values, int limit) {
        return values.entrySet().stream()
            .sorted(
                Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                    .thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
            )
            .limit(limit)
            .map(entry -> new BasketStatsDTO.NameCount(entry.getKey(), entry.getValue()))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Creates the hierarchical category statistics from slash-delimited category paths.
     *
     * @param categoryCounts  sold quantities keyed by category path
     * @param prop  localization provider for synthetic node names
     * @return root of the category statistics tree
     */
    private BasketStatsDTO.CategoryNode createCategoryTree(Map<String, Long> categoryCounts, Prop prop) {
        CategoryAccumulator root = new CategoryAccumulator(prop.getText(CATEGORY_ROOT_KEY));
        categoryCounts.forEach((categoryPath, count) -> addCategoryPath(root, categoryPath, count));
        return toCategoryNode(root, prop.getText(CATEGORY_DIRECT_KEY));
    }

    /**
     * Adds a category path and its direct item count to an accumulator tree.
     *
     * @param root  root accumulator to update
     * @param categoryPath  slash-delimited category path
     * @param count  items assigned directly to the category
     */
    private void addCategoryPath(CategoryAccumulator root, String categoryPath, long count) {
        String[] pathSegments = Tools.getTokens(categoryPath, "/");
        if (pathSegments.length == 0) return;

        CategoryAccumulator node = root;
        for (String pathSegment : pathSegments) {
            node = node.children.computeIfAbsent(pathSegment, CategoryAccumulator::new);
        }
        node.directCount += count;
    }

    /**
     * Converts an accumulator subtree into the category DTO representation.
     *
     * @param category  accumulator subtree to convert
     * @param directCategoryName  label for products assigned directly to a parent category
     * @return converted category node
     */
    private BasketStatsDTO.CategoryNode toCategoryNode(CategoryAccumulator category, String directCategoryName) {
        List<BasketStatsDTO.CategoryNode> children = category.children.values().stream()
            .map(child -> toCategoryNode(child, directCategoryName))
            .collect(Collectors.toCollection(ArrayList::new));

        if (children.isEmpty() == false && category.directCount > 0) {
            children.add(
                new BasketStatsDTO.CategoryNode(directCategoryName, category.directCount, new ArrayList<>())
            );
        }

        children.sort(
            Comparator.comparingLong(this::getCategoryNodeTotal)
                .reversed()
                .thenComparing(BasketStatsDTO.CategoryNode::getName, String.CASE_INSENSITIVE_ORDER)
        );

        Long value = children.isEmpty() ? category.directCount : null;
        return new BasketStatsDTO.CategoryNode(category.name, value, children);
    }

    /**
     * Calculates the total item count represented by a category subtree.
     *
     * @param category  category subtree to total
     * @return sum of the node value or all descendant values
     */
    private long getCategoryNodeTotal(BasketStatsDTO.CategoryNode category) {
        if (category.getValue() != null) return category.getValue();
        return category.getChildren().stream().mapToLong(this::getCategoryNodeTotal).sum();
    }

    /**
     * Accumulates VAT-inclusive and VAT-exclusive revenue for one timeline day.
     */
    private static class TimelineAccumulator {

        private BigDecimal revenueWithVat = BigDecimal.ZERO;
        private BigDecimal revenueWithoutVat = BigDecimal.ZERO;
    }

    /**
     * Accumulates direct item counts and child categories while building the category tree.
     */
    private static class CategoryAccumulator {

        private final String name;
        private final Map<String, CategoryAccumulator> children = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private long directCount;

        private CategoryAccumulator(String name) {
            this.name = name;
        }
    }
}
