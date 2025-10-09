package sk.iway.iwcm.components.basket.rest;

import java.util.List;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.basket.rest.ProductListService.AddingStatus;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrDefRepository;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.editor.rest.WebpagesDatatable;
import sk.iway.iwcm.editor.rest.GetAllItemsDocOptions;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.system.datatable.json.LabelValueInteger;
import sk.iway.iwcm.users.UserDetails;

@Datatable
@RestController
@RequestMapping(value = "/admin/rest/eshop/product-list")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_basket')")
public class ProductListRestController extends WebpagesDatatable {

    private static final String NOTIFY_TITLE_KEY = "apps.product-list.add_group.title";

    @Autowired
    public ProductListRestController(DocDetailsRepository docDetailsRepository, EditorFacade editorFacade, DocAtrDefRepository docAtrDefRepository) {
        super(docDetailsRepository, editorFacade, docAtrDefRepository);
    }

    @Override
    public Page<DocDetails> getAllItems(Pageable pageable) {

        GetAllItemsDocOptions options = new GetAllItemsDocOptions(getRequest());
        options.setPageable(pageable);
        options.setDocAtrDefRepository(super.docAtrDefRepository);
        options.setDocDetailsRepository(super.docDetailsRepository);

        int groupId = Tools.getIntValue(getRequest().getParameter("groupId"), -1);
        options.setGroupId(groupId);

        return ProductListService.getAllItems(options);
    }

    @Override
    public boolean processAction(DocDetails entity, String action) {
        if("addProductGroup".equals(action)) {
            String customData = getRequest().getParameter("customData");
            AddingStatus status = ProductListService.addProductGroup(getUser(), customData, editorFacade);

            if(status == AddingStatus.SUCCESS)
                addNotify( new NotifyBean(getProp().getText(NOTIFY_TITLE_KEY), getProp().getText("apps.product-list.add_new_group.success"), NotifyBean.NotifyType.SUCCESS, 5000) );
            else if(status == AddingStatus.ALREADY_EXIST)
                addNotify( new NotifyBean(getProp().getText(NOTIFY_TITLE_KEY), getProp().getText("apps.product-list.add_new_group.already_added"), NotifyBean.NotifyType.ERROR, 60000) );
            else
                addNotify( new NotifyBean(getProp().getText(NOTIFY_TITLE_KEY), getProp().getText("apps.product-list.add_new_group.failed"), NotifyBean.NotifyType.ERROR, 60000) );

            return true;
        }
        return false;
    }

    @Override
    public DocDetails editItem(DocDetails entity, long id) {

        DocDetails original = DocDB.getInstance().getDoc(entity.getDocId(), -1, false);

        if(super.isImporting()) {
            original = getOne(id);

            //Only fields, that can be edited during import
            original.setFieldJ( entity.getFieldJ() );
            original.setFieldK( entity.getFieldK() );
            original.setFieldL( entity.getFieldL() );
            original.setFieldM( entity.getFieldM() );
            original.setFieldN( entity.getFieldN() );
            original.setFieldO( entity.getFieldO() );

            //swap
            entity = original;
        }

        DocDetails saved = editorFacade.save(entity);

        List<UserDetails> approveByUsers = editorFacade.getApprovers();
        super.addInsertEditNotify(false, approveByUsers);

        if (editorFacade.isForceReload()) setForceReload(true);

        if (super.isRefreshMenuRequired(original, saved)) setForceReload(true);

        addNotify(editorFacade.getNotify());

        if (RequestBean.getAttribute("forceReloadTree")!=null) setForceReload(true);

        return saved;
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<DocDetails> root, CriteriaBuilder builder) {
        //This order is important
        int rootGroupId = Tools.getIntValue(params.get("groupId"), -1);
        List<Integer> groupIds = ProductListService.getGroupTreeIds( rootGroupId, super.docDetailsRepository );
        params.remove("groupId");

        super.addSpecSearch(params, predicates, root, builder);

        predicates.add(root.get("groupId").in(groupIds));
        predicates.add(root.get("fieldK").isNotNull());

        predicates.add(builder.greaterThan(builder.length(root.get("fieldK")), 0));
    }

    @RequestMapping(value="/product-groups")
    public List<LabelValueInteger> getListOfProductsGroups() {
        return ProductListService.getListOfProductsGroups(super.docDetailsRepository);
    }

    @RequestMapping(value="/supported-currencies")
    public List<LabelValue> getListOfSupportedCurrencies() {
        return ProductListService.getListOfSupportedCurrencies();
    }
}