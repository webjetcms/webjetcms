package sk.iway.iwcm.admin;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.layout.RefreshBean;
import sk.iway.iwcm.system.msg.AdminMessageBean;
import sk.iway.iwcm.system.msg.MessageDB;

/**
 * #53119
 * Zabezpecuje drzanie session v admin casti, tiez vrati zoznam neprecitanych sprav
 */
@RestController
@PreAuthorize("@WebjetSecurityService.isAdmin()")
public class RefresherRestController {

    @PostMapping("/admin/rest/refresher")
    public RefreshBean refresh(final HttpServletRequest request) {
       RefreshBean r = new RefreshBean();
       r.setTimestamp(Tools.getNow());

       if (InitServlet.isTypeCloud()==false)
       {
          List<AdminMessageBean> newMessages = MessageDB.getInstance(false).getUnreadedMessages(request.getSession());
          if (newMessages!=null && newMessages.size()>0)
          {
             List<Integer> ids = new ArrayList<>();
             for (AdminMessageBean m : newMessages) {
                ids.add(m.getAdminMessageId());
             }
             r.setMessages(ids);
          }
       }
       return r;
    }

}
