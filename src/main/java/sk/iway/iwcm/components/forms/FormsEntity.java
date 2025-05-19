package sk.iway.iwcm.components.forms;

import sk.iway.iwcm.system.adminlog.EntityListenersType;

import jakarta.persistence.*;

@Entity
@Table(name = "forms")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_FORMMAIL)
public class FormsEntity extends FormsEntityBasic {


}
