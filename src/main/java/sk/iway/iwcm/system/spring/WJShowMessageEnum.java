package sk.iway.iwcm.system.spring;

/**
 * IwcmTextMessageEnum.java
 *
 * Class IwcmTextMessageEnum is used for
 *
 *
 * Title        webjet8
 * Company      Interway a.s. (www.interway.sk)
 * Copyright    Interway a.s. (c) 2001-2019
 * @author      $Author: mhruby $
 * @version     $Revision: 1.0 $
 * created      19. 12. 2019 21:43
 * modified     17. 12. 2019 12:20
 */

public enum WJShowMessageEnum {
    SUCCESS("Success"),
    ERROR("Error"),
    INFO("Info");

    private String text;

    WJShowMessageEnum(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
