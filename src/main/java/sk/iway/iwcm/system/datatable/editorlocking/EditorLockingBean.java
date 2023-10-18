package sk.iway.iwcm.system.datatable.editorlocking;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EditorLockingBean {

    private int userId;
    private int entityId;
    private Date lastChange;

}
