package sk.iway.iwcm.components.proxy.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.components.proxy.ProxyDB;
import sk.iway.iwcm.components.proxy.jpa.ProxyBean;
import sk.iway.iwcm.components.proxy.jpa.ProxyRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/proxy")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_proxy')")
@Datatable
public class ProxyRestController extends DatatableRestControllerV2<ProxyBean, Long> {

    @Autowired
    public ProxyRestController(ProxyRepository proxyRepository) {
        super(proxyRepository);
    }

    @Override
    public ProxyBean getOneItem(long id) {

        if(id != -1) return super.getOneItem(id);

        ProxyBean entity = new ProxyBean();
        entity.setProxyId(-1);
        entity.setRemotePort(80);
        entity.setEncoding("utf-8");
        entity.setProxyMethod("ProxyByHttpClient");
        entity.setIncludeExt(".htm,.html,.php,.asp,.aspx,.jsp,.do,.action");

        return entity;
    }

    @Override
    public void afterSave(ProxyBean entity, ProxyBean saved) {
        ProxyDB.getInstance(true);
    }

}