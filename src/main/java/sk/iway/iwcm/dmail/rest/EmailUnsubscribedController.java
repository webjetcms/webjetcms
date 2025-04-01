package sk.iway.iwcm.dmail.rest;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.dmail.Sender;
import sk.iway.iwcm.dmail.jpa.UnsubscribedEntity;
import sk.iway.iwcm.dmail.jpa.UnsubscribedRepository;

@RestController
public class EmailUnsubscribedController {

    private UnsubscribedRepository unsubscribedRepository;

    @Autowired
    public EmailUnsubscribedController(UnsubscribedRepository unsubscribedRepository) {
        this.unsubscribedRepository = unsubscribedRepository;
    }

    /**
     * Unsubscribe email from mailing list. Used as one click unsubscribe action (link) in email header List-Unsubscribe.
     * @param request
     */
    @RequestMapping(path={"/rest/dmail/unsubscribe"})
	public String unsubscribeEmail(HttpServletRequest request)
	{
        String dmailStatParamValue = request.getParameter(Constants.getString("dmailStatParam"));
        int emailId = Sender.getEmailIdFromClickHash(dmailStatParamValue);

        if(emailId < 1 || Tools.isEmpty(dmailStatParamValue)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

        String email = (new SimpleQuery()).forString("SELECT recipient_email FROM emails WHERE email_id = ?", emailId);
        if(Tools.isEmpty(email)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

        UnsubscribedEntity newUnsubscribedEntity = new UnsubscribedEntity();
        newUnsubscribedEntity.setEmail(email);
        newUnsubscribedEntity.setCreateDate(new Date());
        newUnsubscribedEntity.setDomainId(CloudToolsForCore.getDomainId());
        unsubscribedRepository.save(newUnsubscribedEntity);

        return "OK";
	}
}
