package sk.iway.iwcm.system.spring;

/**
 * IwcmTextMessage.java
 *
 * Class IwcmTextMessage is used for
 *
 *
 * Title        webjet8
 * Company      Interway a.s. (www.interway.sk)
 * Copyright    Interway a.s. (c) 2001-2019
 * @author      $Author: mhruby $
 * @version     $Revision: 1.0 $
 * created      19. 12. 2019 21:43
 * modified     13. 12. 2019 18:19
 */

public class WJShowMessage {

    private String type = WJShowMessageEnum.INFO.getText();
    private String textKey;
    private String param1;
    private String param2;
    private String param3;

    public WJShowMessage() {
    }

    public WJShowMessage(String textKey) {
        this.textKey = textKey;
    }

    public String getTextKey() {
        return textKey;
    }

    public WJShowMessage setTextKey(String textKey) {
        this.textKey = textKey;
        return this;
    }

    public String getParam1() {
        return param1;
    }

    public WJShowMessage setParam1(String param1) {
        this.param1 = param1;
        return this;
    }

    public String getParam2() {
        return param2;
    }

    public WJShowMessage setParam2(String param2) {
        this.param2 = param2;
        return this;
    }

    public String getParam3() {
        return param3;
    }

    public WJShowMessage setParam3(String param3) {
        this.param3 = param3;
        return this;
    }

    public String getType() {
        return type;
    }

    public WJShowMessage setType(String type) {
        this.type = type;
        return this;
    }

    public WJShowMessage setType(WJShowMessageEnum WJShowMessageEnum) {
        this.type = WJShowMessageEnum.getText();
        return this;
    }
}
