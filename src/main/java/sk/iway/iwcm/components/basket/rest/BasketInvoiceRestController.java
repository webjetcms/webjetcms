package sk.iway.iwcm.components.basket.rest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceEditorFields;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicesRepository;
import sk.iway.iwcm.components.basket.jpa.InvoiceStatus;
import sk.iway.iwcm.components.basket.payment_methods.rest.PaymentMethodsService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

@RestController
@RequestMapping("/admin/rest/eshop/basket")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_basket')")
@Datatable
public class BasketInvoiceRestController extends DatatableRestControllerV2<BasketInvoiceEntity, Long> {

    private final BasketInvoicesRepository bir;
    private final BasketInvoiceItemsRepository biir;
    private final BasketInvoicePaymentsRepository bipr;

    private static final String ORDER_PLACEHOLDER = "{ORDER_DETAILS}";
    private static final String STATUS_PLACEHOLDER = "{STATUS}";

    @Autowired
    public BasketInvoiceRestController(BasketInvoicesRepository bir, BasketInvoiceItemsRepository biir, BasketInvoicePaymentsRepository bipr) {
        super(bir);
        this.bir = bir;
        this.biir = biir;
        this.bipr = bipr;
    }

    @Override
    public Page<BasketInvoiceEntity> getAllItems(Pageable pageable) {
        DatatablePageImpl<BasketInvoiceEntity> page = new DatatablePageImpl<>(super.getAllItemsIncludeSpecSearch(new BasketInvoiceEntity(), pageable));

        page.addOptions("paymentMethod", PaymentMethodsService.getConfiguredPaymentMethodsLabels(getProp()), "label", "value", false);

        fillStatusSelect(page);
        prepareCountriesSelect(page);
        processFromEntity(page, ProcessItemAction.GETALL);
        return page;
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<BasketInvoiceEntity> root, CriteriaBuilder builder) {

        predicates.add(builder.equal(root.get("domainId"), CloudToolsForCore.getDomainId()));

        String firstName = params.get("searchEditorFields.firstName");
        if(Tools.isNotEmpty(firstName)) {
            params.remove("searchEditorFields.firstName");
            firstName = normalizeValueForSearch(firstName);
            predicates.add(builder.or(
                builder.like(root.get("contactFirstName"), firstName),
                builder.like(root.get("deliveryName"), firstName),
                builder.like(builder.concat(builder.concat(root.get("contactFirstName"), " ("), builder.concat(root.get("deliveryName"), ")")), firstName)
            ));
        }

        String lastName = params.get("searchEditorFields.lastName");
        if(Tools.isNotEmpty(lastName)) {
            params.remove("searchEditorFields.lastName");
            lastName = normalizeValueForSearch(lastName);
            predicates.add(builder.or(
                builder.like(root.get("contactLastName"), "%" + lastName + "%"),
                builder.like(root.get("deliverySurName"), "%" + lastName + "%"),
                builder.like(builder.concat(builder.concat(root.get("contactLastName"), " ("), builder.concat(root.get("deliverySurName"), ")")), "%" + lastName + "%")
            ));
        }

        super.addSpecSearch(params, predicates, root, builder);
    }

    /* replaced by @DataTableColumn.orderProperty annotation, this is here just for reference
    @Override
    public Pageable addSpecSort(Map<String, String> params, Pageable pageable) {

        Sort modifiedSort = pageable.getSort();

        String[] sortList = Tools.getTokens(params.get("sort"), "\n", true);
        for (String sort : sortList ) {
            String[] data = Tools.getTokens(sort, ",", true);
            if (data.length!=2) continue;
            String field = data[0];
            Direction direction;
            if ("asc".equals(data[1])) {
                direction = Direction.ASC;
            } else if ("desc".equals(data[1])) {
                direction = Direction.DESC;
            } else {
                continue;
            }
            if ("editorFields.firstName".equals(field)) {
                modifiedSort = modifiedSort.and(Sort.by(direction, "contactFirstName", "deliveryName"));
            } else if ("editorFields.lastName".equals(field)) {
                modifiedSort = modifiedSort.and(Sort.by(direction, "contactLastName", "deliverySurName"));
            }
        }

        // create new Pageable object with modified sort
        Pageable modifiedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), modifiedSort);
        return super.addSpecSort(params, modifiedPageable);
    }*/

    @Override
    public void afterSave(BasketInvoiceEntity entity, BasketInvoiceEntity saved) {
        //Update invoice stats
        ProductListService.updateInvoiceStats(entity.getId(), false);

        BasketInvoiceEditorFields bied = entity.getEditorFields();
        if(bied != null && Boolean.TRUE.equals(bied.getSendNotification())) {
            StringBuilder sb = new StringBuilder();
            //Styles
            sb.append(bied.getOrderRecapHead());
            //Email body AND replace "{ORDER_DETAILS}" with actual order details
            sb.append( bied.getBody().replace(ORDER_PLACEHOLDER, bied.getOrderRecapBody()) );
            //
            Integer actualStatus = bir.getStatusId(saved.getId(), CloudToolsForCore.getDomainId());
            sb.replace(sb.indexOf(STATUS_PLACEHOLDER), sb.indexOf(STATUS_PLACEHOLDER) + STATUS_PLACEHOLDER.length(), actualStatus.toString());

            boolean emailSendOK = SendMail.send(getUser().getFullName(), getUser().getEmail(), entity.getContactEmail(), bied.getSubject(), sb.toString(), getRequest());
            if(!emailSendOK) throwError("components.basket.errorSendingEmail");
        }
    }

    @Override
    public BasketInvoiceEntity processFromEntity(BasketInvoiceEntity entity, ProcessItemAction action) {
        if(entity.getEditorFields() == null) {
           BasketInvoiceEditorFields bief = new BasketInvoiceEditorFields();
           bief.fromBasketInvoice(entity, getRequest());

           //Set fields definition
           bief.setFieldsDefinition(bief.getFields(entity, "components.invoice", 'F'));
        }
        return entity;
    }

    @Override
    public boolean deleteItem(BasketInvoiceEntity entity, long id) {
        //DELETE action is allowed only if basketInvoiced is set as CANCELLED
        if(InvoiceStatus.fromValue(entity.getStatusId()) == InvoiceStatus.INVOICE_STATUS_CANCELLED ) {
            return super.deleteItem(entity, id);
        } else {
            //Reject delete
            throwError("components.basket.invoice.delete_err");
            return false;
        }
    }

    @Override
    public BasketInvoiceEntity insertItem(BasketInvoiceEntity entity) {
        throwError(getProp().getText("config.not_permitted_action_err"));
        return null;
    }

    @Override
    public void beforeDuplicate(BasketInvoiceEntity entity) {
        throwError(getProp().getText("config.not_permitted_action_err"));
    }

    private final void fillStatusSelect(DatatablePageImpl<BasketInvoiceEntity> page) {
        String label = "statusId";
        Prop prop = getProp();

        //Add default statuses
        String defaultKeyPrefix = "components.basket.invoice.status.";
        page.addDefaultOption(label, prop.getText( defaultKeyPrefix + "1" ), "1");
        page.addDefaultOption(label, prop.getText( defaultKeyPrefix + "2" ), "2");
        page.addDefaultOption(label, prop.getText( defaultKeyPrefix + "3" ), "3");
        page.addDefaultOption(label, prop.getText( defaultKeyPrefix + "4" ), "4");
        page.addDefaultOption(label, prop.getText( defaultKeyPrefix + "5" ), "5");
        page.addDefaultOption(label, prop.getText( defaultKeyPrefix + "8" ), "8");

        //Add custom statuses
        Map<String, String> bonusStatuses = Constants.getHashtable("basketInvoiceBonusStatuses");
        for (Map.Entry<String, String> entry : bonusStatuses.entrySet()) {
            //It must be number 10 or higher, lower numbers are reserved for default statuses
            if(Integer.valueOf(entry.getKey()) >= 10)
                page.addDefaultOption(label, prop.getText( entry.getValue() ), entry.getKey());
        }
    }

    private final void prepareCountriesSelect(DatatablePageImpl<BasketInvoiceEntity> page) {
        String[] supprotedCountries = Constants.getArray("basketInvoiceSupportedCountries");
        String defaultKeyPrefix = "stat.countries.tld";

        Prop prop = getProp();

        for (String countryCode : supprotedCountries) {
            page.addDefaultOption("contactCountry", prop.getText( defaultKeyPrefix + countryCode ), countryCode);
            page.addDefaultOption("deliveryCountry", prop.getText( defaultKeyPrefix + countryCode ), countryCode);
        }
    }

    @GetMapping("/getPriceToPay")
    public BigDecimal getPriceToPay(@RequestParam("invoiceId") Long invoiceId) {
        return ProductListService.getPriceToPay(invoiceId, biir);
    }

    @GetMapping("/getPayedPrice")
    public BigDecimal getPayedPrice(@RequestParam("invoiceId") Long invoiceId) {
        return ProductListService.getPayedPrice(invoiceId, bipr);
    }

    private String normalizeValueForSearch(String value) {
        if (value.startsWith("^") && value.endsWith("$")) {
            value = value.substring(1);
            return value.substring(0, value.length() - 1);
        }

        if (value.startsWith("^")) return value.substring(1) + "%";
        else if (value.endsWith("$")) return "%" + value.substring(0, value.length() - 1);
        return "%" + value + "%";
    }
}