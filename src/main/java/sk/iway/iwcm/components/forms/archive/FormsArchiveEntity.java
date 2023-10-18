package sk.iway.iwcm.components.forms.archive;

import sk.iway.iwcm.components.forms.FormsEntityBasic;
import sk.iway.iwcm.system.adminlog.EntityListenersType;

import javax.persistence.*;

@Entity
@Table(name = "forms_archive")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_FORMMAIL)
public class FormsArchiveEntity extends FormsEntityBasic {

}
