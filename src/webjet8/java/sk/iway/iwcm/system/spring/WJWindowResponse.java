package sk.iway.iwcm.system.spring;

import java.util.ArrayList;
import java.util.List;

/**
 * WJWindowResponse.java
 *
 * Class WJWindowResponse is used for
 *
 *
 * Title        webjet8
 * Company      Interway a.s. (www.interway.sk)
 * Copyright    Interway a.s. (c) 2001-2019
 * @author      $Author: mhruby $
 * @version     $Revision: 1.0 $
 * created      19. 12. 2019 21:54
 * modified     19. 12. 2019 21:54
 */

public class WJWindowResponse {

    private List<WJShowMessage> messages;
    private boolean reloadWebPagesTree;

    public WJWindowResponse() {
        this.messages = new ArrayList<>();
    }

    public List<WJShowMessage> getMessages() {
        return messages;
    }

    public boolean isReloadWebPagesTree() {
        return reloadWebPagesTree;
    }

    public void setReloadWebPagesTree(boolean reloadWebPagesTree) {
        this.reloadWebPagesTree = reloadWebPagesTree;
    }

    public void addMessage(WJShowMessage wjShowMessage) {
        this.messages.add(wjShowMessage);
    }

    public void addMessages(List<WJShowMessage> wjShowMessages) {
        this.messages.addAll(wjShowMessages);
    }
}
