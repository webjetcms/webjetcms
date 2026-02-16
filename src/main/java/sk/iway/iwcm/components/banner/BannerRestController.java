package sk.iway.iwcm.components.banner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.banner.model.BannerBean;
import sk.iway.iwcm.components.banner.model.BannerWebDocBean;
import sk.iway.iwcm.components.banner.model.BannerWebGroupBean;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

@RestController
@RequestMapping("/admin/rest/banner")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuBanner')")
@Datatable
public class BannerRestController extends DatatableRestControllerV2<BannerBean, Long>{

    private final BannerRepository bannerRepository;

    @Autowired
    public BannerRestController(BannerRepository bannerRepository) {
        super(bannerRepository);
        this.bannerRepository = bannerRepository;
    }

    @Override
    public Page<BannerBean> getAllItems(Pageable pageable) {
        return super.getAllItemsIncludeSpecSearch(new BannerBean(), null);
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<BannerBean> root, CriteriaBuilder builder) {
        super.addSpecSearch(params, predicates, root, builder);
        if (getUser().isDisabledItem("cmp_banner_seeall")) {
            predicates.add(builder.equal(root.get("clientId"), getUser().getUserId()));
        }
    }

    @Override
    public BannerBean getOne(@PathVariable("id") long id) {

        BannerBean entity;
        if(id == -1) {
            entity = new BannerBean();
            entity.setMaxViews(0);
            entity.setMaxClicks(0);
            entity.setPriority(10);
            entity.setActive(true);
        } else {
            entity = bannerRepository.findFirstByIdAndDomainId(id, CloudToolsForCore.getDomainId()).orElse(null);

            if (getUser().isDisabledItem("cmp_banner_seeall") && (entity.getClientId()==null || entity.getClientId().intValue()!=getUser().getUserId())) {
                //na entitu nema pravo
                entity = null;
            }
        }

        return processFromEntity(entity, ProcessItemAction.GETONE);
    }

    @Override
    public BannerBean processFromEntity(BannerBean entity, ProcessItemAction action) {

        BannerEditorFields bef = new BannerEditorFields();
        Date now = new Date(Tools.getNow());
        if (Boolean.FALSE.equals(entity.getActive()) ||
            (entity.getDateFrom()!=null && now.before(entity.getDateFrom())) ||
            (entity.getDateTo()!=null && now.after(entity.getDateTo())) ||
            (entity.getMaxViews()!=null && entity.getStatViews()!=null && entity.getMaxViews().intValue()>0 && entity.getStatViews().intValue()>entity.getMaxViews().intValue()) || //NOSONAR
            (entity.getMaxClicks()!=null && entity.getStatClicks()!=null && entity.getMaxClicks().intValue()>0 && entity.getStatClicks().intValue()>entity.getMaxClicks().intValue())   //NOSONAR
           ) {
            bef.setViewable(false);
            bef.addRowClass("is-disabled");
        } else {
            bef.setViewable(true);
        }

        bef.setFieldsDefinition(bef.getFields(entity, "components.banner", 'F'));

        entity.setEditorFields(bef);
        return entity;
    }

    /**
     * Vrati zoznam uz existujucich skupin
     * @param term
     * @return
     */
    @GetMapping("/autocomplete")
    public List<String> getAutocomplete(@RequestParam String term) {

        List<String> ac = new ArrayList<>();

        //Get all where group name is like %term%, and distict because its autocomplete list and we dont want duplicity
        List<BannerBean> groupNamesPage =  bannerRepository.findDistinctAllByBannerGroupLikeAndDomainId("%" + term + "%", CloudToolsForCore.getDomainId());

        //Loop gained entities and add group name to autcomplete list "ac"
        for(BannerBean entity : groupNamesPage) {
            if (ac.contains(entity.getBannerGroup())==false) ac.add(entity.getBannerGroup());
        }

        return ac;
    }

    @Override
    public void beforeDuplicate(BannerBean entity) {
        if (entity.getDocIds()!=null) {
            for(BannerWebDocBean docBean : entity.getDocIds()) {
                docBean.setId(null);
            }
        }
        if (entity.getGroupIds() != null) {
            for(BannerWebGroupBean groupBean : entity.getGroupIds()) {
                groupBean.setId(null);
            }
        }
        super.beforeDuplicate(entity);
    }

    @Override
    public void afterDelete(BannerBean entity, long id) {
        deleteCache();
    }

    @Override
    public void afterSave(BannerBean entity, BannerBean saved) {
        deleteCache();
    }

    /**
     * Zmaze celu BannerDB cache + cached banner restrictions
     */
    private void deleteCache() {
        Cache c = Cache.getInstance();
        c.removeObjectStartsWithName("BannerDB.");
    }

    /**
     * vrati vyskladanu URL pre zobrazenie Kampanoveho bannera
     * @param bannerLocation - umiestnenie bannera
     * @param campaignTitle - hodnota parametra z konf. premennej bannerCampaignParamName
     */
    @GetMapping("/generate-url-for-campaign-title")
    public String getUrlForCamapaignTitle(HttpServletRequest request, @RequestParam String bannerLocation, @RequestParam String campaignTitle)
    {
        StringBuilder result=new StringBuilder();
        if(Tools.isNotEmpty(campaignTitle))
        {
            result.append(Tools.getDomainBaseHref(request));
            result.append(bannerLocation);
            result.append("?").append(Constants.getString("bannerCampaignParamName"));
            result.append("=").append(campaignTitle);
        }

        return result.toString();
    }

    @Override
    public void getOptions(DatatablePageImpl<BannerBean> page) {
        List<UserDetails> admins;

        if (getUser().isEnabledItem("cmp_banner_seeall")) {
            admins = UsersDB.getAdmins();
            UserDetails nepriradeny = new UserDetails();
            nepriradeny.setUserId(-1);
            nepriradeny.setFirstName(getProp().getText("components.banner.nepriradeny"));
            admins.add(0, nepriradeny);
        } else {
            admins = new ArrayList<>();
            admins.add(getUser());
        }
        page.addOptions("clientId", admins, "fullName", "userId", false);
    }

    @Override
    public void beforeSave(BannerBean entity) {
        if (entity.getWidth()==null) entity.setWidth(0);
        if (entity.getHeight()==null) entity.setHeight(0);
        if (entity.getStatViews()==null) entity.setStatViews(0);
        if (entity.getStatClicks()==null) entity.setStatClicks(0);
        if (entity.getFrameRate()==null) entity.setFrameRate(0);
        if (entity.getClientId()==null) entity.setClientId(-1);
    }

}
