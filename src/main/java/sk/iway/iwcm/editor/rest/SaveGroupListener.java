package sk.iway.iwcm.editor.rest;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.components.structuremirroring.SaveListener;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.editor.service.WebpagesService;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.system.spring.events.WebjetEventType;
import sk.iway.iwcm.users.UsersDB;

@Component
public class SaveGroupListener {

  @Autowired
  private HttpServletRequest request;

  private static final String REQUEST_KEY = "SaveGroupListener.regenerateUrl";

  @EventListener(condition = "#event.clazz eq 'sk.iway.iwcm.doc.GroupDetails'")
  public void changeDirection(final WebjetEvent<GroupDetails> event) {

    Identity user = UsersDB.getCurrentUser(request);
    GroupDetails groupToSave = event.getSource();

    if (event.getEventType().equals(WebjetEventType.ON_START) && groupToSave.getGroupId()>0) {

      //sme ON_START, ulozena v DB je este stara verzia
      int parentGroupId = (new SimpleQuery()).forInt("SELECT parent_group_id FROM groups WHERE group_id=?", groupToSave.getGroupId());
      String urlDirName = (new SimpleQuery()).forString("SELECT url_dir_name FROM groups WHERE group_id=?", groupToSave.getGroupId());
      if (urlDirName == null) urlDirName = "";
      // Over či bola zmenená poloha group v stromovej štrukture alebo bola zmenena
      // virtualPath hodnota
      if (parentGroupId != groupToSave.getParentGroupId()
          || urlDirName.equals(groupToSave.getUrlDirName()) == false) {
        //musime to preniest takto
        request.setAttribute(REQUEST_KEY, Boolean.TRUE);
      }
    }

    if (event.getEventType().equals(WebjetEventType.AFTER_SAVE) && request.getAttribute(REQUEST_KEY) != null) {
      try {
        request.removeAttribute(REQUEST_KEY);
        WebpagesService.regenerateUrl(groupToSave.getGroupId(), user, request, true);
      } catch (Exception ex) {
        Logger.error(SaveListener.class, ex);
      }
    }
  }
}
