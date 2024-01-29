package sk.iway.iwcm.editor.service;

import java.util.ArrayList;
import java.util.List;

import sk.iway.iwcm.system.datatable.NotifyBean;

/**
 * base class for services with notify
 */
public class NotifyService {

    //list upozorneni
    private List<NotifyBean> notify;

    /**
	 * Vrati zoznam moznych notifikacii pre pouzivatela
	 * @return
	 */
	public List<NotifyBean> getNotify() {
		return notify;
	}

	/**
	 * Prida notifikaciu
	 * @param notifyBean
	 */
	public void addNotify(NotifyBean notifyBean) {
        if(this.notify == null) this.notify = new ArrayList<>();
        this.notify.add(notifyBean);
    }
}
