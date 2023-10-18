package sk.iway.iwcm.dmail.rest;

import java.util.Date;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.dmail.DmailUtil;
import sk.iway.iwcm.dmail.jpa.UnsubscribedEntity;
import sk.iway.iwcm.dmail.jpa.UnsubscribedRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

/**
 * Odhlasene emaily z hromadneho mailingu
 */

@RestController
@RequestMapping("/admin/rest/dmail/unsubscribed")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuEmail')")
@Datatable
public class UnsubscribedRestController extends DatatableRestControllerV2<UnsubscribedEntity, Long>{

    //private final UnsubscribedRepository unsubscribedRepository;

    @Autowired
    public UnsubscribedRestController(UnsubscribedRepository unsubscribedRepository) {
        super(unsubscribedRepository);
        //this.unsubscribedRepository = unsubscribedRepository;
    }

    @Override
    public UnsubscribedEntity insertItem(UnsubscribedEntity entity) {
        Set<String> unsubscribedEmails = DmailUtil.getUnsubscribedEmails();
        if (unsubscribedEmails.contains(entity.getEmail())) return null;

        return super.insertItem(entity);
    }

    @Override
    public void beforeSave(UnsubscribedEntity entity) {
        //Set date created
        if (entity.getCreateDate()==null) entity.setCreateDate(new Date());

        //do pola je mozne zadat viacero email adries, ulozime vsetky zadane
        String[] emails = Tools.getTokens(entity.getEmail(), ", ;\n", true);
        if (emails.length>0) {
            for (int i=0; i<emails.length; i++) {
                String email = emails[i];
                if (Tools.isEmail(email)==false) {
                    throwConstraintViolation(getProp().getText("components.dmail.unsubscribe.email.error", email));
                }
                if (i==0) entity.setEmail(email);
                else {
                    //musime vytvorit novu entitu a ulozit separe, je mozne zadat viac email adries
                    UnsubscribedEntity e = new UnsubscribedEntity();
                    e.setEmail(email);
                    e.setCreateDate(new Date());
                    insertItem(e);
                }
            }
        }
    }
}
